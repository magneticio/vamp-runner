package io.vamp.runner.recipe

import akka.actor.ActorSystem

class VampHttpFlipFlopDependency(implicit actorSystem: ActorSystem) extends FlipFlop {

  def name = "http-flip-flop-dependency"

  protected def port: Int = 9055

  protected def resourcePath: String = "httpFlipFlopDependency"
}
