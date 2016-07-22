package io.vamp.runner

import akka.NotUsed
import akka.actor.ActorLogging
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Flow, GraphDSL, RunnableGraph, Sink, Source }

import scala.concurrent.Future

trait RecipeRunner extends VampApiClient {
  this: ActorLogging ⇒

  implicit def materializer: ActorMaterializer

  private val recipeActionTimeout = Config.duration("vamp.runner.recipes.timeout")

  protected def update(recipe: Recipe, step: RecipeStep): RecipeStep

  protected def run(recipes: List[Recipe]) = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder ⇒
      Source.fromIterator[Recipe](() ⇒ recipes.iterator).flatMapConcat { recipe ⇒
        Source.fromIterator[RecipeStep](() ⇒ recipe.steps.iterator).map { step ⇒
          step.copy(state = Recipe.State.Running)
        }.map(update(recipe, _)).via {
          flow(recipe, { step ⇒ execute(step.run) })
        }
      } ~> Sink.last[AnyRef]
      ClosedShape
    }).run()
  }

  protected def run(recipe: Recipe, step: RecipeStep) = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder ⇒
      Source.single(step).map { step ⇒
        step.copy(state = Recipe.State.Running)
      }.map(update(recipe, _)) ~>
        flow(recipe, { step ⇒ execute(step.run) }) ~>
        Sink.head[RecipeStep]
      ClosedShape
    }).run()
  }

  protected def cleanup(recipes: List[Recipe]) = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder ⇒
      Source.fromIterator[Recipe](() ⇒ recipes.iterator).flatMapConcat { recipe ⇒
        Source.fromIterator[RecipeStep](() ⇒ recipe.steps.reverseIterator).via { flow(recipe, { step ⇒ execute(step.cleanup, step.state) }) }
      } ~> Sink.head[AnyRef]
      ClosedShape
    }).run()
  }

  private def flow(recipe: Recipe, action: RecipeStep ⇒ Future[Recipe.State.Value]): Flow[RecipeStep, RecipeStep, NotUsed] = {
    Flow[RecipeStep].mapAsync(1) { step ⇒
      action(step).map {
        state ⇒
          log.info(s"${state.toString}: [${recipe.name} :: ${step.description}]")
          update(recipe, step.copy(state = state))
      }
    }.completionTimeout(recipeActionTimeout)
  }

  private def execute(action: RecipeStepAction, success: Recipe.State.Value = Recipe.State.Succeeded): Future[Recipe.State.Value] = {

    def recover: AnyRef ⇒ Recipe.State.Value = {
      failure ⇒
        log.error(failure.toString)
        Recipe.State.Failed
    }

    def result: AnyRef ⇒ Recipe.State.Value = {
      case Right(_) ⇒ Recipe.State.Failed
      case _        ⇒ success
    }

    action.method match {
      case Recipe.Method.PUT    ⇒ apiPut(input = action.resource, recoverWith = recover).map(result)
      case Recipe.Method.POST   ⇒ apiPost(input = action.resource, recoverWith = recover).map(result)
      case Recipe.Method.DELETE ⇒ apiDelete(input = action.resource, recoverWith = recover).map(result)
    }
  }
}
