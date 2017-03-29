package com.example.qa

import com.typesafe.config.ConfigFactory

object ConfigLoader {
  private val config = ConfigFactory.load()
  private lazy val root = config.getConfig("app")

  lazy val mongodbUrl = root.getString("inventory.mongodb.url")
  lazy val mongodbQueryTimeout = root.getInt("inventory.mongodb.query-timeout-seconds")
  lazy val vastServiceScheme = root.getString("inventory.vastservice.scheme")
  lazy val vastServiceHost = root.getString("inventory.vastservice.host")
  lazy val vastServicePort = root.getInt("inventory.vastservice.port")
  lazy val jmxPort = root.getInt("inventory.vastservice.jmx.port")
  lazy val jmxBeanName = root.getString("inventory.vastservice.jmx.bean")
  lazy val cacheReloadTimeout = root.getInt("inventory.vastservice.cache-flush-interval-ms")
  lazy val confDbQueryTimeout = root.getInt("inventory.settingsdb.query-timeout-seconds")
  lazy val timeShiftSeconds = config.getInt("system.time-shift-seconds")
}
