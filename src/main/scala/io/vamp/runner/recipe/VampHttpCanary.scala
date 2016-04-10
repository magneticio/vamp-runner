package io.vamp.runner.recipe

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import io.vamp.runner.Vamp
import org.json4s._

import scala.concurrent.Future

class VampHttpCanary(implicit actorSystem: ActorSystem) extends Recipe with StressMethods {

  def name = "http-canary"

  protected def run = for {

    _ ← deploy("blueprint1")
    _ ← waitForService("1.0", force = true, extended = false)
    _ ← deploy("blueprint2")
    _ ← waitForService("1.1")
    _ ← undeploy("blueprint1")
    _ ← waitForService("1.1")

  } yield {}

  private def deploy(blueprint: String) = {
    logger.info(s"Deploying $blueprint")
    apiPut(s"deployments/$name", resource(s"httpCanary/$blueprint.yml"))
  }

  private def undeploy(blueprint: String) = {
    logger.info(s"Undeploying $blueprint")
    apiDelete(s"deployments/$name", resource(s"httpCanary/$blueprint.yml"))
  }

  private def waitForService(id: String, force: Boolean = false, extended: Boolean = true): Future[Any] = {

    val port = 9056

    def handle(index: Option[Int]): JValue ⇒ Boolean = { json ⇒
      val responseId = <<[String](json \ "id")
      logger.info(s"Response id${if (index.isDefined) s"[${index.get}]" else ""}: $responseId")
      if (force && responseId != id) throw new RuntimeException(s"Expected id == '$id', not: $responseId")
      responseId == id
    }

    extended match {
      case false ⇒
        logger.info(s"Waiting for '$id' deployment...")
        waitFor({ () ⇒ vgaGet(port, "") }, {
          handle(None)
        }, {
          () ⇒ logger.debug(s"Waiting for http://${Vamp.vgaHost}:9056")
        }) runWith Sink.headOption

      case true ⇒
        logger.info(s"Keep requesting '$id'...")
        keepRequesting({ index ⇒
          vgaGet(port, "").map {
            handle(Option(index))
          }
        })
    }
  }
}
