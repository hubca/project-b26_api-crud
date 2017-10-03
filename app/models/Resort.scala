package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.libs.json.Json

/* Created by sambo on 17/08/2017 */

class CollectionClass[+T] // covariant (parent type) of all models mapping to Mongo JSON Collctions - http://blog.kamkor.me/Covariance-And-Contravariance-In-Scala/

case class Resort(_id: Option[BSONObjectID] = None, resortName: String = "", resortCountry: String = "", resortContinent: String = "", resortCountryPrefix: String = "", resortMiles: Double = 0.0, scoreBA: Double = 0.0, scoreSF: Double = 0.0) extends CollectionClass[Resort] {
  def idAsBsonId = _id.get//.getOrElse("")
  def idAsString = idAsBsonId.stringify
}

case class ResortFormData(resortName: String, resortCountry: String, resortContinent: String, resortCountryPrefix: String, resortMiles: Double, scoreBA: Double, scoreSF: Double)

object Resort {
  implicit val resortFormat = Json.format[Resort]

  val orderingByResortMiles: Ordering[Resort] = Ordering.by(e => e.resortMiles)
  val orderingByResortName: Ordering[Resort] = Ordering.by(e => e.resortName)
  val orderingByScoreBA: Ordering[Resort] = Ordering.by(e => e.scoreBA)
  val orderingByScoreSF: Ordering[Resort] = Ordering.by(e => e.scoreSF)
}

object ResortForm {

  val form = Form(
    mapping(
      "resortName" -> nonEmptyText,
      "resortCountry" -> nonEmptyText,
      "resortContinent" -> nonEmptyText,
      "resortCountryPrefix" -> nonEmptyText,
      "resortMiles" -> of(doubleFormat),
      "scoreBA" -> of(doubleFormat),
      "scoreSF" -> of(doubleFormat)
    )(ResortFormData.apply)(ResortFormData.unapply)
  )

}