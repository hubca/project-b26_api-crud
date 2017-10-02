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


  def getWthCol: Future[Seq[Weather]] = {

    wCollection.flatMap(
      _.find(Json.obj())
        .cursor[Weather](ReadPreference.primary)
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[Weather]]())
    )

  }

  def getWeatherAggregateCol = {
    wCollection.flatMap(res => getWeatherAggregate(res))
  }

  def getWeatherAggregate(col: JSONCollection) = {

    //    wCollection.flatMap().aggregateWith(
    //      Group(BSONString("$rstId"))( "totalSnowfall" -> SumField("snowfall")),
    //      List(Match(BSONDocument("totalSnowfall" -> BSONDocument("$gte" -> 1)))))

    import col.BatchCommands.AggregationFramework.{Group, Match, SumField, AvgField, MinField, MaxField}

    /*
    col.aggregate(Group(BSONDocument("state" -> "$state", "city" -> "$city"))(
    "pop" -> SumField("population")),
    List(Group(BSONString("$_id.state"))("avgCityPop" -> AvgField("pop")))).
    map(_.documents)
    */
    col.aggregate(
      Group(JsString("$rstId"))(
        "totalSnowfall" -> SumField("snowfall"),
        "avgSnowfall" -> AvgField("snowfall"),
        "minSnowfall" -> MinField("snowfall"),
        "maxSnowfall" -> MaxField("snowfall")
      )//,
      //List(Match(Json.obj("avgSnowfall" -> Json.obj("$gte" -> 1))))
    )
      .map(_.head[WeatherAggregate])

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
