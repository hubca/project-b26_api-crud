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
import services.dbClient._

@Singleton
class AdminCtr @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, resortService: ResortService, wthService: WthService, nghService: NeighborhoodsService) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

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
        val newResort = Resort(None, data.name, data.country, data.continent, data.countryPrefix, data.boardingArea_km2, data.scoreBA, data.scoreSF, None)

        resortService.createDoc(newResort).map(res =>
          Redirect(routes.AdminCtr.rstIdx())
        )

      }
    )

  }

  def rstDelete(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    resortService.deleteDoc(id) map { res =>
      Redirect(routes.AdminCtr.rstIdx())
    }

  }

  def rstEdit(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    val oId = Option(id)

    ResortForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.rst.edit(errorForm, Seq.empty[Resort], Option.empty[Resort]))),
      data => {
        val editedResort = Resort(None, data.name, data.country, data.continent, data.countryPrefix, data.boardingArea_km2, data.scoreBA, data.scoreSF, None)

        resortService.updateDoc(oId, editedResort).map(res =>
          Redirect(routes.AdminCtr.rstEditIdx(id))
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
          Redirect(routes.AdminCtr.wthIdx())
        )

      }
    )

  }

  def wthDelete(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    wthService.deleteDoc(id) map { res =>
      Redirect(routes.AdminCtr.wthIdx())
    }

  }

  def wthEdit(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    val oId = Option(id)

    WeatherForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.wth.edit(errorForm, Seq.empty[Weather], Option.empty[Weather]))),
      data => {

        val oBsonRstId = Some(serviceClientDb.string2BSONObjectID(data.rstId))
        val oJsObjectDate = Some(serviceClientDb.string2MongoDate(data.date, "date"))

        val editedWeather = Weather(None, oBsonRstId, oJsObjectDate, data.snowfall)

        wthService.updateDoc(oId, editedWeather).map(res =>
          Redirect(routes.AdminCtr.wthEditIdx(id))
        )
      })

  }

  def nghIdx = Action.async { implicit request: Request[AnyContent] =>
    nghService.getAllDocs.map(neighborhoods =>
      Ok(views.html.admin.db.neighborhoods.index(NeighborhoodForm.form, neighborhoods))
    )
  }

  def nghEditIdx(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    for {
      wthLst <- wthService.getAllDocs
      editWeatherOpt <- wthService.getDocById(id)
    } yield {
      Ok(views.html.admin.db.wth.edit(WeatherForm.form, wthLst, editWeatherOpt))
    }

  }

  def nghCreateDoc = Action.async { implicit request: Request[AnyContent] =>

    WeatherForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.wth.index(errorForm, Seq.empty[Weather]))),

      data => {

        val oBsonRstId = Some(serviceClientDb.string2BSONObjectID(data.rstId))
        val oJsObjectDate = Some(serviceClientDb.string2MongoDate(data.date, "date"))

        val newWeather = Weather(None, oBsonRstId, oJsObjectDate, data.snowfall)

        wthService.createDoc(newWeather).map(res =>
          Redirect(routes.AdminCtr.wthIdx())
        )

      }
    )

  }

  def nghDelete(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    wthService.deleteDoc(id) map { res =>
      Redirect(routes.AdminCtr.rstIdx())
    }

  }

  def nghEdit(id: BSONObjectID) = Action.async { implicit request: Request[AnyContent] =>

    val oId = Option(id)

    ResortForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.rst.edit(errorForm, Seq.empty[Resort], Option.empty[Resort]))),
      data => {
        val editedResort = Resort(None, data.name, data.country, data.continent, data.countryPrefix, data.boardingArea_km2, data.scoreBA, data.scoreSF, None)

        resortService.updateDoc(oId, editedResort).map(res =>
          Redirect(routes.AdminCtr.rstEditIdx(id))
        )
      })

  }


  //---- aggregates ----//

  def wthAgg(fromDate: String, toDate: String) = Action.async { implicit request: Request[AnyContent] =>
    wthService.getWeatherAggregateCol(Some(fromDate), Some(toDate)).map(weather =>
      Ok(views.html.admin.db.colWeatherAggregate(weather, Some(fromDate), Some(toDate)))
    )
  }

  def rstLocalIataAgg = Action.async { implicit request: Request[AnyContent] =>
    resortService.getLocalIataAggregateCol.map(nis =>
      Ok(views.html.admin.db.rst.localIata(nis))
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
