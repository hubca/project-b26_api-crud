package services.dbClient

import javax.inject.{Inject, Singleton}

import models.db._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ScrService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport with SelectOptionsTrait with DateTransformTrait {

  override lazy val parse: PlayBodyParsers = cc.parsers

  protected val collectionName = "scr"

  //--- database commands ---\\

  def createDoc(newDoc: ScrMongo) = serviceClientDb.createDoc[ScrMongo](collectionName, newDoc)

  def deleteDoc(oId: Option[BSONObjectID]) = serviceClientDb.deleteDoc(collectionName, oId)

  def updateDoc(oId: Option[BSONObjectID], formData: ScrMongo): Future[Result] = serviceClientDb.updateDoc[ScrMongo](collectionName, formData, oId) // todo - change Rst model?

  def updateOneField(oId: Option[BSONObjectID], mongoField: MongoField): Future[Result] = serviceClientDb.updateOneField(collectionName, oId, mongoField)

  def getAllDocs: Future[Seq[ScrMongo]] = serviceClientDb.getAllDocs[ScrMongo](collectionName)

  def updateMultipleFields(oId: Option[BSONObjectID], formData: ScrMongo): Future[Result] = serviceClientDb.updateMultipleFields[ScrMongo](collectionName, formData, oId)

  def getDocById(oId: Option[BSONObjectID]): Future[Option[ScrMongo]] = serviceClientDb.getDocById[ScrMongo](collectionName, oId)

  def getDocByField(fieldName: String, fieldValue: String): Future[Option[ScrMongo]] = serviceClientDb.getDocByField[ScrMongo](fieldName, fieldValue, collectionName)


  //--- arrange select input options  ---\\
  def getOpts: Future[Option[Map[String, Seq[(String, String)]]]] = Future(None) // no options for select inputs

  //--- mapping form data with model ---\\
  // todo - check if this is typesafe, add output in signature & store in ServiceClientDb OR model/Cty.scala as trait method
  def embedPostDataInModel(dbAction: String, thisData: ScrMongo): Option[ScrMongo] = {

    val oAdminModifiedId = Some(1) // todo - hook into session (log-in, etc) & remove
    val oAdminCreatedId = Some(1) // todo - hook into session (log-in, etc) & remove

    val dateCreated = dbAction match {
      case "create" => thisData.getNowDate
      case "update" => thisData.dateCreated
    }

    val dataModel = ScrMongo(None, thisData.varName, thisData.weight, thisData.scoreGroup,
      oAdminCreatedId, dateCreated, oAdminModifiedId, thisData.getNowDate)

    Some(dataModel)
    // todo - JsSuccess, JsErrror HERE **
    //case otherData : _ => OtherFormData1(_, _, _, ..)
    //case e: _ => e

  }

}

