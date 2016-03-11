package io.vamp.runner

import akka.actor.ActorSystem
import io.vamp.runner.recipe.{ Recipe, VampHttpNoDependency, VampInfo }

import scala.collection.immutable.ListMap

trait VampRecipes {

  implicit def actorSystem: ActorSystem

  lazy val recipes: List[(String, Recipe)] = List(
    "info" -> new VampInfo,
    "http-no-dependency" -> new VampHttpNoDependency()
  )
}
