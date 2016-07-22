package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.agent.Agent
import akka.stream.ActorMaterializer
import io.vamp.runner.Hub.Broadcast

import scala.collection.mutable

object RunnerActor {

  def props(implicit materializer: ActorMaterializer): Props = Props(classOf[RunnerActor], materializer)

  object ProvideRecipes

  case class Run(arguments: AnyRef)

  case class Cleanup(arguments: AnyRef)

  case class Recipes(recipes: List[Recipe]) extends Response

  case class UpdateState(recipe: Recipe, step: RecipeStep, state: Recipe.State.Value)

  case class UpdateDirtyFlag(recipe: Recipe, step: RecipeStep, dirty: Boolean)
}

class RunnerActor(implicit val materializer: ActorMaterializer) extends Actor with ActorLogging with RecipeLoader with RecipeRunner {

  import RunnerActor._

  val timeout = Config.duration("vamp.runner.timeout")

  implicit def system: ActorSystem = context.system

  private val recipes = mutable.LinkedHashMap(load.map(recipe ⇒ recipe.id -> recipe): _*)

  private val running = Agent[Boolean](false)

  def receive: Receive = {
    case ProvideRecipes                       ⇒ sender() ! Recipes(recipes.values.toList)
    case Run(arguments)                       ⇒ if (!running.get()) run()(arguments)
    case Cleanup(arguments)                   ⇒ if (!running.get()) cleanup()(arguments)
    case UpdateState(recipe, step, state)     ⇒ update(recipe, step, state)
    case UpdateDirtyFlag(recipe, step, dirty) ⇒ update(recipe, step, dirty)
    case _                                    ⇒
  }

  private def run(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒
      running.send(true)
      run(ids2recipes(list).map { recipe ⇒
        recipes += (recipe.id -> recipe.copy(steps = recipe.steps.map { step ⇒ step.copy(state = Recipe.State.Idle) }))
        recipe
      }).onComplete(_ ⇒ running.send(false))

    case map: Map[_, _] ⇒ for {
      recipe ← map.asInstanceOf[Map[String, _]].get("recipe").flatMap(id ⇒ recipes.get(id.toString))
      step ← map.asInstanceOf[Map[String, _]].get("step").flatMap(id ⇒ recipe.steps.find(_.id == id))
    } yield {
      running.send(true)
      run(recipe, step).onComplete(_ ⇒ running.send(false))
    }
  }

  private def cleanup(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒
      running.send(true)
      cleanup(ids2recipes(list)).onComplete(_ ⇒ running.send(false))
  }

  private def update(recipe: Recipe, step: RecipeStep, state: Recipe.State.Value): Unit = {
    update(recipe, step, { step ⇒ step.copy(state = state) })
  }

  private def update(recipe: Recipe, step: RecipeStep, dirty: Boolean): Unit = {
    update(recipe, step, { step ⇒ step.copy(dirty = dirty) })
  }

  private def update(recipe: Recipe, step: RecipeStep, update: RecipeStep ⇒ RecipeStep): Unit = {
    recipes.get(recipe.id).map { r ⇒
      recipes += (r.id -> r.copy(steps = r.steps.map { s ⇒
        if (s.id == step.id) update(s) else s
      }))
    }
    context.parent ! Broadcast(Recipes(RunnerActor.this.recipes.values.toList))
  }

  private def ids2recipes(ids: List[_]): List[Recipe] = ids.flatMap(id ⇒ recipes.get(id.toString))
}
