package services.dbClient

import java.time.Instant
import java.util.Date
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.math._
import play.api.libs.json.{Json, _}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.{AbstractController, ControllerComponents, PlayBodyParsers, Result}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import models.db._
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.commands.{Project, Push}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.{Helpers, JSONCollection}

/**
  * Created by sambo on 18/10/2017.
  */
class UpdateScores @Inject() (cc: ControllerComponents, actorSystem: ActorSystem)(val reactiveMongoApi: ReactiveMongoApi)
                             (serviceClientDb: ServiceClientDb, rstService: RstService)(implicit mat: Materializer)
  extends AbstractController(cc) with MongoController with ReactiveMongoComponents with StringTransformTrait {

  override lazy val parse: PlayBodyParsers = cc.parsers

  // todo - control types expected if using different models on output OR use unit
  def updateScore(cat: String): Future[Unit] = cat match {

    case "ba" => upsertScoreBA(1)
    //case "sf_def" => updateScoreSF_def
    //case "bg" => updateScoreBG,
    //case "fm_pre" => updateScoreFM_pre,
    //case "fm_defPre" => updateScoreFM_defPre,
    //case "lc_pre" => updateScoreLC_pre,
    //case "gr" => updateScoreGR,
    //case "ad" => updateScoreAD,
    //case "nl" => updateScoreNL,
    //case "fd" => updateScoreFD,
    //case "pr" => updateScorePR,

  }

  def getScore(attr: String, xThis: Double, xMax: Double, xMin: Double): Double = {

    val bestIs = attr match {
      case "ba" | "sf_def" => "high" // e.g. ba, sf
      case "_" => "low"
      // ba, sf, bg, fm, lc, gr, ad, tt, nl, fd, pr
    }

    calculateFixedScore(bestIs, xThis, xMax, xMin)
  }

  def calculateFixedScore(best: String, xThis: Double, xMax: Double, xMin: Double): Double = best match {

    case "high" => (1 / (xMax - xMin)) * (xThis - xMin)
    case "low" => (1 / (xMax - xMin)) * (xMax - xThis)
    case "_" => 0

  }

  def upsertScoreBA(part: Int): Future[Unit] = {

    serviceClientDb.getCollection("rst").flatMap(col =>
      part match {
        case 1 => upsertCollectionScoreBA(col)
        //case 2 => rawResult
      }
    )
    //)
  }


  def upsertCollectionScoreBA(col: JSONCollection): Future[Unit] = {

    import col.BatchCommands.AggregationFramework.{Sort, Descending, Project, MaxField, MinField, UnwindField, GroupField, Push, Out}

    col.aggregate(
        Sort(Descending("metricsAndVisitors_e.pisteArea_km2")),
        List(
          GroupField("all")(
            "idArr" -> Push(
              Json.obj(
                "rstId" -> "$_id",
                "paThis" -> "$metricsAndVisitors_e.pisteArea_km2"
              )
            ),
            "paMax" -> MaxField("metricsAndVisitors_e.pisteArea_km2"),
            "paMin" -> MinField("metricsAndVisitors_e.pisteArea_km2")
          ),
          UnwindField("idArr"),
          Project(
            Json.obj(
              "_id" -> "$idArr.rstId",
              "scoreBA" -> Json.obj(
                "$multiply" -> Json.arr(
                  Json.obj(
                    "$divide" -> Json.arr(1, Json.obj("$subtract" -> Json.arr("$paMax", "$paMin")))
                  ),
                  Json.obj("$subtract" -> Json.arr("$idArr.paThis", "$paMin"))
                )
              ),
              "lastModified" -> Json.obj(
                "$date" -> new Date()
              )
            )
          ),
          Out("scoreBA")
        )
    ).map(_ => {})
    //(1 /  (paMax - paMin)) * (paThis - paMin)

  }


  def usBulkUpdateScoreBA(part: Int) = {

    //val ScoreBAList = List(ScoreBA(Some(str2bson("5a28258d9830b7614f566cc7")), 7.2, Some(new Date())))
    serviceClientDb.getCollection("rst").flatMap(col =>
      part match {
        //case 1 => usBulkUpdateScoreBA1(col)
        //case 2 => usBulkUpdateScoreBA2(col)
        //case 3 => usBulkUpdateScoreBA3(col)
        //case 4 => usBulkUpdateScoreBA4(col)
        case 5 => usBulkUpdateScoreBA5(col)
      }
    )

  }


  def getJsObjDocs(collectionName: String)(implicit reads: Reads[JsObject]): Future[Seq[JsObject]] = {

    serviceClientDb.getCollection(collectionName).flatMap(
      // find all
      _.find(Json.obj())
        // perform the query and get a cursor of JsObject
        .cursor[JsObject](ReadPreference.primary)
        // Collect the results as a list
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]())
    )

  }

  def displayTransformer = {

    val fromJsPath = (__ \ "scoreBA")
    val toJsPath = (__ \ "scores_e.scoreBA")

    setTransformedDocs(fromJsPath, toJsPath).map(seq => Ok(seq.mkString("\n")))

  }
  
  def tryTransformer(obj: JsObject, transformer: Reads[JsObject]) = {

    obj.transform(transformer) match {
      case JsSuccess(r: JsObject, _) => r//Ok(r)
      case e: JsError => JsError.toJson(e)//Ok(JsError.toJson(e))
      //case e: JsError => s"Errors: ${JsError.toJson(e).toString()}"
    }

  }

  // todo - move to queryBuilder \\
  def set(data: JsObject): JsObject = Json.obj("$set" -> data)

  def getQueryId(idStr: String) = Json.obj("_id" -> Json.obj("$oid" -> idStr))

  def getSetUpdate(fieldName: String, fieldValue: Double) = set(Json.obj(fieldName -> fieldValue))


  def getTransformedList = {

    val fromJsPath = (__ \ "scoreBA")
    val toJsPath = (__ \ "scores_e.scoreBA")
    //val toJsPath = (__ \ "u" \ "scores_e.scoreBA")

    setTransformedDocs(fromJsPath, toJsPath)

  }

  def setTransformedDocs(fromJsPath: JsPath, toJsPath: JsPath): Future[Seq[Map[String, JsObject]]] = { // : Future[Seq[JsObject]]

    val qBranch = (__ \ '_id).json.copyFrom( (__ \ '_id).json.pick )
    //val uBranch = (__ \ 'scores_e \ 'scoreBA).json.copyFrom( (__ \ 'scoreBA).json.pick )
    val uBranch = toJsPath.json.copyFrom( fromJsPath.json.pick )


    getJsObjDocs("scoreBA").map(docs =>


      docs.map(doc => Map(
        "q" -> tryTransformer(doc, qBranch),
        "u" -> set(tryTransformer(doc, uBranch))
      ))

      /*
      // attempt to lessen transformations made
      val qBranch2 = ((__ \ 'q \ '_id).json.copyFrom( (__ \ '_id).json.pick ) and
        toJsPath.json.copyFrom( fromJsPath.json.pick )) reduce

      docs.map(doc => tryTransformer(doc, qBranch2))
      */

    )

  }


  def usBulkUpdateScoreBA5(coll: JSONCollection) = {

    val builder: coll.UpdateBuilder = coll.update(ordered = false)

    getTransformedList.map(docs =>

      Future.sequence(
        docs.map(doc =>
          builder.element(doc("q"), doc("u"), false, false)
        )
      ).flatMap(builder.many(_)).map { r =>
        (r.n, r.nModified, r.upserted.size)
      }

    )

  }

