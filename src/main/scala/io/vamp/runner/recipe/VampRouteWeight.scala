package io.vamp.runner.recipe

import akka.actor.ActorSystem

class VampRouteWeight(implicit actorSystem: ActorSystem) extends Recipe with StressMethods {

  def name = "route-weight"

  private val port = 9057

  private val deviation = config.getDouble("deviation")

  protected def run = for {
    _ ← deploy()
    (service1, service2) ← requests()
  } yield {

    val total = (service1 + service2).toDouble

    val percentage1 = service1 / total * 100
    val percentage2 = service2 / total * 100

    logger.info(s"service 1 responses: $service1 [ $percentage1 % ]")
    logger.info(s"service 2 responses: $service2 [ $percentage2 % ]")

    if (Math.abs(80 - percentage1) > deviation) throw new RuntimeException(s"Expected 80% for service 1 with deviation of $deviation%.")
    if (Math.abs(20 - percentage2) > deviation) throw new RuntimeException(s"Expected 20% for service 1 with deviation of $deviation%.")
  }

  private def deploy() = {
    logger.info(s"Deploying...")
    for {
      _ ← apiPost("scales", resource("routeWeight/scale.yml"))
      _ ← apiPost("breeds", resource("routeWeight/breed1.yml"))
      _ ← apiPost("breeds", resource("routeWeight/breed2.yml"))
      _ ← apiPost("gateways", resource("routeWeight/gateway.yml"))
      _ ← apiPut(s"deployments/$name", resource("routeWeight/blueprint.yml"))
      _ ← {
        logger.info(s"Waiting for deployment...")
        waitFor(port)
      }
    } yield {}
  }

  private def requests() = {
    var count1 = 0
    var count2 = 0

    logger.info(s"Sending requests...")

    keepRequesting({ index ⇒
      if (index % 100 == 0) logger.debug(s"Request count: $index")
      vgaGet(port, s"$index", {
        case any ⇒ throw new RuntimeException(any.toString)
      }).map { json ⇒
        <<[String](json \ "id") match {
          case "1.0.0" ⇒ count1 = count1 + 1
          case "2.0.0" ⇒ count2 = count2 + 1
          case any     ⇒ throw new RuntimeException(any.toString)
        }
      }
    }) map { _ ⇒ (count1, count2) }
  }
}
