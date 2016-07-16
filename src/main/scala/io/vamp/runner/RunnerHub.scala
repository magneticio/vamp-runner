package io.vamp.runner

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.util.Timeout

class RunnerHub(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends Hub {

  import InfoActor._

  implicit val timeout: Timeout = Config.duration("vamp.runner.timeout")

  override def children: Map[String, Props] = Map("info" -> InfoActor.props)

  protected def onReceive(sender: ActorRef) = {
    case "info" ⇒ forward("info", ProvideInfo, sender)
    case _      ⇒
  }
}
