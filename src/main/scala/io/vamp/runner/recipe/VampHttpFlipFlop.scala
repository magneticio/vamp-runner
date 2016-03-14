package io.vamp.runner.recipe

import akka.actor.ActorSystem

class VampHttpFlipFlop(implicit actorSystem: ActorSystem) extends FlipFlop {

  def name = "http-flip-flop"

  protected def port: Int = 9051

  protected def resourcePath: String = "httpFlipFlop"
}
