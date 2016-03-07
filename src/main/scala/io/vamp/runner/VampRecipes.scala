package io.vamp.runner

import akka.actor.ActorSystem
import io.vamp.runner.recipe.{ Recipe, VampInfo }

trait VampRecipes {

  implicit def actorSystem: ActorSystem

  lazy val recipes: Map[String, Recipe] = Map {
    "info" -> new VampInfo
  }
}
