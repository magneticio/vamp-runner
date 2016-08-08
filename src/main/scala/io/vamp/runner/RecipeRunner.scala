package io.vamp.runner

import java.util.concurrent.BlockingQueue

import akka.NotUsed
import akka.actor.{ Actor, ActorLogging }
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source }
import io.vamp.runner.RunnerActor.{ UpdateDirtyFlag, UpdateState }
import io.vamp.runner.VampEventReader.{ VampEvent, VampEventMessage, VampEventRelease }

import scala.annotation.tailrec
import scala.concurrent.Future

trait RecipeRunner extends VampApiClient {
  this: Actor with ActorLogging ⇒

  implicit def materializer: ActorMaterializer

  private val actionTimeoutShort = Config.duration("vamp.runner.recipes.timeout.short")
  private val actionTimeoutLong = Config.duration("vamp.runner.recipes.timeout.long")

  protected def events: BlockingQueue[VampEventMessage]

  protected def run(recipes: List[Recipe]): Future[_] = {

    val futures = recipes.map { recipe ⇒
      recipe.run.foldLeft(Future.successful(Recipe.State.succeeded))((f, s) ⇒ f.flatMap(_ ⇒ run(recipe, s))).recover {
        case t: Throwable ⇒ Recipe.State.failed
      } flatMap {
        case Recipe.State.`failed` ⇒ Future.successful(Recipe.State.failed)
        case _ ⇒
          recipe.cleanup.foldLeft(Future.successful(Recipe.State.succeeded))((f, s) ⇒ f.flatMap(_ ⇒ cleanup(recipe, s))).recover {
            case t: Throwable ⇒ Recipe.State.failed
          } map { _ ⇒
            recipe.run.foreach { run ⇒ self ! UpdateDirtyFlag(recipe, run, dirty = false) }
          }
      }
    }
    Future.sequence(futures)
  }

  protected def cleanup(recipes: List[Recipe]): Future[_] = {
    val futures = recipes.map { recipe ⇒
      recipe.cleanup.foldLeft(Future.successful(Recipe.State.succeeded))((f, s) ⇒ f.flatMap(_ ⇒ cleanup(recipe, s))).recover {
        case t: Throwable ⇒ Recipe.State.failed
      } map { _ ⇒
        recipe.run.foreach { run ⇒ self ! UpdateDirtyFlag(recipe, run, dirty = false) }
      }
    }
    Future.sequence(futures)
  }

  protected def run(recipe: Recipe, step: RunRecipeStep): Future[Recipe.State.Value] = {
    log.info(s"Running: [${recipe.name} :: ${step.description}]")
    val result = Sink.head[Recipe.State.Value]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.single(step).map { step ⇒
          self ! UpdateState(recipe, step, Recipe.State.running)
          step
        } ~> runFlow(recipe, step) ~> sink.in
        ClosedShape
    }).run().flatMap {
      case Recipe.State.`failed` ⇒ Future.failed(new RuntimeException())
      case state               ⇒ Future.successful(state)
    }
  }

  protected def cleanup(recipe: Recipe, step: CleanupRecipeStep): Future[Recipe.State.Value] = {
    log.info(s"Cleaning up: [${recipe.name} :: ${step.description}]")
    val result = Sink.head[Recipe.State.Value]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.single(step) ~> cleanupFlow(recipe, step) ~> sink.in
        ClosedShape
    }).run()
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

      val in = builder.add(Broadcast[RecipeStep](3))
      val collect = builder.add(Merge[AnyRef](3))

      val execution = Flow[RecipeStep].mapAsync(1) { step ⇒ execute(step).map { result ⇒ step -> result } }
      val awaiting = Flow[RecipeStep].mapAsync(1) { step ⇒ Future(await(step)).map { result ⇒ step -> result } }

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

      val out = builder.add(Merge[Recipe.State.Value](1))

      in ~> execution ~> collect ~> resolve ~> out
      in ~> awaiting ~> collect
      in ~> timeout ~> collect

      FlowShape(in.in, out.out)
    }

    Flow.fromGraph(graph)
  }

  private def execute(action: RecipeStep): Future[AnyRef] = {

    def recover: AnyRef ⇒ Recipe.State.Value = {
      failure ⇒
        log.error(failure.toString)
        Recipe.State.failed
    }

    (action match {
      case _: RunRecipeStep     ⇒ apiPut(input = action.resource, recoverWith = recover)
      case _: CleanupRecipeStep ⇒ apiDelete(input = action.resource, recoverWith = recover)
    }) map {
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
