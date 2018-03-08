package models.db

import java.util.Date

import play.api.data.Form
import play.api.data.Forms.{date, list, optional, _}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.libs.json._

// /api/1.1/db/r/retrieve/rst?oid=dsfpjsdfinsnf
// json.obj( _id -> json.obj("$oid" -> "dsfpjsdfinsnf"), "countryName" -> "France")
case class MongoCollection[T <: VarDataTrait](_id: Option[BSONObjectID] = None, adminCreatedId: Option[Int], dateCreated: Option[Date],
                    adminModifiedId: Option[Int], lastModified: Option[Date], varData_e: Option[T] = None
                   ) extends VarDataTrait with AllCollectionsTrait with AdminCollectionTrait with StringTransformTrait {//JsObject = Json.obj())

  def oVarDataAsObj = varData_e.getOrElse(Json.obj())


  // todo - put in trait
  //def varDataAsJson: JsValue = Json.toJson(varData_e)
  //def varDataAsObj: Option[JsResult[CtyVar]] = varData_e.map(x => x.validate[CtyVar])
}

object MongoCollection extends VarDataTrait with ReadAndWriteTrait {

  val tabNames: Option[List[String]] = Some(List("Countries", "Admin"))

  // options for select inputs (dropdowns)
  val continentList: List[String] = List("Africa", "Antarctica", "Asia", "Europe", "North America", "South America")
  val hemisphereList: List[String] = List("Northern", "Southern")

  implicit val mongoCollectionFormat: OFormat[MongoCollection] = Json.format[MongoCollection]

}

case class CtyVar(countryName: String = "", countryCode: Option[String] = None, continent: Option[String] = None,
                  hemisphere: Option[String] = None, languagesArr: Option[List[String]] = None,
                  currencyCode: Option[String] = None
                 ) extends VarDataTrait with StringTransformTrait {

  //val tabNames = List("One", "Two", "Three")
  def oCountryCodeAsString = countryCode.getOrElse("")
  def oContinentAsString = continent.getOrElse("")
  def oHemisphereAsString = hemisphere.getOrElse("")
  def oCurrencyCodeAsString = currencyCode.getOrElse("")
  def oLanguagesArrAsString = strList2String(languagesArr)

}

object CtyVar extends ReadAndWriteTrait {

  val tabNames: Option[List[String]] = None

  // options for select inputs (dropdowns)
  val continentList: List[String] = List("Africa", "Antarctica", "Asia", "Europe", "North America", "South America")
  val hemisphereList: List[String] = List("Northern", "Southern")

  implicit val ctyVarFormat: OFormat[CtyVar] = Json.format[CtyVar]

  //val orderingByCountryName: Ordering[CtyMongo] = Ordering.by(e => e.countryName)

}

object CtyMongoForm extends TransformersTrait {

  val form = Form(
    mapping(
      "_id" -> ignored(Option.empty[BSONObjectID]),
      "adminCreatedId" -> optional(number),
      "dateCreated" -> optional(date),
      "adminModifiedId" -> optional(number),
      "lastModified" -> optional(date).transform(date2Date, date2NowDate),
      "varData_e" -> optional(mapping(
        "countryName" -> nonEmptyText,
        "countryCode" -> optional(text),
        "continent" -> optional(text),
        "hemisphere" -> optional(text),
        "languagesArr" -> optional(
          list(text).transform(filterEmptyStringList, filterEmptyStringList)
        ),
        "currencyCode" -> optional(text)
      )(CtyVar.apply)(CtyVar.unapply))
    )(MongoCollection.apply)(MongoCollection.unapply)
  )

}