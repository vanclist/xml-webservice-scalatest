package com.example.qa

import javax.management._
import javax.management.remote._

import com.typesafe.scalalogging.StrictLogging

object JMXClient extends StrictLogging {
  val port = ConfigLoader.jmxPort
  val beanName = ConfigLoader.jmxBeanName

  def jmxConnector: Option[JMXConnector] = {
    try
    {
      val target = new JMXServiceURL(s"service:jmx:rmi:///jndi/rmi://$port/jmxrmi")
      Some(JMXConnectorFactory.connect(target))
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        None
    }
  }

  def reloadCache(settingsReloadDelay: Long): Boolean = {
    logger.info("Trying to start settings cache reload...")
    try
    {
      val target = new JMXServiceURL(s"service:jmx:rmi:///jndi/rmi://$port/jmxrmi")
      val connector = JMXConnectorFactory.connect(target)
      val remote = connector.getMBeanServerConnection()
      val bean = new ObjectName(s"$beanName:type=JMXSettings")
      remote.invoke(bean, "reloadConfigurationCache", null, null)
      connector.close()
      logger.info(s"Cache reload has been started. Waiting ${settingsReloadDelay}ms for completion.")
      Thread sleep settingsReloadDelay
      true
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        false
    }
  }

  def getStatsAttribute(attrName: String): Long = {
    val connector = jmxConnector.get
    val remote = connector.getMBeanServerConnection()
    val bean = new ObjectName(s"$beanName:type=JMXStats")
    val attrValue: Long = remote.getAttribute(bean, attrName).asInstanceOf[Long]
    connector.close()
    attrValue
  }
}
