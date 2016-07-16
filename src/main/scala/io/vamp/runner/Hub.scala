package io.vamp.runner

import java.util.UUID

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.collection.mutable

object Hub {

  sealed trait SessionEvent

  case class SessionOpened(id: UUID, subscriber: ActorRef) extends SessionEvent

  case class SessionClosed(id: UUID) extends SessionEvent

  case class Request(id: UUID, request: AnyRef) extends SessionEvent

  case class Broadcast(message: AnyRef) extends SessionEvent

  case class Forward(child: String, request: AnyRef, recipient: ActorRef)

}

trait Hub {

  import Hub._

  implicit def system: ActorSystem

  implicit def timeout: Timeout

  implicit def executionContext = system.dispatcher

  protected val sessions = mutable.Map[UUID, ActorRef]()

  def channel: Flow[String, AnyRef, Any] = {
    val id = UUID.randomUUID()
    val in = Flow[String].map(Request(id, _)).to(Sink.actorRef[SessionEvent](actor, SessionClosed(id)))
    val out = Source.actorRef[AnyRef](10, OverflowStrategy.dropHead)
      .mapMaterializedValue(actor ! SessionOpened(id, _))
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

      case Request(id, action)                ⇒ requestMessage(id, action)

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

    private def requestMessage(id: UUID, request: AnyRef) = {
      log.info(s"Request: $request")
      sessions.get(id).foreach(onReceive(_)(request))
    }

    private def broadcast(message: AnyRef): Unit = {
      log.info(s"Broadcast: $message")
      sessions.values.foreach(_ ! message)
    }
  }))
}
