package com.example.qa

import com.example.qa.vastservice.model.UriParams
import akka.http.scaladsl.model.Uri
import Uri._

trait UriBuilderFixture {

  implicit val uriParams: UriParams

  object UriBuilder {

    def baseUri(implicit params: UriParams) = Uri().withScheme(params.scheme).withHost(params.host).withPort(params.port)

    val stats = baseUri.withPath(Path("/stats"))

    val staticWrapperVast = baseUri.withPath(Path("/wrapper.xml"))

    val staticInLineVast = baseUri.withPath(Path("/inline.xml"))

    def search(params: Map[String, String]) = baseUri.withPath(Path("/search")).withQuery(Query(params))

    def search(publisherId: Int, referer: Option[String] = None, width: Int = 320, height: Int = 480,
               ua: String = "user.agent", ip: String = "8.8.8.8", bidFloor: Double = 5, adCount: Int = 1) = {
      baseUri.withPath(Path("/search")).withQuery(
        Query(
          Map(
            "publisher_id" -> publisherId.toString, "referer" -> referer.getOrElse(publisherId.toString),
            "width" -> width.toString, "height" -> height.toString,
            "ua" -> ua, "user_ip" -> ip, "bid_floor" -> bidFloor.toString ,"ad_count" -> adCount.toString
          )
        )
      )
    }
  }
}
