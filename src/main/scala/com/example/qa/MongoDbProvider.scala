package com.example.qa

import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object MongoDbProvider {
  implicit val db: DefaultDB = MongoConnection.parseURI(ConfigLoader.mongodbUrl) match {
    case Success(url) =>
      val dbName = url.db.getOrElse(throw new IllegalArgumentException(s"Database is not defined at $url"))
      Await.result(new MongoDriver().connection(url).database(dbName), 10.seconds)

    case Failure(e) =>
      val errorMessage = s"Unable to connect to mongodb via ${ConfigLoader.mongodbUrl}"
      System.err.println(errorMessage, e)
      e.printStackTrace(System.err)
      throw new Exception(errorMessage)
  }
}
