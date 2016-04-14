package io.vamp.runner

import akka.actor.ActorSystem
import io.vamp.runner.recipe._

trait VampRecipes {

  implicit def actorSystem: ActorSystem

  lazy val recipes: List[Recipe] = List(
    new VampInfo,
    new VampHttp,
    new VampHttpCanary,
    new VampHttpDependency,
    new VampHttpFlipFlop,
    new VampHttpFlipFlopDependency,
    new VampTcp,
    new VampTcpDependency,
    new VampRouteWeight,
    new VampRouteWeightFilterStrength,
    new VampScale
  )
}
