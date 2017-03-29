package com.example.qa.vastservice.orm

import java.sql.{Clob, Timestamp}

import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax._
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._


trait PublishersRecord { this: ConfigurationTables.type =>
  object Publishers {
    type TableType = Int :: String :: String :: Int ::
      String :: String :: String :: BigDecimal :: Int :: Int ::
      BigDecimal :: BigDecimal :: Clob :: Clob :: Timestamp ::
      HNil
  }

  class Publishers(tag: Tag) extends Table[Publishers.TableType](tag, "publisher") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def status = column[String]("status")

    def userId = column[Int]("user_id")

    def description = column[String]("description")

    def directionType = column[String]("type")

    def paymentType = column[String]("payment_type")

    def cpm = column[BigDecimal]("cpm")

    def margin = column[Int]("margin")

    def discount = column[Int]("discount")

    def marketFloorPrice = column[BigDecimal]("open_floor_cpm")

    def clientFloorPrice = column[BigDecimal]("client_floor_cpm")

    def blackList = column[Clob]("black_list")

    def whiteList = column[Clob]("white_list")

    def updatedAt = column[Timestamp]("updated_at")

    def * = id ::
      name ::
      status ::
      userId ::
      description ::
      directionType ::
      paymentType ::
      cpm ::
      margin ::
      discount ::
      marketFloorPrice ::
      clientFloorPrice ::
      blackList ::
      whiteList ::
      updatedAt ::
      HNil

    def create(name: String) {
      (publishers.map(a => a.name) returning publishers.map(_.id)) += name
    }
  }
}
