package io.vamp.runner.recipe

import java.io.InputStream

import akka.actor.{ ActorSystem, Cancellable }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ HttpRequest, _ }
import akka.stream._
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.scalalogging.Logger
import io.vamp.runner.Vamp
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object Recipe {

  val interval = 3 seconds

  val timeout = 60 seconds
}

abstract class Recipe(implicit system: ActorSystem) extends HttpMethods with FlowMethods {

  implicit val actorSystem = system
  implicit val executionContext = actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()(actorSystem)

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def run: Future[Any]

  protected def resource(path: String) = getClass.getResourceAsStream(path)
}

trait HttpMethods {
  this: Recipe ⇒

  implicit val formats = DefaultFormats

  def vgaGet(port: Int, path: String): Future[JValue] = {
    request(GET, s"http://${Vamp.vgaHost}:$port/$path")
  }

  def apiGet(path: String): Future[JValue] = api(GET, path)

  def apiPut(path: String, input: InputStream): Future[JValue] = {
    api(PUT, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray))
  }

  def apiPost(path: String, input: InputStream): Future[JValue] = {
    api(POST, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray))
  }

  def apiDelete(path: String, input: InputStream): Future[JValue] = {
    api(DELETE, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray))
  }

  def api(method: HttpMethod, path: String, body: Option[Array[Byte]] = None): Future[JValue] = {
    request(method, s"${Vamp.apiUrl}/$path", body)
  }

  private def request(method: HttpMethod, uri: String, body: Option[Array[Byte]] = None): Future[JValue] = {

    val url = new java.net.URL(uri)

    val httpRequest = HttpRequest(uri = url.getPath)
      .addHeader(Accept(`application/json`))
      .withMethod(method)

    val httpRequestWithBody = body.map { bytes ⇒
      httpRequest.withEntity(ContentType(MediaType.applicationBinary("x-yaml", Compressible, "yml", "yaml")), bytes)
    } getOrElse httpRequest

    Source.single(httpRequestWithBody)
      .via(Http().outgoingConnection(url.getHost, url.getPort))
      .mapAsync(1) {
        case HttpResponse(status, _, entity, _) if status.isSuccess() ⇒ entity.toStrict(Vamp.timeout).map(_.data.decodeString("UTF-8"))
        case _ ⇒ Future.successful("")
      }.map {
        parse(_)
      } runWith Sink.head
  }

  def <<[T](jv: JValue)(implicit mf: scala.reflect.Manifest[T]) = jv.extract[T]
}

trait FlowMethods {
  this: Recipe with HttpMethods ⇒

  def reset(): Future[Any] = {
    waitFor(
      request = { () ⇒ apiGet("reset") },
      validate = { _ ⇒ },
      recover = { () ⇒ logger.debug(s"Still waiting for reset to complete...") }
    ) runWith Sink.headOption
  }

  def waitForFlow(port: Int, path: String, validate: JValue ⇒ Unit): Source[Boolean, Cancellable] = {
    waitFor({ () ⇒ vgaGet(port, path) }, validate, { () ⇒ logger.debug(s"Still waiting for :$port/$path") })
  }

  def waitForSink(port: Int, path: String, validate: JValue ⇒ Unit): Future[Any] = {
    waitForFlow(port, path, validate) runWith Sink.headOption
  }

  private def waitFor(request: () ⇒ Future[JValue], validate: JValue ⇒ Unit, recover: () ⇒ Unit): Source[Boolean, Cancellable] = {
    Source.tick(initialDelay = 0 seconds, interval = Recipe.interval, None).mapAsync[Boolean](1) { _ ⇒
      request().map {
        case JNothing ⇒
          recover(); false
        case json ⇒
          try {
            validate(json); true
          } catch {
            case e: Exception ⇒ logger.info(e.getMessage); false
          }
      } recover {
        case _ ⇒ recover(); false
      }
    } completionTimeout Recipe.timeout dropWhile {
      !_
    }
  }
}