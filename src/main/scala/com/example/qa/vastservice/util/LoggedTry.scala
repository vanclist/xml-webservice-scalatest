package com.example.qa.vastservice.util

import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Try}

trait LoggedTry {
  this: StrictLogging =>

  implicit class RichTry[T](t: Try[T]) {
    def logFailure(message: => String): Option[T] = {
      t.recoverWith {
        case ex =>
          logger.error(message, ex)
          Failure(ex)
      }.toOption
    }
  }
}