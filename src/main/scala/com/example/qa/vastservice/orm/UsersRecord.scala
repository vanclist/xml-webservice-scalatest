package com.example.qa.vastservice.orm

import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax._
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._


trait UsersRecord {

  this: ConfigurationTables.type =>

  object Users {

    type UHList = Int::Int::Int::String::String::String::HNil
  }

  class Users(tag: Tag) extends Table[Users.UHList](tag, "users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def clientId = column[Int]("client_id")

    def groupId = column[Int]("group_id")

    def firstName = column[String]("first_name")

    def lastName = column[String]("last_name")

    def email = column[String]("email")

    def * = id :: clientId :: groupId :: firstName :: lastName :: email :: HNil
  }
}
