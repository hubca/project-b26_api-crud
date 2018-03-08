package models.db

import java.util.Date

import play.api.data.Form
import play.api.data.Forms.{date, list, optional, _}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.libs.json.{Json, _}

case class LastVisit(selection: String = "", date: Option[Date] = None) extends DateTransformTrait {

  def dateAsString = date2String(date)

}

case class OurMongo(_id: Option[BSONObjectID] = None, usrId: Option[BSONObjectID] = None, sessionId: Option[String] = None,
                    rstId: Option[BSONObjectID] = None, userSkillLevel: Option[String] = None, numOfResortVisits: Option[Int] = None,
                    lastVisit_e: Option[LastVisit] = None, lastGroupType: Option[String] = None,
                    liftFacilitiesRating: Option[Int] = None, atmosphereRating: Option[Int] = None, funRating: Option[Int] = None,
                    suitabilityRating: Option[Int] = None, valueRating: Option[Int] = None, overallRating: Option[Int] = None,
                    liklihoodToVisitAgain: Option[Int] = None, adminCreatedId: Option[Int], dateCreated: Option[Date],
                    adminModifiedId: Option[Int], lastModified: Option[Date]
                   ) extends AllCollectionsTrait with AdminCollectionTrait {

  //val tabNames = List("One", "Two", "Three")
  def lastVisitAsObj = lastVisit_e.getOrElse(LastVisit.apply())
}

object OurMongo extends ReadAndWriteTrait with DateTransformTrait {

  val tabNames: Option[List[String]] = Some(List("Info", "Ratings", "Admin"))

  // options for select inputs (dropdowns)
  // lastVisit: { now (ISODate), exact dates (ISODate), seasons (String), anytime (null) }
  val lastVisitList: List[String] = List("now", "departure date", "season starting", "undisclosed")
  val lastVisitSeasonList: List[Int] = getSeasonOpts(10)

  val userSkillLevelList: List[String] = List("beginner", "intermediate", "advanced", "pro")
  val lastGroupTypeList: List[String] = List("group", "family", "solo")

  implicit val lastVisitFormat: OFormat[LastVisit] = Json.format[LastVisit]
  implicit val OurMongoFormat: OFormat[OurMongo] = Json.format[OurMongo]

  //val orderingByCountryName: Ordering[OurMongo] = Ordering.by(e => e.countryName)

}

object OurMongoForm extends TransformersTrait {

  val form = Form(
    mapping(
      "_id" -> ignored(Option.empty[BSONObjectID]),
      "usrId" -> optional(
        text.transform(str2bson, bson2str)
      ),
      "sessionId" -> optional(text),
      "rstId" -> optional(
        text.transform(str2bson, bson2str)
      ),
      "userSkillLevel" -> optional(text),
      "numOfResortVisits" -> optional(number),
      "lastVisit_e" -> optional(mapping(
        "selection" -> nonEmptyText,
        "date" -> optional(date)
      )(LastVisit.apply)(LastVisit.unapply)),
      "lastGroupType" -> optional(text),
      "liftFacilitiesRating" -> optional(number(min = 0, max = 100)),
      "atmosphereRating" -> optional(number(min = 0, max = 100)),
      "funRating" -> optional(number(min = 0, max = 100)),
      "suitabilityRating" -> optional(number(min = 0, max = 100)),
      "valueRating" -> optional(number(min = 0, max = 100)),
      "overallRating" -> optional(number(min = 0, max = 100)),
      "liklihoodToVisitAgain" -> optional(number(min = 0, max = 100)),
      "adminCreatedId" -> optional(number),
      "dateCreated" -> optional(date),
      "adminModifiedId" -> optional(number),
      "lastModified" -> optional(date).transform(date2Date, date2NowDate)
      /*,
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
      */
    )(OurMongo.apply)(OurMongo.unapply)
  )


}


// test root model for mongo
// "nestedField1" : "Lazarus", "nestedField2" : "Jesus"
case class TestModel(_id: Option[BSONObjectID] = None, field1: String = "", field2: String = "", nestedField_e: JsObject = Json.obj())

