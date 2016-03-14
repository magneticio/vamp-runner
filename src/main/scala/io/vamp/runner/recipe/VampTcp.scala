package io.vamp.runner.recipe

import akka.actor.ActorSystem
import org.json4s._

class VampTcp(implicit actorSystem: ActorSystem) extends Recipe {

  def name = "tcp"

  protected def run = apiPut(s"deployments/$name", resource("tcp/blueprint.yml")).flatMap { _ ⇒

    logger.info(s"Waiting for deployment...")

    waitForTcp(9052, "*", { json ⇒

      logger.info("Response has been received:")

      val id = <<[String](json \ "id")
      val runtime = <<[String](json \ "runtime")
      val port = <<[Int](json \ "port")
      val request = <<[String](json \ "request")

      logger.info(s"Id     : $id")
      logger.info(s"Runtime: $runtime")
      logger.info(s"Port   : $port")
      logger.info(s"Request: $request")

      if (id != "1.0.0") throw new RuntimeException(s"Expected '1.0.0' but not id: $id")
      if (port != 8091) throw new RuntimeException(s"Expected '8081' but not port: $port")
      if (request != "*") throw new RuntimeException(s"Expected '*' but not path: $request")
    })
  }
}
