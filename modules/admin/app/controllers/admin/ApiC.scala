package controllers.admin

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID
import models.db._
import services.dbClient._
import db.{routes, _}
import models.db.{CtyMongo, CtyMongoForm}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiC @Inject()(parser: BodyParsers.Default)(cc: ControllerComponents, actorSystem: ActorSystem)
                    (val reactiveMongoApi: ReactiveMongoApi)
                    (rstService: RstService, ctyService: CtyService, rgnService: RgnService, ourService: OurService, scrService: ScrService, updateScores: UpdateScores)
                    (serviceClientDb: ServiceClientDb)
                    (implicit mat: Materializer) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {

  override lazy val parse: PlayBodyParsers = cc.parsers


  def testModelData(collName: String, oId: Option[BSONObjectID], oReturnField: Option[String]) = Action.async { implicit request: Request[AnyContent] =>

    //val allDocs = serviceClientDb.getAllDocs[TestModel](collName)
    val oneDoc = serviceClientDb.getDocByIdJson(collName, oId, oReturnField)

    //allDocs.map(x =>
    oneDoc.map(x =>
      Ok(x.getOrElse(Json.obj()))
    )

  }

  def retrieveData(collName: String, oId: Option[BSONObjectID]) = Action.async { implicit request: Request[AnyContent] =>

    // /api/1.1/db/r/retrieve/rst?fields=

    val allDocs = collName match {

      case "cty" => serviceClientDb.getAllDocsJson("cty")
      //case "rst" => rstService
      //case "rgn" => rgnService
      //case "our" => ourService
      //case "scr" => scrService
      case _ => serviceClientDb.getAllDocsJson("rst")

    }

    //val DBObject = new QueryBuilder()

    allDocs.map(x =>
      Ok(x.head.toString)
    )

  }



  /*
  def index(inputSource: String, serviceName: String) = Action.async { implicit request: Request[AnyContent] =>

    inputSource match {

      case "db" => rstC.index(queryType, oId)(activeTabIdx)(request)
      case _ => rstC.index(queryType, oId)(activeTabIdx)(request)

    }

  }

  def getServiceFile(collName: String) = {

    collName match {

      case "rst" => rstService
      case "cty" => ctyService
      case "rgn" => rgnService
      case "our" => ourService
      case "scr" => scrService
      case "rst" => rstService

    }

  }


  def dbQuery(collName: String, queryType: String, oId: Option[BSONObjectID]) = Action.async { implicit request: Request[AnyContent] =>


    // split into read/write
    // split by collName to handle different types?


  val serviceFile =

    collName match {

      case "rst" => rstService
      case "cty" => ctyC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case "rgn" => rgnC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case "our" => ourC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case "scr" => scrC.queryCollection(queryType, oId)(activeTabIdx)(request)
      case _ => rstC.queryCollection(queryType, oId)(activeTabIdx)(request)

    }

  }

  def queryCollection(collName: String, queryType: String, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

    val serviceFile = getServiceFile(collName)
    val inputOptions = serviceFile.getOpts

    CtyMongoForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Ok(views.html.db.cty.form(collectionName, queryType)(errorForm, oId)(inputOptions)(activeTabIdx, CtyMongo.tabNames))),
      data => {

        val thisDoc = serviceFile.embedPostDataInModel(queryType, data).get
        //val adminId = 1 // todo - add adminId here? or where we added lastModified?
        queryType match {
          case "create" => createDoc(thisDoc)(request)
          case "update" => updateDoc(thisDoc, oId)(activeTabIdx)(request)
          case _ => createDoc(thisDoc)(request)
        }

      }
    )

  }

  def createDoc(doc: CtyMongo) = Action.async { implicit request: Request[AnyContent] =>

    serviceFile.createDoc(doc).map(x =>
      Redirect(routes.DatabaseC.index(collectionName, "create", None, 1))
    )

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
*/

}