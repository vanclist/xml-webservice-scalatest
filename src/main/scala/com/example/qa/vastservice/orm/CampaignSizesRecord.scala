package com.example.qa.vastservice.orm

import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag
import slick.relational.RelationalProfile.ColumnOption.Default


trait CampaignSizesRecord {
  this: ConfigurationTables.type =>

  class CampaignToSizes(tag: Tag) extends Table[(Int, Int, Int, Int, Int)](tag, "campaigns_to_player_sizes") {
    def campaignId = column[Int]("campaign_id")

    def minWidth = column[Int]("width_min", Default(0))
    def maxWidth = column[Int]("width_max", Default(1 << 16))
    def minHeight = column[Int]("height_min", Default(0))
    def maxHeight = column[Int]("height_max", Default(1 << 16))

    def * = (campaignId, minWidth, maxWidth, minHeight, maxHeight)

    def cmpFk = foreignKey("campaigns_to_player_sizes_idfk_1", campaignId, campaigns)(_.id)
  }
}
