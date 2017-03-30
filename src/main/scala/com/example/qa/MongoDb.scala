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

/** Let's assume that our MongoDB storage designed to have separate collection for each month,
  * so collection names are `requests_3_2017`, `actions_12_2017` etc.
  */
object MongoDb extends LazyLogging {
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
