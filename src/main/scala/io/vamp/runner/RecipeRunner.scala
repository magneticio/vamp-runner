package io.vamp.runner

import akka.NotUsed
import akka.actor.{ Actor, ActorLogging }
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Flow, GraphDSL, RunnableGraph, Sink, Source }
import io.vamp.runner.RunnerActor.{ UpdateDirtyFlag, UpdateState }

import scala.concurrent.Future

trait RecipeRunner extends VampApiClient {
  this: Actor with ActorLogging ⇒

  implicit def materializer: ActorMaterializer

  private val recipeActionTimeout = Config.duration("vamp.runner.recipes.timeout")

  protected def run(recipes: List[Recipe]): Future[_] = {
    val result = Sink.last[AnyRef]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.fromIterator[Recipe](() ⇒ recipes.iterator).mapAsync(1) { recipe ⇒
          Future.sequence {
            recipe.steps.map { step ⇒
              run(recipe, step)
            }
          } flatMap { _ ⇒
            Future.sequence {
              recipe.steps.reverse.map { step ⇒
                cleanup(recipe, step)
              }
            }
          }
        } ~> sink.in
        ClosedShape
    }).run()
  }

  protected def run(recipe: Recipe, step: RecipeStep): Future[RecipeStep] = {
    val result = Sink.last[RecipeStep]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.single(step).map { step ⇒
          self ! UpdateState(recipe, step, Recipe.State.Running)
          step
        } ~>
          runFlow(recipe) ~>
          sink.in
        ClosedShape
    }).run()
  }

  protected def cleanup(recipes: List[Recipe]): Future[_] = {
    val result = Sink.last[AnyRef]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.fromIterator[Recipe](() ⇒ recipes.iterator).mapAsync(1) { recipe ⇒
          Future.sequence {
            recipe.steps.reverse.map { step ⇒
              cleanup(recipe, step)
            }
          }
        } ~> sink.in
        ClosedShape
    }).run()
  }

  protected def cleanup(recipe: Recipe, step: RecipeStep): Future[RecipeStep] = {
    val result = Sink.last[RecipeStep]
    RunnableGraph.fromGraph(GraphDSL.create(result) { implicit builder ⇒
      sink ⇒
        Source.single(step) ~>
          cleanupFlow(recipe) ~>
          sink.in
        ClosedShape
    }).run()
  }

  private def runFlow(recipe: Recipe): Flow[RecipeStep, RecipeStep, NotUsed] = {
    flow("Run", recipe, { step ⇒ execute(step.cleanup) }, { (step, state) ⇒
      self ! UpdateState(recipe, step, state)
      self ! UpdateDirtyFlag(recipe, step, dirty = true)
    })
  }

  private def cleanupFlow(recipe: Recipe): Flow[RecipeStep, RecipeStep, NotUsed] = {
    flow("Cleanup", recipe, { step ⇒ execute(step.cleanup) }, { (step, state) ⇒
      self ! UpdateDirtyFlag(recipe, step, dirty = false)
    })
  }

  private def flow(designator: String, recipe: Recipe, action: RecipeStep ⇒ Future[Recipe.State.Value], update: (RecipeStep, Recipe.State.Value) ⇒ Unit): Flow[RecipeStep, RecipeStep, NotUsed] = {
    Flow[RecipeStep].mapAsync(1) { step ⇒
      action(step).map {
        state ⇒
          log.info(s"$designator - ${state.toString.toLowerCase}: [${recipe.name} :: ${step.description}]")
          update(step, state)
          step
      }
    }.completionTimeout(recipeActionTimeout)
  }

  private def execute(action: RecipeStepAction): Future[Recipe.State.Value] = {

    def recover: AnyRef ⇒ Recipe.State.Value = {
      failure ⇒
        log.error(failure.toString)
        Recipe.State.Failed
    }

    def result: AnyRef ⇒ Recipe.State.Value = {
      case Right(_) ⇒ Recipe.State.Failed
      case _        ⇒ Recipe.State.Succeeded
    }

    action.method match {
      case Recipe.Method.PUT    ⇒ apiPut(input = action.resource, recoverWith = recover).map(result)
      case Recipe.Method.POST   ⇒ apiPost(input = action.resource, recoverWith = recover).map(result)
      case Recipe.Method.DELETE ⇒ apiDelete(input = action.resource, recoverWith = recover).map(result)
    }
  }
}
