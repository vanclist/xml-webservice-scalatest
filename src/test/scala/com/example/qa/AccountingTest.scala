package com.example.qa

import com.example.qa.vastservice.model.{SearchError, TestData}
import com.example.qa.vastservice.orm.ConfigurationTables._
import reactivemongo.bson._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class SecondPricePaymentTypeTest extends TestUtils {

  case class FixtureParam(data: TestData)

  def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(testFixture))
  }

  val testFixture = FixtureParam(genTestData)

  def genTestData: TestData = {
    val data = for {
      user <- dbRun(userQuery(randomInt = random.nextInt, clientId = externalClientId))
      publisher <- dbRun(publisherQuery(userId = user, paymentType = secondPrice))
      exchange <- dbRun(exchangeQuery())
      _ <- dbRun(publisherToExchangesQuery(publisher, exchange))
      campaign <- dbRun(campaignQuery(userId = user, fixCPM = 1.0))
      lineItem <- dbRun(lineItemsQuery(exchange))
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
    eventually {
      for (collection <- Seq(MongoDb.currentAdRequests, MongoDb.currentActions)) {
        val doc = Await.result(
          MongoDb.getCollection(collection).remove(
            BSONDocument("publisherId" -> BSONInteger(testFixture.data.publisher))
          ), statsQueryTimeout)
        doc.ok shouldBe true
      }
    }
  }

  override def afterAll() {
    cleanUpConfDb(testFixture.data)
  }

  test("Second price payment type: price filter is passed for default query params") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    H(vast \\ "Impression")

    eventually {
      val stats = ActionsStats(mobile, fixture.data.publisher)
      stats.spent shouldEqual 0
      stats.grossRevenue shouldEqual 5
      stats.margin shouldEqual 0
      stats.netRevenue shouldEqual 10
    }
  }

  test("Second price payment type: price value is replaced by 'bid_floor' query param") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, bidFloor = 5.0)).vast
    errorText(vast) shouldEqual SearchError.noResults
  }
}

class FixedPricePaymentTypeTest extends TestUtils {

  case class FixtureParam(data: TestData)

  def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(testFixture))
  }

  val testFixture = FixtureParam(genTestData)

  def genTestData: TestData = {
    // take attention on user.clientId
    val entities = for {
      user <- dbRun(userQuery(randomInt = random.nextInt, clientId = internalClientId))
      publisher <- dbRun(
        publisherQuery(
          userId = user, paymentType = fixedPrice, cpm = 5.0, directionType = mobile,
          discount = 10, margin = 20, marketFloorPrice = 10.0
        )
      )
      exchange <- dbRun(exchangeQuery())
      _ <- dbRun(publisherToExchangesQuery(publisher, exchange))
      lineItem <- dbRun(lineItemsQuery(exchange))
      campaign <- dbRun(campaignQuery(fixCPM = 10.0, userId = user, directionType = mobile))
      _ <- dbRun(campaignToLineItemQuery(campaign, lineItem))
      _ <- dbRun(campaignToCountryQuery(campaign, jmId))
    } yield {
      TestData(user, publisher, List(exchange), List(campaign), List(lineItem))
    }

    Await.result(entities, confDbQueryTimeout)
  }

  override def beforeAll() {
    JMXClient.reloadCache(settingsReloadLatency)
  }

  override def afterEach() {
    eventually {
      for (collection <- Seq(MongoDb.currentAdRequests, MongoDb.currentActions)) {
        val doc = Await.result(
          MongoDb.getCollection(collection).remove(
            BSONDocument("publisherId" -> BSONInteger(testFixture.data.publisher))
          ), statsQueryTimeout)
        doc.ok shouldBe true
      }
    }
  }

  override def afterAll() {
    cleanUpConfDb(testFixture.data)
  }

  test("Campaign should be filtered by price if publisher's clientId is internal client") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    errorText(vast) shouldEqual SearchError.noResults
  }

  test("Campaign should not be filtered by price with if exchange's ignoreDiscount = true & clientId is internal") { fixture =>
    dbAwait(exchangeIgnoreDiscountQuery(fixture.data.exchanges.head).update(true, currentTimestamp))
    JMXClient.reloadCache(settingsReloadLatency)

    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    H(vast \\ "Impression")

    eventually {
      val stats = ActionsStats(mobile, fixture.data.publisher)
      stats.spent shouldEqual 100
      stats.grossRevenue shouldEqual 100
      stats.margin shouldEqual 20
      stats.netRevenue shouldEqual 20
    }
  }

  test("Campaign should not be filtered by price with if exchange's ignoreDiscount = true & clientId is external") { fixture =>
    dbAwait(userClientIdQuery(fixture.data.user).update(externalClientId))
    JMXClient.reloadCache(settingsReloadLatency)

    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp)).vast
    H(vast \\ "Impression")

    eventually {
      val stats = ActionsStats(mobile, fixture.data.publisher)
      stats.spent shouldEqual 100
      stats.grossRevenue shouldEqual 50
      stats.margin shouldEqual 20
      stats.netRevenue shouldEqual 20
    }
  }

  test("Query param 'bid_floor' should be ignored") { fixture =>
    val vast = H(U.search(publisherId = fixture.data.publisher, ip = jmIp, bidFloor = 99.9)).vast
    H(vast \\ "Impression")

    eventually {
      val stats = ActionsStats(mobile, fixture.data.publisher)
      stats.spent shouldEqual 0
      stats.grossRevenue shouldEqual 100
      stats.margin shouldEqual 0
      stats.netRevenue shouldEqual 50
    }
  }
}