object TestModel extends ReadAndWriteTrait {

  implicit val testModelFormat: OFormat[TestModel] = Json.format[TestModel]

}

case class TestCommands(_id: Option[BSONObjectID] = None, field1: String = "", field2: String = "", nestedField_e: JsObject = Json.obj())

object TestCommands extends ReadAndWriteTrait {

  implicit val testCommandsModelFormat: OFormat[TestCommands] = Json.format[TestCommands]

}

// test outputs to aggregations to update scoreBG in rst - TODO DELETE **
case class BGrunTypes(greenRunsNum: Int)
case class BGliftTypes(buttonLiftsNum: Int)
case class RstData(runTypes_e: Option[BGrunTypes], liftTypes_e: Option[BGliftTypes]) {
  def runTypesAsObj = runTypes_e.get
  def liftTypesAsObj = liftTypes_e.get
}

//case class ScoreBG(_id: Option[BSONObjectID] = None, userSkillLevel: String, overallRating: Double) extends AdminCollectionTrait {
//case class ScoreBG(_id: Option[BSONObjectID] = None, rstId: Option[BSONObjectID], userSkillLevel: String, lastVisit_e: Option[LastVisit],
//                   overallRating: Double, rstData_e: Option[RstData]) extends AdminCollectionTrait {
case class ScoreBG(_id: Option[BSONObjectID] = None, rstId: Option[BSONObjectID], userSkillLevel: String, lastVisit_e: Option[LastVisit],
                   overallRating: Double, rstData_e: Option[RstData]) extends AdminCollectionTrait {

  def lastVisitAsObj = lastVisit_e.get
  def rstDataAsObj = rstData_e.get

  //"_id" : ObjectId("5a6680b30ff285f13593f1b9"), "rstId" : ObjectId("59ea36d3fdc2ce3d73c4be94"), "userSkillLevel" : "beginner", "lastVisit_e" : { "selection" : "season starting", "date" : ISODate("2018-01-01T00:00:00Z") }, "overallRating" : 7, "rstData" : { "runTypes_e" : { "greenRunsNum" : 14 }, "liftTypes_e" : { "buttonLiftsNum" : 18 } } }
  def idAsBsonId(oId: Option[BSONObjectID]) = oId.get

  def idAsString(oId: Option[BSONObjectID]) = idAsBsonId(oId).stringify

  def idAsStringLast6(oId: Option[BSONObjectID]) = {
    val idString = idAsString(oId)
    idString.substring(idString.length - 6)
  }

}

object ScoreBG extends ReadAndWriteTrait {

  implicit val lastVisitFormat: OFormat[LastVisit] = Json.format[LastVisit]
  implicit val BGrunTypesFormat: OFormat[BGrunTypes] = Json.format[BGrunTypes]
  implicit val BGliftTypesFormat: OFormat[BGliftTypes] = Json.format[BGliftTypes]
  implicit val RstDataFormat: OFormat[RstData] = Json.format[RstData]
  implicit val scoreBGFormat: OFormat[ScoreBG] = Json.format[ScoreBG]
  //val orderingByCountryName: Ordering[OurMongo] = Ordering.by(e => e.countryName)

}


case class ScoreBA(_id: Option[BSONObjectID] = None, scoreBA: Double, lastModified: Option[Date]) extends ReadAndWriteTrait with DateTransformTrait {

  def idAsBsonId(oId: Option[BSONObjectID]) = oId.get

  def idAsString(oId: Option[BSONObjectID]) = idAsBsonId(oId).stringify

}

object ScoreBA extends ReadAndWriteTrait {


  implicit val scoreBGFormat: OFormat[ScoreBA] = Json.format[ScoreBA]

}

//case class BulkUpdateTry()

// "q" -> Json.obj("_id" -> Json.obj("$oid" -> "5a28258d9830b7614f566cc7")),
// "u" -> Json.obj("scores_e.scoreBA" -> 1.11)

