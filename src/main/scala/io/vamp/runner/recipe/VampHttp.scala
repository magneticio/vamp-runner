package io.vamp.runner.recipe

import akka.actor.ActorSystem
import org.json4s._

class VampHttp(implicit actorSystem: ActorSystem) extends Recipe {

  def run = apiPut("deployments/http", resource("http/blueprint.yml")).flatMap { _ ⇒

    logger.info(s"Waiting for deployment...")

    waitForSink(9050, "*", { json ⇒

      logger.info("Response has been received:")

      val id = <<[String](json \ "id")
      val runtime = <<[String](json \ "runtime")
      val port = <<[Int](json \ "port")
      val path = <<[String](json \ "path")

      logger.info(s"Id     : $id")
      logger.info(s"Runtime: $runtime")
      logger.info(s"Port   : $port")
      logger.info(s"Path   : $path")

      if (id != "1.0.0") throw new RuntimeException(s"Expected '1.0.0' but not id: $id")
      if (port != 8081) throw new RuntimeException(s"Expected '8081' but not port: $port")
      if (path != "*") throw new RuntimeException(s"Expected '*' but not path: $path")
    })
  } flatMap { _ ⇒
    reset()
  }
}
