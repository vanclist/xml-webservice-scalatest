package com.example.qa.vastservice.model

case class TestData(
                     user: Int,
                     publisher: Int,
                     exchanges: List[Int] = List(),
                     campaigns: List[Int] = List(),
                     lineItems: List[Int] = List()
                   )
