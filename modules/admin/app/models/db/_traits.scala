package models.db

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

import models.db.CtyVar._
import models.db.CtyMongo._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.data.Form
import play.api.data.Forms.{date, list, optional, _}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.libs.json._

import scala.reflect.internal.util.Statistics.Quantity

trait AllCollectionsTrait extends DateTransformTrait {

  val adminCreatedId: Option[Int]// = Some(1)
  val dateCreated: Option[Date]
  val adminModifiedId: Option[Int]// = Some(1)
  val lastModified: Option[Date]

  def adminCreatedIdAsString = adminCreatedId.get
  def dateCreatedAsString = date2String(dateCreated, format = dateTimeFormat)
  def adminModifiedIdAsString = adminModifiedId.get
  def lastModifiedAsString = date2String(lastModified, format = dateTimeFormat)


  // data types - id (BSONObjectID)
  def idAsBsonId(oId: Option[BSONObjectID]) = oId.get

  def idAsString(oId: Option[BSONObjectID]) = idAsBsonId(oId).stringify

  def idAsStringLast6(oId: Option[BSONObjectID]) = {
    val idString = idAsString(oId)
    idString.substring(idString.length - 6)
  }


}

trait StringTransformTrait {

  // data types - lists
  def strList2String(oList: Option[List[String]]): String = oList.getOrElse(List()).mkString(", ")
  def intList2String(oList: Option[List[Int]]): String = strList2String(Some(oList.getOrElse(List()).map(x => x.toString)))
  def filterEmptyStringList(list: List[String]): List[String] = list.filter(_.nonEmpty)

  def bson2str(id: BSONObjectID): String = id.stringify
  def str2bson(idString: String): BSONObjectID = BSONObjectID.parse(idString).get

}

trait DateTransformTrait {

  // data types - date/time
  def simpleDateFormat(format: String) = new java.text.SimpleDateFormat(format)

  def dateFormat = simpleDateFormat("yyyy-MM-dd")
  def dateTimeFormat = simpleDateFormat("yyyy-MM-dd HH:mm:ss")
 // def yearStartFormat = simpleDateFormat("yyyy-01-01 00:00:00")

  def date2String(oDate: Option[Date], format: SimpleDateFormat = dateFormat): String = format.format(Date.from(Instant.ofEpochMilli(oDate.getOrElse(new Date()).getTime)))

  def str2Date(dateStr: String): Option[Date] = {

    val date = dateFormat.parse(dateStr)

    date match {
      case d: Date => Some(date)
      case _ => None
    }

  }

  def date2DateTime(oDate: Option[Date]): Option[DateTime] = {

    oDate.map(x =>

      DateTime.parse(
        date2String(oDate)
      )

    )

  }

  def getSouthernHemisphereSeasonFormat(oDate: Option[Date]): String = date2String(oDate, simpleDateFormat("YYYY"))

  def getNorthernHemisphereSeasonFormat(oDate: Option[Date]): String = {

    val firstYear: String = getSouthernHemisphereSeasonFormat(oDate)
    val secondYear: String = (firstYear.toInt + 1).toString.substring(0, 2)
    List(firstYear, secondYear).mkString("/")

  }

  def getSeasonOpts(numOfYears: Int, oDate: Option[Date] = Some(new Date())): List[Int] = {

    val seasonFormat = getSouthernHemisphereSeasonFormat(oDate).toInt + 1
    List.range(seasonFormat - numOfYears, seasonFormat).reverse

  }
  // not currently in use but could be useful
  def addTime(oDate: Option[Date], metric: String, quantity: Int): Option[Date] = {

    date2DateTime(oDate).map(dateTime => {

      val newTime = metric.toLowerCase match {
        case "minute" | "minutes" => dateTime.plusMinutes(quantity)
        case "hour" | "hours" => dateTime.plusHours(quantity)
        case "day" | "days" => dateTime.plusDays(quantity)
        case "week" | "weeks" => dateTime.plusWeeks(quantity)
        case "month" | "months" => dateTime.plusMonths(quantity)
        case "year" | "years" => dateTime.plusYears(quantity)
      }

      newTime.toDate()

    })

  }

  def getDateDiff(startDate: Date, endDate: Date, timeUnit: TimeUnit): Long = {
    val diffInMillies = endDate.getTime() - startDate.getTime
    timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS)
  }

  def date2Long(oDate: Option[Date]): Option[Long] = {

    oDate match {

      case Some(_) => Some(

        new DateTime(oDate.get.getTime())
          .withZoneRetainFields(DateTimeZone.UTC)
          .withZone(DateTimeZone.getDefault())
          .getMillis

      )

      case _ => None
    }

  }

  def dateStr2Long(dateStr: String): Option[Long] = {
    val parseDate = str2Date(dateStr)
    date2Long(parseDate)
  }

  def str2MongoDate(dateStr: String, dateFieldName: String): JsObject = {
    val oLongDate: Option[Long] = dateStr2Long(dateStr)

    Json.obj(s"$$$dateFieldName" -> JsNumber(oLongDate.get))
  }

  def getMongoDateRange(fromDate: String, toDate: String, dateFieldName: String): JsObject = {

    val fromDateJsObj = str2MongoDate(fromDate, dateFieldName)
    val toDateJsObj = str2MongoDate(toDate, dateFieldName)

    Json.obj("date" -> Json.obj("$gte" -> fromDateJsObj, "$lt" -> toDateJsObj))

  }

  def getNowDate: Option[Date] = Some(new Date()) // used for dateCreated and lastModified

  def getYearStartDate(oDate: Option[Date] = getNowDate): Option[Date] = {
    str2Date(date2String(oDate, dateTimeFormat))
  }

  def date2Date(oDate: Option[Date]): Option[Date] = oDate
  def date2NowDate(oDate: Option[Date]): Option[Date] = getNowDate
  //def date2NowDate(oDate: Option[Date]): Option[Date] = oDate.map(x => getNowDate)

}

