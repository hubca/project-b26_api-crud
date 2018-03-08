package controllers

import java.time.Instant
import java.util.Date
import javax.inject._

import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.modules.reactivemongo.json._
import ImplicitBSONHandlers._
import play.api.Logger
import play.api.data.Form
import play.api.libs.json._
import play.modules.reactivemongo.json.JSONSerializationPack.Writer
import reactivemongo.play.json.collection
import reactivemongo.play.json.collection.JSONCollection
import services.dbClient._

@Singleton
class AdminC @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, wthService: WthService) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  //--- aggregates ---\\

  def wthAgg(fromDate: String, toDate: String) = Action.async { implicit request: Request[AnyContent] =>
    wthService.getWeatherAggregateCol(Some(fromDate), Some(toDate)).map(weather =>
      Ok(views.html.admin.db.colWeatherAggregate(weather, Some(fromDate), Some(toDate)))
    )
  }


}
