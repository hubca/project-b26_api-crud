package services.dbClient

import java.util.Date
import javax.inject.{Inject, Singleton}

import models.db._
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RgnService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport with SelectOptionsTrait {

  override lazy val parse: PlayBodyParsers = cc.parsers

  protected val collectionName = "rgn"

  //--- database commands ---\\

  def createDoc(newDoc: RgnMongo) = serviceClientDb.createDoc[RgnMongo](collectionName, newDoc)

  def deleteDoc(oId: Option[BSONObjectID]) = serviceClientDb.deleteDoc(collectionName, oId)

  def updateDoc(oId: Option[BSONObjectID], formData: RgnMongo): Future[Result] = serviceClientDb.updateDoc[RgnMongo](collectionName, formData, oId) // todo - change Rst model?

  def updateOneField(oId: Option[BSONObjectID], mongoField: MongoField): Future[Result] = serviceClientDb.updateOneField(collectionName, oId, mongoField)

  def getAllDocs: Future[Seq[RgnMongo]] = serviceClientDb.getAllDocs[RgnMongo](collectionName)

  def updateMultipleFields(oId: Option[BSONObjectID], formData: RgnMongo): Future[Result] = serviceClientDb.updateMultipleFields[RgnMongo](collectionName, formData, oId)

  def getDocById(oId: Option[BSONObjectID]): Future[Option[RgnMongo]] = serviceClientDb.getDocById[RgnMongo](collectionName, oId)

  def getDocByField(fieldName: String, fieldValue: String): Future[Option[RgnMongo]] = serviceClientDb.getDocByField[RgnMongo](fieldName, fieldValue, collectionName)


  //--- arrange select input options  ---\\

  def getOpts: Future[Option[Map[String, Seq[(String, String)]]]] = Future(None) // no options for select inputs

  // used to get Region options for rst
  def getRegionOpts: Future[Option[Seq[(String, String)]]] = {

    getAllDocs.map(docs =>
      // todo pattern match success here?
      Some( docs.map(doc => doc.idAsString(doc._id) -> doc.name) )
    )

  }

  //--- mapping form data with model ---\\

  // todo - check if this is typesafe, add output in signature & store in ServiceClientDb OR model/Cty.scala as trait method
  def embedPostDataInModel(dbAction: String, thisData: RgnMongo): Option[RgnMongo] = {

    val dateCreated = dbAction match {
      case "create" => thisData.getNowDate
      case "update" => thisData.dateCreated
    }

    val oGeoLocation = Some(GeoLocation("Point", Some(List.empty))) // todo - remove when we have coordinates in place
    val oAdminModifiedId = Some(1) // todo - hook into session (log-in, etc) & remove
    val oAdminCreatedId = Some(1) // todo - hook into session (log-in, etc) & remove

    val dataModel = RgnMongo(None, thisData.name, thisData.geoLocation_e, oAdminCreatedId, dateCreated, oAdminModifiedId,
      thisData.getNowDate)
    Some(dataModel)

    // todo - JsSuccess, JsErrror HERE **
    //case otherData : _ => OtherFormData1(_, _, _, ..)
    //case e: _ => e

  }

  // todo - add rgn (region) collection in mongodb with area coordinates to define the region. Then hook up with rst to automate region id arr insertion
  // todo - test with coordinates [6.870129, 45.923733]

}

