package io.vamp.runner

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Runner extends App {

  implicit val actorSystem = ActorSystem("vamp-runner")

  val logger = Logger(LoggerFactory.getLogger(Runner.getClass))

  sys.addShutdownHook {
    actorSystem.terminate()
  }

  logger.info(
    s"""
       |██╗   ██╗ █████╗ ███╗   ███╗██████╗
       |██║   ██║██╔══██╗████╗ ████║██╔══██╗
       |██║   ██║███████║██╔████╔██║██████╔╝
       |╚██╗ ██╔╝██╔══██║██║╚██╔╝██║██╔═══╝
       | ╚████╔╝ ██║  ██║██║ ╚═╝ ██║██║
       |  ╚═══╝  ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
       |                       runner
       |                       by magnetic.io
       |
    """.stripMargin)

  val url = ConfigFactory.load().getString("vamp.runner.url")

  logger.info(s"Vamp URL: $url")

  sys.exit(0)
}
