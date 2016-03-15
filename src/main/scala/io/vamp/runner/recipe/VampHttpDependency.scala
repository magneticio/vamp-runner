package io.vamp.runner.recipe

import akka.actor.ActorSystem
import org.json4s._

class VampHttpDependency(implicit actorSystem: ActorSystem) extends Recipe {

  def name = "http-dependency"

  protected def run = apiPut(s"deployments/$name", resource("httpDependency/blueprint.yml")).flatMap { _ ⇒

    logger.info(s"Waiting for deployment...")

    waitFor(9053, "***", { json ⇒

      logger.info("Response has been received:")

      val id = <<[String](json \ "id")
      val runtime = <<[String](json \ "runtime")
      val port = <<[Int](json \ "port")
      val path = <<[String](json \ "path")

      logger.info(s"Id     : $id")
      logger.info(s"Runtime: $runtime")
      logger.info(s"Port   : $port")
      logger.info(s"Path   : $path")

      if (id != "backend") throw new RuntimeException(s"Expected id == 'backend', not: $id")
      if (port != 8085) throw new RuntimeException(s"Expected port == '8085', not: $port")
      if (path != "***") throw new RuntimeException(s"Expected path == '***', not: $path")
    })
  }
}
