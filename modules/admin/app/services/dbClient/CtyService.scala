package services.dbClient

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
class CtyService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport with SelectOptionsTrait {

  override lazy val parse: PlayBodyParsers = cc.parsers

  protected val collectionName = "cty"

  //--- database commands ---\\

  def createDoc(newDoc: CtyMongo) = serviceClientDb.createDoc[CtyMongo](collectionName, newDoc)

  def deleteDoc(oId: Option[BSONObjectID]) = serviceClientDb.deleteDoc(collectionName, oId)

  def updateDoc(oId: Option[BSONObjectID], formData: CtyMongo): Future[Result] = serviceClientDb.updateDoc[CtyMongo](collectionName, formData, oId) // todo - change Rst model?

  def updateOneField(oId: Option[BSONObjectID], mongoField: MongoField): Future[Result] = serviceClientDb.updateOneField(collectionName, oId, mongoField)

  def getAllDocs: Future[Seq[CtyMongo]] = serviceClientDb.getAllDocs[CtyMongo](collectionName)

  def updateMultipleFields(oId: Option[BSONObjectID], formData: CtyMongo): Future[Result] = serviceClientDb.updateMultipleFields[CtyMongo](collectionName, formData, oId)

  def getDocById(oId: Option[BSONObjectID]): Future[Option[CtyMongo]] = serviceClientDb.getDocById[CtyMongo](collectionName, oId)

  def getDocByField(fieldName: String, fieldValue: String): Future[Option[CtyMongo]] = serviceClientDb.getDocByField[CtyMongo](fieldName, fieldValue, collectionName)


  // arrange select input options
  def getOpts: Option[Map[String, Seq[(String, String)]]] = {

    val optsMap = Map(
      "continents" -> CtyVar.continentList,
      "hemispheres" -> CtyVar.hemisphereList
    )

    val opts = getAllOpts(optsMap)
    Some(opts)

  }

  // used to get Country Name options for rst
  def getCountryNameOpts: Future[Option[List[String]]] = {

    getAllDocs.map(docs =>
      // todo pattern match success here?
      Some( docs.map(doc => doc.oVarDataAsObj.countryName).toList )
    )

  }

  //--- mapping form data with model ---\\

  // todo - check if this is typesafe, add output in signature & store in ServiceClientDb OR model/Cty.scala as trait method
  def embedPostDataInModel(dbAction: String, thisData: CtyMongo): Option[CtyMongo] = {

    val oAdminModifiedId = Some(1) // todo - hook into session (log-in, etc) & remove
    val oAdminCreatedId = Some(1) // todo - hook into session (log-in, etc) & remove

    val dateCreated = dbAction match {
      case "create" => thisData.getNowDate
      case "update" => thisData.dateCreated
    }

    val dataModel = CtyMongo(None, oAdminCreatedId, dateCreated, oAdminModifiedId, thisData.getNowDate, thisData.varData_e)

    //val somethimng = Json.toJson(dataModel)
    Some(dataModel)
    // todo - JsSuccess, JsErrror HERE **
    //case otherData : _ => OtherFormData1(_, _, _, ..)
    //case e: _ => e

  }


  // todo - test with coordinates [6.870129, 45.923733]

}

