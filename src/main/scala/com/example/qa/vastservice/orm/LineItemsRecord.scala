package com.example.qa.vastservice.orm

import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._

trait LineItemsRecord {

  this: ConfigurationTables.type =>

  class LineItems(tag:Tag) extends Table[(Int, Int, Int)](tag,"lineitems") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def index = column[Int]("index")

    def exchangeId = column[Int]("exchange_id")

    def * = (id, exchangeId, index)

  }


  class CampaignsToLineItems(tag: Tag) extends Table[(Int,Int)](tag,"line_items_to_campaigns")
  {

    def campaignId = column[Int]("campaign_id")
    def lineItemId = column[Int]("lineitem_id")

    def * = (campaignId, lineItemId)

  }

}
