package io.vamp.runner.recipe

import akka.actor.ActorSystem
import org.json4s._

class VampInfo(implicit actorSystem: ActorSystem) extends Recipe {

  def run = apiGet("info").map {
    case response ⇒
      val version = <<[String](response \ "version")
      val message = <<[String](response \ "message")
      logger.info(s"Vamp [$version]: $message")
  }
}
