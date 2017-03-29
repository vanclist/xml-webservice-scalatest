package com.example.qa

import akka.http.scaladsl.model.Uri
import com.example.qa.vastservice.model.TestData
import org.scalatest._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ExchangeTypeTest extends fixture.FunSuiteLike with TestUtils {

  case class FixtureParam(data: TestData)

  def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(testFixture))
  }

  val testFixture = FixtureParam(genTestData)

  def genTestData: TestData = {
    val data = for {
      user <- dbRun(userQuery(random.nextInt))
      publisher <- dbRun(publisherQuery(userId = user, directionType = mobile))
      exchange <- dbRun(exchangeQuery(isServer = true))
      _ <- dbRun(publisherToExchangesQuery(publisher, exchange))
      entities <- Future.sequence(
        (1 to 2).map(_ =>
          for {
            campaign <- dbRun(campaignQuery(userId = user, directionType = mobile, vastUrl = wrapperVastClob))
            lineItem <- dbRun(lineItemsQuery(exchange))
            _ <- dbRun(campaignToLineItemQuery(campaign, lineItem))
            _ <- dbRun(campaignToCountryQuery(campaign, jmId))
          } yield campaign -> lineItem
        ))
    } yield {
      TestData(user, publisher, List(exchange), entities.map(_._1).toList, entities.map(_._2).toList)
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

  test("CDN exchange: single campaign with wrapper VAST") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, adCount = 1)).vast
    H(vast \\ "VASTAdTagURI")

    eventually {
      val stats = AdRequestsStats(mobile, fixture.data.publisher)
      stats.total shouldEqual 1
      stats.statuses shouldEqual List(("200", 1))
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.head)))
      stats.adDelivery shouldEqual 0
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 0
      stats.adRequestsWrapper shouldEqual 1
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.last)))
      stats.adDelivery shouldEqual 1
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 0
      stats.adRequestsWrapper shouldEqual 1
    }
  }

  test("CDN exchange: multiple campaigns with wrapper VASTs") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, adCount = 2)).vast
    (vast \\ "VASTAdTagURI").foreach(adOpUrl => H(adOpUrl))

    eventually {
      val stats = AdRequestsStats(mobile, fixture.data.publisher)
      stats.total shouldEqual 1
      stats.statuses shouldEqual List(("200", 1))
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.head)))
      stats.adDelivery shouldEqual 1
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 0
      stats.adRequestsWrapper shouldEqual 1
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.last)))
      stats.adDelivery shouldEqual 1
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 0
      stats.adRequestsWrapper shouldEqual 1
    }
  }

  test("CDN exchange: wrapper campaign + inline campaign") { fixture =>
    dbAwait(campaignVastUrlQuery(fixture.data.campaigns.last).update(inlineVastClob, currentTimestamp))
    JMXClient.reloadCache(settingsReloadLatency)

    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, adCount = 2)).vast
    (vast \\ "VASTAdTagURI").foreach(H(_))

    eventually {
      val stats = AdRequestsStats(mobile, fixture.data.publisher)
      stats.total shouldEqual 1
      stats.statuses shouldEqual List(("200", 1))
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.head)))
      stats.adDelivery shouldEqual 1
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 0
      stats.adRequestsWrapper shouldEqual 1
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.last)))
      stats.adDelivery shouldEqual 1
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 1
      stats.adRequestsWrapper shouldEqual 0
    }
  }

  test("CDN exchange: incorrect hash in ad opportunity URL") { f =>
    val vast = H(U.search(publisherId = f.data.publisher, ip = jmIp, adCount = 2)).vast
    (vast \\ "VASTAdTagURI").foreach(adOpUrl =>
      H(adOpUrl.text + "foo").vast shouldEqual <VAST version="2.0" error="incorrect id"/>
    )

    f.data.campaigns.foreach {cmp =>
      eventually {
        val stats = ActionsStats(mongoSelector(mobile, f.data.publisher, campaign = Some(cmp)))
        stats.adDelivery shouldEqual 1
      }
    }
  }

  test("Bare-metal exchange exchange: wrapper campaign + inline campaign") { fixture =>
    dbAwait(exchangeIsCdnQuery(fixture.data.exchanges.last).update(false, currentTimestamp))
    JMXClient.reloadCache(settingsReloadLatency)

    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, adCount = 2)).vast
    (vast \\ "VASTAdTagURI").foreach(H(_))

    eventually {
      val stats = AdRequestsStats(mobile, fixture.data.publisher)
      stats.total shouldEqual 1
      stats.statuses shouldEqual List(("200", 1))
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.head)))
      stats.adDelivery shouldEqual 1
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 1
      stats.adRequestsWrapper shouldEqual 0
    }

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.last)))
      stats.adDelivery shouldEqual 1
      stats.adRequests shouldEqual 1
      stats.adRequestsInline shouldEqual 1
      stats.adRequestsWrapper shouldEqual 0
    }
  }

  test("Bare-metal exchange: invalid hash in ad opportunity URL, single campaign") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, adCount = 1)).vast
    val adOpUrl = vast \\ "VASTAdTagURI"
    val u = Uri(adOpUrl.text)
    H(u.withPath(u.path.reverse.tail.reverse + "foo" + u.path.reverse.head.toString).toString)

    eventually {
      val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(fixture.data.campaigns.last)))
      stats.adDelivery shouldEqual 1
    }
  }

  test("Bare-metal exchange: invalid hash in ad opportunity URL, multiple campaigns") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, adCount = 2)).vast
    (vast \\ "VASTAdTagURI").foreach { adOpUrl =>
      val u = Uri(adOpUrl.text)
      H(u.withPath(u.path.reverse.tail.reverse + "foo" + u.path.reverse.head.toString).toString)
    }

    fixture.data.campaigns.foreach {
      cmp =>
        eventually {
          val stats = ActionsStats(mongoSelector(mobile, fixture.data.publisher, campaign = Some(cmp)))
          stats.adDelivery shouldEqual 1
        }
    }
  }
}
