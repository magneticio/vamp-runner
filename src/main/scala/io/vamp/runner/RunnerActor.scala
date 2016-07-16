package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, Props }
import io.vamp.runner.Hub.Broadcast
import io.vamp.runner.Recipe.State

import scala.concurrent.duration._

object RunnerActor {

  def props: Props = Props(classOf[RunnerActor])

  object ProvideRecipes

  object StopExecutions

  case class StartExecutions(ids: List[String])

  case class PurgeExecutions(ids: List[String])

  case class Recipes(recipes: List[Recipe]) extends Response

  private object MockExecutionResult

}

class RunnerActor extends Actor with ActorLogging {

  import RunnerActor._

  private val recipes = scala.collection.mutable.LinkedHashMap[String, Recipe](List(
    Recipe("http-deployment", "HTTP Deployment", State.Idle),
    Recipe("http-canary", "HTTP Canary", State.Idle),
    Recipe("http-dependencies", "HTTP with Dependencies", State.Idle),
    Recipe("http-flip-flop-versions", "HTTP Flip-Flop Versions", State.Idle),
    Recipe("http-flip-flop-versions-dependencies", "HTTP Flip-Flop Versions with Dependencies", State.Idle),
    Recipe("tcp-deployment", "TCP Deployment", State.Idle),
    Recipe("tcp-dependencies", "TCP with Dependencies", State.Idle),
    Recipe("route-weights", "Route Weights", State.Idle),
    Recipe("route-weights-condition-strength", "Route Weights with Condition Strength", State.Idle),
    Recipe("scaling-in-out", "Scaling In/Out", State.Idle)
  ).map(recipe ⇒ recipe.id -> recipe): _*)

  def receive: Receive = {
    case ProvideRecipes       ⇒ sender() ! Recipes(recipes.values.toList)
    case StartExecutions(ids) ⇒ start(ids)
    case PurgeExecutions(ids) ⇒ purge(ids)
    case StopExecutions       ⇒ stop()
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

  private def stop() = {
    recipes.values.foreach { recipe ⇒
      recipes += (recipe.id -> recipe.copy(state = State.Idle))
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
