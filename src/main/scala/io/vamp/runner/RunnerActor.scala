package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.stream.ActorMaterializer
import io.vamp.runner.Hub.Broadcast

import scala.collection.mutable

object RunnerActor {

  def props(implicit materializer: ActorMaterializer): Props = Props(classOf[RunnerActor], materializer)

  object ProvideRecipes

  case class Run(arguments: AnyRef)

  case class Cleanup(arguments: AnyRef)

  case class Recipes(recipes: List[Recipe]) extends Response
}

class RunnerActor(implicit val materializer: ActorMaterializer) extends Actor with ActorLogging with RecipeLoader with RecipeRunner {

  import RunnerActor._

  val timeout = Config.duration("vamp.runner.timeout")

  implicit def system: ActorSystem = context.system

  private val recipes = mutable.LinkedHashMap(load.map(recipe ⇒ recipe.id -> recipe): _*)

  def receive: Receive = {
    case ProvideRecipes     ⇒ sender() ! Recipes(recipes.values.toList)
    case Run(arguments)     ⇒ run()(arguments)
    case Cleanup(arguments) ⇒ cleanup()(arguments)
    case _                  ⇒
  }

  protected def update(recipe: Recipe, step: RecipeStep): RecipeStep = {
    recipes.get(recipe.id).map { r ⇒
      recipes += (r.id -> r.copy(steps = r.steps.map { s ⇒
        if (s.id == step.id) step else s
      }))
    }
    context.parent ! Broadcast(Recipes(RunnerActor.this.recipes.values.toList))
    step
  }

  private def run(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒
      run(ids2recipes(list).map { recipe ⇒
        recipes += (recipe.id -> recipe.copy(steps = recipe.steps.map { step ⇒ step.copy(state = Recipe.State.Idle) }))
        recipe
      })

    case map: Map[_, _] ⇒ for {
      recipe ← map.asInstanceOf[Map[String, _]].get("recipe").flatMap(id ⇒ recipes.get(id.toString))
      step ← map.asInstanceOf[Map[String, _]].get("step").flatMap(id ⇒ recipe.steps.find(_.id == id))
    } yield run(recipe, step)
  }

  private def cleanup(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒ cleanup(ids2recipes(list))
  }

  private def ids2recipes(ids: List[_]): List[Recipe] = ids.flatMap(id ⇒ recipes.get(id.toString))
}
