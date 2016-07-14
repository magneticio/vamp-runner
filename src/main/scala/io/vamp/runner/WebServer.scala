package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import io.vamp.runner.util.Config
import org.slf4j.LoggerFactory

import scala.sys.process._
import scala.util.Try

trait WebServer {

  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
  implicit def executionContext = system.dispatcher

  val config = Config.config("vamp.runner")

  val index = config.string("ui.index")
  val directory = config.string("ui.directory")

  private val ui = {
    get {
      pathEnd {
        redirect("./", MovedPermanently)
      } ~ pathSingleSlash {
        if (index.isEmpty) reject else getFromFile(index)
      } ~ pathPrefix("") {
        if (directory.isEmpty) reject else getFromDirectory(directory)
      }
    }
  }

  private val route = {
    withRequestTimeout(config.duration("timeout")) {
      encodeResponse {
        ui
      }
    }
  }

  def httpServe() = Http().bindAndHandle(route, config.string("interface"), config.int("port"))
}