trait TransformersTrait extends JsTransformTrait with StringTransformTrait with DateTransformTrait

trait JsTransformTrait {

  def tryTransformer(obj: JsObject, transformer: Reads[JsObject]) = {

    obj.transform(transformer) match {
      case JsSuccess(r: JsObject, _) => r//Ok(r)
      case e: JsError => JsError.toJson(e)//Ok(JsError.toJson(e))
      //case e: JsError => s"Errors: ${JsError.toJson(e).toString()}"
    }

  }

}

trait VarDataTrait {

  /*
  val adminCreatedId: Option[Int]// = Some(1)
  val dateCreated: Option[Date]
  val adminModifiedId: Option[Int]// = Some(1)
  val lastModified: Option[Date]
  */

}

object VarDataTrait {

  def apply(varData: VarDataTrait) = { //Option[(String, JsValue)] = {
    val (prod: Product, sub) = varData match {
      case cv: CtyVar => (cv, Json.toJson(cv))
    }
    Some(prod.productPrefix -> sub)
    //sub
    //Some(sub)
  }

  def unapply(`class`: String, data: JsValue) = {//: VarDataTrait = {
    (`class` match {
      case "CtyVar" => Some("tony")//Json.fromJson[CtyVar](data)(ctyVarFormat)
    }).get
  }

  /*
  implicit object DeliveryJSONHandler {
    override def read(myJson: JsObject): VarDataTrait = myJson.productPrefix match {
      case "CtyVar" => ctyVarFormat(myJson)
      case "CtyMongo" => furnitureRead(myJson)
    }

    override def write(varData: VarDataTrait): JsObject = varData match {
      case cv: CtyVar     => clothesWrite(cv)
      case cm: CtyMongo  => furnitureWrite(cm)
    }
  }
  */

}

trait AdminCollectionTrait {

  // layout
  def oddOrEvenTableRow(num: Int): String = num match {
    case x if((x % 2)==0) => "odd"
    case _ => "even"
  }

  // content
  def getTitle(action: String, collectionName: String): String = s"""$collectionName collection"""

  def getFormValues(action: String, value: String, inputType: String = "input"): String = action match {
    case "Edit" => value
    case _ => ""
  }

  // todo - create different 'mixers', i.e. traits for filters, mongo conversions, etc

  //def getTabs(dbAction: String, form: Form[RstMongo]) = List(views.html.db.rst.formFields.tab1(dbAction, form), views.html.db.rst.formFields.tab2(dbAction, form))
  //def keepY(b1: testTuples, b2: testTuples) = b2.copy(name = b1.name)
}

trait NumbersFormattingTrait {

  def roundBy(num: Double)(dp: Int): String = s"%1.${dp}f".format(num)

}

trait ReadAndWriteTrait {

  implicit val dateRead: Reads[Date] = (__ \ "$date").read[Long].map { date =>
    new Date(date)
  }

  implicit val dateWrite: Writes[Date] = new Writes[Date] {
    def writes(date: Date): JsValue = Json.obj(
      "$date" -> date.getTime
    )
  }

}

trait SelectOptionsTrait {

  def getAllOpts(optsMap: Map[String, Seq[Any]]): Map[String, Seq[(String, String)]] = optsMap map {

    case (key, value) => value match {

      case seq: Seq[_] => (key, seq.map(x =>

        x match {
          case s: String => s -> s
          case i: Int => i.toString -> i.toString
          case d: Double => d.toString -> d.toString
          case (a: String, b: String) => a -> b
          //case (a: Date, b: String) => a.toString -> b
          // todo - error handling here ??
        }

      ))
      // todo - error handling here ??
    }

  }

  def getRatingsList10: Seq[Int] = List.range(1, 11)

}

trait DoubleTrait {

  def betweenZeroAndOne(num: Double): Double = num match {
    case num if(num < 0) => 0
    case num if(num > 1) => 1
    case _ => num
  }

  def double2double(num: Double): Double = num

}

//trait CollectionClass[+T] { // covariant (parent type) of all models mapping to Mongo JSON Collections - http://blog.kamkor.me/Covariance-And-Contravariance-In-Scala/

//--- other generic models ---\\
case class MongoField(fieldName: String, fieldValue: Any) // used in the updateOneField function
