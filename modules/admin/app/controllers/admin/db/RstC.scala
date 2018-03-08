package controllers.admin.db

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source.fromFuture
import data.Pagelet
import models.db._
import play.api.data.Form
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID
import services.dbClient._
import ui.HtmlStream
import ui.HtmlStreamImplicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RstC @Inject()(parser: BodyParsers.Default)(cc: ControllerComponents, actorSystem: ActorSystem)
                    (val reactiveMongoApi: ReactiveMongoApi)
                    (serviceClientDb: ServiceClientDb, rstService: RstService, ctyService: CtyService, rgnService: RgnService)
                    (implicit mat: Materializer)
  extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {


    override lazy val parse: PlayBodyParsers = cc.parsers

    protected val collectionName = "rst"
    protected val serviceFile = rstService

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

    def showForm(queryType: String, oDocs: Option[Seq[RstMongo]], oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      val docs = oDocs.getOrElse(Seq.empty[RstMongo])
      val form = getForm(queryType, docs, oId)

      serviceFile.getOpts.map(inputOptions =>
        Ok(views.html.db.rst.form(collectionName, queryType)(form, oId)(inputOptions)(activeTabIdx, RstMongo.tabNames))
      )

    }

    def showCollection(oDocs: Option[Seq[RstMongo]])(activeTabIdx: Int) = Action { implicit request: Request[AnyContent] =>
      Ok(views.html.db.rst.collection(collectionName, oDocs)(activeTabIdx))
    }

    def getForm(queryType: String, docs: Seq[RstMongo], oId: Option[BSONObjectID]): Form[RstMongo] = queryType match {
      case "update" => RstMongoForm.form.fill(docs.find(_._id.get == oId.get).get)
      case _ => RstMongoForm.form
    }

    // query actions
    def queryCollection(queryType: String, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      serviceFile.getOpts.flatMap(inputOptions =>

        RstMongoForm.form.bindFromRequest.fold(
          errorForm => Future.successful(Ok(views.html.db.rst.form(collectionName, queryType)(errorForm, oId)(inputOptions)(activeTabIdx, RstMongo.tabNames))),
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

      )
    }

    def createDoc(doc: RstMongo) = Action.async { implicit request: Request[AnyContent] =>

      serviceFile.createDoc(doc).map(x =>
        Redirect(routes.DatabaseC.index(collectionName, "create", None, 1))
      )

    }

    def updateDoc(doc: RstMongo, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

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