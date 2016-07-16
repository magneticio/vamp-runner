package io.vamp.runner

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

class RunnerHub(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends Hub {

  import InfoActor._
  import RunnerActor._

  private val logger = Logger(LoggerFactory.getLogger(getClass))

  implicit val timeout: Timeout = Config.duration("vamp.runner.timeout")

  override def children: Map[String, Props] = Map("info" -> InfoActor.props, "runner" -> RunnerActor.props)

  protected def onReceive(sender: ActorRef) = {
    case "info"                                ⇒ forward("info", ProvideInfo, sender)
    case "recipes"                             ⇒ forward("runner", ProvideRecipes, sender)
    case "stop"                                ⇒ forward("runner", StopExecution, sender)
    case cmd: String if cmd.startsWith("run:") ⇒ forward("runner", StartExecution(cmd.substring("run:".length).split(',').toList), sender)
    case other                                 ⇒ logger.info(s"Unknown action: $other")
  }
}
