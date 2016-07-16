package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

trait WebServer {

  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  implicit def executionContext = system.dispatcher

  def messenger: MessageHub

  val config = Config.config("vamp.runner")

  val index = config.string("ui.index")
  val directory = config.string("ui.directory")

  def server = Http().bindAndHandle({
    withRequestTimeout(config.duration("timeout")) {
      encodeResponse {
        get {
          pathEnd {
            redirect("/", MovedPermanently)
          } ~ pathSingleSlash {
            if (index.isEmpty) reject else getFromFile(index)
          } ~ pathPrefix("") {
            if (directory.isEmpty) reject else getFromDirectory(directory)
          }
        }
      }
    } ~ path("channel") {
      handleWebSocketMessages {
        Flow[Message].collect {
          case TextMessage.Strict(message) ⇒ message
        } via messenger.channel map (message ⇒ TextMessage.Strict(write(message)(DefaultFormats)))
      }
    }
  }, config.string("interface"), config.int("port"))
}
