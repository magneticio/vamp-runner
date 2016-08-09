package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ HttpRequest, _ }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait VampApiClient {

  def timeout: FiniteDuration

  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  implicit def executionContext = system.dispatcher

  protected def apiUrl: String = Config.string("vamp.runner.api.url")

  protected def apiGet(path: String): Future[Either[JValue, AnyRef]] = {
    apiRequest(GET, path, None, { _ ⇒ "" })
  }

  protected def apiPut(path: String = "", input: String, recoverWith: AnyRef ⇒ AnyRef = { _ ⇒ "" }): Future[Either[JValue, AnyRef]] = {
    apiRequest(PUT, path, Option(input.getBytes), recoverWith)
  }

  protected def apiPost(path: String = "", input: String, recoverWith: AnyRef ⇒ AnyRef = { _ ⇒ "" }): Future[Either[JValue, AnyRef]] = {
    apiRequest(POST, path, Option(input.getBytes), recoverWith)
  }

  protected def apiDelete(path: String = "", input: String, recoverWith: AnyRef ⇒ AnyRef = { _ ⇒ "" }): Future[Either[JValue, AnyRef]] = {
    apiRequest(DELETE, path, Option(input.getBytes), recoverWith)
  }

  protected def apiRequest(method: HttpMethod, path: String, body: Option[Array[Byte]], recoverWith: AnyRef ⇒ AnyRef): Future[Either[JValue, AnyRef]] = {
    http(method, s"$apiUrl/$path", body, recoverWith)
  }

  protected def http(method: HttpMethod, uri: String, body: Option[Array[Byte]], recoverWith: AnyRef ⇒ AnyRef): Future[Either[JValue, AnyRef]] = {

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
        case failure ⇒ failure
      }.mapAsync(1) {
        case HttpResponse(status, _, entity, _) if status.isSuccess() ⇒ entity.toStrict(timeout).map(_.data.decodeString("UTF-8"))
        case failure ⇒ Future(recoverWith(failure))
      }.map {
        case response: String ⇒ Left(parse(response))
        case other            ⇒ Right(other)
      } runWith Sink.head
  }

  protected def <<[T](jv: JValue)(implicit mf: scala.reflect.Manifest[T]) = jv.extract[T](DefaultFormats, mf)
}

