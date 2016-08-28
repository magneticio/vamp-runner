package io.vamp.runner

import java.util.concurrent.BlockingQueue

import akka.NotUsed
import akka.actor.{ Actor, ActorLogging }
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, ZipWith }
import io.vamp.runner.RunnerActor.{ UpdateDirtyFlag, UpdateState }
import io.vamp.runner.VampEventReader.{ VampEvent, VampEventMessage, VampEventRelease }
import org.json4s.JsonAST.{ JNothing, JNull }

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration._

trait RecipeRunner extends VampApiClient {
  this: Actor with ActorLogging ⇒

  implicit def materializer: ActorMaterializer

  private val actionTimeoutShort = Config.duration("vamp.runner.recipes.timeout.short")
  private val actionTimeoutLong = Config.duration("vamp.runner.recipes.timeout.long")

  protected def events: BlockingQueue[VampEventMessage]

  protected def run(recipes: List[Recipe]): Future[_] = {
    recipes.foldLeft(Future.successful(Recipe.State.succeeded))((f, recipe) ⇒ f.flatMap { _ ⇒
      recipe.run.foldLeft(Future.successful(Recipe.State.succeeded))((f, s) ⇒ f.flatMap(_ ⇒ run(recipe, s))).recover {
        case t: Throwable ⇒ Recipe.State.failed
      }.flatMap {
        case Recipe.State.`failed` ⇒ Future.successful(Recipe.State.failed)
        case _ ⇒
          recipe.cleanup.foldLeft(Future.successful(Recipe.State.succeeded))((f, s) ⇒ f.flatMap(_ ⇒ cleanup(recipe, s))).recover {
            case t: Throwable ⇒ Recipe.State.failed
          }.map { any ⇒
            recipe.run.foreach { run ⇒ self ! UpdateDirtyFlag(recipe, run, dirty = false) }
            any
          }
      }
    })
  }

  protected def cleanup(recipes: List[Recipe]): Future[_] = {
    recipes.foldLeft(Future.successful(Recipe.State.succeeded))((f, recipe) ⇒ f.flatMap { _ ⇒
      recipe.cleanup.foldLeft(Future.successful(Recipe.State.succeeded))((f, s) ⇒ f.flatMap { _ ⇒ Thread.sleep(1000); cleanup(recipe, s) }).recover {
        case t: Throwable ⇒ Recipe.State.failed
      }.map { any ⇒
        recipe.run.foreach { run ⇒
          self ! UpdateState(recipe, run, Recipe.State.idle)
          self ! UpdateDirtyFlag(recipe, run, dirty = false)
        }
        any
      }
    })
  }

