package io.vamp.runner

import akka.actor.ActorSystem
import io.vamp.runner.recipe._

trait VampRecipes {

  implicit def actorSystem: ActorSystem

  lazy val recipes: List[(String, Recipe)] = List(
    "info" -> new VampInfo,
    "http" -> new VampHttp,
    "http-flip-flop" -> new VampHttpFlipFlop,
    "tcp" -> new VampTcp,
    "http-dependency" -> new VampHttpDependency,
    "tcp-dependency" -> new VampTcpDependency,
    "http-flip-flop-dependency" -> new VampHttpFlipFlopDependency
  )
}
