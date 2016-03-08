package io.vamp.runner.recipe

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.Accept
import akka.stream._
import com.typesafe.scalalogging.Logger
import io.vamp.runner.VampApi
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.language.postfixOps

abstract class Recipe(implicit actorSystem: ActorSystem) {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  implicit val formats = DefaultFormats
  implicit val executionContext = actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()(actorSystem)

  def run: Future[Any]

  protected def api(path: String): Future[JValue] = {

    val httpRequest = HttpRequest(uri = s"${VampApi.url}/$path").withHeaders(Accept(`application/json`))

    Http().singleRequest(httpRequest).flatMap {
      case response ⇒ response.entity.toStrict(VampApi.timeout).map(_.data.decodeString("UTF-8"))
    }.map {
      case body ⇒ parse(body)
    }
  }

  protected def <<[T](jv: JValue)(implicit mf: scala.reflect.Manifest[T]) = jv.extract[T]
}
