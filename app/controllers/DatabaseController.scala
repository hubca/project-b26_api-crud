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
import models.{CollectionClass, Resort, ResortForm, _}
import play.api.Logger
import play.api.data.Form
import play.api.libs.json._
import play.modules.reactivemongo.json.JSONSerializationPack.Writer
import reactivemongo.play.json.collection
import reactivemongo.play.json.collection.JSONCollection
import services.dbClient.{ResortService, ServiceClientDb, WthService}

@Singleton
class DatabaseController @Inject() (cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, resortService: ResortService, wthService: WthService) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  // TODO - generalise CRUD methods?

  override lazy val parse: PlayBodyParsers = cc.parsers

  def rstIdx = Action.async { implicit request: Request[AnyContent] =>
    resortService.getAllDocs.map(resorts =>
      Ok(views.html.admin.db.rst.index(ResortForm.form, resorts))
    )
  }

  def rstEditIdx(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    for {
      resortsLst <- resortService.getAllDocs
      editResortOpt <- resortService.getDocById(id)
    } yield {
      Ok(views.html.admin.db.rst.edit(ResortForm.form, resortsLst, editResortOpt))
    }

  }

  def rstCreateDoc = Action.async { implicit request: Request[AnyContent] =>

    ResortForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.rst.index(errorForm, Seq.empty[Resort]))),
      data => {
        val newResort = Resort(None, data.resortName, data.resortCountry, data.resortContinent, data.resortCountryPrefix, data.resortMiles)

        resortService.createDoc(newResort).map(res =>
          Redirect(routes.DatabaseController.rstIdx())
        )

      }
    )

  }

  def rstDelete(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    resortService.deleteDoc(id) map { res =>
      Redirect(routes.DatabaseController.rstIdx())
    }

  }

  def rstEdit(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    val oId = Option(id)

    ResortForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.rst.edit(errorForm, Seq.empty[Resort], Option.empty[Resort]))),
      data => {
        val editedResort = Resort(oId, data.resortName, data.resortCountry, data.resortContinent, data.resortCountryPrefix, data.resortMiles, data.scoreBA, data.scoreSF)

        resortService.updateDoc(oId, editedResort).map(res =>
          Redirect(routes.DatabaseController.rstEditIdx(id))
        )
      })

  }

  def wthIdx = Action.async { implicit request: Request[AnyContent] =>
    wthService.getAllDocs.map(weathers =>
      Ok(views.html.admin.db.wth.index(WeatherForm.form, weathers))
    )
  }

  def wthEditIdx(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    for {
      wthLst <- wthService.getAllDocs
      editWeatherOpt <- wthService.getDocById(id)
    } yield {
      Ok(views.html.admin.db.wth.edit(WeatherForm.form, wthLst, editWeatherOpt))
    }

  }

  def wthCreateDoc = Action.async { implicit request: Request[AnyContent] =>

    WeatherForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.wth.index(errorForm, Seq.empty[Weather]))),

      data => {

        val oBsonRstId = Some(serviceClientDb.string2BSONObjectID(data.rstId))
        val oJsObjectDate = Some(serviceClientDb.string2MongoDate(data.date, "date"))

        val newWeather = Weather(None, oBsonRstId, oJsObjectDate, data.snowfall)

        wthService.createDoc(newWeather).map(res =>
          Redirect(routes.DatabaseController.wthIdx())
        )

      }
    )

  }

  def wthDelete(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    wthService.deleteDoc(id) map { res =>
      Redirect(routes.DatabaseController.rstIdx())
    }

  }

  def wthEdit(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    val oId = Option(id)

    ResortForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.rst.edit(errorForm, Seq.empty[Resort], Option.empty[Resort]))),
      data => {
        val editedResort = Resort(oId, data.resortName, data.resortCountry, data.resortContinent, data.resortCountryPrefix, data.resortMiles, data.scoreBA, data.scoreSF)

        resortService.updateDoc(oId, editedResort).map(res =>
          Redirect(routes.DatabaseController.rstEditIdx(id))
        )
      })

  }

  //---- aggregates ----//

  def wthAgg(fromDate: String, toDate: String) = Action.async { implicit request: Request[AnyContent] =>
    wthService.getWeatherAggregateCol(Some(fromDate), Some(toDate)).map(weather =>
      Ok(views.html.admin.db.colWeatherAggregate(weather, Some(fromDate), Some(toDate)))
    )
  }


  //---- scoring updates & additions ----//

  def showAllCollections = Action.async { implicit request: Request[AnyContent] =>

//    val format = new java.text.SimpleDateFormat("dd-MM-yyyy")

//    val fromDate = "01-01-1980"
//    val toDate = format.format(new Date())

    for {
      rstCol <- resortService.getAllDocs
      wthCol <- wthService.getAllDocs
      wthAggCol <- wthService.getWeatherAggregateCol(None, None)
    } yield {
      Ok(views.html.admin.db.allCollections(rstCol, wthCol, wthAggCol))
    }

  }

  def triggerScoreBA = Action.async { implicit request: Request[AnyContent] =>
    resortService.triggerScoreBA.map(resorts =>
      Ok(views.html.admin.db.rst.index(ResortForm.form, resorts))
    )
  }


/*
  //---- examples, references of other useful methods ----//

  //Inserts the POST results into the database
  // curl -H "Content-Type: application/json" -X POST -d "{"""resortName""":"""LesArcs""","""resortCountry""":"""France""","""resortContinent""":"""Europe""","""resortCountryPrefix""":"""FR"""}" http://localhost:9000/resortInfo/add
  def createDocument = Action.async(playBodyParsers.json) { request =>

    val jsonString: JsValue = request.body
    insertDocument(jsonString)

  }

  //Inserts the resort into the database
  def createFromCode = Action.async { request: Request[AnyContent] =>

    val jsonString: JsValue = Json.parse("""{
      "resortName":"Chamo2",
      "resortCountry":"France2",
      "resortContinent":"Europe2",
      "resortCountryPrefix":"FR2"}""")

    insertDocument(jsonString)

  }
*/

  /*
  def mapCall[T](url: String)(implicit reads: Reads[T]) = {
    sc.makeCall(url).map { response =>
      val jsonString = response.json
      Json.fromJson[T](jsonString)
    }
  }
  */

}
