package io.vamp.runner

import java.util.UUID

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.util.Timeout
import org.json4s.native.Serialization.{ read, write }

import scala.collection.mutable

object Hub {

  sealed trait SessionEvent

  case class SessionOpened(id: UUID, subscriber: ActorRef) extends SessionEvent

  case class SessionClosed(id: UUID) extends SessionEvent

  case class Request(id: UUID, request: Command) extends SessionEvent

  case class Broadcast(message: Response) extends SessionEvent

  case class Forward(child: String, request: AnyRef, recipient: ActorRef)

  case class Command(command: String, arguments: AnyRef = None)

}

trait Hub {

  import Hub._

  implicit def system: ActorSystem

  implicit def timeout: Timeout

  implicit def executionContext = system.dispatcher

  protected val sessions = mutable.Map[UUID, ActorRef]()

  private implicit val format = Json.format

  def channel: Flow[AnyRef, String, Any] = {
    val id = UUID.randomUUID()

    val in =
      Flow[AnyRef]
        .map {
          case command: Command ⇒ Request(id, command)
          case message          ⇒ Request(id, read[Command](message.toString))
        }
        .to(Sink.actorRef[SessionEvent](actor, SessionClosed(id)))

    val out =
      Source.actorRef[AnyRef](10, OverflowStrategy.dropHead)
        .mapMaterializedValue(actor ! SessionOpened(id, _)).map(message ⇒ write(message))

    Flow.fromSinkAndSource(in, out)
  }

  def children: Map[String, Props] = Map()

  protected def onReceive(sender: ActorRef): PartialFunction[Any, Unit]

  protected def onOpen(actor: ActorRef): Unit = ()

  protected def onClose(actor: ActorRef): Unit = {}

  protected def forward(child: String, message: AnyRef, recipient: ActorRef) = {
    actor ! Forward(child, message, recipient)
  }

  protected val actor = system.actorOf(Props(new Actor with ActorLogging {

    def receive = {

      case SessionOpened(id, subscriber)      ⇒ sessionOpened(id, subscriber)

      case SessionClosed(id)                  ⇒ sessionClosed(id)

      case Terminated(subscriber)             ⇒ terminated(subscriber)

      case Request(id, command)               ⇒ requestMessage(id, command)

      case Broadcast(message)                 ⇒ broadcast(message)

      case Forward(child, request, recipient) ⇒ context.child(child).foreach(from ⇒ from.tell(request, recipient))

      case _                                  ⇒
    }

    @scala.throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      super.preStart()
      children.map {
        case (name, props) ⇒ context.actorOf(props, name)
      }
    }

    private def sessionOpened(id: UUID, subscriber: ActorRef) = {
      context.watch(subscriber)
      sessions += (id -> subscriber)
      onOpen(subscriber)
    }

    private def sessionClosed(id: UUID) = {
      sessions.remove(id).foreach { subscriber ⇒
        subscriber ! Status.Success(Unit)
        onClose(subscriber)
      }
    }

    private def terminated(subscriber: ActorRef) = {
      sessions.retain((_, v) ⇒ v != subscriber)
      onClose(subscriber)
    }

    private def requestMessage(id: UUID, command: Command) = {
      log.info(s"Request command: $command")
      sessions.get(id).foreach(onReceive(_)(command))
    }

    private def broadcast(message: Response): Unit = {
      log.info(s"Broadcast: ${message.`type`}")
      sessions.values.foreach(_ ! message)
    }
  }))
}

