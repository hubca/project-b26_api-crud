package services.dbClient

import org.joda.time.DateTime
import java.time.Instant
import java.util.Date
import javax.inject.{Inject, Singleton}

import controllers.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import models.{Resort, _}
import play.api.data.Field
import play.api.libs.json
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.core.commands.{Ascending, Group, Match, SumField}

@Singleton
class ResortService @Inject() (cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  def wCollection: Future[JSONCollection] = database.map(_.collection[JSONCollection]("weatherInfoStr"))

  def createDoc(newResort: Resort) = serviceClientDb.createDoc[Resort]("resortInfoStr", newResort)

  def deleteDoc(id: BSONObjectID) = serviceClientDb.deleteDoc("resortInfoStr", id)

  def updateDoc(oId: Option[BSONObjectID], editedResort: Resort): Future[Result] = serviceClientDb.updateDoc[Resort]("resortInfoStr", editedResort, oId)

  def updateOneField(id: Option[BSONObjectID], field: JsObject): Future[Result] = serviceClientDb.updateOneField("resortInfoStr", id, field)

  def getAllDocs: Future[Seq[Resort]] = serviceClientDb.getAllDocs[Resort]("resortInfoStr")

  def getDocById(id: BSONObjectID): Future[Option[Resort]] = serviceClientDb.getDocById[Resort]("resortInfoStr", id)


  /*
  def add(resort: Resort) = {

    val jsonString = Json.toJson(resort)
    val infoFromJson: JsResult[Resort] = Json.fromJson[Resort](jsonString)

    infoFromJson match {

      case JsSuccess(r: Resort, path: JsPath) =>
        for {
          resortInformation <- rCollection
          lastError <- resortInformation.insert(r)
        } yield {
          Logger.debug(s"Successfully inserted with LastError: $lastError")
          Created("Created 1 resort")
        }
      case JsError(e) => Future.successful(BadRequest("Could not create a resort from the json provided. "))
    }

  }


  def getWeatherCol: Future[Seq[Weather]] = {

    wCollection.flatMap(
      _.find(Json.obj())
        .cursor[Weather](ReadPreference.primary)
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[Weather]]())
    )

  }
*/

  def getWeatherAggregateCol = {
    wCollection.flatMap(res => getWeatherAggregate(res))
  }

  def getWeatherAggregate(col: JSONCollection) = {

    import col.BatchCommands.AggregationFramework.{Group, Match, SumField, AvgField, MinField, MaxField}
//  Json.obj("date" -> Json.obj("$gte" -> fromDate, "$lt" -> toDate))

    col.aggregate(
      //Match(Json.obj("snowfall" -> Json.obj("$lte" -> 2))),
      //Match(Json.obj("date" -> Json.obj("$gte" -> Json.obj("$date" -> JsNumber(fromLongDate)), "$lt" -> Json.obj("$date" -> JsNumber(toLongDate))))),
      Match(serviceClientDb.getMongoDateRange("2017-09-20", "2017-09-21", "date")),
      List(Group(JsString("$rstId"))(
        "totalSnowfall" -> SumField("snowfall"),
        "avgSnowfall" -> AvgField("snowfall"),
        "minSnowfall" -> MinField("snowfall"),
        "maxSnowfall" -> MaxField("snowfall")
      ))
    )
    .map(_.head[WeatherAggregate])

  }

  def triggerScoreBA: Future[Seq[Resort]] = {

    val docs = getAllDocs.map(r => r.sorted(Resort.orderingByResortMiles))
    updateScoreBA(docs)
    docs

  }

  def updateScoreBA(docs: Future[Seq[Resort]]) = {

    docs.map(r =>
      r.map(r2 =>
        //updateAllFields(r2._id, Resort(r2._id, r2.resortName, r2.resortCountry, r2.resortContinent, r2.resortCountryPrefix, r2.resortMiles, getScoreBA(r2.resortMiles, r.last.resortMiles, r.head.resortMiles), r2.scoreSF))
        updateOneField(r2._id, Json.obj("scoreBA" -> getScore("miles", r2.resortMiles, r.last.resortMiles, r.head.resortMiles)))
      )
    )

  }

  def triggerScoreSF: Future[Seq[Resort]] = {

    // 1) get list of resort ids & averages for time period
    // 2) loop through rst collection and
    val docs = getAllDocs.map(r => r.sorted(Resort.orderingByResortMiles))
    updateScoreBA(docs)
    docs
    /*
    val docs = getSnowfall//.map(r => r.sorted(Resort.orderingByResortMiles))
    updateScoreSF(docs)
    docs
    */
  }

  def updateScoreSF(docs: Future[Seq[WeatherAggregate]]) = {

    "hello"
    /*
    rCollection.flatMap(collection =>
      collection.update(idSelector, resort).map {
        lastError =>
          //logger.debug(s"Successfully updated with LastError: $lastError")
          Created(s"Resort updated")
      }
    )

    docs.map(r =>
      r.map(r2 =>
        update(r2._id, Resort(r2._id, r2.resortName, r2.resortCountry, r2.resortContinent, r2.resortCountryPrefix, r2.resortMiles, r2.scoreBA, getScoreSF(r2.resortMiles, r.max.resortMiles, r.min.resortMiles)))
      )
    )
    */
  }

  def getScore(attr: String, xThis: Double, xMax: Double, xMin: Double): Double = attr match {

    case "miles" => calculateFixedScore("low", xThis, xMax, xMin)
    case "_" => calculateFixedScore("high", xThis, xMax, xMin)
    // ba, sf, bg, fm, lc, gr, ad, tt, nl, fd, pr

  }

  def calculateFixedScore(best: String, xThis: Double, xMax: Double, xMin: Double): Double = best match {

    case "high" => (1 / (xMax - xMin)) * (xThis - xMin)
    case "low" => (1 / (xMax - xMin)) * (xMax - xThis)
    case "_" => 0

  }
  //def calculateFixedScore2(xThis: Double, xMax: Double, xMin: Double): Double = (1 / (xMax - xMin)) * (xMax - xThis)

/*
  def updateScore(x: String): Double = x match {

      case "fixed" => calculateFixedScore(xThis
      case JsError(e) => Future.successful(BadRequest("Could not create a resort from the json provided. "))
    }

    //(1 / (xMax - xMin)) * (xThis - xMin)

  }
  */
  //def sortBy(list: Future[List[Resort]]) = list.map(r => r.sorted(Resort.getClass.getMethod("orderingByResortMiles")))
  //def testing(list: Future[List[Resort]])(f: String) = list.map(r => r.sorted(Resort.orderingByResortMiles))

  // 1) get average snowfall for time period from weatherInfoStr
  // 2) use averages to update score in resortInfoStr

}

