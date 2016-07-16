package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

trait WebServer extends JsonSerializer {

  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  implicit def executionContext = system.dispatcher

  def messenger: Hub

  val config = Config.config("vamp.runner.http")

  val index = config.string("ui.index")
  val directory = config.string("ui.directory")

  def server = Http().bindAndHandle({
    withRequestTimeout(Config.duration("vamp.runner.timeout")) {
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
        } via messenger.channel map (message ⇒ TextMessage.Strict(writeJson(message)))
      }
    }
  }, config.string("interface"), config.int("port"))
}
