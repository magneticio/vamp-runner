package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.agent.Agent
import io.vamp.runner.Hub.Broadcast
import org.json4s.JsonAST.JValue

import scala.concurrent.duration._
import scala.language.postfixOps

object InfoActor {

  def props(implicit materializer: ActorMaterializer): Props = Props(classOf[InfoActor], materializer)

  private object Query

  object ProvideInfo

  trait Response {
    def `type`: String
  }

  case class Info(uuid: String, version: String, persistence: String, keyValueStore: String, gatewayDriver: String, containerDriver: String, workflowDriver: String) extends Response {
    val `type`: String = "info"
  }

  case class Load(uuid: String, cpu: Double, heap: Heap) extends Response {
    val `type`: String = "load"
  }

  case class Heap(max: Double, used: Double)
}

class InfoActor(implicit val materializer: ActorMaterializer) extends Actor with ActorLogging with VampApiClient {

  import InfoActor._

  override implicit def system: ActorSystem = context.system

  override protected val apiUrl = Config.string("vamp.runner.api.url")

  override protected val timeout = Config.duration("vamp.runner.timeout")

  private val info = Agent[Option[Info]](None)

  def receive: Receive = {
    case Query       ⇒ query()
    case ProvideInfo ⇒ info().foreach(sender() ! _)
    case _           ⇒
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(0 seconds, Config.duration("vamp.runner.info.interval"), self, Query)
  }

  private def query() = {

    val path = info().map(_ ⇒ "info?for=jvm").getOrElse("info")

    apiGet(path) map {
      json ⇒
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

            context.parent ! Broadcast(loadResult)
        }
    } recover {
      case e ⇒ log.error(e.getMessage)
    }
  }

  private def parseInfo(json: JValue) = {
    Info(
      uuid = <<[String](json \ "uuid"),
      version = <<[String](json \ "version"),
      persistence = <<[String](json \ "persistence" \ "database" \ "type"),
      keyValueStore = <<[String](json \ "key_value" \ "type"),
      gatewayDriver = s"haproxy ${<<[String](json \ "gateway_driver" \ "marshaller" \ "haproxy")}",
      containerDriver = <<[String](json \ "container_driver" \ "type"),
      workflowDriver =
        <<[Any](json \ "workflow_driver") match {
          case map: Map[_, _] ⇒ map.keys.mkString(",")
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
