package com.example.qa

import com.example.qa.vastservice.model.TestData
import com.example.qa.vastservice.orm.ConfigurationTables._
import org.scalatest._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class FraudProofTest extends fixture.FunSuiteLike with TestUtils {

  case class FixtureParam(data: TestData)

  def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(testFixture))
  }

  val testFixture = FixtureParam(genTestData)

  def genTestData: TestData = {
    val data = for {
      user <- dbRun(userQuery(random.nextInt))
      publisher <- dbRun(publisherQuery(user))
      exchange <- dbRun(exchangeQuery())
      _ <- dbRun(publisherToExchangesQuery(publisher, exchange))
      lineItem <- dbRun(lineItemsQuery(exchange))
      campaign <- dbRun(campaignQuery(user))
      _ <- dbRun(campaignToLineItemQuery(campaign, lineItem))
      _ <- dbRun(campaignToCountryQuery(campaign, jmId))
    } yield {
      TestData(user, publisher, List(exchange), List(campaign), List(lineItem))
    }

    Await.result(data, confDbQueryTimeout)
  }

  override def beforeAll() {
    JMXClient.reloadCache(settingsReloadLatency)
  }

  override def afterEach() {
    flushMongoStats(testFixture.data.publisher)
  }

  override def afterAll() {
    cleanUpConfDb(testFixture.data)
  }

  test("ad opportunity fraud proof") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    2 times H(vast \\ "VASTAdTagURI")

    eventually {
      val stats = ActionsStats(desktop, fixture.data.publisher)
      stats.adDelivery shouldEqual 1
    }
  }

  test("impression fraud proof") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    2 times H(vast \\ "Impression")

    eventually {
      val stats = ActionsStats(desktop, fixture.data.publisher)
      stats.impressions shouldEqual 1
    }
  }

  test("click fraud proof") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    2 times H(vast \\ "ClickTracking")

    eventually {
      val stats = ActionsStats(desktop, fixture.data.publisher)
      stats.clicks shouldEqual 1
    }
  }

  test("tracking event fraud proof: ad started") { fixture =>
    /** By VAST spec, tracking event URLs are placed under <TrackingEvents> node sequence, i.e.:
    <Linear>
      <TrackingEvents>
        <Tracking event="start"><![CDATA[http://example.com/tracking/start?id=hash2&data=]]></Tracking>
        <Tracking event="midpoint"><![CDATA[http://example.com/tracking/midpoint?id=hash2&data=]]></Tracking>
        <Tracking event="firstQuartile"><![CDATA[http://example.com/tracking/firstQuartile?id=hash2&data=]]></Tracking>
        <Tracking event="thirdQuartile"><![CDATA[http://example.com/tracking/thirdQuartile?id=hash2&data=]]></Tracking>
        <Tracking event="complete"><![CDATA[http://example.com/tracking/complete?id=hash2&data=]]></Tracking>
      </TrackingEvents>
    </Linear>
    */

    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    val startedUrl = (vast \\ "Tracking").filter(e => (e \ "@event").text == "started").text
    2 times H(startedUrl)

    eventually {
      val stats = ActionsStats(desktop, fixture.data.publisher)
      stats.quartiles shouldEqual ViewProgress(1, 0, 0, 0, 0)
    }
  }

  test("tracking event fraud proof: ad completed") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    val completeUrl = (vast \\ "Tracking").filter(e => (e \ "@event").text == "complete").text
    2 times H(completeUrl)

    eventually {
      val stats = ActionsStats(desktop, fixture.data.publisher)
      stats.quartiles shouldEqual ViewProgress(0, 0, 0, 0, 1)
    }
  }
}
