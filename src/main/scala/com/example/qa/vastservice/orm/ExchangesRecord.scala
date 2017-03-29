package com.example.qa.vastservice.orm

import java.sql.{Clob, Timestamp}

import com.example.qa.vastservice.orm.ExchangesRecord.ExType
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlProfile.ColumnOption.{NotNull, SqlType}


object ExchangesRecord {
  type ExType = (Int, String, String, Clob, Clob, Int, Boolean, Boolean, Boolean, Timestamp)
}

trait ExchangesRecord {

  this: ConfigurationTables.type =>

  class Exchanges(tag: Tag) extends Table[ExType](tag, "exchanges") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def directionType = column[String]("type")

    def name = column[String]("name")

    def blackList = column[Clob]("black_list")

    def whiteList = column[Clob]("white_list")

    def enabled = column[Int]("enable")

    def isAdmin = column[Boolean]("is_admin")

    def ignoreDiscount = column[Boolean](
      "ignore_discount",
      SqlType("tinyint(1)"), NotNull
    )(
      MappedColumnType.base[Boolean, Int](
        scalaValue => if (scalaValue) 1 else 0,
        sqlValue => sqlValue != 0
      )
    )

    def isCdn = column[Boolean](
      "environment_type",
      SqlType("enum('bare_metal', 'cdn')")
    )(
      MappedColumnType.base[Boolean, String](
        isCdn => if (isCdn) "cdn" else "bare_metal",
        str => str == "cdn"
      )
    )

    def updatedAt = column[Timestamp]("updated_at")

    def * = (
      id,
      directionType,
      name,
      blackList,
      whiteList,
      enabled,
      isAdmin,
      isCdn,
      ignoreDiscount,
      updatedAt
    )
  }

  class PublishersToExchanges(tag: Tag) extends Table[(Int, Int)](tag, "publishers_to_exchanges") {
    def publisherId = column[Int]("publisher_id")

    def exchangeId = column[Int]("exchange_id")

    def * = (publisherId, exchangeId)

    def exchangeFk = foreignKey("exchanges_fk", exchangeId, exchanges)(_.id)
  }
}
