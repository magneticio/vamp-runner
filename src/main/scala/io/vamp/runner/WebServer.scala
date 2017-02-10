package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait WebServer {

  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

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
          case TextMessage.Strict(message)  ⇒ Future.successful(message)
          case TextMessage.Streamed(stream) ⇒ stream.limit(100).completionTimeout(5 seconds).runFold("")(_ + _)
        }.mapAsync(parallelism = 3)(identity) via messenger.channel map (message ⇒ TextMessage.Strict(message))
      }
    }
  }, config.string("interface"), config.int("port"))
}
