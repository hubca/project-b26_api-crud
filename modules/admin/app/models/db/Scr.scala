package models.db

import java.util.Date

import play.api.data.Form
import play.api.data.Forms.{date, list, number, optional, _}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.libs.json._

case class ScrMongo(_id: Option[BSONObjectID] = None, varName: String = "", weight: Double, scoreGroup: String = "",
                    adminCreatedId: Option[Int], dateCreated: Option[Date], adminModifiedId: Option[Int],
                    lastModified: Option[Date]
                   ) extends AllCollectionsTrait with AdminCollectionTrait

object ScrMongo extends ReadAndWriteTrait {

  val tabNames: Option[List[String]] = None

  implicit val ScrMongoFormat: OFormat[ScrMongo] = Json.format[ScrMongo]

}

object ScrMongoForm extends TransformersTrait with DoubleTrait {

  val form = Form(
    mapping(
      "_id" -> ignored(Option.empty[BSONObjectID]),
      "varName" -> nonEmptyText,
      "weight" -> of(doubleFormat).transform(betweenZeroAndOne, double2double),
      "scoreGroup" -> nonEmptyText,
      "adminCreatedId" -> optional(number),
      "dateCreated" -> optional(date),
      "adminModifiedId" -> optional(number),
      "lastModified" -> optional(date).transform(date2Date, date2NowDate)
    )(ScrMongo.apply)(ScrMongo.unapply)
  )

}