  protected def run(recipe: Recipe, step: RunRecipeStep): Future[Recipe.State.Value] = {
    log.info(s"Running: [${recipe.name} :: ${step.description}]")
    val result = Sink.head[Recipe.State.Value]

    val future = RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.single(step).map { step ⇒
          self ! UpdateState(recipe, step, Recipe.State.running)
          step
        } ~> runFlow(recipe, step) ~> sink.in
        ClosedShape
    }).run().flatMap {
      case Recipe.State.`failed` ⇒ Future.failed(new RuntimeException())
      case state                 ⇒ Future.successful(state)
    }.map { result ⇒
      recipe.cleanup.foreach { cleanup ⇒ self ! UpdateState(recipe, cleanup, Recipe.State.idle) }
      result
    }

    future.onComplete { _ ⇒ self ! VampEventRelease}

    future
  }

  protected def cleanup(recipe: Recipe, step: CleanupRecipeStep): Future[Recipe.State.Value] = {
    log.info(s"Cleaning up: [${recipe.name} :: ${step.description}]")
    val result = Sink.head[Recipe.State.Value]

    val future = RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        self ! UpdateState(recipe, step, Recipe.State.running)
        Source.single(step) ~> cleanupFlow(recipe, step).map { any ⇒
          self ! UpdateState(recipe, step, Recipe.State.succeeded)
          any
        } ~> sink.in
        ClosedShape
    }).run()

    future.onComplete { _ ⇒ self ! VampEventRelease}

    future
  }

  private def runFlow(recipe: Recipe, step: RunRecipeStep): Flow[RecipeStep, Recipe.State.Value, NotUsed] = {
    flow("Run", recipe, step, { (step, state) ⇒
      step match {
        case s: RunRecipeStep ⇒
          self ! UpdateState(recipe, s, state)
          self ! UpdateDirtyFlag(recipe, s, dirty = true)
        case _ ⇒
      }
    }, silent = false)
  }

  private def cleanupFlow(recipe: Recipe, step: CleanupRecipeStep): Flow[RecipeStep, Recipe.State.Value, NotUsed] = {
    flow("Cleanup", recipe, step, { (_, _) ⇒ }, silent = true)
  }

  private def flow(designator: String, recipe: Recipe, step: RecipeStep, update: (RecipeStep, Recipe.State.Value) ⇒ Unit, silent: Boolean): Flow[RecipeStep, Recipe.State.Value, NotUsed] = {

    val graph = GraphDSL.create() { implicit builder ⇒

      val input = builder.add(Broadcast[RecipeStep](3))
      val collect = builder.add(Merge[AnyRef](3))

      val execution = Flow[RecipeStep].mapAsync(1) { step ⇒ execute(step).map { result ⇒ step -> result } }
      val awaiting = Flow[RecipeStep].mapAsync(1) { step ⇒
        events.clear()
        Future(await(step)).map { result ⇒ step -> result }
      }

      val waitUntil = step.timeout match {
        case Recipe.Timeout.`short` ⇒ actionTimeoutShort
        case Recipe.Timeout.`long`  ⇒ actionTimeoutLong
      }

      val timeout = Flow[RecipeStep].flatMapConcat { step ⇒ Source.tick(waitUntil, waitUntil, step -> Recipe.State.failed) }

      val resolve = Flow[AnyRef].collect {
        case (step: RecipeStep, state: Recipe.State.Value) ⇒

          if (!silent && state == Recipe.State.failed)
            log.error(s"$designator - ${state.toString.toLowerCase}: [${recipe.name} :: ${step.description}]")
          else
            log.info(s"$designator - ${state.toString.toLowerCase}: [${recipe.name} :: ${step.description}]")

          update(step, state)
          state
      }

      val zipper = builder.add(ZipWith[RecipeStep, Any, RecipeStep]((step, _) ⇒ step))
      val ticker = Source.tick(initialDelay = 1.second, interval = 1.second, None)

      val out = builder.add(Merge[Recipe.State.Value](1))

      input ~> awaiting ~> collect
      input ~> timeout ~> collect

      // workaround for delaying execution so awaiting events can be setup
      input ~> zipper.in0
      ticker ~> zipper.in1
      zipper.out ~> execution ~> collect ~> resolve ~> out

      FlowShape(input.in, out.out)
    }

    Flow.fromGraph(graph)
  }

  private def execute(action: RecipeStep): Future[AnyRef] = {

    def condition(): Future[Boolean] = {
      if (action.condition.nonEmpty) apiGet(action.condition).map {
        case Left(json) ⇒ json != JNothing && json != JNull
        case _          ⇒ true
      }
      else Future.successful(true)
    }

    def apiRun() = action.method match {
      case Recipe.Method.`create` ⇒ apiPut(input = action.resource, recoverWith = recover)
      case Recipe.Method.`delete` ⇒ apiDelete(input = action.resource, recoverWith = recover)
    }

    def recover: AnyRef ⇒ Recipe.State.Value = {
      failure ⇒
        log.error(failure.toString)
        Recipe.State.failed
    }

    condition().flatMap {
      case true ⇒ apiRun()
      case _    ⇒ Future.successful(Recipe.State.succeeded)
    } map {
      case Right(_) ⇒ Recipe.State.failed
      case other    ⇒ if (action.await.isEmpty) Recipe.State.succeeded else other
    }
  }

  @tailrec
  private def await(action: RecipeStep): Recipe.State.Value = events.take() match {
    case VampEventRelease   ⇒ if (action.await.isEmpty) Recipe.State.succeeded else Recipe.State.failed
    case VampEvent(tags, _) ⇒ if (action.await.forall(tag ⇒ tags.contains(tag))) Recipe.State.succeeded else await(action)
  }
}
