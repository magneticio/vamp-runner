package io.vamp.runner.recipe

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.Accept
import akka.stream._
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.Logger
import io.vamp.runner.VampApi
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

abstract class Recipe(implicit actorSystem: ActorSystem) {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  implicit val formats = DefaultFormats
  implicit val executionContext = actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()(actorSystem)

  def run: Future[Any]

  protected def request(path: String) = {
    Source.single(HttpRequest(uri = s"${VampApi.api}/$path").withHeaders(Accept(`application/json`)))
      .via(Http().outgoingConnection(VampApi.host, VampApi.port))
      .mapAsync(1) {
        case response ⇒ response.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8"))
      }.map {
        case entity ⇒ parse(entity)
      }
  }

  protected def extract[T](jv: JValue)(implicit mf: scala.reflect.Manifest[T]) = jv.extract[T]
}
