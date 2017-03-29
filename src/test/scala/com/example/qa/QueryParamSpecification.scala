package com.example.qa

import akka.http.scaladsl.model.StatusCodes._
import org.scalacheck.Prop._
import org.scalacheck.{Gen, Properties}


object QueryParamSpecification extends Properties("arbitrary query params") with CommonVals {
  property("user agent") = forAll { ua: String =>
    H(U.search(publisherId = smartTvPub, ua = ua)).status == OK
  }

  property("app name") = forAll { app: String =>
    H(U.search(publisherId = mobilePub, appName = app)).status == OK
  }

  val widths = Gen.choose(0, 65535)
  property("ad width") = forAll(widths) { width: Int =>
    H(U.search(publisherId = mobilePub, width = width)).status == OK
  }
}
