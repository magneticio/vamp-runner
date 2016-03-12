package io.vamp.runner.recipe

import akka.actor.ActorSystem

class VampHttpFlipFlop(implicit actorSystem: ActorSystem) extends FlipFlop {

  def port: Int = 9051

  def resourcePath: String = "httpFlipFlop"

  def deployment: String = "deployments/http-flip-flop"
}
