package io.vamp.runner.recipe

import akka.actor.ActorSystem
import org.json4s._

class VampScale(implicit actorSystem: ActorSystem) extends Recipe with StressMethods {

  private val port = 9061

  def name = "scale"

  protected def run = for {
    _ ← deploy()
    _ ← scale(2)
    _ ← requests()
    _ ← scale(3)
    _ ← requests()
    _ ← scale(2)
    _ ← requests()
    _ ← scale(1)
    _ ← requests()
  } yield {}

  private def deploy() = {
    logger.info(s"Deploying...")
    for {
      _ ← apiPut(s"deployments/$name", resource("scale/blueprint.yml"))
      _ ← {
        logger.info(s"Waiting for deployment...")
        waitFor(port)
      }
    } yield {}
  }

  private def requests() = {
    logger.info(s"Sending requests...")
    keepRequesting({ _ ⇒
      vgaGet(port, "").map { json ⇒
        val id = <<[String](json \ "id")
        if (id != "1.0.0") throw new RuntimeException(s"Expected id == '1.0.0', not: $id")
        true
      }
    })
  }

  private def scale(instances: Int) = {
    logger.info(s"Setting number of instances to: $instances")
    for {
      _ ← apiPut(s"deployments/$name/clusters/runner/services/sava:1.0/scale", resource(s"scale/scale$instances.yml"))
    } yield {}
  }
}
