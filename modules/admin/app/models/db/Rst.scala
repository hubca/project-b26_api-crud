package models.db

import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.util
import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}
import javax.swing.text.DateFormatter

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.ISODateTimeFormat
import play.api.data.{Form, FormError, Mapping}
import play.api.data.Forms.{date, list, optional, _}
import reactivemongo.bson.{BSONDateTime, BSONObjectID}
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.http.MediaRange.parse
import play.api.libs.json._
import play.api.data.format.Formatter
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.libs.json
import play.api.mvc.QueryStringBindable
import services.dbClient.{RstService, ServiceClientDb}
import services.CustomBinders.UrlFormatter

import scala.util.{Failure, Success, Try}
/* Created by sambo on 20/10/2017 */


case class MetricsAndVisitors(pisteArea_km2: Double, highestAltitude_m: Int, avgAnnualVisitors: Long,
                              avgVistorDensityPerKm2PerDay: Option[Double]) {

  def avgVistorDensityPerKm2PerDayAsObj = avgVistorDensityPerKm2PerDay.get

}

case class Description(short: String, full: String)

//-- bof_ embedded in Location --//
case class Region(id: Option[BSONObjectID], name: Option[String])

case class GeoLocation(`type`: String, coordinates: Option[List[Double]]) {

  def typeAsString = `type`
  def getCoordinatesAsString: String = coordinates.getOrElse(List()).reverse.mkString(", ")

}
//-- eof_ embedded in Location --//

case class Location(countryCode: Option[String], countryName: String, continent: Option[String], hemisphere: Option[String], region_ee: Option[Region],
                    language: Option[String], geoLocation_ee: Option[GeoLocation]) {
  def regionAsObj = region_ee.get
  def geoLocationAsObj = geoLocation_ee.get
}

case class LocalIata(iataCode: Option[String], distance_km: Option[Double], travelTime_mins: Option[Int], roundTripCosts_usd: Option[Double])

case class RunsParksLifts(runsTotal: Int, parksTotal: Int, liftsTotal: Int)

case class RunTypes(greenRunsNum: Int, blueRunsNum: Int, redRunsNum: Int, blackRunsNum: Int, greenCircleRunsNum: Int,
                    blueSquareRunsNum: Int, blackDiamondRunsNum: Int, blackDoubleDiamondRunsNum: Int, blackTripleDiamondRunsNum: Int)

case class LiftTypes(gondolaLiftsNum: Int, buttonLiftsNum: Int, chairLiftsNum: Int, cableCarLiftsNum: Int, movingCarpetLiftsNum: Int)

//-- embedded in OpeningClosingDates --//
case class OpenClosedDates(open: Option[Date], closed: Option[Date])

case class Season(last_ee: Option[OpenClosedDates], next_ee: Option[OpenClosedDates]) extends DateTransformTrait {

  def lastAsObj = last_ee.getOrElse(OpenClosedDates(None, None))

  //def lastOpenAsDate = lastAsObj.open)

  def lastOpenAsString = date2String(lastAsObj.open)
  def lastClosedAsString = date2String(lastAsObj.closed)

  def nextAsObj = next_ee.getOrElse(OpenClosedDates(None, None))
  def nextOpenAsString = date2String(nextAsObj.open)
  def nextClosedAsString = date2String(nextAsObj.closed)

}

case class LiftPassPrices(areaValidArr: Option[List[String]], pricesValidFrom: Option[Date], pricesValidUntil: Option[Date],
                          priceChildWeekDay_usd: Double, priceAdultWeekDay_usd: Double, priceChild6Day_usd: Double,
                          priceAdult6Day_usd: Double, priceDeepLinkArr: Option[List[String]])

case class Scores(scoreBA: Double, scoreSFdef: Double, scoreBG: Double, scoreFMpre: Double, scoreFMdefPre: Double, scoreLCpre: Double,
                  scoreGR: Double, scoreAD: Double, scoreNL: Double, scoreFDpre: Double, scorePR: Double)

case class EventsProductsPromotionsThisSeason(totalEventsThisSeason: Int, totalProductItemsThisSeason: Int, totalPromotionsThisSeason: Int)

