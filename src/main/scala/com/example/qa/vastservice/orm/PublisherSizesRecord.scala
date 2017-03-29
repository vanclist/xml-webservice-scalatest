package com.example.qa.vastservice.orm

import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag
import slick.relational.RelationalProfile.ColumnOption.Default


trait PublisherSizesRecord {
  this: ConfigurationTables.type =>

  class PublisherToSizes(tag: Tag) extends Table[(Int, Int, Int, Int, Int)](tag, "publishers_to_player_sizes") {
    def publisherId = column[Int]("publisher_id")

    def minWidth = column[Int]("width_min", Default(0))
    def maxWidth = column[Int]("width_max", Default(1 << 16))
    def minHeight = column[Int]("height_min", Default(0))
    def maxHeight = column[Int]("height_max", Default(1 << 16))

    def * = (publisherId, minWidth, maxWidth, minHeight, maxHeight)

    def fkCmp = foreignKey("publishers_to_player_sizes_fk_1", publisherId, publishers)(_.id)
  }
}
