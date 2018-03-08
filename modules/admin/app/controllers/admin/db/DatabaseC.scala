package controllers.admin.db

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source.fromFuture
import data.Pagelet
import models.db._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID

import services.dbClient._
import play.api.data.Form
import play.twirl.api.HtmlFormat

import ui.HtmlStreamImplicits._
import ui.HtmlStream

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class DatabaseC @Inject()(parser: BodyParsers.Default)(cc: ControllerComponents, actorSystem: ActorSystem)
                         (val reactiveMongoApi: ReactiveMongoApi)
                         (rstC: RstC, ctyC: CtyC, rgnC: RgnC, ourC: OurC, scrC: ScrC, updateScores: UpdateScores)
                         (implicit mat: Materializer) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {

  override lazy val parse: PlayBodyParsers = cc.parsers

  // todo 1) reduce top bar (JS)
  // todo 2) create sticky header for collection field names
  // todo 3) (future) is it possible/feasible/more efficient to: a] cache data when retrieving it b] then match it to newly submitted form data c] only modify the values that are not the same ???
  // todo 4) limit retrieving files to 30? and figure if/how to get next 30

  def index(collName: String, queryType: String, oId: Option[BSONObjectID] = None, activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>


    collName match {

      case "rst" => rstC.index(queryType, oId)(activeTabIdx)(request)
      case "cty" => ctyC.index(queryType, oId)(activeTabIdx)(request)
      case "rgn" => rgnC.index(queryType, oId)(activeTabIdx)(request)
      case "our" => ourC.index(queryType, oId)(activeTabIdx)(request)
      case "scr" => scrC.index(queryType, oId)(activeTabIdx)(request)
      case _ => rstC.index(queryType, oId)(activeTabIdx)(request)

    }

  }

  def queryCollection(collName: String, queryType: String, oId: Option[BSONObjectID], activeTabIdx: Int = 0) = Action.async { implicit request: Request[AnyContent] =>

    collName match {

      case "rst" => rstC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case "cty" => ctyC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case "rgn" => rgnC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case "our" => ourC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case "scr" => scrC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case _ => rstC.queryCollection(queryType, oId)(activeTabIdx)(request)

    }

  }

  def deleteDoc(collName: String, oId: Option[BSONObjectID], activeTabIdx: Int = 0) = Action.async { implicit request: Request[AnyContent] =>

    collName match {

      case "rst" => rstC.deleteDoc(oId)(activeTabIdx)(request)
      case "cty" => ctyC.deleteDoc(oId)(activeTabIdx)(request)
      case "rgn" => rgnC.deleteDoc(oId)(activeTabIdx)(request)
      case "our" => ourC.deleteDoc(oId)(activeTabIdx)(request)
      case "scr" => scrC.deleteDoc(oId)(activeTabIdx)(request)
      case _ => rstC.deleteDoc(oId)(activeTabIdx)(request)

    }

  }

  // todo - delete after testing output of scoreBG (mongo aggregation)
  //def displayScore(scoreName: String, part: Int) = Action.async { implicit request: Request[AnyContent] =>
  def updateScore(scoreName: String) = Action.async { implicit request: Request[AnyContent] =>

    scoreName match {
      case "scoreBA" => updateScoreBA(1)(request)
    }

  }


  def updateScoreBA(part: Int) = Action.async { implicit request: Request[AnyContent] =>

    updateScores.upsertScoreBA(part).map(x =>

      part match {
        case 1 => Redirect(routes.DatabaseC.index("rst", "create", None, 6))
        //case 1 => Ok(views.html.db.scores.scoreBG1(x))
        //case 2 => Ok(views.html.db.scores.scoreBG2(x))
      }

    )

  }

  def bulkUpdateScore(scoreName: String, part: Int) = Action.async { implicit request: Request[AnyContent] =>

    scoreName match {
      case "scoreBA" => bulkUpdateScoreBA(part)(request)
    }

  }


  def bulkUpdateScoreBA(part: Int) = Action.async { implicit request: Request[AnyContent] =>

    updateScores.usBulkUpdateScoreBA(part).map(x =>
      Redirect(routes.DatabaseC.index("rst", "create", None, 6))
    )

  }

  def testTransform = Action.async { implicit request: Request[AnyContent] =>

    updateScores.displayTransformer

  }

    // todo - delete after testing output of scoreBG (mongo aggregation)
  /*
  def displayScoreBG(part: Int) = Action.async { implicit request: Request[AnyContent] =>

    //UpdateScores.
    updateScores.updateScoreBG(part).map(x =>

      part match {
        case 1 => Ok(views.html.db.scores.scoreBG1(x))
        case 2 => Ok(views.html.db.scores.scoreBG2(x))
      }

    )

  }
  */


  // TODO - test correct tab index upon form submission - IN USE ??
  def getSubmittedTabIdx(implicit request: Request[AnyContent]): Int = {
    request.body.asFormUrlEncoded.get("submit").headOption.getOrElse("0").toInt
  }

}


/*

db.our.aggregate([
   {
      $lookup:
         {
           from: "rst",
           pipeline: [
              { $project: { thisData: { name: "$runTypes_e", date: "$liftTypes_e" } } },
              { $replaceRoot: { newRoot: "$thisData" } }
           ],
           as: "rstData"
         }
    }
])

// db.our.aggregate([{ $lookup: { from: "rst", pipeline: [{ $project: { thisData: { name: "$runTypes_e", date: "$liftTypes_e" }}}, { $replaceRoot: { newRoot: "$thisData" }}], as: "rstData" }}])
 */


