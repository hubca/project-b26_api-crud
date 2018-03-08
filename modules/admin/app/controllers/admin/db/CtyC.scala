package controllers.admin.db

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import data.Pagelet
import models.db._
import play.api.data.Form
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID
import services.dbClient._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CtyC @Inject()(parser: BodyParsers.Default)(cc: ControllerComponents, actorSystem: ActorSystem)
                    (val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, ctyService: CtyService)
                    (implicit mat: Materializer)
  extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {


    override lazy val parse: PlayBodyParsers = cc.parsers

    protected val collectionName = "cty"
    protected val serviceFile = ctyService

    def index(queryType: String, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      val allDocs = serviceFile.getAllDocs
      val collectionPanelF = allDocs.flatMap(allDocs => showCollection(Some(allDocs))(activeTabIdx)(request))
      val formPanelF = allDocs.flatMap(allDocs => showForm(queryType, Some(allDocs), oId)(activeTabIdx)(request))

      for {

        collectionPanelRes <- collectionPanelF
        collectionHtml <- Pagelet.readBody(collectionPanelRes)

        formPanelRes <- formPanelF
        formHtml <- Pagelet.readBody(formPanelRes)

      } yield {
        Ok(views.html.db.index(collectionName)(formHtml, collectionHtml))
      }

    }

    def showForm(queryType: String, oDocs: Option[Seq[CtyMongo]], oId: Option[BSONObjectID])(activeTabIdx: Int) = Action { implicit request: Request[AnyContent] =>

      val docs = oDocs.getOrElse(Seq.empty[CtyMongo]) // todo - remove and change oDocs to docs
      val form = getForm(queryType, docs, oId)

      val inputOptions = serviceFile.getOpts
      Ok(views.html.db.cty.form(collectionName, queryType)(form, oId)(inputOptions)(activeTabIdx, CtyMongo.tabNames))

    }

    def showCollection(oDocs: Option[Seq[CtyMongo]])(activeTabIdx: Int) = Action { implicit request: Request[AnyContent] =>
      Ok(views.html.db.cty.collection(collectionName, oDocs)(activeTabIdx))
    }

    def getForm(queryType: String, docs: Seq[CtyMongo], oId: Option[BSONObjectID]): Form[CtyMongo] = queryType match {
      case "update" => CtyMongoForm.form.fill(docs.find(_._id.get == oId.get).get)
      case _ => CtyMongoForm.form
    }

    // query actions
    def queryCollection(queryType: String, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

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

    def updateDoc(doc: CtyMongo, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      serviceFile.updateMultipleFields(oId, doc).map(x =>
        Redirect(routes.DatabaseC.index(collectionName, "update", oId, activeTabIdx))
      )

    }

    def deleteDoc(oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      serviceFile.deleteDoc(oId).map(x =>
        Redirect(routes.DatabaseC.index(collectionName, "create", oId, activeTabIdx))
      )

    }

}


//db.insert({ "rstId": ObjectId("5a28258d9830b7614f566cc7"), "usrId": ObjectId("5a28258d9830b7614f566cc7"), "sessionId": "Elg9dsadslm409e32", "userSkillLevel": "Beginner", "numOfResortVisits": 3, "lastVisit": Date("2017-11-05"), "lastGroupType": "Group", userSkillLevel": "Beginner", "liftFacilitiesRating": 4, "atmosphereRating": 3, "funRating": 4, "suitabilityRating": 4, "valueRating": 4, "liklihoodToVisitAgain": 2, "totalScore": 3.8, "adminModifiedId": 1, "dateCreated": new Date(), "lastModified": new Date() })