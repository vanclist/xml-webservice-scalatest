package com.example.qa.vastservice.orm

import java.sql.{Clob, Timestamp}

import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax._
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._


trait CampaignsRecord { this: ConfigurationTables.type =>
  object Campaigns {
    type TableType =
      Int :: String :: String :: Int :: String ::
      BigDecimal :: Clob :: Clob :: Clob :: Timestamp :: HNil
  }

  class Campaigns(tag:Tag) extends Table[Campaigns.TableType](tag, "campaigns") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def status = column[String]("status")

    def userId = column[Int]("user_id")

    def directionType = column[String]("type")

    def cpm = column[BigDecimal]("cpm")

    def vastUrl = column[Clob]("vast")

    def blackList = column[Clob]("block_list")

    def whiteList = column[Clob]("white_list")

    def updatedAt = column[Timestamp]("updated_at")

    override def * = id ::
      name ::
      status ::
      userId ::
      directionType ::
      cpm ::
      vastUrl ::
      blackList ::
      whiteList ::
      updatedAt ::
      HNil
  }
}
