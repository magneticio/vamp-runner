package io.vamp.runner.recipe

import akka.actor.ActorSystem

class VampHttpFlipFlopDependency(implicit actorSystem: ActorSystem) extends FlipFlop {

  def port: Int = 9055

  def resourcePath: String = "httpFlipFlopDependency"

  def deployment: String = "deployments/http-flip-flop-dependency"
}
