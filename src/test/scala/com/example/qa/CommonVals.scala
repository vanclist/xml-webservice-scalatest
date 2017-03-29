package com.example.qa

import java.sql.{Blob, Clob, Date, Timestamp}
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}

import akka.http.scaladsl.model.Uri.Host
import com.typesafe.scalalogging.StrictLogging
import com.example.qa.vastservice.model.{TestData, UriParams}
import com.example.qa.vastservice.orm.ConfigurationTables._
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest._
import reactivemongo.bson.{BSONDocument, BSONInteger, BSONLong, BSONString}
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax.{HNil => _}
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}


trait CommonVals extends HttpClientFixture with UriBuilderFixture {
  implicit val uriParams = UriParams(
    ConfigLoader.vastServiceScheme,
    Host(ConfigLoader.vastServiceHost),
    ConfigLoader.vastServicePort
  )
  val U = UriBuilder
  val H = HttpClient
  val settingsDb = Database.forConfig("app.inventory.settingsdb")
  val settingsReloadLatency = Integer.getInteger("cacheReloadTimeoutMs", ConfigLoader.cacheReloadTimeout).toLong
  val timeShift = Integer.getInteger("timeShiftSeconds", ConfigLoader.timeShiftSeconds)
  val statsQueryTimeout = Integer.getInteger("mongodbQueryTimeout", ConfigLoader.mongodbQueryTimeout).toLong seconds
  val confDbQueryTimeout = Integer.getInteger("confDbQueryTimeout", ConfigLoader.confDbQueryTimeout).toLong seconds
  val random = scala.util.Random
  val internalClientId = 0
  val externalClientId = 1
  val adminGroup = 0
  val secondPrice = "second_price"
  val fixedPrice = "fix_cpm"
  val desktop = "display"
  val mobile = "mobile_app"
  val smartTv = "smart_tv"
  val jmIp = "63.143.64.1"
  val jmId = 42
  val usIp = "8.8.8.8"
  val usId = 1337
  val desktopPub = 314
  val mobilePub = 1618
  val smartTvPub = 2718
}

