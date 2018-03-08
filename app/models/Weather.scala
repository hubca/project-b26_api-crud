package models

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import models.db.AdminCollectionTrait
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._

case class WeatherAggregate(_id: Option[BSONObjectID] = None, fromDate: Option[JsObject] = None, toDate: Option[JsObject] = None, totalSnowfall: Double = 0.0, avgSnowfall: Double = 0.0, minSnowfall: Double = 0.0, maxSnowfall: Double = 0.0) {
  def idAsString = _id.getOrElse("")
  //def idAsString = rstId.map(bson => bson.toString.split("\"")(1))//.getOrElse("")
  val format = new java.text.SimpleDateFormat("dd-MM-yyyy")
  def fromDateAsString = format.format(Date.from(Instant.ofEpochMilli(fromDate.get.apply("$fromDate").as[Long])))
  def toDateAsString = format.format(Date.from(Instant.ofEpochMilli(toDate.get.apply("$toDate").as[Long])))
}

object WeatherAggregate {
  implicit val weatherAggregateFormat = Json.format[WeatherAggregate]

  val orderingByAvgSnowfall: Ordering[WeatherAggregate] = Ordering.by(e => e.avgSnowfall)
}