package com.example.qa.vastservice.model

import akka.http.scaladsl.model.Uri._

case class UriParams(scheme: String, host: Host, port: Int)
