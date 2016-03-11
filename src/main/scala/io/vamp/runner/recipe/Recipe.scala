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
import akka.stream.scaladsl.{ Sink, Source, Tcp }
import akka.util.ByteString
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

  def vgaGet(port: Int, path: String = "", recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    http(GET, s"http://${Vamp.vgaHost}:$port/$path", None, recoverWith)
  }

  def apiGet(path: String, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = api(GET, path, None, recoverWith)

  def apiPut(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    api(PUT, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  def apiPost(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    api(POST, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  def apiDelete(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    api(DELETE, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  def api(method: HttpMethod, path: String, body: Option[Array[Byte]], recoverWith: AnyRef ⇒ String): Future[JValue] = {
    http(method, s"${Vamp.apiUrl}/$path", body, recoverWith)
  }

  def http(method: HttpMethod, uri: String, body: Option[Array[Byte]], recoverWith: AnyRef ⇒ String): Future[JValue] = {

    val url = new java.net.URL(uri)

    val httpRequest = HttpRequest(uri = url.getPath)
      .addHeader(Accept(`application/json`))
      .withMethod(method)

    val httpRequestWithBody = body.map { bytes ⇒
      httpRequest.withEntity(ContentType(MediaType.applicationBinary("x-yaml", Compressible, "yml", "yaml")), bytes)
    } getOrElse httpRequest

    Source.single(httpRequestWithBody)
      .via(Http().outgoingConnection(url.getHost, url.getPort))
      .recover {
        case failure ⇒ recoverWith(failure)
      }.mapAsync(1) {
        case HttpResponse(status, _, entity, _) if status.isSuccess() ⇒ entity.toStrict(Vamp.timeout).map(_.data.decodeString("UTF-8"))
        case failure ⇒ Future(recoverWith(failure))
      }.map {
        parse(_)
      } runWith Sink.head
  }

  def tcp(host: String, port: Int, send: String, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    Source.single(ByteString(send))
      .via(Tcp().outgoingConnection(host, port))
      .recover {
        case failure ⇒ recoverWith(failure)
      }.map {
        case response: ByteString ⇒ response.utf8String
        case failure              ⇒ recoverWith(failure)
      }.map {
        parse(_)
      } runWith Sink.head
  }

  def <<[T](jv: JValue)(implicit mf: scala.reflect.Manifest[T]) = jv.extract[T]
}

trait FlowMethods {
  this: Recipe with HttpMethods ⇒

  def reset(): Future[Any] = {
    waitFor({ () ⇒ apiGet("reset") }, { _ ⇒ }, { () ⇒ logger.debug(s"Still waiting for reset to complete...") }) runWith Sink.headOption
  }

  def waitFor(port: Int, path: String, validate: JValue ⇒ Unit): Future[Any] = {
    waitFor({ () ⇒ vgaGet(port, path) }, validate, { () ⇒ logger.debug(s"Still waiting for :${if (path.isEmpty) port else s"$port/$path"}") }) runWith Sink.headOption
  }

  def waitForTcp(port: Int, send: String, validate: JValue ⇒ Unit): Future[Any] = {
    waitFor({ () ⇒ tcp(Vamp.vgaHost, port, "*") }, validate, { () ⇒ logger.debug(s"Still waiting for :$port") }) runWith Sink.headOption
  }

  def waitFor(request: () ⇒ Future[JValue], validate: JValue ⇒ Unit, recover: () ⇒ Unit): Source[Boolean, Cancellable] = {
    Source.tick(initialDelay = 0 seconds, interval = Recipe.interval, None).mapAsync[Boolean](1) { _ ⇒
      request().map {
        case JNothing ⇒
          recover(); false
        case json ⇒
          try {
            validate(json); true
          } catch {
            case e: Exception ⇒ logger.error(e.getMessage, e); false
          }
      } recover {
        case _ ⇒ recover(); false
      }
    } completionTimeout Recipe.timeout dropWhile {
      !_
    }
  }
}