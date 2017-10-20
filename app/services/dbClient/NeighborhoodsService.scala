package services.dbClient

import javax.inject.{Inject, Singleton}

import models.{Resort, _}
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.ReadPreference
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NeighborhoodsService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  protected val collectionName = "neighborhoods"

  def createDoc(newNeighborhood: Neighborhood) = serviceClientDb.createDoc[Neighborhood](collectionName, newNeighborhood)

  def deleteDoc(id: BSONObjectID) = serviceClientDb.deleteDoc(collectionName, id)

  def updateDoc(oId: Option[BSONObjectID], editedNeighborhood: Neighborhood): Future[Result] = serviceClientDb.updateDoc[Neighborhood](collectionName, editedNeighborhood, oId)

  def updateOneField(id: Option[BSONObjectID], field: JsObject): Future[Result] = serviceClientDb.updateOneField(collectionName, id, field)

  def getAllDocs: Future[Seq[Neighborhood]] = serviceClientDb.getAllDocs[Neighborhood](collectionName)

  def getDocById(id: BSONObjectID): Future[Option[Neighborhood]] = serviceClientDb.getDocById[Neighborhood](collectionName, id)



  def getNeighborhoodAggregateCol(oFromDate: Option[String], oToDate: Option[String]) = {
    serviceClientDb.getCollection(collectionName).flatMap(res => getNeighborhoodAggregate(res, oFromDate, oToDate))
  }

  def getNeighborhoodAggregate(col: JSONCollection, oFromDate: Option[String], oToDate: Option[String]) = {

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

