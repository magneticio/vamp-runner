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

  private val actionTimeout = Config.duration("vamp.runner.recipes.timeout")

  protected def events: BlockingQueue[VampEventMessage]

  protected def run(recipes: List[Recipe]): Future[_] = {
    val result = Sink.last[AnyRef]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.fromIterator[Recipe](() ⇒ recipes.iterator).mapAsync(1) { recipe ⇒
          Future.sequence {
            recipe.steps.map { step ⇒ run(recipe, step) }
          } flatMap { _ ⇒
            Future.sequence {
              recipe.steps.reverse.map { step ⇒ cleanup(recipe, step) }
            }
          }
        } ~> sink.in
        ClosedShape
    }).run()
  }

  protected def run(recipe: Recipe, step: RecipeStep): Future[RecipeStep] = {
    log.info(s"Running: [${recipe.name} :: ${step.description}]")
    val result = Sink.head[RecipeStep]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.single(step).map { step ⇒
          self ! UpdateState(recipe, step, Recipe.State.Running)
          step
        } ~> runFlow(recipe) ~> sink.in
        ClosedShape
    }).run()
  }

  protected def cleanup(recipes: List[Recipe]): Future[_] = {
    val result = Sink.last[AnyRef]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.fromIterator[Recipe](() ⇒ recipes.iterator).mapAsync(1) { recipe ⇒
          Future.sequence {
            recipe.steps.reverse.map { step ⇒ cleanup(recipe, step) }
          }
        } ~> sink.in
        ClosedShape
    }).run()
  }

  protected def cleanup(recipe: Recipe, step: RecipeStep): Future[RecipeStep] = {
    log.info(s"Cleaning up: [${recipe.name} :: ${step.description}]")
    val result = Sink.head[RecipeStep]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.single(step) ~> cleanupFlow(recipe) ~> sink.in
        ClosedShape
    }).run()
  }

  private def runFlow(recipe: Recipe): Flow[RecipeStep, RecipeStep, NotUsed] = {
    flow("Run", recipe, { step ⇒ step.run }, { (step, state) ⇒
      self ! UpdateState(recipe, step, state)
      self ! UpdateDirtyFlag(recipe, step, dirty = true)
    }, silent = false)
  }

  private def cleanupFlow(recipe: Recipe): Flow[RecipeStep, RecipeStep, NotUsed] = {
    flow("Cleanup", recipe, { step ⇒ step.cleanup }, { (step, state) ⇒
      self ! UpdateDirtyFlag(recipe, step, dirty = false)
    }, silent = true)
  }

  private def flow(designator: String, recipe: Recipe, action: RecipeStep ⇒ RecipeStepAction, update: (RecipeStep, Recipe.State.Value) ⇒ Unit, silent: Boolean): Flow[RecipeStep, RecipeStep, NotUsed] = {

    val graph = GraphDSL.create() { implicit builder ⇒

      val in = builder.add(Broadcast[RecipeStep](3))
      val collect = builder.add(Merge[AnyRef](3))

      val execution = Flow[RecipeStep].mapAsync(1) { step ⇒ execute(action(step)).map { result ⇒ step -> result } }
      val awaiting = Flow[RecipeStep].mapAsync(1) { step ⇒ Future(await(action(step))).map { result ⇒ step -> result } }
      val timeout = Flow[RecipeStep].flatMapConcat { step ⇒ Source.tick(actionTimeout, actionTimeout, step -> Recipe.State.Failed) }

      val resolve = Flow[AnyRef].collect {
        case (step: RecipeStep, state: Recipe.State.Value) ⇒

          if (!silent && state == Recipe.State.Failed)
            log.error(s"$designator - ${state.toString.toLowerCase}: [${recipe.name} :: ${step.description}]")
          else
            log.info(s"$designator - ${state.toString.toLowerCase}: [${recipe.name} :: ${step.description}]")

          update(step, state)
          step
      }

      val out = builder.add(Merge[RecipeStep](1))

      in ~> execution ~> collect ~> resolve ~> out
      in ~> awaiting ~> collect
      in ~> timeout ~> collect

      FlowShape(in.in, out.out)
    }

    Flow.fromGraph(graph)
  }

  private def execute(action: RecipeStepAction): Future[AnyRef] = {

    def recover: AnyRef ⇒ Recipe.State.Value = {
      failure ⇒
        log.error(failure.toString)
        Recipe.State.Failed
    }

    (action.method match {
      case Recipe.Method.PUT    ⇒ apiPut(input = action.resource, recoverWith = recover)
      case Recipe.Method.POST   ⇒ apiPost(input = action.resource, recoverWith = recover)
      case Recipe.Method.DELETE ⇒ apiDelete(input = action.resource, recoverWith = recover)
    }) map {
      case Right(_) ⇒ Recipe.State.Failed
      case other    ⇒ other
    }
  }

  @tailrec
  private def await(action: RecipeStepAction): Recipe.State.Value = {
    events.take() match {
      case VampEventRelease ⇒ Recipe.State.Failed
      case VampEvent(tags)  ⇒ if (action.await.forall(tag ⇒ tags.contains(tag))) Recipe.State.Succeeded else await(action)
    }
  }
}