case class UpdateElements1(idStr: String, preU: JsObject, upsert: Boolean = false, multi: Boolean = false) {

  def q = Json.obj("_id" -> Json.obj("$oid" -> idStr))
  def u = Json.obj("$set" -> preU)
  //UpdateElements2(q, u)
}

case class UpdateElements2(q: JsObject, u: JsObject, upsert: Boolean = false, multi: Boolean = false)

case class IdObject($oid: String)
case class IdSelector(_id: IdObject)

case class _Scores(scoreBA: Double)
case class UpdateSelector(scores_e: _Scores)

case class UpdateScore(q: IdSelector, u: UpdateSelector) extends ReadAndWriteTrait

object IdSelector extends ReadAndWriteTrait {
  implicit val _scoresFormat: OFormat[_Scores] = Json.format[_Scores]
  implicit val updateSelectorFormat: OFormat[UpdateSelector] = Json.format[UpdateSelector]
}

object UpdateSelector extends ReadAndWriteTrait {
  implicit val idObjectFormat: OFormat[IdObject] = Json.format[IdObject]
  implicit val idSelectorFormat: OFormat[IdSelector] = Json.format[IdSelector]
}

object UpdateScore extends ReadAndWriteTrait {

  implicit val idObjectFormat: OFormat[IdObject] = Json.format[IdObject]
  implicit val idSelectorFormat: OFormat[IdSelector] = Json.format[IdSelector]

  implicit val _scoresFormat: OFormat[_Scores] = Json.format[_Scores]
  implicit val updateSelectorFormat: OFormat[UpdateSelector] = Json.format[UpdateSelector]
  implicit val updateScoreFormat: OFormat[UpdateScore] = Json.format[UpdateScore]

}

