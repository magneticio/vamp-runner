package io.vamp.runner

import akka.actor.ActorSystem
import io.vamp.runner.recipe.{ VampHttp, VampHttpFlipFlop, Recipe, VampInfo }

import scala.collection.immutable.ListMap

trait VampRecipes {

  implicit def actorSystem: ActorSystem

  lazy val recipes: List[(String, Recipe)] = List(
    "info" -> new VampInfo,
    "http" -> new VampHttp,
    "http-flip-flop" -> new VampHttpFlipFlop
  )
}
