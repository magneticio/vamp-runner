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
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.vamp.runner.Vamp
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object Recipe {

  private val config = ConfigFactory.load().getConfig("vamp.runner.recipes")

  val interval = config.getInt("interval") seconds

  val timeout = config.getInt("timeout") seconds
}

abstract class Recipe(implicit system: ActorSystem) extends HttpMethods with FlowMethods {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  implicit val actorSystem = system
  implicit val executionContext = actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()(actorSystem)

  protected val config = ConfigFactory.load().getConfig(s"vamp.runner.recipes.$name")

  protected val clean = config.getBoolean("clean")

  protected val enabled = config.getBoolean("enabled")

  def name: String

  final def execute: Future[Any] = {
    if (enabled) run.flatMap { case any ⇒ if (clean) reset() else Future.successful(any) } else Future.successful(false)
  }

  protected def run: Future[Any]

  protected def resource(path: String) = getClass.getResourceAsStream(path)
}

trait HttpMethods {
  this: Recipe ⇒

  protected implicit val formats = DefaultFormats

  protected def vgaGet(port: Int, path: String = "", recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    http(GET, s"http://${Vamp.vgaHost}:$port/$path", None, recoverWith)
  }

  protected def apiGet(path: String, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = api(GET, path, None, { _ ⇒ throw new RuntimeException("No connection.") })

  protected def apiPut(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    api(PUT, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  protected def apiPost(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    api(POST, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  protected def apiDelete(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    api(DELETE, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  protected def api(method: HttpMethod, path: String, body: Option[Array[Byte]], recoverWith: AnyRef ⇒ String): Future[JValue] = {
    http(method, s"${Vamp.apiUrl}/$path", body, recoverWith)
  }

  protected def http(method: HttpMethod, uri: String, body: Option[Array[Byte]], recoverWith: AnyRef ⇒ String): Future[JValue] = {

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

  protected def tcp(host: String, port: Int, send: String, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
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

  protected def <<[T](jv: JValue)(implicit mf: scala.reflect.Manifest[T]) = jv.extract[T]
}

trait FlowMethods {
  this: Recipe with HttpMethods ⇒

  protected def reset(): Future[Any] = {
    logger.info(s"Performing reset...")
    apiGet("reset") flatMap { _ ⇒
      waitFor({ () ⇒ apiGet("deployments") }, {
        case JArray(Nil) ⇒ true
        case _           ⇒ logger.debug(s"Waiting for reset to complete..."); false
      }, {
        () ⇒ logger.debug(s"Waiting for reset to complete...")
      }) runWith Sink.headOption
    }
  }

  protected def waitFor(port: Int, path: String, validate: JValue ⇒ Unit): Future[Any] = {
    waitFor({ () ⇒ vgaGet(port, path) }, {
      json ⇒ validate(json); true
    }, {
      () ⇒ logger.debug(s"Waiting for http://${Vamp.vgaHost}:${if (path.isEmpty) port else s"$port/$path"}")
    }) runWith Sink.headOption
  }

  protected def waitForTcp(port: Int, send: String, validate: JValue ⇒ Unit): Future[Any] = {
    waitFor({ () ⇒ tcp(Vamp.vgaHost, port, send) }, {
      json ⇒ validate(json); true
    }, {
      () ⇒ logger.debug(s"Waiting for ${Vamp.vgaHost}:$port")
    }) runWith Sink.headOption
  }

  protected def waitFor(request: () ⇒ Future[JValue], successful: JValue ⇒ Boolean, recover: () ⇒ Unit): Source[Boolean, Cancellable] = {
    Source.tick(initialDelay = 0 seconds, interval = Recipe.interval, None).mapAsync[Boolean](1) { _ ⇒
      request().map {
        case JNothing ⇒
          recover(); false
        case json ⇒
          try {
            successful(json)
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

trait StressMethods {
  this: Recipe ⇒

  protected val parallelism = config.getInt("parallelism")

  protected val requestCount = config.getInt("request-count")

  protected val throttle = config.getBoolean("throttle")

  protected def keepRequesting(request: Int ⇒ Future[Any]): Future[Any] = {
    val flow = Source(1 to requestCount).mapAsync(parallelism) { index ⇒ request(index) }
    throttle match {
      case false ⇒ flow runWith Sink.ignore
      case true  ⇒ flow throttle (1, 1.0 / parallelism seconds, 1, ThrottleMode.shaping) runWith Sink.ignore
    }
  }
}