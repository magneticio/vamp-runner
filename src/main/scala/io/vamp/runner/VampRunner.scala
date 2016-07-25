package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object VampRunner extends App with WebServer with Banner {

  implicit lazy val system = ActorSystem("vamp-runner")

  implicit lazy val materializer = ActorMaterializer()

  banner()

  system.actorOf(VampEventReader.props)

  val messenger = new RunnerHub

  val binding = server

  sys.addShutdownHook {
    binding.map(_.unbind()).onComplete(_ ⇒ Http().shutdownAllConnectionPools().onComplete(_ ⇒ system.terminate()))
  }
}
