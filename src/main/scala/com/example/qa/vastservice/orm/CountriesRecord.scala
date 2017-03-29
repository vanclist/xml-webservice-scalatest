package com.example.qa.vastservice.orm

import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._


trait CountriesRecord {

  this: ConfigurationTables.type =>

  class Countries(tag: Tag) extends Table[(Int, String)](tag, "countries") {
    def id = column[Int]("country_id")

    def country = column[String]("country")

    def * = (id, country)
  }

  class CampaignsToCountries(tag: Tag) extends Table[(Int, Int)](tag, "campaign_countries") {
    def campaignId = column[Int]("campaign_id")

    def countryId = column[Int]("country_id")

    def * = (campaignId, countryId)

    def campaignFk = foreignKey("campaigns_fk", campaignId, campaigns)(_.id)
  }

  class PublishersToCountries(tag: Tag) extends Table[(Int, Int)](tag, "publisher_countries") {
    def publisherId = column[Int]("publisher_id")

    def countryId = column[Int]("country_id")

    def * = (publisherId, countryId)
  }
}
