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

class RgnC @Inject()(parser: BodyParsers.Default)(cc: ControllerComponents, actorSystem: ActorSystem)
                    (val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, rgnService: RgnService)
                    (implicit mat: Materializer)
  extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {

    override lazy val parse: PlayBodyParsers = cc.parsers

    protected val collectionName = "rgn"
    protected val serviceFile = rgnService

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

    def showForm(queryType: String, oDocs: Option[Seq[RgnMongo]], oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      val docs = oDocs.getOrElse(Seq.empty[RgnMongo])
      val form = getForm(queryType, docs, oId)

      serviceFile.getOpts.map(inputOptions =>
        Ok(views.html.db.rgn.form(collectionName, queryType)(form, oId)(inputOptions)(activeTabIdx, CtyMongo.tabNames))
      )

    }

    def showCollection(oDocs: Option[Seq[RgnMongo]])(activeTabIdx: Int) = Action { implicit request: Request[AnyContent] =>
      Ok(views.html.db.rgn.collection(collectionName, oDocs)(activeTabIdx))
    }

    def getForm(queryType: String, docs: Seq[RgnMongo], oId: Option[BSONObjectID]): Form[RgnMongo] = queryType match {
      case "update" => RgnMongoForm.form.fill(docs.find(_._id.get == oId.get).get)
      case _ => RgnMongoForm.form
    }

    // query actions
    def queryCollection(queryType: String, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      RgnMongoForm.form.bindFromRequest.fold(
        errorForm => Future.successful(Ok(views.html.db.rgn.form(collectionName, queryType)(errorForm, oId)(None)(activeTabIdx, RgnMongo.tabNames))),
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

    def createDoc(doc: RgnMongo) = Action.async { implicit request: Request[AnyContent] =>

      serviceFile.createDoc(doc).map(x =>
        Redirect(routes.DatabaseC.index(collectionName, "create", None, 1))
      )

    }

    def updateDoc(doc: RgnMongo, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

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
