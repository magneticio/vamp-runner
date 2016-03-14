package io.vamp.runner.recipe

import akka.actor.ActorSystem
import org.json4s._

class VampTcpDependency(implicit actorSystem: ActorSystem) extends Recipe {

  def name = "tcp-dependency"

  protected def run = apiPut(s"deployments/$name", resource("tcpDependency/blueprint.yml")).flatMap { _ ⇒

    logger.info(s"Waiting for deployment...")

    waitForTcp(9054, "---", { json ⇒

      logger.info("Response has been received:")

      val id = <<[String](json \ "id")
      val runtime = <<[String](json \ "runtime")
      val port = <<[Int](json \ "port")
      val request = <<[String](json \ "request")

      logger.info(s"Id     : $id")
      logger.info(s"Runtime: $runtime")
      logger.info(s"Port   : $port")
      logger.info(s"Request: $request")

      if (id != "backend") throw new RuntimeException(s"Expected 'backend' but not id: $id")
      if (port != 8095) throw new RuntimeException(s"Expected '8095' but not port: $port")
      if (request != "---") throw new RuntimeException(s"Expected '---' but not path: $request")
    })
  }
}
