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

  /*
  def wthCreateDoc = Action.async { implicit request: Request[AnyContent] =>

    WeatherForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.admin.db.wth.index(errorForm, Seq.empty[Weather]))),


      data => {

        val bsonRstId = BSONObjectID.parse(data.rstId).get
        val jsObjectDate = Json.obj("$date" -> data.date)
        val longDate = jsObjectDate.as[Long]

        //val dateAsDate = Date.from(Instant.ofEpochMilli(longDate))
        val dateAsDate = serviceClientDb.convert2MongoDate(data.date, "date")




        val dateAsString = format.format(Date.from(Instant.ofEpochMilli(date.get.apply("$date").as[Long])))
        //val dateAsDate = Date.from(Instant.ofEpochMilli(data.date.get.apply("$date").as[Long]))

        val newWeather = Weather(None, Some(bsonRstId), Some(dateAsDate), data.snowfall)

        wthService.createDoc(newWeather).map(res =>
          Redirect(routes.DatabaseController.wthIdx())
        )

      }
    )

  }
*/

//---- scoring updates & additions ----//
  def showAllCollections = Action.async { implicit request: Request[AnyContent] =>

    for {
      resortsCol <- resortService.getAllDocs
      weatherCol <- wthService.getAllDocs
      weatherAggCol <- resortService.getWeatherAggregateCol
    } yield {
      Ok(views.html.admin.db.allCollections(resortsCol, weatherCol, weatherAggCol))
    }

  }

  def triggerScoreBA = Action.async { implicit request: Request[AnyContent] =>
    resortService.triggerScoreBA.map(resorts =>
      Ok(views.html.admin.db.rst.index(ResortForm.form, resorts))
    )
  }

  def weatherAgg = Action.async { implicit request: Request[AnyContent] =>
    resortService.getWeatherAggregateCol.map(weather =>
      Ok(views.html.admin.db.colWeatherAggregate(weather))
    )
  }

/*
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
