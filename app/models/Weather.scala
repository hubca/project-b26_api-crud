package models

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import javax.print.attribute.standard.MediaSize.ISO

import akka.http.scaladsl.model.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger

/**
  * Created by sambo on 17/08/2017.
  */


case class Weather(_id: Option[BSONObjectID] = None, rstId: Option[BSONObjectID] = None, date: Option[JsObject] = None, snowfall: Double = 0.0) extends CollectionClass[Weather] {

  def idAsBsonId = _id.get//.getOrElse("")
  def idAsString = idAsBsonId.stringify

  def rstIdAsBsonId = rstId.get//.getOrElse("")
  def rstIdAsString = rstIdAsBsonId.stringify

  val format = new java.text.SimpleDateFormat("dd-MM-yyyy")
  def dateAsString = format.format(Date.from(Instant.ofEpochMilli(date.get.apply("$date").as[Long])))

  // /  def dateAsDate = date.get
//  def dateAsString = format.format(dateAsDate)

//  def dateAsString2 = Date.from(Instant.ofEpochMilli(date.get.apply("$date").as[Long]))

}

case class WeatherAggregate(_id: Option[BSONObjectID] = None, fromDate: Option[JsObject] = None, toDate: Option[JsObject] = None, totalSnowfall: Double = 0.0, avgSnowfall: Double = 0.0, minSnowfall: Double = 0.0, maxSnowfall: Double = 0.0) {
  def idAsString = _id.getOrElse("")
  //def idAsString = rstId.map(bson => bson.toString.split("\"")(1))//.getOrElse("")
  val format = new java.text.SimpleDateFormat("dd-MM-yyyy")
  def fromDateAsString = format.format(Date.from(Instant.ofEpochMilli(fromDate.get.apply("$fromDate").as[Long])))
  def toDateAsString = format.format(Date.from(Instant.ofEpochMilli(toDate.get.apply("$toDate").as[Long])))
}
case class WeatherFormData(rstId: String, date: String, snowfall: Double)

object Weather {
  implicit val weatherFormat = Json.format[Weather]
  //val orderingByResortMiles: Ordering[Weather] = Ordering.by(e => e.resortMiles)
}

object WeatherAggregate {
  implicit val weatherAggregateFormat = Json.format[WeatherAggregate]
}

object WeatherForm {

  val form = Form(
    mapping(
      "rstId" -> nonEmptyText,
      "date" -> nonEmptyText,
      "snowfall" -> of(doubleFormat)
    )(WeatherFormData.apply)(WeatherFormData.unapply)
  )

}
