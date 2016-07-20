package io.vamp.runner

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import io.vamp.runner.Hub.Command
import org.slf4j.LoggerFactory

class RunnerHub(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends Hub {

  import InfoActor._
  import RunnerActor._

  private val logger = Logger(LoggerFactory.getLogger(getClass))

  implicit val timeout: Timeout = Config.duration("vamp.runner.timeout")

  override def children: Map[String, Props] = Map(/*"info" -> InfoActor.props, */"runner" -> RunnerActor.props)

  protected def onReceive(sender: ActorRef) = {
    case Command("info", _)             ⇒ forward("info", ProvideInfo, sender)
    case Command("recipes", _)          ⇒ forward("runner", ProvideRecipes, sender)
    case Command("abort", _)            ⇒ forward("runner", AbortExecutions, sender)
    case Command("run", ids: List[_])   ⇒ forward("runner", StartExecutions(ids.asInstanceOf[List[String]]), sender)
    case Command("purge", ids: List[_]) ⇒ forward("runner", PurgeExecutions(ids.asInstanceOf[List[String]]), sender)
    case other                          ⇒ logger.info(s"Unknown command: $other")
  }
}
