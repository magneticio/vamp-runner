package io.vamp.runner.recipe

import akka.actor.ActorSystem
import org.json4s._

class VampInfo(implicit actorSystem: ActorSystem) extends Recipe {

  def run = request("info").runForeach {
    case response ⇒
      val version = extract[String](response \ "version")
      val message = extract[String](response \ "message")
      logger.info(s"Vamp [$version]: $message")
  }
}