trait TestUtils extends CommonVals with fixture.FunSuiteLike with Matchers with Eventually with IntegrationPatience with BeforeAndAfterAll with BeforeAndAfterEach
  with HttpClientFixture with MongoDbFixture with StrictLogging {

  implicit def intTimes(i: Int) = new {
    def times(fn: => Unit) = (1 to i) foreach (x => fn)
  }

  def emptyClob: Clob = settingsDb.createSession().conn.createClob()
  def emptyBlob: Blob = settingsDb.createSession().conn.createBlob()
  def currentDate: Date = new java.sql.Date(System.currentTimeMillis)
  def nextYearDate: Date = new java.sql.Date(
    LocalDateTime
      .now()
      .plusYears(1L)
      .atZone(ZoneId.systemDefault())
      .toEpochSecond * 1000
  )
  def currentTimestamp: Timestamp = new java.sql.Timestamp(System.currentTimeMillis + timeShift * 1000)
  def truncatedTimestamp: Long = LocalDateTime
    .now()
    .atZone(ZoneId.systemDefault())
    .truncatedTo(ChronoUnit.HOURS)
    .toEpochSecond

  val inlineVastClob = emptyClob
  inlineVastClob.setString(1L, U.staticInLineVast.toString)

  val wrapperVastClob = emptyClob
  wrapperVastClob.setString(1L, U.staticWrapperVast.toString)

  def errorText(vast: scala.xml.Elem): String = (vast \\ "Error").text.replaceAll("\n", " ").trim

  def dbRun[R](query: DBIOAction[R, NoStream, Nothing]): Future[R] = {
    settingsDb.run(query)
  }

  def dbAwait[R](query: DBIOAction[R, NoStream, Nothing], timeout: FiniteDuration = confDbQueryTimeout): R = {
    Await.result(dbRun(query), timeout)
  }

  /* Queries for essential DB entities creation */

  def publisherQuery(userId: Int, status: String = "enabled", directionType: String = mobile,
                     paymentType: String = secondPrice, cpm: BigDecimal = 1.0,
                     discount: Int = 100, margin: Int = 100,
                     marketFloorPrice: BigDecimal = 0.0, clientFloorPrice: BigDecimal = 0.0,
                     blackList: Clob = emptyClob, whiteList: Clob = emptyClob) =
    (publishers returning publishers.map(_.id)) +=
      0 :: "automated test" :: status :: userId :: "test description" :: directionType :: paymentType :: cpm ::
        discount :: margin :: marketFloorPrice :: clientFloorPrice ::
        blackList :: whiteList :: currentTimestamp :: HNil

  def exchangeQuery(directionType: String = mobile, isServer: Boolean = false, isAdmin: Boolean = false,
                    ignoreDiscount: Boolean = false, isCdn: Boolean = true) =
    (exchanges.map(
      e => (
        e.name, e.directionType, e.whiteList, e.blackList, e.enabled, e.ignoreDiscount, e.isCdn, e.isAdmin, e.updatedAt
        )) returning exchanges.map(_.id)) +=
      ("automated test", directionType, emptyClob, emptyClob, 1, ignoreDiscount, isCdn, isAdmin, currentTimestamp)

  def publisherToExchangesQuery(publisher: Int, exchange: Int) = publishersToExchanges += (publisher, exchange)

  def userQuery(randomInt: Int, clientId: Int = internalClientId, groupId: Int = adminGroup) =
    (users returning users.map(_.id)) +=
      0 :: clientId :: groupId :: "automated" :: "test" :: s"$randomInt@example.com" :: HNil

  def campaignQuery(userId: Int, status: String = "enabled", directionType: String = desktop, fixCPM: BigDecimal = 0.5,
                    vastUrl: Clob = inlineVastClob, blackDomains: Clob = emptyClob, whiteDomains: Clob = emptyClob) =
    (campaigns returning campaigns.map(_.id)) +=
      0 :: "automated test" :: status :: userId :: directionType :: fixCPM :: vastUrl :: blackDomains ::
        whiteDomains :: currentTimestamp :: HNil

  def lineItemsQuery(exchangeId: Int) =
    (lineItems.map(t => (t.exchangeId, t.index)) returning lineItems.map(_.id)) += (exchangeId, 0)

  def campaignToLineItemQuery(campaignId: Int, lineItemId: Int) = campaignsToLineItems += (campaignId, lineItemId)

  def campaignToCountryQuery(campaignId: Int, countryId: Int) = campaignsToCountries += (campaignId, countryId)

  /* Auxiliary DB queries */

  def userClientIdQuery(user: Int) = for {u <- users if u.id === user} yield u.clientId

  def exchangeIsCdnQuery(exchangeId: Int) = for {e <- exchanges if e.id === exchangeId} yield (e.isCdn, e.updatedAt)

  def exchangeIgnoreDiscountQuery(exchange: Int) = for {e <- exchanges if e.id === exchange} yield (e.ignoreDiscount, e.updatedAt)

  def campaignVastUrlQuery(campaignId: Int) = for {c <- campaigns if c.id === campaignId} yield (c.vastUrl, c.updatedAt)

  def campaignUpdatedAtQuery(campaign: Int) = for {c <- campaigns if c.id === campaign} yield c.updatedAt

  def cleanUpConfDb(data: TestData) = {
    Seq(
      campaignsToCountries.filter(_.campaignId inSet data.campaigns).delete,
      campaignsToLineItems.filter(_.lineItemId inSet data.lineItems).delete,
      lineItems.filter(_.id inSet data.lineItems).delete,
      campaigns.filter(_.id inSet data.campaigns).delete,
      publishersToExchanges.filter(_.publisherId === data.publisher).delete,
      exchanges.filter(_.id inSet data.exchanges).delete,
      publishers.filter(_.id === data.publisher).delete,
      users.filter(_.id === data.user).delete
    ).foreach(dbAwait(_))
  }

  /* MongoDB helpers */

  def mongoSelector(directionType: String = desktop, publisher: Int, sid: Option[String] = None,
                    domain: Option[String] = Some("site.url"), appName: Option[String] = Some("app.name"),
                    appBundle: Option[String] = Some("app.bundle"), campaign: Option[Int] = None): BSONDocument = {

    var doc = BSONDocument(
      "publisherId" -> publisher,
      "referer" -> sid.getOrElse(publisher.toString),
      "timestamp" -> truncatedTimestamp
    )

    doc = doc.merge(
      directionType match {
        case `mobile` | `smartTv` =>
          BSONDocument(
            "domain" -> None,
            "appName" -> appName,
            "appBundle" -> appBundle
          )
        case `desktop` =>
          BSONDocument(
            "domain" -> domain,
            "appName" -> None,
            "appBundle" -> None
          )
      }
    )

    campaign match {
      case Some(c) => doc.merge("cmp" -> c)
      case _ => doc
    }
  }

  object AdRequestsStats {
    def apply(doc: BSONDocument) = new AdRequestsStats(getCollection(doc, MongoDb.currentAdRequests))
    def apply(directionType: String, publisher: Int) = {
      val doc = mongoSelector(directionType, publisher)
      new AdRequestsStats(getCollection(doc, MongoDb.currentAdRequests))
    }
  }

  class AdRequestsStats(bson: BSONDocument) {
    val publisher = bson.getAs[Int]("publisherId")
    val referer = bson.getAs[String]("referer").getOrElse("")
    val timestamp = getAsLong(bson, "timestamp")
    val domain = bson.getAs[String]("domain")
    val appBundle = bson.get("appBundle").map(v => v.asInstanceOf[BSONString].value)
    val appName = bson.getAs[String]("appName")
    val country = bson.getAs[String]("country")
    val statuses = getStatuses(bson)
    val total = getAsLong(bson, "total")
  }

  object ActionsStats {
    def apply(doc: BSONDocument) = new ActionsStats(getCollection(doc, MongoDb.currentActions))
    def apply(directionType: String, publisher: Int) = {
      val doc = mongoSelector(directionType, publisher)
      new ActionsStats(getCollection(doc, MongoDb.currentActions))
    }
  }

  class ActionsStats(bson: BSONDocument) {
    val publisher = bson.getAs[Int]("publisherId")
    val referer = bson.getAs[String]("referer").getOrElse("")
    val timestamp = getAsLong(bson, "timestamp")
    val domain = bson.getAs[String]("domain")
    val appName = bson.getAs[String]("appName")
    val appBundle = bson.getAs[String]("appBundle")
    val country = bson.getAs[String]("country")
    val adRequests = getAsLong(bson, "adRequests")
    val adRequestsInline = getAsLong(bson, "adRequestsInline")
    val adRequestsWrapper = getAsLong(bson, "adRequestsWrapper")
    val adDelivery = getAsLong(bson, "adDelivery")
    val impressions = getAsLong(bson, "impressions")
    val spent = getAsLong(bson, "spent")
    val grossRevenue = getAsLong(bson, "grossRevenue")
    val margin = getAsLong(bson, "margin")
    val netRevenue = getAsLong(bson, "netRevenue")
    val clicks = getAsLong(bson, "clicks")
    val errors = getAsLong(bson, "error")
    val quartiles = ViewProgress(
      getAsLong(bson, "trackingStarted"),
      getAsLong(bson, "trackingFirstQuartile"),
      getAsLong(bson, "trackingMidpoint"),
      getAsLong(bson, "trackingThirdQuartile"),
      getAsLong(bson, "trackingCompleted")
    )
  }

  private def getCollection(doc: BSONDocument, collection: String): BSONDocument = {
    Await.result(MongoDb.getCollection(collection).find(doc).one[BSONDocument], statsQueryTimeout).get
  }

  private def getStatuses(doc: BSONDocument): List[(String, Long)] = {
    doc.getAs[BSONDocument]("statuses")
      .map(v => v.elements.toList).get
      .map(s => (s.name, s.value.asInstanceOf[BSONLong].value))
  }

  private def getAsLong(bson: BSONDocument, key: String): Long = bson.getAs[Long](key).get

  def flushMongoStats(publisher: Int) = {
    eventually {
      for (collection <- Seq(MongoDb.currentAdRequests, MongoDb.currentActions)) {
        val doc = Await.result(
          MongoDb.getCollection(collection).remove(
            BSONDocument("publisherId" -> BSONInteger(publisher))
          ), statsQueryTimeout)
        doc.ok shouldBe true
      }
    }
  }
}
