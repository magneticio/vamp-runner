package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, Props }

import scala.collection.mutable

object RunnerActor {

  def props: Props = Props(classOf[RunnerActor])

  object ProvideRecipes

  object Abort

  case class Run(arguments: AnyRef)

  case class Cleanup(arguments: AnyRef)

  case class Recipes(recipes: List[Recipe]) extends Response

}

class RunnerActor extends Actor with ActorLogging with RecipeLoader with RecipeRunner {

  import RunnerActor._

  private val recipes = mutable.LinkedHashMap(load.map(recipe ⇒ recipe.id -> recipe): _*)

  def receive: Receive = {
    case ProvideRecipes     ⇒ sender() ! Recipes(recipes.values.toList)
    case Run(arguments)     ⇒ run()(arguments)
    case Cleanup(arguments) ⇒ cleanup()(arguments)
    case Abort              ⇒ abort()
    case _                  ⇒
  }

  private def run(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒ run(ids2recipes(list))
    case map: Map[_, _] ⇒ for {
      recipe ← map.asInstanceOf[Map[String, _]].get("recipe").flatMap(id ⇒ recipes.get(id.toString))
      step ← map.asInstanceOf[Map[String, _]].get("step").flatMap(id ⇒ recipe.steps.find(_.id == id))
      indefinite ← map.asInstanceOf[Map[String, _]].get("indefinite").map(_.toString.toBoolean)
    } yield run(recipe, step, indefinite)
  }

  private def cleanup(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒ cleanup(ids2recipes(list))
  }

  private def ids2recipes(ids: List[_]): List[Recipe] = ids.flatMap(id ⇒ recipes.get(id.toString))
}
