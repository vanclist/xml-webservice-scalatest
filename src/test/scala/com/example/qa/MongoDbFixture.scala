package com.example.qa

trait MongoDbFixture {
  implicit val db = MongoDbProvider.db
  val dbConnection = db.connection
}
