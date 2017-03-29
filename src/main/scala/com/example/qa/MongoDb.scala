package com.example.qa

import java.time.LocalDate

import com.typesafe.scalalogging.LazyLogging
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection

final case class ViewProgress(
                               started: Long,
                               firstQuartile: Long,
                               midpoint: Long,
                               thirdQuartile: Long,
                               completed: Long
                             )

object MongoDb extends LazyLogging {
  // TODO: add scalaDoc
  def getCollection(name: String)(implicit db: DefaultDB): BSONCollection = {
    db.collection[BSONCollection](name)
  }

  def getCurrentPostfix: String = {
    val now = LocalDate.now
    s"${now.getMonth.getValue}_${now.getYear}"
  }

  def currentAdRequests: String = s"requests_${MongoDb.getCurrentPostfix}"

  def currentActions: String = s"actions_${MongoDb.getCurrentPostfix}"
}
