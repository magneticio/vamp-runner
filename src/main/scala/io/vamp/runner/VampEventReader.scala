package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, ActorSystem }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import de.heikoseeberger.akkasse.ServerSentEvent
import de.heikoseeberger.akkasse.pattern.ServerSentEventClient
import io.vamp.runner.VampEventReader.VampEvent
import org.json4s.native.Serialization._

import scala.concurrent.ExecutionContext

object VampEventReader {

  sealed trait VampEventMessage

  case class VampEvent(tags: Set[String]) extends VampEventMessage

  object VampEventRelease extends VampEventMessage
}

trait VampEventReader {
  this: Actor with ActorLogging ⇒

  private implicit val format = Json.format

  private implicit val system: ActorSystem = context.system

  private implicit val executionContext: ExecutionContext = context.dispatcher

  implicit def materializer: ActorMaterializer

  def sse(): Unit = {
    ServerSentEventClient(s"${Config.string("vamp.runner.api.url")}/events/stream", Sink.foreach[ServerSentEvent](
      event ⇒ self ! read[VampEvent](event.data)
    )).runWith(Sink.ignore)
  }
}