/*
db.rst.updateOne({"_id": ObjectId("59ea36d3fdc2ce3d73c4be94")}, { "name": "Chamonix22", "metricsAndVisitors_e": { "pisteArea_km2": 22.5, "highestAltitude_m": 1016, "avgAnnualVisitors": 1153200, "avgVistorDensityPerKm2PerDay": 330.67 }, "description_e": { "short": "A very short description for Chamonix", "full": "A muchos fullerz description of what to expect in this resorts" }, "location_e": { "countryCode": "FR", "countryName": "France", "continent": "Europe", "hemisphere": "Northern", "region_ee": { "id": ObjectId("5a4d055472b91d8f2e4b9204"), "name": "Alps") }, "language": "French", "geoLocation_ee": { "type": "point", coordinates: [45.923733, 6.870129]] }} "runsParksLifts_e": { "runsTotal": 31, "parksTotal": 5, "liftsTotal": 57 }, "runTypes_e": { "greenRunsNum": 14, "blueRunsNum": 22, "redRunsNum": 19, "blackRunsNum": 6, "greenCircleRunsNum": 0, "blueSquareRunsNum": 0, "blackDiamondRunsNum": 0, "blackDoubleDiamondRunsNum": 0, "blackTripleDiamondRunsNum": 0 }, "liftTypes_e": { "gondolaLiftsNum": 5, "buttonLiftsNum": 18, "chairLiftsNum": 57, "cableCarLiftsNum": 14, "movingCarpetLiftsNum": 2 }, "season_e": { "last_ee": { "open": new Date("2017-11-20"), "closed": new Date("2018-04-25") }, "next_ee": { "open": new Date("2018-11-22"), "closed": new Date("2017-12-02") }}, "liftPassPrices_e": { "areaValidArr": [ "Brevent-Flegere" ], "pricesValidFrom": new Date("2017-11-20"), "pricesValidUntil": new Date("2018-04-24"), "priceChildWeekDay_usd": 20.0, "priceAdultWeekDay_usd": 80.0, "priceChild6Day_usd": 40.0, "priceAdult6Day_usd": 200.0, "priceDeepLinkArr": ["http://www.google.com/extra", "http://www.lift-house2.com"]}, "localIataArr_e": {[{ "iataCode": "GVS", "distance_km": 120.4, "travelTime_mins": 115, "roundTripCosts_usd": 81.80 }, { "iataCode": "CMF", "distance_km": 150.7, "travelTime_mins": 138, "roundTripCosts_usd": 89.95 }]}, "localDomesticAirportArr_e": [], "scores_e": { "scoreBA" : 1.1, "scoreSFdef" : 2.22, "scoreBG" : 0.48863140005023187, "scoreFMpre" : 0.1, "scoreFMdefPre" : 0.1, "scoreLCpre" : 0.1, "scoreGR" : 0.1, "scoreAD" : 0.1, "scoreNL" : 0.1, "scoreFDpre" : 0.1, "scorePR" : 0.1 }, "eventsProductsPromotionsThisSeason_e": { "totalEventsThisSeason": 15, "totalProductItemsThisSeason": 57, "totalPromotionsThisSeason": 8 }, "adminModifiedId": 1, "dateCreated": new Date(), "lastModified": new Date() })

db.rst.updateOne({"_id": ObjectId("59ea36d3fdc2ce3d73c4be94")}, {
"name": "Chamonix22",
"metricsAndVisitors_e": { "pisteArea_km2": 22.5, "highestAltitude_m": 1016, "avgAnnualVisitors": 1153200, "avgVistorDensityPerKm2PerDay": 330.67 },
"description_e": { "short": "A very short description for Chamonix", "full": "A muchos fullerz description of what to expect in this resorts" },
"location_e": { "countryCode": "FR", "countryName": "France", "continent": "Europe", "hemisphere": "Northern",
"region_ee": { "id": ObjectId("5a4d055472b91d8f2e4b9204"), "name": "Alps") },
"language": "French",
"geoLocation_ee": { "type": "point", coordinates: [45.923733, 6.870129]] }
}
"runsParksLifts_e": { "runsTotal": 31, "parksTotal": 5, "liftsTotal": 57 },
"runTypes_e": { "greenRunsNum": 14, "blueRunsNum": 22, "redRunsNum": 19, "blackRunsNum": 6, "greenCircleRunsNum": 0, "blueSquareRunsNum": 0, "blackDiamondRunsNum": 0, "blackDoubleDiamondRunsNum": 0, "blackTripleDiamondRunsNum": 0 },
"liftTypes_e": { "gondolaLiftsNum": 5, "buttonLiftsNum": 18, "chairLiftsNum": 57, "cableCarLiftsNum": 14, "movingCarpetLiftsNum": 2 },
"season_e": { "last_ee": { "open": new Date("2017-11-20"), "closed": new Date("2018-04-25") }, "next_ee": { "open": new Date("2018-11-22"), "closed": new Date("2017-12-02") }},
"liftPassPrices_e": {
"areaValidArr": [ "Brevent-Flegere" ], "pricesValidFrom": new Date("2017-11-20"), "pricesValidUntil": new Date("2018-04-24"),
"priceChildWeekDay_usd": 20.0, "priceAdultWeekDay_usd": 80.0, "priceChild6Day_usd": 40.0,
"priceAdult6Day_usd": 200.0, "priceDeepLinkArr": ["http://www.google.com/extra", "http://www.lift-house2.com"]
},
"localIataArr_e": {[
{ "iataCode": "GVS", "distance_km": 120.4, "travelTime_mins": 115, "roundTripCosts_usd": 81.80 }
{ "iataCode": "CMF", "distance_km": 150.7, "travelTime_mins": 138, "roundTripCosts_usd": 89.95 }
]},
"localDomesticAirportArr_e": [],
"scores_e": { "scoreBA" : 1.1, "scoreSFdef" : 2.22, "scoreBG" : 0.48863140005023187, "scoreFMpre" : 0.1, "scoreFMdefPre" : 0.1, "scoreLCpre" : 0.1, "scoreGR" : 0.1, "scoreAD" : 0.1, "scoreNL" : 0.1, "scoreFDpre" : 0.1, "scorePR" : 0.1 },
"eventsProductsPromotionsThisSeason_e": { "totalEventsThisSeason": 15, "totalProductItemsThisSeason": 57, "totalPromotionsThisSeason": 8 },
"adminModifiedId": 1,
"dateCreated": new Date(),
"lastModified": new Date()
})
*/