case class RstMongo(_id: Option[BSONObjectID] = None, name: String = "", metricsAndVisitors_e: Option[MetricsAndVisitors] = None,
                    description_e: Option[Description] = None, location_e: Option[Location] = None,
                    runsParksLifts_e: Option[RunsParksLifts] = None, runTypes_e: Option[RunTypes] = None, liftTypes_e: Option[LiftTypes] = None,
                    season_e: Option[Season] = None, liftPassPrices_e: Option[LiftPassPrices] = None,
                    localIataArr_e: Option[List[LocalIata]] = None, localDomesticAirportArr_e: Option[List[LocalIata]] = None,
                    scores_e: Option[Scores] = None, eventsProductsPromotionsThisSeason_e: Option[EventsProductsPromotionsThisSeason] = None,
                    adminCreatedId: Option[Int], dateCreated: Option[Date], adminModifiedId: Option[Int],
                    lastModified: Option[Date] /*, testField: URL*/
                   ) extends AllCollectionsTrait with AdminCollectionTrait with StringTransformTrait {

  def descriptionAsObj = description_e.get
  def locationAsObj = location_e.get
  def metricsAndVisitorsAsObj = metricsAndVisitors_e.get  //
  def localIataArrAsArr = localIataArr_e.get  //
  def localDomesticAirportArr = localDomesticAirportArr_e.get
  def runsParksLiftsAsObj = runsParksLifts_e.get
  def runTypesAsObj = runTypes_e.get
  def liftTypesAsObj = liftTypes_e.get

  def seasonAsObj = season_e.get

  def liftPassPricesAsObj = liftPassPrices_e.get
  def areaValidArrAsString = strList2String(liftPassPricesAsObj.areaValidArr)//liftPassPricesAsObj.regionIdArr
  def pricesValidFromAsString = date2String(liftPassPricesAsObj.pricesValidFrom)
  def pricesValidUntilAsString = date2String(liftPassPricesAsObj.pricesValidUntil)
  def priceDeepLinkArrAsString = strList2String(liftPassPricesAsObj.priceDeepLinkArr)

  def scoresAsObj = scores_e.get
  def eventsProductsPromotionsThisSeasonAsObj = eventsProductsPromotionsThisSeason_e.get

  //def adminModifiedIdAsString = adminModifiedId.get
  //def dateCreatedAsString = date2String(dateCreated, format = dateTimeFormat)
  //def lastModifiedAsString = date2String(lastModified, format = dateTimeFormat)

}

object RstMongo extends ReadAndWriteTrait {

  /*
  implicit val bsonObjectIdWrite: Writes[BSONObjectID] = new Writes[BSONObjectID] {
    def writes(bsonObjectId: BSONObjectID): JsValue = Json.obj(
      "$oid" -> bsonObjectId.stringify
    )
  }

  implicit val countryCodeWrite: Writes[Location] = new Writes[Location] {
    def writes(location: Location): JsValue = Json.toJson(location)
      //Json.obj(

      //val jsonReads = Json.reads[Location]
      //implicit val cleanReads = jsonReads.map(location => location.copy(countryCode = "hope"))
    //)
  }
  */

