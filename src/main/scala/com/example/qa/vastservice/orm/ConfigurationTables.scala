package com.example.qa.vastservice.orm

import slick.jdbc.MySQLProfile.api._

object ConfigurationTables extends PublishersRecord
                              with ExchangesRecord
                              with CampaignsRecord
                              with CountriesRecord
                              with LineItemsRecord
                              with UsersRecord
{

  lazy val publishers = TableQuery[Publishers]

  lazy val exchanges = TableQuery[Exchanges]

  lazy val lineItems = TableQuery[LineItems]

  lazy val publishersToExchanges = TableQuery[PublishersToExchanges]

  lazy val publishersToCountries = TableQuery[PublishersToCountries]

  lazy val users = TableQuery[Users]

  lazy val campaigns = TableQuery[Campaigns]

  lazy val countries = TableQuery[Countries]

  lazy val campaignsToCountries = TableQuery[CampaignsToCountries]

  lazy val campaignsToLineItems = TableQuery[CampaignsToLineItems]

}
