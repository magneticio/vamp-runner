package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, Props }
import io.vamp.runner.Hub.Broadcast
import io.vamp.runner.Recipe.State

import scala.concurrent.duration._

object RunnerActor {

  def props: Props = Props(classOf[RunnerActor])

  object ProvideRecipes

  object AbortExecutions

  case class StartExecutions(ids: List[String])

  case class PurgeExecutions(ids: List[String])

  case class Recipes(recipes: List[Recipe]) extends Response

  private object MockExecutionResult

}

class RunnerActor extends Actor with ActorLogging with RecipeLoader {

  import RunnerActor._

  private val recipes = scala.collection.mutable.LinkedHashMap[String, Recipe](load.map(recipe ⇒ recipe.id -> recipe): _*)

  def receive: Receive = {
    case ProvideRecipes       ⇒ sender() ! Recipes(recipes.values.toList)
    case StartExecutions(ids) ⇒ start(ids)
    case PurgeExecutions(ids) ⇒ purge(ids)
    case AbortExecutions      ⇒ abort()
    case MockExecutionResult  ⇒ mock()
    case _                    ⇒
  }

  private def start(ids: List[String]) = {
    ids.flatMap(id ⇒ recipes.get(id)).foreach { recipe ⇒
      recipes += (recipe.id -> recipe.copy(state = State.Running))
    }

    context.system.scheduler.scheduleOnce(5.seconds, self, MockExecutionResult)(context.dispatcher)

    context.parent ! Broadcast(Recipes(recipes.values.toList))
  }

  private def purge(ids: List[String]) = {}

  private def abort() = {
    recipes.values.foreach { recipe ⇒
      if (recipe.state == State.Running) recipes += (recipe.id -> recipe.copy(state = State.Aborted))
    }

    context.parent ! Broadcast(Recipes(recipes.values.toList))
  }

  private def mock() = {
    recipes.values.foreach { recipe ⇒
      if (recipe.state == State.Running) recipes += (recipe.id -> recipe.copy(state = if (Math.random() > 0.5) State.Success else State.Failure))
    }

    context.parent ! Broadcast(Recipes(recipes.values.toList))
  }
}
