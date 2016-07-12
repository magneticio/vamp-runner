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

object VampRunner extends App {

  implicit val system = ActorSystem("vamp-runner")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val logger = Logger(LoggerFactory.getLogger(VampRunner.getClass))

  val version = Option(getClass.getPackage.getImplementationVersion).orElse {
    Try(Option("git describe --tags".!!.stripLineEnd)).getOrElse(None)
  } getOrElse ""

  val config = Config.config("vamp.runner")

  val index = config.string("ui.index")
  val directory = config.string("ui.directory")

  logger.info(
    s"""
       |██╗   ██╗ █████╗ ███╗   ███╗██████╗
       |██║   ██║██╔══██╗████╗ ████║██╔══██╗
       |██║   ██║███████║██╔████╔██║██████╔╝
       |╚██╗ ██╔╝██╔══██║██║╚██╔╝██║██╔═══╝
       | ╚████╔╝ ██║  ██║██║ ╚═╝ ██║██║
       |  ╚═══╝  ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
       |                       runner $version
       |                       by magnetic.io
    """.stripMargin)

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

  val route = {
    withRequestTimeout(config.duration("timeout")) {
      encodeResponse {
        ui
      }
    }
  }

  val http = Http().bindAndHandle(route, config.string("interface"), config.int("port"))

  sys.addShutdownHook {
    http.map(_.unbind()).onComplete(_ ⇒ system.terminate())
  }
}
