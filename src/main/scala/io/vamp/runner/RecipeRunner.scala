package io.vamp.runner

import akka.actor.ActorLogging

trait RecipeRunner {
  this: ActorLogging ⇒

  protected def run(recipes: List[Recipe]): Unit = {}

  protected def run(recipe: Recipe, step: RecipeStep, indefinite: Boolean): Unit = {}

  protected def abort(): Unit = {}

  protected def cleanup(recipes: List[Recipe]): Unit = {}

  //    context.parent ! Broadcast(Recipes(recipes.values.toList))
}
