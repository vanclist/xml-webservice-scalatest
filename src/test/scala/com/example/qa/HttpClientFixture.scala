package com.example.qa

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.xml.{Elem, NodeSeq, XML}

trait HttpClientFixture extends StrictLogging {
  object HttpClient {
    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer()

    def apply(uri: Uri) = new Response(response(uri))

    def apply(uri: String): Response = apply(Uri(uri))

    def apply(url: NodeSeq): Response = apply(Uri(url.text))

    def response(uri: Uri) = {
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uri))
      responseFuture.onComplete {
        case Success(res) => logger.debug(s"Got response for $uri")
        case Failure(err) => logger.warn(s"Unable to get response for $uri", err)
      }
      Await.result(responseFuture, 10 seconds)
    }

    def content(response: HttpResponse): String = {
      Await.result(response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.utf8String), 10 seconds)
    }

    class Response(response: HttpResponse) {
      val body: String = content(response)
      val vast: Elem = XML.loadString(body)
      val status: StatusCode = response.status
    }
  }
}
