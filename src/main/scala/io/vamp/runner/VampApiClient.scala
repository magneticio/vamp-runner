package io.vamp.runner

import java.io.InputStream

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

  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  implicit def executionContext = system.dispatcher

  protected def apiUrl: String

  protected def timeout: FiniteDuration

  protected def apiGet(path: String, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = apiRequest(GET, path, None, { _ ⇒ throw new RuntimeException("No connection.") })

  protected def apiPut(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    apiRequest(PUT, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  protected def apiPost(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    apiRequest(POST, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  protected def apiDelete(path: String, input: InputStream, recoverWith: AnyRef ⇒ String = { _ ⇒ "" }): Future[JValue] = {
    apiRequest(DELETE, path, Option(scala.io.Source.fromInputStream(input).map(_.toByte).toArray), recoverWith)
  }

  protected def apiRequest(method: HttpMethod, path: String, body: Option[Array[Byte]], recoverWith: AnyRef ⇒ String): Future[JValue] = {
    http(method, s"$apiUrl/$path", body, recoverWith)
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
        case HttpResponse(status, _, entity, _) if status.isSuccess() ⇒ entity.toStrict(timeout).map(_.data.decodeString("UTF-8"))
        case failure ⇒ Future(recoverWith(failure))
      }.map {
        parse(_)
      } runWith Sink.head
  }

  protected def <<[T](jv: JValue)(implicit mf: scala.reflect.Manifest[T]) = jv.extract[T](DefaultFormats, mf)
}

