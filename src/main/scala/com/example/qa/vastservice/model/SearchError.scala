package com.example.qa.vastservice.model

object SearchError {
  val noResults = "Campaigns were filtered."
  val widthNotPassed = "Required query parameter width was not passed"
  val widthOnlyPositive = "Query parameter width can be only a positive integer"
  val publisherOnlyPositive = "Query parameter publisher can be only a positive integer"
  val countryNotAllowed = "Country is not allowed for this publisher."
  val ipNotValid = "Query parameter ip is not valid"
}