  /*
  // understanding writing List[String]
  implicit val strArrayWrite: Writes[List[String]] = new Writes[List[String]] {
    def writes(list: List[String]): JsValue = Json.toJson(list.filterNot(_.isEmpty).toIndexedSeq)
  }
  /*

   */
  // understanding transforming JsObjects
  implicit val jsObjectArrWrite: Writes[List[LocalIata]] = new Writes[List[LocalIata]] {
    def writes(list: List[LocalIata]): JsValue = {

      //implicit val localIataFormat: OFormat[LocalIata] = Json.format[LocalIata]
      //val dfadsfv = list.filterNot(_.fields.head._1.isEmpty)
      Json.toJson(

        list.filter(_.iataCode.isDefined).map(x => x match {
          case LocalIata(a, b, c, d) => Json.obj("keyMo" -> a) // works
          case _ => Json.obj("wrong" -> "six")
        })
          //fields.map(_._1.isEmpty()).head)
      )
    }

  */

  /*
  // todo - custom bind URL type to priceDeepLinkArr
  implicit object UrlFormatter extends Formatter[URL] {
    override val format = Some(("format.url", Nil))
    override def bind(key: String, data: Map[String, String]) = parsing(new URL(_), "error.url", Nil)(key, data)
    override def unbind(key: String, value: URL) = Map(key -> value.toString)
  }
   */

  /*
  implicit val priceDeepLinkArrWrite: Writes[String] = new Writes[String] {
    def writes(str: String): JsValue = Json.arr(str)
    // Json.toJson(Seq(1, 2, 3, 4))
  }
  */

  val tabNames: Option[List[String]] = Some(List("Location/Season", "Piste Stats", "Lifts", "Metrics/Descriptions", "IATAs", "Scores", "Admin"))

  implicit val metricsAndVisitorsFormat: OFormat[MetricsAndVisitors] = Json.format[MetricsAndVisitors]
  implicit val descriptionFormat: OFormat[Description] = Json.format[Description]

  implicit val regionFormat: OFormat[Region] = Json.format[Region]
  implicit val geoLocationFormat: OFormat[GeoLocation] = Json.format[GeoLocation]
  implicit val locationFormat: OFormat[Location] = Json.format[Location]

  implicit val runsParksLiftsFormat: OFormat[RunsParksLifts] = Json.format[RunsParksLifts]
  implicit val runTypesFormat: OFormat[RunTypes] = Json.format[RunTypes]
  implicit val liftTypesFormat: OFormat[LiftTypes] = Json.format[LiftTypes]

  implicit val openClosedDatesFormat: OFormat[OpenClosedDates] = Json.format[OpenClosedDates]
  implicit val seasonFormat: OFormat[Season] = Json.format[Season]

  implicit val localIataFormat: OFormat[LocalIata] = Json.format[LocalIata]

  //implicit val urlFormat = UrlFormatter.bind("1" -> URL) // drastically wrong *
  //implicit val urlFormat: OFormat[URL] = UrlFormatter.bind[URL]()
  implicit val liftPassPricesFormat: OFormat[LiftPassPrices] = Json.format[LiftPassPrices]
  implicit val scoresFormat: OFormat[Scores] = Json.format[Scores]
  implicit val eventsProductsPromotionsThisSeasonFormat: OFormat[EventsProductsPromotionsThisSeason] = Json.format[EventsProductsPromotionsThisSeason]

  implicit val rstMongoFormat: OFormat[RstMongo] = Json.format[RstMongo]

  val orderingByBoardingArea: Ordering[RstMongo] = Ordering.by(e => e.metricsAndVisitorsAsObj.pisteArea_km2)
  val orderingByResortName: Ordering[RstMongo] = Ordering.by(e => e.name)
  val orderingByScoreBA: Ordering[RstMongo] = Ordering.by(e => e.scoresAsObj.scoreBA)

}

object RstMongoForm extends TransformersTrait {

  def filterEmptyLocalIata(list: List[LocalIata]): List[LocalIata] = list.filter(_.iataCode.isDefined)

