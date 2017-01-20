package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.agent.Agent
import akka.stream.ActorMaterializer
import io.vamp.runner.Hub.Broadcast
import org.json4s.JsonAST.JValue

import scala.concurrent.duration._
import scala.language.postfixOps

object InfoActor {

  def props(implicit materializer: ActorMaterializer): Props = Props(classOf[InfoActor], materializer)

  private object Query

  object ProvideInfo

  object VampConnectionError extends Response {
    override val `type`: String = "vamp-connection-error"
  }

  case class Info(uuid: String, version: String, persistence: String, keyValueStore: String, gatewayDriver: String, containerDriver: String, workflowDriver: String) extends Response

  case class Load(uuid: String, cpu: Double, heap: Heap) extends Response

  case class Heap(max: Double, used: Double)

}

class InfoActor(implicit val materializer: ActorMaterializer) extends Actor with ActorLogging with VampApiClient {

  import InfoActor._

  implicit def system: ActorSystem = context.system

  val timeout = Config.duration("vamp.runner.timeout")

  private val load = Agent[Option[Load]](None)
  private val info = Agent[Option[Info]](None)

  def receive: Receive = {
    case Query       ⇒ query()
    case ProvideInfo ⇒ provideInfo()
    case _           ⇒
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(0 seconds, Config.duration("vamp.runner.info.interval"), self, Query)
  }

  private def query() = {

    val path = info().map(_ ⇒ "info?on=jvm").getOrElse("info")

    apiGet(path) map {
      case Left(json) ⇒
        info() match {
          case None ⇒

            val infoResult = parseInfo(json)
            val loadResult = parseLoad(json)

            info send Option(infoResult)

            context.parent ! Broadcast(infoResult)
            context.parent ! Broadcast(loadResult)

          case Some(infoValue) ⇒

            val loadResult = parseLoad(json)

            if (loadResult.uuid != infoValue.uuid) info send None

            load send Option(loadResult)

            context.parent ! Broadcast(loadResult)
        }
      case _ ⇒
    } recover {
      case e ⇒
        log.error(e.getMessage)
        context.parent ! Broadcast(VampConnectionError)
    }
  }

  private def provideInfo() = {
    info().foreach(sender() ! _)
    load().foreach(sender() ! _)
  }

  private def parseInfo(json: JValue) = {
    Info(
      uuid = <<[String](json \ "uuid"),
      version = <<[String](json \ "version"),
      persistence = <<[String](json \ "persistence" \ "database" \ "type"),
      keyValueStore = <<[String](json \ "key_value" \ "type"),
      gatewayDriver = <<[String](json \ "gateway_driver" \ "marshallers" \\ "type").split(' ').distinct.mkString(","),
      containerDriver = <<[String](json \ "container_driver" \ "type"),
      workflowDriver =
        <<[Any](json \ "workflow_driver") match {
          case map: Map[_, _] ⇒ map.keys.mkString(", ")
          case _              ⇒ ""
        }
    )
  }

  private def parseLoad(json: JValue) = {
    Load(
      uuid = <<[String](json \ "uuid"),
      cpu = <<[Double](json \ "jvm" \ "operating_system" \ "system_load_average"),
      Heap(
        max = <<[Double](json \ "jvm" \ "memory" \ "heap" \ "max"),
        used = <<[Double](json \ "jvm" \ "memory" \ "heap" \ "used")
      )
    )
  }
}
