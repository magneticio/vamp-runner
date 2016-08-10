package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object VampWebRunner extends App with WebServer with Banner {

  val logger = Logger(LoggerFactory.getLogger(VampWebRunner.getClass))

  implicit lazy val system = ActorSystem("vamp-runner")

  implicit lazy val materializer = ActorMaterializer()

  implicit lazy val executionContext = system.dispatcher

  banner()

  val messenger = new RunnerHub

  val binding = server

  sys.addShutdownHook {
    binding.map(_.unbind()).onComplete(_ ⇒ Http().shutdownAllConnectionPools().onComplete(_ ⇒ system.terminate()))
  }
}
