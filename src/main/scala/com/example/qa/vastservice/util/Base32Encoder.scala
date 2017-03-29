package com.example.qa.vastservice.util

import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Base32

trait Base32Encoder {

  private val base32HexPadded = new Base32(true, '='.toByte)

  def encodeBase32HexPadded(str: String): String = base32HexPadded.encodeToString(str.getBytes(StandardCharsets.UTF_8))

  def decodeBase32HexPadded(base32: String): String = new String(base32HexPadded.decode(base32), StandardCharsets.UTF_8)

}