  val form = Form(
    mapping(
      "_id" -> ignored(Option.empty[BSONObjectID]),
      "name" -> nonEmptyText,
      "metricsAndVisitors_e" -> optional(mapping(
        "pisteArea_km2" -> of(doubleFormat),
        "highestAltitude_m" -> number,
        "avgAnnualVisitors" -> longNumber,
        "avgVistorDensityPerKm2PerDay" -> optional(of(doubleFormat))
      )(MetricsAndVisitors.apply)(MetricsAndVisitors.unapply)),
      "description_e" -> optional(mapping(
        "short" -> nonEmptyText,
        "full" -> nonEmptyText
      )(Description.apply)(Description.unapply)),
      "location_e" -> optional(mapping(
        "countryCode" -> optional(text), // todo - input needed?
        "countryName" -> nonEmptyText,
        "continent" -> optional(text), // todo - input needed?
        "hemisphere" -> optional(text), // geo needed
        "region_ee" -> optional(mapping(
          "id" -> optional(
            text.transform(str2bson, bson2str)
          ),
          "name" -> optional(text) // todo - input needed?
        )(Region.apply)(Region.unapply)),
        "language" -> optional(text), // todo - input needed?
        "geoLocation_ee" -> optional(mapping(
          "type" -> ignored("Point"),
          "coordinates" -> optional(list(of(doubleFormat)))
        )(GeoLocation.apply)(GeoLocation.unapply))
      )(Location.apply)(Location.unapply)),//.transform(autoFillLocation, autoFillLocation)),
      "runsParksLifts_e" -> optional(mapping(
        "runsTotal" -> number,
        "parksTotal" -> number,
        "liftsTotal" -> number
      )(RunsParksLifts.apply)(RunsParksLifts.unapply)),
      "runTypes_e" -> optional(mapping(
        "greenRunsNum" -> number,
        "blueRunsNum" -> number,
        "redRunsNum" -> number,
        "blackRunsNum" -> number,
        "greenCircleRunsNum" -> number,
        "blueSquareRunsNum" -> number,
        "blackDiamondRunsNum" -> number,
        "blackDoubleDiamondRunsNum" -> number,
        "blackTripleDiamondRunsNum" -> number
      )(RunTypes.apply)(RunTypes.unapply)),
      "liftTypes_e" -> optional(mapping(
        "gondolaLiftsNum" -> number,
        "buttonLiftsNum" -> number,
        "chairLiftsNum" -> number,
        "cableCarLiftsNum" -> number,
        "movingCarpetLiftsNum" -> number
      )(LiftTypes.apply)(LiftTypes.unapply)),
      "season_e" -> optional(mapping(
        "last_ee" -> optional(mapping(
          "open" -> optional(date),//ignored(Option.empty[DateTime]),
          "closed" -> optional(date)//ignored(Option.empty[DateTime])
        )(OpenClosedDates.apply)(OpenClosedDates.unapply)),
        "next_ee" -> optional(mapping(
          "open" -> optional(date),//ignored(Option.empty[DateTime]),
          "closed" -> optional(date)//ignored(Option.empty[DateTime])
        )(OpenClosedDates.apply)(OpenClosedDates.unapply))
      )(Season.apply)(Season.unapply)),
      "liftPassPrices_e" -> optional(mapping(
        "areaValidArr" -> optional(
          list(text).transform(filterEmptyStringList, filterEmptyStringList)
        ),
        "pricesValidFrom" -> optional(date),
        "pricesValidUntil" -> optional(date),
        "priceChildWeekDay_usd" -> of(doubleFormat),
        "priceAdultWeekDay_usd" -> of(doubleFormat),
        "priceChild6Day_usd" -> of(doubleFormat),
        "priceAdult6Day_usd" -> of(doubleFormat),
        "priceDeepLinkArr" -> optional(
          list(text).transform(filterEmptyStringList, filterEmptyStringList)
        )
      )(LiftPassPrices.apply)(LiftPassPrices.unapply)),
      "localIataArr_e" -> optional(list(mapping(
        "iataCode" -> optional(text),
        "distance_km" -> optional(of(doubleFormat)),
        "travelTime_mins" -> optional(number),
        "roundTripCosts_usd" -> optional(of(doubleFormat))
      )(LocalIata.apply)(LocalIata.unapply)).transform(filterEmptyLocalIata, filterEmptyLocalIata)),
      "localDomesticAirportArr_e" -> optional(list(mapping(
        "iataCode" -> optional(text),
        "distance_km" -> optional(of(doubleFormat)),
        "travelTime_mins" -> optional(number),
        "roundTripCosts_usd" -> optional(of(doubleFormat))
      )(LocalIata.apply)(LocalIata.unapply))),
      "scores_e" -> optional(mapping(
        "scoreBA" -> of(doubleFormat),
        "scoreSFdef" -> of(doubleFormat),
        "scoreBG" -> of(doubleFormat),
        "scoreFMpre" -> of(doubleFormat),
        "scoreFMdefPre" -> of(doubleFormat),
        "scoreLCpre" -> of(doubleFormat),
        "scoreGR" -> of(doubleFormat),
        "scoreAD" -> of(doubleFormat),
        "scoreNL" -> of(doubleFormat),
        "scoreFDpre" -> of(doubleFormat),
        "scorePR" -> of(doubleFormat)
      )(Scores.apply)(Scores.unapply)),
      "eventsProductsPromotionsThisSeason_e" -> optional(mapping(
        "totalEventsThisSeason" -> number,
        "totalProductItemsThisSeason" -> number,
        "totalPromotionsThisSeason" -> number
      )(EventsProductsPromotionsThisSeason.apply)(EventsProductsPromotionsThisSeason.unapply)),
      "adminCreatedId" -> optional(number),
      "dateCreated" -> optional(date),
      "adminModifiedId" -> optional(number),
      "lastModified" -> optional(date).transform(date2Date, date2NowDate)/*,//ignored(Option.empty[Date])
      "testField" -> of[URL]*/
    )(RstMongo.apply)(RstMongo.unapply)
  )

}
// todo - long term: define regions with coordinates in new collection? Then automatically assign region upon creation.

// update/insert rst in mongo
//db.rst.update({ "_id": ObjectId("5a28258d9830b7614f566cc7") }, { "name" : "Whistler/Blackcomb", "description_e" : { "short" : "A very short description for Whistler/Blackcomb", "full" : "Write up of Whistler/Blackcomb" }, "location_e" : { "countryCode" : "CA", "countryName" : "Canada", "continent" : "North America", "hemisphere" : "Northern", "region_ee" : { "id" : 5, "name" : "Rockies2" }, "language" : "English", "geoLocation_ee" : { "type" : "Point", "coordinates" : [50.115198, -122.948647] }}, "metricsAndVisitors_e" : { "pisteArea_km2" : 31.1, "highestAltitude_m" : 1421, "avgAnnualVisitors" : 1553700 }, "localIataArr_e" : [{ "iataCode" : "YVR", "distance_km" : 1900, "travelTime_mins" : 500, "roundTripCosts_usd" : 80 }], "localDomesticAirportArr_e" : [ ], "runsParksLifts_e" : { "runsTotal" : 41, "parksTotal" : 7, "liftsTotal" : 50 }, "runTypes_e" : { "greenRunsNum" : 12, "blueRunsNum" : 18, "redRunsNum" : 21, "blackRunsNum" : 10, "greenCircleRunsNum" : 0, "blueSquareRunsNum" : 0, "blackDiamondRunsNum" : 0, "blackDoubleDiamondRunsNum" : 0, "blackTripleDiamondRunsNum" : 0 }, "liftTypes_e" : { "gondolaLiftsNum" : 4, "buttonLiftsNum" : 12, "chairLiftsNum" : 75, "cableCarLiftsNum" : 11, "movingCarpetLiftsNum" : 4 }, "liftPassPrices_e" : { "regionIdArr": [2, 4], "pricesValidFrom": new Date("2017-12-10"), "pricesValidUntil" : new Date("2017-05-02"), "priceChildWeekDay_usd": 20, "priceAdultWeekDay_usd": 40.0, "priceChild6Day_usd": 80.0, "priceAdult6Day_usd": 200.0, "priceAverage_usd": 85.0, "priceDeepLinkArr": ["http: //www.google.com/extra", "http: //www.lift-house.com"] }, "scores_e" : { "scoreBA" : 0.1, "scoreSFdef" : 0.3, "scoreBG" : 0.1, "scoreFMpre" : 0, "scoreFMdefPre" : 0.1, "scoreLCpre" : 0.2, "scoreGR" : 0.1, "scoreAD" : 0, "scoreNL" : 0.1, "scoreFDpre" : 0.3, "scorePR" : 0.4 }, "eventsProductsPromotionsThisSeason_e" : { "totalEventsThisSeason" : 24, "totalProductItemsThisSeason" : 43, "totalPromotionsThisSeason" : 15 }, "season_e" : { "last_ee" : { "open" : new Date("2016-12-01"), "closed" : new Date("2017-05-01") }, "next_ee" : { "open" : new Date("2017-12-02"), "closed" : new Date("2018-04-30") }}, "lastModified" : new Date("2017-12-14"), "adminModifiedId" : 1, "dateCreated" : new Date("2017-12-06T00:00:00Z") })

// update liftPassPrices_e
//db.rst.update({ "_id": ObjectId("5a28258d9830b7614f566cc7") }, { "$set": { "liftPassPrices_e" : { "regionIdArr": [2, 4], "pricesValidFrom": new Date("2017-12-10"), "pricesValidUntil" : new Date("2017-05-02"), "priceChildWeekDay_usd": 20, "priceAdultWeekDay_usd": 40.0, "priceChild6Day_usd": 80.0, "priceAdult6Day_usd": 200.0, "priceAverage_usd": 85.0, "priceDeepLinkArr": ["http: //www.google.com/extra", "http: //www.lift-house.com"] }})

