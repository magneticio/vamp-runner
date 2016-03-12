package io.vamp.runner.recipe

import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{ Sink, Source }
import org.json4s._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

abstract class FlipFlop(implicit actorSystem: ActorSystem) extends Recipe {

  private val parallelism = 1

  private val requestCount = 10

  private val throttle = true

  private val delete = false

  def deployment: String
  
  def resourcePath: String

  def port: Int

  private var current = ""

  def run = {
    logger.info(s"Parallelism  : $parallelism")
    logger.info(s"Request count: $requestCount")
    logger.info(s"Throttle     : $throttle")
    logger.info(s"Delete       : $delete")

    deploy() flatMap { _ ⇒
      throttle match {
        case false ⇒ flipFlop() runWith Sink.ignore
        case true  ⇒ flipFlop() throttle (1, 1.0 / parallelism seconds, 1, ThrottleMode.shaping) runWith Sink.ignore
      }
    } flatMap { _ ⇒
      reset()
    }
  }

  private def deploy() = {
    apiPut(deployment, resource(s"$resourcePath/blueprint_1.0.yml")).flatMap { _ ⇒
      waitFor(port, "", { json ⇒
        current = <<[String](json \ "id")
        if (current != "1.0" && current != "1.1") throw new RuntimeException(s"Expected '1.0' but not id: $current")
      })
    } flatMap { _ ⇒
      apiPut(deployment, resource(s"$resourcePath/blueprint_1.1.yml"))
    }
  }

  private def flipFlop() = {
    def flip() = {
      def switch(to: String) = {
        val old = current
        logger.info(s"$current -> $to")
        apiPut(deployment, resource(s"$resourcePath/blueprint_$to.yml")) flatMap { _ ⇒
          apiPut(deployment, resource(s"$resourcePath/set_100%_to_$to.yml")) flatMap { some ⇒
            if (delete)
              apiDelete(deployment, resource(s"$resourcePath/blueprint_$old.yml"))
            else Future.successful(some)
          }
        }
        current = to
      }

      current match {
        case "1.0" ⇒ switch("1.1")
        case "1.1" ⇒ switch("1.0")
        case _     ⇒ throw new RuntimeException(s"Unknown: $current")
      }
    }

    Source(1 to requestCount).mapAsync(parallelism) { index ⇒
      vgaGet(port, s"$index", {
        case e: Throwable ⇒
          logger.error(e.getMessage, e)
          throw e
        case any ⇒
          logger.error(any.toString)
          throw new RuntimeException(any.toString)
      }).map { json ⇒
        val id = <<[String](json \ "id")
        logger.trace(s"[${<<[String](json \ "path")}]: $id")
        if (id == current) flip()
      } recover {
        case failure ⇒
          logger.error(failure.getMessage, failure)
          throw failure
      }
    }
  }
}
