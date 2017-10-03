package services.dbClient

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents, PlayBodyParsers, Result}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import models.{Resort, Weather, WeatherAggregate}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.core.commands.{Ascending, Group, Match, SumField}

class WthService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  def wCollection: Future[JSONCollection] = database.map(_.collection[JSONCollection]("weatherInfoStr"))

  def createDoc(newWeather: Weather) = serviceClientDb.createDoc[Weather]("weatherInfoStr", newWeather)

  def deleteDoc(id: BSONObjectID) = serviceClientDb.deleteDoc("weatherInfoStr", id)

  def updateDoc(oId: Option[BSONObjectID], editedWeather: Weather): Future[Result] = serviceClientDb.updateDoc[Weather]("weatherInfoStr", editedWeather, oId)

  def updateOneField(id: Option[BSONObjectID], field: JsObject): Future[Result] = serviceClientDb.updateOneField("weatherInfoStr", id, field)

  def getAllDocs: Future[Seq[Weather]] = serviceClientDb.getAllDocs[Weather]("weatherInfoStr")

  def getDocById(id: BSONObjectID): Future[Option[Weather]] = serviceClientDb.getDocById[Weather]("weatherInfoStr", id)


  def getWeatherAggregateCol(oFromDate: Option[String], oToDate: Option[String]) = {
    serviceClientDb.getCollection("weatherInfoStr").flatMap(res => getWeatherAggregate(res, oFromDate, oToDate))
  }

  def getWeatherAggregate(col: JSONCollection, oFromDate: Option[String], oToDate: Option[String]) = {

    import col.BatchCommands.AggregationFramework.{Group, Match, SumField, AvgField, MinField, MaxField}

    val aggr = oFromDate match {

      case Some(_) =>
        col.aggregate(
          Match(serviceClientDb.getMongoDateRange(oFromDate.get, oToDate.get, "date")), //  Json.obj("date" -> Json.obj("$gte" -> fromDate, "$lt" -> toDate))
          List(Group(JsString("$rstId"))(
            "totalSnowfall" -> SumField("snowfall"),
            "avgSnowfall" -> AvgField("snowfall"),
            "minSnowfall" -> MinField("snowfall"),
            "maxSnowfall" -> MaxField("snowfall")
          ))
        )

      case None =>
        col.aggregate(
          Group(JsString("$rstId"))(
            "totalSnowfall" -> SumField("snowfall"),
            "avgSnowfall" -> AvgField("snowfall"),
            "minSnowfall" -> MinField("snowfall"),
            "maxSnowfall" -> MaxField("snowfall")
          )
        )

    }

    aggr.map(_.head[WeatherAggregate])

  }

  /*
  def triggerScoreSF: Future[Seq[Resort]] = {


    // 1) get list of resort ids & averages for time period
    // 2) loop through rst collection and
    val docs = getResortsCol.map(r => r.sorted(Resort.orderingByResortMiles))
    updateScoreBA(docs)
    docs
  }
*/
}
