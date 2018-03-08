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

class OurC @Inject()(parser: BodyParsers.Default)(cc: ControllerComponents, actorSystem: ActorSystem)
                    (val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, ourService: OurService)
                    (implicit mat: Materializer)
  extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {


    override lazy val parse: PlayBodyParsers = cc.parsers

    protected val collectionName = "our"
    protected val serviceFile = ourService

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

    def showForm(queryType: String, oDocs: Option[Seq[OurMongo]], oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      val docs = oDocs.getOrElse(Seq.empty[OurMongo]) // todo - remove and change oDocs to docs
      val form = getForm(queryType, docs, oId) //errorForm.getOrElse(getForm(queryType, docs, oId))

      serviceFile.getOpts.map(inputOptions =>
        Ok(views.html.db.our.form(collectionName, queryType)(form, oId)(inputOptions)(activeTabIdx, OurMongo.tabNames))
      )

    }

    def showCollection(oDocs: Option[Seq[OurMongo]])(activeTabIdx: Int) = Action { implicit request: Request[AnyContent] =>
      Ok(views.html.db.our.collection(collectionName, oDocs)(activeTabIdx))
    }

    def getForm(queryType: String, docs: Seq[OurMongo], oId: Option[BSONObjectID]): Form[OurMongo] = queryType match {
      case "update" => OurMongoForm.form.fill(docs.find(_._id.get == oId.get).get)
      case _ => OurMongoForm.form
    }

    /*
    def getErrorForm(queryType: String, oId: Option[BSONObjectID])(errorForm: Option[Form[OurMongo]] = None)(activeTabIdx: Int): Future[Result] = {
      Redirect(routes.DatabaseC.index(collectionName, "update", oId, activeTabIdx))
    }
    */

    // query actions
    def queryCollection(queryType: String, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

      serviceFile.getOpts.flatMap(inputOptions =>

        OurMongoForm.form.bindFromRequest.fold(
          // todo - handle errors in all collection controllers (getErrorForm)
          errorForm => Future.successful(Ok(views.html.db.our.form(collectionName, queryType)(errorForm, oId)(inputOptions)(activeTabIdx, OurMongo.tabNames))
            //getErrorForm(queryType, oId)(Some(errorForm))(activeTabIdx)
          ),
          data => {

            val thisDoc = serviceFile.embedPostDataInModel(queryType, data).get
            //val adminId = 1 // todo - add adminId here? or where we added lastModified?
            queryType match {
              case "update" => updateDoc(thisDoc, oId)(activeTabIdx)(request)
              case _ => createDoc(thisDoc)(request)
            }

          }
        )

      )
    }

    def createDoc(doc: OurMongo) = Action.async { implicit request: Request[AnyContent] =>

      serviceFile.createDoc(doc).map(x =>
        Redirect(routes.DatabaseC.index(collectionName, "create", None, 1))
      )

    }

    def updateDoc(doc: OurMongo, oId: Option[BSONObjectID])(activeTabIdx: Int) = Action.async { implicit request: Request[AnyContent] =>

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