/*

db.our.aggregate([
{
  $lookup: {
    "from": "rst",
    "localField": "rstId",
    "foreignField": "_id",
    "as": "rstData"
  }
}, {
  $project: {
    "rstId": 1,
    "lastVisit_e": 1,
    "userSkillLevel": 1,
    "overallRating": 1,
    "rstData.runTypes_e.greenRunsNum": 1,
    "rstData.liftTypes_e.buttonLiftsNum": 1,
    "dog": "rstData.liftTypes_e.buttonLiftsNum"
  }
}, {
  $unwind: "$rstData"
}, {
  $group: {
    "_id": "all",
    "maxGCR": {
        $max: "$rstData.runTypes_e.greenRunsNum"
     },
     "minGCR": {
        $min: "$rstData.runTypes_e.greenRunsNum"
     },
     "maxBL": {
        $max: "$rstData.liftTypes_e.buttonLiftsNum"
     },
     "minBL": {
        $min: "$rstData.liftTypes_e.buttonLiftsNum"
     },
     "rstArr": {
        $push: {
          "rstId": "$rstId",
          "userSkillLevel": "$userSkillLevel",
          "lastVisit_e": "$lastVisit_e",
          "overallRating": "$overallRating",
          "gcrNum": "$rstData.runTypes_e.greenRunsNum",
          "blNum": "$rstData.liftTypes_e.buttonLiftsNum"
        }
      }
    }
 }, {
   $unwind: "$rstArr"
 }, {
   $match: {
      "rstArr.userSkillLevel": "beginner"
   }
 }, {
   $group: {
      "_id": {
          "rstId": "$rstArr.rstId",
          "manualFactor": {
            $cond: [
              {
                $gt: ["$rstArr.lastVisit_e.date", new Date("2017")]
              },
              0.8,
              0.2
            ]
          }
      },
      "count": { "$sum": 1 },
      "avgOverallRating": { $avg: "$rstArr.overallRating" },
      "thisGCR": { $first: "$rstArr.gcrNum" },
      "maxGCR": { $first: "$maxGCR" },
      "minGCR": { $first: "$minGCR" },
      "thisBL": { $first: "$rstArr.blNum" },
      "maxBL": { $first: "$maxBL" },
      "minBL": { $first: "$minBL" }
    }
  }, {
    $addFields: {
      "log10MaxScore": {


      $let: { "vars": {"myLog10": {$log10: {$add: ["$count", 1]}}}, "in": {$subtract: [1, { $divide: [1, { $pow: [{ $add: [1, "$$myLog10" ]}, { $divide: [ "$$myLog10", 1.5 ]}]}]}]}}

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
                        $add: [
                          1,
                          "$$myLog10"
                        ]
                      }, {
                        $divide: [
                          "$$myLog10",
                          1.5
                        ]
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
    $project: {
      "count": 1,
      "userSkillLevel": 1,
      "avgOverallRating": 1,
      "log10MaxScore": 1,
      "maxScore": {
        $multiply: [ "$log10MaxScore", "$_id.manualFactor" ]
      },
      "score": {
        $multiply: [
          "$log10MaxScore",
          {
            $divide: ["$avgOverallRating", 100]
          },
          "$_id.manualFactor"
        ]
      },
      "thisGCR": 1,
      "maxGCR": 1,
      "minGCR": 1,
      "thisBL": 1,
      "maxBL": 1,
      "minBL": 1
    }
  }, {
    $group: {
      "_id": "$_id.rstId",
      "totalMaxScore": { $sum: "$maxScore" },
      "totalScore": { $sum: "$score" },
      "thisGCR": { $first: "$thisGCR" },
      "maxGCR": { $first: "$maxGCR" },
      "minGCR": { $first: "$minGCR" },
      "thisBL": { $first: "$thisBL" },
      "maxBL": { $first: "$maxBL" },
      "minBL": { $first: "$minBL" }
    }
  }, {
    $project: {
      "maxScoreBR": {
        $multiply: [0.5, "$totalMaxScore"]
      },
      "scoreBR": {
        $multiply: [0.5, "$totalScore"]
      },
      "maxScoreGCR": {
        $multiply: [
          0.5,
          {
            $subtract: [
              1,
              {
                $multiply: [
                  0.5,
                  "$totalMaxScore"
                ]
              }
            ]
          }
        ]
      },
      "maxScoreBL": {
        $multiply: [
          0.5,
          {
            $subtract: [
              1,
              {
                $multiply: [0.5, "$totalMaxScore"]
              }
            ]
          }
        ]
      },
      "thisGCR": 1,
      "maxGCR": 1,
      "minGCR": 1,
      "thisBL": 1,
      "maxBL": 1,
      "minBL": 1
    }
  }, {
    $addFields: {
      "scoreGCR": {
        $multiply: [
          "$maxScoreGCR",
          {
            $divide: [
              1,
              {
                $subtract: [ "$maxGCR", "$minGCR" ]
              }
            ]
          }, {
            $subtract: [ "$thisGCR", "$minGCR" ]
          }
        ]
      },
      "scoreBL": {
        $multiply: [
          "$maxScoreBL",
          {
            $divide: [
              1,
              {
                $subtract: [ "$maxBL", "$minBL" ]
              }
            ]
          }, {
            $subtract: [ "$maxBL", "$thisBL" ]
          }
        ]
      }
    }
  }, {
    $project: {
      "scores_e.scoreBG": {
        $add: ["$scoreGCR", "$scoreBL", "$scoreBR"]
       }
    }
  }
])
 */