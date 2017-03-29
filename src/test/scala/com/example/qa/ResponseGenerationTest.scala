package com.example.qa

import java.net.URL

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri.{Path, Query}
import com.example.qa.vastservice.model.SearchError
import com.example.qa.{XmlValidator => V}
import org.scalatest._

import scala.language.postfixOps

class SearchResponseSchemaValidationTest extends FunSuite with CommonVals with Matchers with HttpClientFixture {
  val successfulResponse = H(U.search(publisherId = mobilePub, ip = usIp))
  val errorResponse = H(U.search(publisherId = 0))

  def resource(path: String): URL = getClass.getResource(path)

  test("/search response status code is always 200") {
    successfulResponse.status shouldEqual OK
    errorResponse.status shouldEqual OK
  }

  test("successful search response conforms to VAST 2.0 schema") {
    V.validate(successfulResponse.body, resource("/vast_2.0.1.xsd")) shouldBe true
  }

  test("successful search response conforms to VAST 3.0 schema") {
    V.validate(successfulResponse.body, resource("/vast3_draft.xsd")) shouldBe true
  }

  test("error search response conforms to VAST 2.0 schema") {
    pending
    V.validate(errorResponse.body, resource("/vast_2.0.1.xsd")) shouldBe true
  }

  test("error search response conforms to VAST 3.0 schema") {
    pending
    V.validate(errorResponse.body, resource("/vast3_draft.xsd")) shouldBe true
  }
}

class SearchQueryValidationTest extends FunSuite with CommonVals with Matchers with HttpClientFixture {
  test("error response for missed publisherId query param") {
    val vast = H(U.baseUri.withPath(Path("/search"))).vast
    (vast \\ "Error") shouldEqual SearchError.widthNotPassed
  }

  test("error response for not mapped for publisherId") {
    val publisher = 0
    val vast = H(U.search(publisher)).vast
    (vast \\ "Error") should contain (s"Mapping for 'publisherId = $publisher' was not found.")
  }

  test("error response for missed publisherId value") {
    /* Let's assume that query params parsing order is width -> height -> publisherId */
    val vast = H(U.baseUri.withPath(Path("/search")).withQuery(Query("publisherId=&width=320&height=480"))).vast
    (vast \\ "Error") shouldEqual SearchError.publisherOnlyPositive
  }

  test("error response for non integer publisherId value") {
    val publisher = "@"
    val vast = H(U.baseUri.withPath(Path("/search")).withQuery(Query(s"publisherId=$publisher&width=320&height=480"))).vast
    (vast \\ "Error") shouldEqual SearchError.publisherOnlyPositive
  }

  test("error response for missed width query param") {
    val vast = H(U.baseUri.withPath(Path("/search")).withQuery(Query(s"publisherId=$mobilePub"))).vast
    (vast \\ "Error") shouldEqual SearchError.widthNotPassed
  }

  test("error response for missed width value") {
    val vast = H(U.baseUri.withPath(Path("/search")).withQuery(Query(s"publisherId=$mobilePub&width="))).vast
    (vast \\ "Error") shouldEqual SearchError.widthNotPassed
  }

  test("error response for non-integer width value") {
    val vast = H(U.baseUri.withPath(Path("/search")).withQuery(Query(s"publisherId=$mobilePub&width=some"))).vast
    (vast \\ "Error") shouldEqual SearchError.widthOnlyPositive
  }

  test("error response for non existent IP") {
    /* Subnet 240.0.0.0/4 is "reserved for future use" and country resolves as "ZZ" */
    val ip = "240.0.0.0"
    val vast = H(U.search(publisherId = smartTvPub, ip = ip)).vast
    (vast \\ "Error") shouldEqual SearchError.countryNotAllowed
  }

  test("error response for invalid IP") {
    val ip = "13.37.ip.address"
    val vast = H(U.search(publisherId = mobilePub, ip = ip)).vast
    (vast \\ "Error") shouldEqual SearchError.ipNotValid
  }

  test("%u in user agent") {
    val vast = H(U.search(publisherId = mobilePub, ua = "%u")).vast
    vast shouldEqual <VAST version="2.0"/>
  }
}
