package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.libs.json._

case class Geometry(coordinates: Option[JsArray], `type`: String) {

  def typeAsString = `type`

  def getCoordinatesAsTuples: List[(Double, Double)] = {

    val coordinatesAsString = coordinates.get.apply(0).toString

    coordinatesAsString.substring(1,coordinatesAsString.length-1)
      .replace("],[","]/[").replace("]","").replace("[","")
      .split("/",0)
      .toList
      .map(_.split(",",0)
      .toList.map(_.toDouble))
      .map(res => (res.head, res.last))

  }

}

case class Neighborhood(_id: Option[BSONObjectID], name: String, geometry: Geometry) extends CollectionClass[Neighborhood] {
  def idAsBsonId = _id.get//.getOrElse("")
  def idAsString = idAsBsonId.stringify

//  def testGeometry = geometry.productElement(0)


}
/*
case class Neighborhood(_id: Option[BSONObjectID], geometry: Geometry, name: String) extends CollectionClass[Neighborhood] {
  def idAsBsonId = _id.get//.getOrElse("")
  def idAsString = idAsBsonId.stringify
}

id
geometry
_coordinates (arr)
_type (String)
name (String)

 */
case class NeighborhoodFormData(name: String)


object Geometry {
  //implicit val geometryFormat = Json.format[Geometry]
}


object Neighborhood {
  implicit val geometryFormat = Json.format[Geometry]
  implicit val neighborhoodFormat = Json.format[Neighborhood]

}

object NeighborhoodForm {

  val form = Form(
    mapping(
      "name" -> nonEmptyText
    )(NeighborhoodFormData.apply)(NeighborhoodFormData.unapply)
  )

}