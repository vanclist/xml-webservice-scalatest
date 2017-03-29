package com.example.qa.vastservice.util

import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Base64

trait Base64Encoder {

  def encodeBase64UrlSafe(str: String): String = Base64.encodeBase64URLSafeString(str.getBytes(StandardCharsets.UTF_8))

  def decodeBase64(base64: String): String = new String(Base64.decodeBase64(base64), StandardCharsets.UTF_8)

}
