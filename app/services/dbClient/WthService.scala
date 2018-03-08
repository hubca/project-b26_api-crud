package services.dbClient

import javax.inject.{Inject, Singleton}

import models.db.MongoField

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
import models.{WeatherAggregate}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.core.commands.{Ascending, Group, Match, SumField}

class WthService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  protected val collectionName = "weatherInfoStr"


  def getWeatherAggregateCol(oFromDate: Option[String], oToDate: Option[String]): Future[Seq[WeatherAggregate]] = {
    serviceClientDb.getCollection(collectionName).flatMap(res => getWeatherAggregate(res, oFromDate, oToDate))
  }

  def getWeatherAggregate(col: JSONCollection, oFromDate: Option[String], oToDate: Option[String]): Future[Seq[WeatherAggregate]] = {

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

}