//db.rst.updateOne({ "_id": ObjectId("59ea36d3fdc2ce3d73c4be94") }, { $set: { "scores_e": { "scoreSFdef": 2.22, "scoreBG": 0.48863140005023187, "scoreFMpre": 0.1, "scoreFMdefPre": 0.1, "scoreLCpre": 0.1, "scoreGR": 0.1, "scoreAD": 0.1 , "scoreNL": 0.1 , "scoreFDpre": 0.1 , "scorePR": 0.1 }}})
//db.rst.updateOne({ "_id": ObjectId("5a28258d9830b7614f566cc7") }, { $set: { "scores_e": { "scoreSFdef": 8.88, "scoreBG": 0.48879753018521005, "scoreFMpre": 0, "scoreFMdefPre": 0.1, "scoreLCpre": 0.2, "scoreGR": 0.1, "scoreAD": 0, "scoreNL": 0.1 , "scoreFDpre": 0.3 , "scorePR": 0.4 }}})

    /*
    def updateScoreSF_def: Future[Seq[Resort]] = {

      val format = new java.text.SimpleDateFormat("dd-MM-yyyy")

      val fromDate = new Date// today
      val durationDays = 3 // default duration
      val endDate = fromDate.toInstant.plusSeconds(60*60*24*durationDays)
      val toDate = format.format(Date.from(endDate))// today + durationDays

      val fromDateAsString = "14-09-2017"//format.format(fromDate)// e.g. 19-10-2017
      val toDateAsString = "24-09-2017"//format.format(toDate)// e.g. 22-10-2017

      val docs = wthService.getWeatherAggregateCol(Some(fromDateAsString), Some(toDateAsString)).map(r => r.sorted(WeatherAggregate.orderingByAvgSnowfall))

      docs.map(r =>
        r.map(r2 =>
          rstService.updateOneField(r2._id, Json.obj("scoreSF" -> getScore("sf_def", r2.avgSnowfall, r.last.avgSnowfall, r.head.avgSnowfall)))
        )
      )

      rstService.getAllDocs

    }


  def updateScoreBG: Future[Seq[RstMongo]] = {


      //-- [br] beginner ratings (OURs)-- (zero influence if no records)
      //-beginner ratings in the most recent season score- (max 0.8)
      //avg beginner ratings in the most recent season
      val brMax_opt = 0.5

      //avg beginner ratings in the most recent season
      val brmrsMax = 0.8 // [brmrs] is beginner ratings in the most recent season
      val braoMax = 0.2 // [brao] is beginner ratings in all other seasons
      // =$D19*(($D20*F23)+($D24*F27))

      //num of beginner ratings in the most recent season (regulator 1.5)
      val brmrsRegulator = 1.5
      val thisBRmrsNumRatings = 1000 // todo - change to get from OUR in DB
      val thisLogBRmrs = log10(thisBRmrsNumRatings + 1)
      val thisBRmrsMax_opt = 1 - (1 / pow(thisLogBRmrs + 1, (thisLogBRmrs/brmrsRegulator)))

      //-all other beginner ratings score- (max 0.2)
      //avg of all other beginner ratings
      //num of all other beginner ratings (regulator 1.5)
      val braoRegulator = 1.5
      val thisBRaoNumRatings = 1000 // todo - change to get from OUR in DB
      val thisLogBRao = log10(thisBRaoNumRatings + 1)
      val thisBRaoMax_opt = 1 - (1 / pow(thisLogBRao + 1, (thisLogBRao/braoRegulator)))

      val thisBRmax = brMax_opt * ((brmrsMax * thisBRmrsMax_opt) + (braoMax * thisBRaoMax_opt))

      //--- [gcr] num of green (or green circle) runs --- //
      val gcrMax = 0.6
      val gcrNum = 20 // todo - change to get from RST in DB
      val thisGCRmax = gcrMax * (1 - thisBRmax)
      //
      //=thisGCRmax*(1/(MAX($F13:$J13)-MIN($F13:$J13)))*(F13-MIN($F13:$J13))
      // (1 / (xMax - xMin)) * (xThis - xMin) // high is best

      //--- [bl] num of button lifts --- //
      val blMax = 0.4
      val blNum = 20 // todo - change to get from RST in DB
      val thisBLmax = blMax * (1 - thisBRmax)

      // (1 / (xMax - xMin)) * (xMax - xThis) // low is best


      // todo extract from DB (as aggregate)
      // __per resort__
      // -rst-
      // _gcrNum - number of button lifts
      // max, min
      // blNum - num of green (or green circle) runs
      // max, min
      // -our-
      // thisBRmrsNumRatings
      // thisBRaoNumRatings
      // db.our.aggregate([{ "$match": { "userSkillLevel": "beginner" }}, { "$bucket":  { "groupBy": "$lastVisit_e.date", "boundaries": [ new Date("2000"), new Date("2017"), new Date() ], "output": { "count": { "$sum": 1 }, "avgOverallRating": { "$avg": "$overallRating" }}}}]);

      */

  /*


// db.our.aggregate([{ $lookup: { "from": "rst", "localField": "rstId", "foreignField": "_id", "as": "rstData" }}, { $project: { "rstId": 1, "lastVisit_e": 1, "userSkillLevel": 1, "overallRating": 1, "rstData.runTypes_e.greenRunsNum": 1, "rstData.liftTypes_e.buttonLiftsNum": 1 }}, { $unwind: "$rstData" }, { $group: { "_id": "all", "maxGCR": { $max: "$rstData.runTypes_e.greenRunsNum" }, "minGCR": { $min: "$rstData.runTypes_e.greenRunsNum" }, "maxBL": { $max: "$rstData.liftTypes_e.buttonLiftsNum" }, "minBL": { $min: "$rstData.liftTypes_e.buttonLiftsNum" }, "rstArr": { $push: { "rstId": "$rstId", "userSkillLevel": "$userSkillLevel", "lastVisit_e": "$lastVisit_e", "overallRating": "$overallRating", "gcrNum": "$rstData.runTypes_e.greenRunsNum", "blNum": "$rstData.liftTypes_e.buttonLiftsNum" }}}}, { $unwind: "$rstArr" }, { $match: { "rstArr.userSkillLevel": "beginner" }}, { $group: { "_id": { "rstId": "$rstArr.rstId", "manualFactor": { $cond: [ { $gt: [ "$rstArr.lastVisit_e.date", new Date("2017")]}, 0.8, 0.2 ]}}, "count": { "$sum": 1 }, "thisGCR": { $first: "$rstArr.gcrNum" }, "maxGCR": { $first: "$maxGCR" }, "minGCR": { $first: "$minGCR" }, "maxBL": { $first: "$maxBL" }, "minBL": { $first: "$minBL" }, "avgOverallRating": { $avg: "$rstArr.overallRating" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10"]}, { $divide: ["$$myLog10", 1.5]}]}]}]}}}}}, { $project: { "count": 1, "userSkillLevel": 1, "avgOverallRating": 1, "thisGCR": 1, "maxGCR": 1, "minGCR": 1, "maxBL": 1, "minBL": 1, "log10MaxScore": 1, "maxScore": { $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]}, "score": { $multiply: ["$log10MaxScore", { $divide: ["$avgOverallRating", 100]}, "$_id.manualFactor"]}}}, { $group: { "_id": "$_id.rstId", "thisGCR": { $first: "$thisGCR" }, "totalMaxScore": { $sum: "$maxScore" }, "totalScore": { $sum: "$score" }}}, { $project: { "maxScoreBR": { $multiply: [0.5, "$totalMaxScore"]}, "scoreBR": { $multiply: [0.5, "$totalScore"]}, "maxScoreGCR": { $multiply: [ 0.6, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}, "maxScoreBL": { $multiply: [ 0.4, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}}}, { $addFields: { "scoreGCR": { $multiply: [ "$maxScoreGCR", { $divide: [1, { $subtract: [ "$maxGCR", "$minGCR" ] }]}, { $subtract: [ "$thisGCR", "$minGCR" ]}]}}}]).pretty();

      { rstId : ObjectId("59ea36d3fdc2ce3d73c4be94"), lastVisit_e: { selection: departureDate, date: 2016-05-02 }, numOfResortVisits: 2, overallRating: 50 }
      { rstId : ObjectId("59ea36d3fdc2ce3d73c4be94"), lastVisit_e: { selection: seasonStarting, date: 2014-01-01 }, numOfResortVisits: 1, overallRating: 50 }
      { rstId : ObjectId("5a732822e62fc5ab4dcb5d8c"), lastVisit_e: { selection: now, date: 2018-02-07 }, numOfResortVisits: 1, overallRating: 50 }

      // todo - WE WANT

        // our beginner ratings - grouped by $rstId & $lastVisit_e.date
        // 1) db.our.aggregate([{ $match: { "userSkillLevel": "beginner" }}, { $group: { "_id": { "rstId": "$rstId", "manualFactor": { $cond: [ { $gt: [ "$lastVisit_e.date", new Date("2017")]}, 0.8, 0.2 ]}}, "count": { "$sum": 1 }, "avgOverallRating": { $avg: "$overallRating" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10"]}, { $divide: ["$$myLog10", 1.5]}]}]}]}}}}}, { $project: { "rstId": 1, "count": 1, "log10MaxScore": 1, "avgOverallRating": 1, "manualFactor": 1, "maxScore": { $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]}, "score": { $multiply: ["$log10MaxScore", { $divide: ["$avgOverallRating", 100]}, "$_id.manualFactor"]}}}]);
        // 2) db.our.aggregate([{ $lookup: { "from": "rst", "localField": "rstId", "foreignField": "_id", "as": "rstData" }}, { $project: { "rstId": 1, "lastVisit_e": 1, "userSkillLevel": 1, "overallRating": 1, "rstData.runTypes_e.greenRunsNum": 1, "rstData.liftTypes_e.buttonLiftsNum": 1 }}, { $unwind: "$rstData" }, { $group: { "_id": "all", "maxGCR": { $max: "$rstData.runTypes_e.greenRunsNum" }, "minGCR": { $min: "$rstData.runTypes_e.greenRunsNum" }, "maxBL": { $max: "$rstData.liftTypes_e.buttonLiftsNum" }, "minBL": { $min: "$rstData.liftTypes_e.buttonLiftsNum" }, "rstArr": { $push: { "rstId": "$rstId", "userSkillLevel": "$userSkillLevel", "lastVisit_e": "$lastVisit_e", "overallRating": "$overallRating", "gcrNum": "$rstData.runTypes_e.greenRunsNum", "blNum": "$rstData.liftTypes_e.buttonLiftsNum" }}}}, { $unwind: "$rstArr" }, { $match: { "rstArr.userSkillLevel": "beginner" }}, { $group: { "_id": { "rstId": "$rstArr.rstId", "manualFactor": { $cond: [ { $gt: [ "$rstArr.lastVisit_e.date", new Date("2017")]}, 0.8, 0.2 ]}}, "count": { "$sum": 1 }, "maxGCR": { $first: "$maxGCR" }, "minGCR": { $first: "$minGCR" }, "maxBL": { $first: "$maxBL" }, "minBL": { $first: "$minBL" }, "avgOverallRating": { $avg: "$rstArr.overallRating" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10"]}, { $divide: ["$$myLog10", 1.5]}]}]}]}}}}}, { $project: { "count": 1, "userSkillLevel": 1, "avgOverallRating": 1, "maxGCR": 1, "minGCR": 1, "maxBL": 1, "minBL": 1, "log10MaxScore": 1, "maxScore": { $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]}, "score": { $multiply: ["$log10MaxScore", { $divide: ["$avgOverallRating", 100]}, "$_id.manualFactor"]}}}]).pretty();
        // todo - get last year by individual resort, i.e. no more than one season ago

        // our beginner ratings - grouped by $rstId
        // db.our.aggregate([{ $match: { "userSkillLevel": "beginner" }}, { $group: { "_id": { "rstId": "$rstId", "manualFactor": { $cond: [ { $gt: [ "$lastVisit_e.date", new Date("2017")]}, 0.8, 0.2 ]}}, "count": { "$sum": 1 }, "avgOverallRating": { $avg: "$overallRating" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10"]}, { $divide: ["$$myLog10", 1.5]}]}]}]}}}}}, { $project: { "rstId": 1, "count": 1, "log10MaxScore": 1, "avgOverallRating": 1, "manualFactor": 1, "maxScore": { $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]}, "score": { $multiply: ["$log10MaxScore", { $divide: ["$avgOverallRating", 100]}, "$_id.manualFactor"]}}}, { $group: { "_id": "$_id.rstId", "totalMaxScore": { $sum: "$maxScore" }, "totalScore": { $sum: "$score" }}}, { $project: { "totalMaxScoreBR": { $multiply: [0.5, "$totalMaxScore"]}, "totalScoreBR": { $multiply: [0.5, "$totalScore"]}}}]);

        // gets all max scores and scoreBR
        // db.our.aggregate([{ $lookup: { "from": "rst", "localField": "rstId", "foreignField": "_id", "as": "rstData" }}, { $project: { "rstId": 1, "lastVisit_e": 1, "userSkillLevel": 1, "overallRating": 1, "rstData.runTypes_e.greenRunsNum": 1, "rstData.liftTypes_e.buttonLiftsNum": 1 }}, { $unwind: "$rstData" }, { $group: { "_id": "all", "maxGCR": { $max: "$rstData.runTypes_e.greenRunsNum" }, "minGCR": { $min: "$rstData.runTypes_e.greenRunsNum" }, "maxBL": { $max: "$rstData.liftTypes_e.buttonLiftsNum" }, "minBL": { $min: "$rstData.liftTypes_e.buttonLiftsNum" }, "rstArr": { $push: { "rstId": "$rstId", "userSkillLevel": "$userSkillLevel", "lastVisit_e": "$lastVisit_e", "overallRating": "$overallRating", "gcrNum": "$rstData.runTypes_e.greenRunsNum", "blNum": "$rstData.liftTypes_e.buttonLiftsNum" }}}}, { $unwind: "$rstArr" }, { $match: { "rstArr.userSkillLevel": "beginner" }}, { $group: { "_id": { "rstId": "$rstArr.rstId", "manualFactor": { $cond: [ { $gt: [ "$rstArr.lastVisit_e.date", new Date("2017")]}, 0.8, 0.2 ]}}, "count": { "$sum": 1 }, "maxGCR": { $first: "$maxGCR" }, "minGCR": { $first: "$minGCR" }, "maxBL": { $first: "$maxBL" }, "minBL": { $first: "$minBL" }, "avgOverallRating": { $avg: "$rstArr.overallRating" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10"]}, { $divide: ["$$myLog10", 1.5]}]}]}]}}}}}, { $project: { "count": 1, "userSkillLevel": 1, "avgOverallRating": 1, "maxGCR": 1, "minGCR": 1, "maxBL": 1, "minBL": 1, "log10MaxScore": 1, "maxScore": { $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]}, "score": { $multiply: ["$log10MaxScore", { $divide: ["$avgOverallRating", 100]}, "$_id.manualFactor"]}}}, { $group: { "_id": "$_id.rstId", "totalMaxScore": { $sum: "$maxScore" }, "totalScore": { $sum: "$score" }}}, { $project: { "maxScoreBR": { $multiply: [0.5, "$totalMaxScore"]}, "scoreBR": { $multiply: [0.5, "$totalScore"]}, "maxScoreGCR": { $multiply: [ 0.6, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}, "maxScoreBL": { $multiply: [ 0.4, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}}}]).pretty();

        // gets all max scores and scores
        db.our.aggregate([{ $lookup: { "from": "rst", "localField": "rstId", "foreignField": "_id", "as": "rstData" }}, { $project: { "rstId": 1, "lastVisit_e": 1, "userSkillLevel": 1, "overallRating": 1, "rstData.runTypes_e.greenRunsNum": 1, "rstData.liftTypes_e.buttonLiftsNum": 1 }}, { $unwind: "$rstData" }, { $group: { "_id": "all", "maxGCR": { $max: "$rstData.runTypes_e.greenRunsNum" }, "minGCR": { $min: "$rstData.runTypes_e.greenRunsNum" }, "maxBL": { $max: "$rstData.liftTypes_e.buttonLiftsNum" }, "minBL": { $min: "$rstData.liftTypes_e.buttonLiftsNum" }, "rstArr": { $push: { "rstId": "$rstId", "userSkillLevel": "$userSkillLevel", "lastVisit_e": "$lastVisit_e", "overallRating": "$overallRating", "gcrNum": "$rstData.runTypes_e.greenRunsNum", "blNum": "$rstData.liftTypes_e.buttonLiftsNum" }}}}, { $unwind: "$rstArr" }, { $match: { "rstArr.userSkillLevel": "beginner" }}, { $group: { "_id": { "rstId": "$rstArr.rstId", "manualFactor": { $cond: [ { $gt: [ "$rstArr.lastVisit_e.date", new Date("2017")]}, 0.8, 0.2 ]}}, "count": { "$sum": 1 }, "avgOverallRating": { $avg: "$rstArr.overallRating" }, "thisGCR": { $first: "$rstArr.gcrNum" }, "maxGCR": { $first: "$maxGCR" }, "minGCR": { $first: "$minGCR" }, "thisBL": { $first: "$rstArr.blNum" }, "maxBL": { $first: "$maxBL" }, "minBL": { $first: "$minBL" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10"]}, { $divide: ["$$myLog10", 1.5]}]}]}]}}}}}, { $project: { "count": 1, "userSkillLevel": 1, "avgOverallRating": 1, "log10MaxScore": 1, "maxScore": { $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]}, "score": { $multiply: ["$log10MaxScore", { $divide: ["$avgOverallRating", 100]}, "$_id.manualFactor"]}, "thisGCR": 1, "maxGCR": 1, "minGCR": 1, "thisBL": 1, "maxBL": 1, "minBL": 1 }}, { $group: { "_id": "$_id.rstId", "totalMaxScore": { $sum: "$maxScore" }, "totalScore": { $sum: "$score" }, "thisGCR": { $first: "$thisGCR" }, "maxGCR": { $first: "$maxGCR" }, "minGCR": { $first: "$minGCR" }, "thisBL": { $first: "$thisBL" }, "maxBL": { $first: "$maxBL" }, "minBL": { $first: "$minBL" }}}, { $project: { "maxScoreBR": { $multiply: [0.5, "$totalMaxScore"]}, "scoreBR": { $multiply: [0.5, "$totalScore"]}, "maxScoreGCR": { $multiply: [ 0.6, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}, "maxScoreBL": { $multiply: [ 0.4, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}, "thisGCR": 1, "maxGCR": 1, "minGCR": 1, "thisBL": 1, "maxBL": 1, "minBL": 1 }}, { $addFields: { "scoreGCR": { $multiply: [ "$maxScoreGCR", { $divide: [1, { $subtract: [ "$maxGCR", "$minGCR" ] }]}, { $subtract: [ "$thisGCR", "$minGCR" ]}]}, "scoreBL": { $multiply: [ "$maxScoreBL", { $divide: [1, { $subtract: [ "$maxBL", "$minBL" ] }]}, { $subtract: [ "$maxBL", "$thisBL" ]}]}}}]).pretty();

        // todo - convert this into reactiveMongo/JSON collection: ** calculates & inserts BG score into rst **
        db.our.aggregate([{ $lookup: { "from": "rst", "localField": "rstId", "foreignField": "_id", "as": "rstData" }}, { $project: { "rstId": 1, "lastVisit_e": 1, "userSkillLevel": 1, "overallRating": 1, "rstData.runTypes_e.greenRunsNum": 1, "rstData.liftTypes_e.buttonLiftsNum": 1 }}, { $unwind: "$rstData" }, { $group: { "_id": "all", "maxGCR": { $max: "$rstData.runTypes_e.greenRunsNum" }, "minGCR": { $min: "$rstData.runTypes_e.greenRunsNum" }, "maxBL": { $max: "$rstData.liftTypes_e.buttonLiftsNum" }, "minBL": { $min: "$rstData.liftTypes_e.buttonLiftsNum" }, "rstArr": { $push: { "rstId": "$rstId", "userSkillLevel": "$userSkillLevel", "lastVisit_e": "$lastVisit_e", "overallRating": "$overallRating", "gcrNum": "$rstData.runTypes_e.greenRunsNum", "blNum": "$rstData.liftTypes_e.buttonLiftsNum" }}}}, { $unwind: "$rstArr" }, { $match: { "rstArr.userSkillLevel": "beginner" }}, { $group: { "_id": { "rstId": "$rstArr.rstId", "manualFactor": { $cond: [ { $gt: [ "$rstArr.lastVisit_e.date", new Date("2017")]}, 0.8, 0.2 ]}}, "count": { "$sum": 1 }, "avgOverallRating": { $avg: "$rstArr.overallRating" }, "thisGCR": { $first: "$rstArr.gcrNum" }, "maxGCR": { $first: "$maxGCR" }, "minGCR": { $first: "$minGCR" }, "thisBL": { $first: "$rstArr.blNum" }, "maxBL": { $first: "$maxBL" }, "minBL": { $first: "$minBL" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10"]}, { $divide: ["$$myLog10", 1.5]}]}]}]}}}}}, { $project: { "count": 1, "userSkillLevel": 1, "avgOverallRating": 1, "log10MaxScore": 1, "maxScore": { $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]}, "score": { $multiply: ["$log10MaxScore", { $divide: ["$avgOverallRating", 100]}, "$_id.manualFactor"]}, "thisGCR": 1, "maxGCR": 1, "minGCR": 1, "thisBL": 1, "maxBL": 1, "minBL": 1 }}, { $group: { "_id": "$_id.rstId", "totalMaxScore": { $sum: "$maxScore" }, "totalScore": { $sum: "$score" }, "thisGCR": { $first: "$thisGCR" }, "maxGCR": { $first: "$maxGCR" }, "minGCR": { $first: "$minGCR" }, "thisBL": { $first: "$thisBL" }, "maxBL": { $first: "$maxBL" }, "minBL": { $first: "$minBL" }}}, { $project: { "maxScoreBR": { $multiply: [0.5, "$totalMaxScore"]}, "scoreBR": { $multiply: [0.5, "$totalScore"]}, "maxScoreGCR": { $multiply: [ 0.5, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}, "maxScoreBL": { $multiply: [ 0.5, { $subtract: [ 1, { $multiply: [0.5, "$totalMaxScore"]}]}]}, "thisGCR": 1, "maxGCR": 1, "minGCR": 1, "thisBL": 1, "maxBL": 1, "minBL": 1 }}, { $addFields: { "scoreGCR": { $multiply: [ "$maxScoreGCR", { $divide: [1, { $subtract: [ "$maxGCR", "$minGCR" ] }]}, { $subtract: [ "$thisGCR", "$minGCR" ]}]}, "scoreBL": { $multiply: [ "$maxScoreBL", { $divide: [1, { $subtract: [ "$maxBL", "$minBL" ] }]}, { $subtract: [ "$maxBL", "$thisBL" ]}]}}}, { $project: { "scores_e.scoreBG": { $add: ["$scoreGCR", "$scoreBL", "$scoreBR"]}}}]).forEach(function(doc) { db.rst.updateOne({ "_id": doc._id}, { $set: { "scores_e.scoreBG": doc.scores_e.scoreBG }})});


        */

  /*
  // updateScoreBG
  def updateScoreBG(part: Int): Future[Option[Seq[ScoreBG]]] = {

    serviceClientDb.getCollection("rst").flatMap(col =>
      part match {
        case 1 => getScoreBG_aggregate1(col)
      }
    )
  }


  def getScoreBG_aggregate1(col: JSONCollection): Future[Option[Seq[ScoreBG]]] = {

    import col.BatchCommands.AggregationFramework.{Group, AddToSet, Lookup, Project, UnwindField, MaxField, MinField, Push, Match, SumField, AvgField}

    //col.aggregate(UnwindField("localIataArr_e")).map(_.head[TestRstAggregate])

    col.aggregate(
      Group(JsString("all"))(
        "_id" -> null,
        "rstData" -> AddToSet(
          Json.obj(
            "rstId" -> "_id",
            "gcrNum" -> "runTypes_e.greenRunsNum",
            "blNum" -> "liftTypes_e.buttonLiftsNum",
            "brCutOffDate" -> let()
              Json.obj(
              ("overallRating",
            "gcrNum" -> "rstData.runTypes_e.greenRunsNum",
            "blNum" -> "rstData.liftTypes_e.buttonLiftsNum"
          )
        )

    "_id": null,
    "rstData": {
      $addToSet: {
      "rstId": "$_id",
      "gcrNum": "$runTypes_e.greenRunsNum",
      "blNum": "$liftTypes_e.buttonLiftsNum",
      "brCutOffDate": {
        $let: {
          "vars": {
            "thisMonth": {
              $cond: [
                {
                  $eq: ["$location_e.hemisphere", "Southern"]
                },
                5,
                11
              ]
            }
          },
        "in": {
          $dateFromParts: {
            "year": {
              $subtract: [
                { $year: new Date() },
                {
                  $cond: [
                    { $eq: ["$$thisMonth", 5 ]},
                    {
                      $cond: [
                        {
                          $gt: [
                            { $month: new Date()},
                            10
                          ]
                        },
                        0,
                        1
                      ]
                    }, {
                      $cond: [
                        {
                          $lt: [
                            { $month: new Date()},
                            5
                          ]
                        },
                        2,
                        1
                      ]
    }
      ]
    }
      ]
    },
      "month" : "$$thisMonth",
      "day": 1
    }
    }
    }
    }
    }
    },

    )
    ).map(x => Option(x.head[ScoreBG]))

  }

  */
}
    /*
    def getScoreBG_aggregate3(col: JSONCollection): Future[Option[Seq[ScoreBG]]] = {

      import col.BatchCommands.AggregationFramework.{Lookup, Project, UnwindField, Group, MaxField, MinField, Push, Match, SumField, AvgField}

      //col.aggregate(UnwindField("localIataArr_e")).map(_.head[TestRstAggregate])

      col.aggregate(
        Lookup("rst", "rstId", "_id", "rstData_e"),
        List(
          Project(
            Json.obj(
              "rstId" -> 1,
              "lastVisit_e" -> 1,
              "userSkillLevel" -> 1,
              "overallRating" -> 1,
              "rstData_e.runTypes_e.greenRunsNum" -> 1,
              "rstData_e.liftTypes_e.buttonLiftsNum" -> 1
            )
          ),
          UnwindField("rstData_e"),
          Group(JsString("all"))(
            "maxGCR" -> MaxField("rstData.runTypes_e.greenRunsNum"),
            "minGCR" -> MinField("rstData.runTypes_e.greenRunsNum"),
            "maxBL" -> MaxField("rstData.liftTypes_e.buttonLiftsNum"),
            "minBL" -> MinField("rstData.liftTypes_e.buttonLiftsNum"),
            "rstArr" -> Push(
              Json.obj(
                "rstId" -> "rstId",
                "userSkillLevel" -> "userSkillLevel",
                "lastVisit_e" -> "lastVisit_e",
                "overallRating" -> "overallRating",
                "gcrNum" -> "rstData.runTypes_e.greenRunsNum",
                "blNum" -> "rstData.liftTypes_e.buttonLiftsNum"
              )
            )
          )

        )
      ).map(x => Option(x.head[ScoreBG]))

    }
    */


  // db.rst.aggregate([{ $group: { "_id": null, "rstData": { $addToSet: { "rstId": "$_id", "gcrNum": "$runTypes_e.greenRunsNum", "blNum": "$liftTypes_e.buttonLiftsNum", "brCutOffDate": { $let: { "vars": { "thisMonth": { $cond: [{ $eq: ["$location_e.hemisphere", "Southern"]}, 5, 11 ]}}, "in": { $dateFromParts: { "year": { $subtract: [{ $year: new Date() }, { $cond: [{ $eq: ["$$thisMonth", 5 ]}, { $cond: [{ $gt: [{ $month: new Date()}, 10 ]}, 0, 1]}, { $cond: [{ $lt: [{ $month: new Date()}, 5 ]}, 2, 1]}]}]}, "month" : "$$thisMonth", "day": 1 }}}}}}, "gcrMaxNum": { $max: "$runTypes_e.greenRunsNum" }, "gcrMinNum": { $min: "$runTypes_e.greenRunsNum" }, "blMaxNum": { $max: "$liftTypes_e.buttonLiftsNum" }, "blMinNum": { $min: "$liftTypes_e.buttonLiftsNum" }}}, { $unwind: "$rstData" }, { $project: { "_id": "$rstData.rstId", "brCutOffDate": "$rstData.brCutOffDate", "gcrPreScore": { $multiply: [{ $divide: [ 1, { $subtract: [ "$gcrMaxNum", "$gcrMinNum" ]}]}, { $subtract: [ "$rstData.gcrNum", "$gcrMinNum" ]}]}, "blPreScore": { $multiply: [{ $divide: [ 1, { $subtract: [ "$blMaxNum", "$blMinNum" ]}]}, { $subtract: [ "$blMaxNum", "$rstData.blNum" ]}]}}}, { $lookup: { "from": "our", "let": { "thisRstId": "$_id" }, "pipeline": [{ $match: { $expr: { $and: [{ $eq: [ "$rstId", "$$thisRstId"] }, { $eq: [ "$userSkillLevel", "beginner" ]}]}}}, { $project: { "lastVisit_e": "$lastVisit_e", "numOfResortVisits": "$numOfResortVisits", "overallRating": "$overallRating" }}], "as": "ourData" }}, { $unwind: "$ourData" }, { $lookup: { "from": "scr", "let": { "varName": "$varName", "weight": "$weight", "scoreGroup": "$scoreGroup" }, "pipeline": [{ $match: { $expr: { $eq: ["$scoreGroup", "scoreBG"] }}}, { $project: { "thisData": { "k": "$varName", "v": "$weight" }}}, { $replaceRoot: { "newRoot": "$thisData" }}], "as": "scrData" }}, { $project: { "rstId": "$_id", "brCutOffDate": 1, "gcrPreScore": 1, "blPreScore": 1, "ourData": 1, "scrData": { $arrayToObject: "$scrData" }}}, { $project: { "brCutOffDate": 1, "gcrPreScore": { $multiply: [ "$gcrPreScore", "$scrData.gcrNum" ]}, "blPreScore": { $multiply: [ "$blPreScore", "$scrData.blNum" ]}, "scrData.brScore": 1, "scrData.brMrsScore": 1, "scrData.brAosScore": 1, "ourData": 1 }}, { $group: { "_id": { "rstId": "$_id", "thisBrMaxWeight": { $cond: [ { $gt: ["$ourData.lastVisit_e.date", "$brCutOffDate"]}, "$scrData.brMrsScore", "$scrData.brAosScore" ]}, "gcrPreScore": "$gcrPreScore", "blPreScore": "$blPreScore", "scrDataBRscore": "$scrData.brScore" }, "count": { "$sum": 1 }, "avgOverallRating": { $avg: "$ourData.overallRating" }}}, { $addFields: { "log10MaxScore": { $let: { "vars": { "myLog10": { $log10: { $add: ["$count", 1]}}}, "in": { $subtract: [ 1, { $divide: [ 1, { $pow: [{ $add: [1, "$$myLog10" ]}, { $divide: [ "$$myLog10", 1.5 ]}]}]}]}}}}}, { $addFields: { "thisBrScore": { $divide: [ { $multiply: [ "$_id.scrDataBRscore", "$_id.thisBrMaxWeight", "$avgOverallRating", "$log10MaxScore" ]}, 100 ]}, "thisBrMaxScore": { $multiply: [ "$_id.thisBrMaxWeight", "$log10MaxScore" ]}}}, { $group: { "_id": { "rstId": "$_id.rstId", "gcrPreScore": "$_id.gcrPreScore", "blPreScore": "$_id.blPreScore" }, "brMaxScore": { $sum: { $multiply: [ "$_id.scrDataBRscore", "$thisBrMaxScore"]}}, "brScore": { $sum: "$thisBrScore" }, }}, { $addFields: { "gcrMaxScorePerc": { $subtract: [ 1, "$brMaxScore" ]}, "blMaxScorePerc": { $subtract: [ 1, "$brMaxScore" ]}}}, { $addFields: { "gcrScore": { $multiply: [ "$_id.gcrPreScore", "$gcrMaxScorePerc" ]}, "blScore": { $multiply: [ "$_id.blPreScore", "$blMaxScorePerc" ]}}}, { $addFields: { "scoreBG": { $add: [ "$gcrScore", "$blScore", "$brScore" ]}}}]).pretty();

  /*
  db.rst.aggregate([{
    $group: {
      "_id": null,
      "rstData": {
        $addToSet: {
          "rstId": "$_id",
          "gcrNum": "$runTypes_e.greenRunsNum",
          "blNum": "$liftTypes_e.buttonLiftsNum",
          "brCutOffDate": {
            $let: {
              "vars": {
                "thisMonth": {
                  $cond: [
                    {
                      $eq: ["$location_e.hemisphere", "Southern"]
                    },
                    5,
                    11
                  ]
                }
              },
              "in": {
                $dateFromParts: {
                  "year": {
                    $subtract: [
                      { $year: new Date() },
                      {
                        $cond: [
                          { $eq: ["$$thisMonth", 5 ]},
                          {
                            $cond: [
                              {
                                $gt: [
                                  { $month: new Date()},
                                  10
                                ]
                              },
                              0,
                              1
                            ]
                          }, {
                            $cond: [
                              {
                                $lt: [
                                  { $month: new Date()},
                                  5
                                ]
                              },
                              2,
                              1
                            ]
                          }
                        ]
                      }
                    ]
                  },
                  "month" : "$$thisMonth",
                  "day": 1
                }
              }
            }
          }
        }
      },
      "gcrMaxNum": {
        $max: "$runTypes_e.greenRunsNum"
      },
      "gcrMinNum": {
        $min: "$runTypes_e.greenRunsNum"
      },
      "blMaxNum": {
        $max: "$liftTypes_e.buttonLiftsNum"
      },
      "blMinNum": {
        $min: "$liftTypes_e.buttonLiftsNum"
      }
    }
  }, {
    $unwind: "$rstData"
  }, {
    $project: {
      "_id": "$rstData.rstId",
      "brCutOffDate": "$rstData.brCutOffDate",
      "gcrPreScore": {
        $multiply: [
          {
            $divide: [
              1,
              {
                $subtract: [ "$gcrMaxNum", "$gcrMinNum" ]
              }
            ]
          }, {
            $subtract: [ "$rstData.gcrNum", "$gcrMinNum" ]
          }
        ]
      },
      "blPreScore": {
        $multiply: [
          {
            $divide: [
              1,
              {
                $subtract: [ "$blMaxNum", "$blMinNum" ]
              }
            ]
          }, {
            $subtract: [ "$blMaxNum", "$rstData.blNum" ]
          }
        ]
      }
    }
  }, {
    $lookup: {
      "from": "our",
      "let": { "thisRstId": "$_id" },
      "pipeline": [
        {
          $match: {
            $expr: {
              $and: [
                {
                  $eq: [ "$rstId", "$$thisRstId"]
                }, {
                  $eq: [ "$userSkillLevel", "beginner" ]
                }
              ]
            }
          }
        }, {
          $project: {
            "lastVisit_e": "$lastVisit_e",
            "numOfResortVisits": "$numOfResortVisits",
            "overallRating": "$overallRating"
          }
        }
      ],
      "as": "ourData"
    }
  }, {
    $unwind: "$ourData"
  }, {
    $lookup: {
      "from": "scr",
      "let": {
        "varName": "$varName",
        "weight": "$weight",
        "scoreGroup": "$scoreGroup"
      },
      "pipeline": [
        {
          $match: {
            $expr: {
              $eq: ["$scoreGroup", "scoreBG"]
            }
          }
        }, {
          $project: {
            "thisData": {
              "k": "$varName",
              "v": "$weight"
            }
          }
        }, {
          $replaceRoot: { "newRoot": "$thisData" }
        }
      ],
      "as": "scrData"
    }
  }, {
    $project: {
      "rstId": "$_id",
      "brCutOffDate": 1,
      "gcrPreScore": 1,
      "blPreScore": 1,
      "ourData": 1,
      "scrData": {
        $arrayToObject: "$scrData"
      }
    }
  }, {
    $project: {
      "brCutOffDate": 1,
      "gcrPreScore": {
        $multiply: [ "$gcrPreScore", "$scrData.gcrNum" ]
      },
      "blPreScore": {
        $multiply: [ "$blPreScore", "$scrData.blNum" ]
      },
      "scrData.brScore": 1,
      "scrData.brMrsScore": 1,
      "scrData.brAosScore": 1,
      "ourData": 1
    }
  }, {
    $group: {
      "_id": {
        "rstId": "$_id",
        "thisBrMaxWeight": {
          $cond: [
            {
              $gt: ["$ourData.lastVisit_e.date", "$brCutOffDate"]
            },
            "$scrData.brMrsScore",
            "$scrData.brAosScore"
          ]
        },
        "gcrPreScore": "$gcrPreScore",
        "blPreScore": "$blPreScore",
        "scrDataBRscore": "$scrData.brScore"
      },
      "count": { "$sum": 1 },
      "avgOverallRating": { $avg: "$ourData.overallRating" },
    }
  }, {
    $addFields: {
      "log10MaxScore": {
        $let: {
          "vars": {
            "myLog10": {
              $log10: {
                $add: ["$count", 1]
              }
            }
          },
          "in": {
            $subtract: [
              1,
              {
                $divide: [
                  1,
                  {
                    $pow: [
                      {
                        $add: [1, "$$myLog10" ]
                      }, {
                        $divide: [ "$$myLog10", 1.5 ]
                      }
                    ]
                  }
                ]
              }
            ]
          }
        }
      }
    }
  }, {
    $addFields: {
      "thisBrScore": {
        $divide: [
          {
            $multiply: [ "$_id.scrDataBRscore", "$_id.thisBrMaxWeight", "$avgOverallRating", "$log10MaxScore" ]
          },
          100
        ]
      },
      "thisBrMaxScore": {
        $multiply: [ "$_id.thisBrMaxWeight", "$log10MaxScore" ]
      }
    }
  }, {
    $group: {
      "_id": {
        "rstId": "$_id.rstId",
        "gcrPreScore": "$_id.gcrPreScore",
        "blPreScore": "$_id.blPreScore"
      },
      "brMaxScore": {
        $sum: {
          $multiply: [ "$_id.scrDataBRscore", "$thisBrMaxScore"]
        }
      },
      "brScore": { $sum: "$thisBrScore" }
    }
  }, {
    $addFields: {
      "gcrMaxScorePerc": {
        $subtract: [ 1, "$brMaxScore" ]
      },
      "blMaxScorePerc": {
        $subtract: [ 1, "$brMaxScore" ]
      }
    }
  }, {
    $addFields: {
      "gcrScore": {
        $multiply: [ "$_id.gcrPreScore", "$gcrMaxScorePerc" ]
      },
      "blScore": {
        $multiply: [ "$_id.blPreScore", "$blMaxScorePerc" ]
      }
    }
  }, {
    $addFields: {
      "scoreBG": {
        $add: [ "$gcrScore", "$blScore", "$brScore" ]
      }
    }
  }]).pretty();

  // .forEach(function(doc) { db.rst.updateOne({ "_id": doc._id}, { $set: { "scoreBG": doc.scores_e.scoreBG }})});
*/

/*

val docs = rstService.getAllDocs.map(r => r.sorted(Resort.orderingByBoardingArea))

// todo add success/failure message?
docs.map(r =>
r.map(r2 =>
  rstService.updateOneField(r2._id, Json.obj("scoreBA" -> getScore("ba", r2.boardingArea_km2, r.last.boardingArea_km2, r.head.boardingArea_km2)))
)
)
docs

}
*/