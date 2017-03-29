package com.example.qa.vastservice.model

import com.example.qa.vastservice.orm.ConfigurationTables.Users

case class User(id: Int, clientId: Int)

object User {

  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._

  def fromDb(params: Users.UHList): User = {
    params match {
      case id :: clientId :: groupId :: firstName :: lastName :: email :: HNil =>
        User(id = id, clientId = clientId)
    }
  }
}