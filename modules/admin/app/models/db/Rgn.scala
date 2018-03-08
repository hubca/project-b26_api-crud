package models.db

import java.util.Date
import play.api.data.Form
import play.api.data.Forms.{date, list, optional, _}
import reactivemongo.bson.{BSONObjectID}
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.libs.json._


case class RgnMongo(_id: Option[BSONObjectID] = None, name: String = "", geoLocation_e: Option[GeoLocation] = None,
                    adminCreatedId: Option[Int], dateCreated: Option[Date], adminModifiedId: Option[Int],
                    lastModified: Option[Date]
                   ) extends AllCollectionsTrait with AdminCollectionTrait {

  //val tabNames = List("One", "Two", "Three")

  def geoLocationAsObj = geoLocation_e.get

}

object RgnMongo extends ReadAndWriteTrait {

  val tabNames: Option[List[String]] = None

  implicit val geoLocationFormat: OFormat[GeoLocation] = Json.format[GeoLocation]
  implicit val rgnMongoFormat: OFormat[RgnMongo] = Json.format[RgnMongo]

  //val orderingBySomething: Ordering[CtyMongo] = Ordering.by(e => e.something)
  //def anyMethodsRequiringDb2Db: String = "flagship"

}

object RgnMongoForm extends TransformersTrait {

  def defaultGeoLocation(oGeoLocation: Option[GeoLocation]): Option[GeoLocation] = Some(GeoLocation("Point", Some(List.empty))) // todo - remove when we have coordinates in place
  def defaultAdminModifiedId(oAdminId: Option[Int]): Option[Int] = Some(1) // todo - change when set-up admin session

  //def date2Date(oDate: Option[Date]): Option[Date] = oDate

  val form = Form(
    mapping(
      "_id" -> ignored(Option.empty[BSONObjectID]),
      "name" -> nonEmptyText,
      "geoLocation_e" -> optional(mapping(
        "type" -> ignored("Polygon"),
        "coordinates" -> ignored(Option.empty[List[Double]])//optional(list(of(doubleFormat)))//ignored(Option.empty[List[Double]])//optional(list(of(doubleFormat)))
      )(GeoLocation.apply)(GeoLocation.unapply)).transform(defaultGeoLocation, defaultGeoLocation),
      "adminCreatedId" -> optional(number),
      "dateCreated" -> optional(date),
      "adminModifiedId" -> optional(number),
      "lastModified" -> optional(date).transform(date2Date, date2NowDate)
    )(RgnMongo.apply)(RgnMongo.unapply)
  )

}