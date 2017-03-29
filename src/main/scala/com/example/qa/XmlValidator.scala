package com.example.qa

import java.io.InputStream
import java.net.URL
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import org.xml.sax.SAXException

object XmlValidator {
  def validate(xmlSource: String, xsdSource: URL): Boolean = validate(xmlSource.asInstanceOf[InputStream], xsdSource)
  def validate(xmlSource: InputStream, xsdSource: URL): Boolean = {
    try {
      val schemaLang = "http://www.w3.org/2001/XMLSchema"
      val factory = SchemaFactory.newInstance(schemaLang)
      val schema = factory.newSchema(xsdSource)
      val validator = schema.newValidator()
      validator.validate(new StreamSource(xmlSource))
      true
    } catch {
      case ex: SAXException =>
        println(ex.getMessage)
        false
      case ex: Exception =>
        ex.printStackTrace()
        false
    }
  }
}
