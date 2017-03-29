package com.example.qa

import akka.http.scaladsl.model.StatusCodes._
import org.scalatest._
import org.scalatest.concurrent.Eventually

import scala.language.{implicitConversions, postfixOps}

class MatchResponseSmokeTest extends FunSuiteLike with CommonVals with Matchers with Eventually {
  test("/match response for static publishers", Tag("smoke")) {
    List(desktopPub, mobilePub, smartTvPub).foreach { publisher =>
      val response = H(U.search(publisher))
      response.status shouldEqual OK
      response.body should include ("Lorem ipsum")
      (response.vast \\ "Ad") should have length 1
      (response.vast \\ "Category").text shouldEqual "IAB1-1"
      (response.vast \\ "Duration").text should startWith("00:15")
    }
  }
}

class StatsSmokeTest extends fixture.FunSuiteLike with TestUtils {

  case class FixtureParam(data: Seq[(String, Int)])

  def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(testFixture))
  }

  val staticEntities = Seq((desktop, desktopPub), (mobile, mobilePub), (smartTv, smartTvPub))
  val testFixture = FixtureParam(staticEntities)

  override def beforeEach() {
    testFixture.data.foreach {
      case (_, publisher) => flushMongoStats(publisher)
    }
  }

  override def afterAll() {
    testFixture.data.foreach {
      case (_, publisher) => flushMongoStats(publisher)
    }
  }

  test("requests stats", Tag("smoke")) { fixture =>
    fixture.data.foreach {
      case (directionType, publisher) =>
        val vast = H(U.search(publisherId = publisher, ip = usIp)).vast
        (vast \\ "Ad") should have length 1

        eventually {
          val stats = AdRequestsStats(mongoSelector(directionType, publisher))
          stats.total shouldEqual 1
          stats.statuses shouldEqual List(("200", 1))
        }
    }
  }

  test("actions stats", Tag("smoke")) { fixture =>
    fixture.data.foreach {
      case (directionType, publisher) =>
        val vast = H(U.search(publisherId = publisher, ip = usIp)).vast
        (vast \\ "Ad") should have length 1
        Seq("VASTAdTagURI", "Error", "Impression", "ClickTracking").map(action => H(vast \\ action))
        Seq("start", "midpoint", "firstQuartile", "thirdQuartile", "complete")
          .map(event => (vast \\ "Tracking").filter(e => (e \ "@event").text == event))

        eventually {
          val stats = ActionsStats(directionType, publisher)
          stats.adRequests shouldEqual 1
          stats.adRequestsInline shouldEqual 1
          stats.adDelivery shouldEqual 1
          stats.impressions shouldEqual 1
          stats.clicks shouldEqual 1
          stats.errors shouldEqual 1
          stats.quartiles shouldEqual ViewProgress(1, 1, 1, 1, 1)
        }
    }
  }
}
