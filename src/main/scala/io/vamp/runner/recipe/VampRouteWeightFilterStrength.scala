package io.vamp.runner.recipe

import akka.actor.ActorSystem

class VampRouteWeightFilterStrength(implicit actorSystem: ActorSystem) extends Recipe with StressMethods {

  def name = "route-weight-filter-strength"

  private val port = 9060

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

    /**
     * service 1
     * weight 80%
     *
     * service 2
     * weight 20%
     * filter strength 50% if path "/2"
     *
     * - any non "/2" path - 50% of all requests:
     * service 1: 80%
     * service 2: 20%
     *
     * - path "/2" - 50% of all requests:
     * service 1: 0%  + 50% * 80% = 40%
     * service 2: 50% + 50% * 20% = 60%
     *
     * Total:
     * service 1: 50% * 80% + 50% * 40% = 60%
     * service 2: 50% * 20% + 50% * 60% = 40%
     */

    if (Math.abs(60 - percentage1) > deviation) throw new RuntimeException(s"Expected 60% for service 1 with deviation of $deviation%.")
    if (Math.abs(40 - percentage2) > deviation) throw new RuntimeException(s"Expected 40% for service 1 with deviation of $deviation%.")
  }

  private def deploy() = {
    logger.info(s"Deploying...")
    for {
      _ ← apiPost("breeds", resource("routeWeightFilterStrength/breed1.yml"))
      _ ← apiPost("breeds", resource("routeWeightFilterStrength/breed2.yml"))
      _ ← apiPost("gateways", resource("routeWeightFilterStrength/gateway.yml"))
      _ ← apiPut(s"deployments/$name-1", resource("routeWeightFilterStrength/blueprint1.yml"))
      _ ← apiPut(s"deployments/$name-2", resource("routeWeightFilterStrength/blueprint2.yml"))
      _ ← {
        logger.info(s"Waiting for deployment 1...")
        waitFor(9058)
      }
      _ ← {
        logger.info(s"Waiting for deployment 2...")
        waitFor(9059)
      }
    } yield {}
  }

  private def requests() = {
    var count1 = 0
    var count2 = 0

    logger.info(s"Sending requests...")

    keepRequesting({ index ⇒
      if (index % 100 == 0) logger.debug(s"Request count: $index")
      vgaGet(port, s"${index % 2 + 1}", {
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
