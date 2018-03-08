package services.dbClient

import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import models.db._
import org.joda.time.{DateTime, Days}
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RstService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, ctyService: CtyService, rgnService: RgnService) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport with NumbersFormattingTrait with SelectOptionsTrait with DateTransformTrait {

  override lazy val parse: PlayBodyParsers = cc.parsers

  protected val collectionName = "rst"

  //--- database commands ---\\

  def createDoc(formData: RstMongo): Future[Result] = {

    autoFillDocument(formData).flatMap(newDoc =>
      serviceClientDb.createDoc[RstMongo](collectionName, newDoc)
    )

  }

  def updateDoc(oId: Option[BSONObjectID], formData: RstMongo): Future[Result] = {
    serviceClientDb.updateDoc[RstMongo](collectionName, formData, oId)
  }

  def deleteDoc(oId: Option[BSONObjectID]) = serviceClientDb.deleteDoc(collectionName, oId)

  // def updateDoc(oId: Option[BSONObjectID], editedRst: RstAdminFormData_tab1): Future[Result] = serviceClientDb.updateDoc[RstAdminFormData_tab1](collectionName, editedRst, oId) // todo - change Rst model?

  def updateOneField(oId: Option[BSONObjectID], mongoField: MongoField): Future[Result] = serviceClientDb.updateOneField(collectionName, oId, mongoField)

  def updateMultipleFields(oId: Option[BSONObjectID], formData: RstMongo): Future[Result] = {

    autoFillDocument(formData).flatMap(updatedDoc =>
      serviceClientDb.updateMultipleFields[RstMongo](collectionName, updatedDoc, oId)
    )

  }

  def getAllDocs: Future[Seq[RstMongo]] = serviceClientDb.getAllDocs[RstMongo](collectionName)

  def getDocById(oId: Option[BSONObjectID]): Future[Option[RstMongo]] = serviceClientDb.getDocById[RstMongo](collectionName, oId)

  def getDocByField(fieldName: String, fieldValue: String): Future[Option[RstMongo]] = serviceClientDb.getDocByField[RstMongo](fieldName, fieldValue, collectionName)

  def autoFillDocument(formData: RstMongo): Future[RstMongo] = {

    val newLocation: Future[Option[Location]] = getNewLocation(formData.location_e)
    val newMetricsAndVisitors: Option[MetricsAndVisitors] = autoFillMetricsAndVisitors(
      formData.metricsAndVisitors_e, formData.season_e
    )

    newLocation.map(newLoc => formData.copy(location_e = newLoc, metricsAndVisitors_e = newMetricsAndVisitors))

  }

  def autoFillMetricsAndVisitors(oFormDataMetricsAndVisitors: Option[MetricsAndVisitors],
                                 oFormDataSeason: Option[Season]): Option[MetricsAndVisitors] = {

    val newMetricsAndVisitors = oFormDataMetricsAndVisitors.get.copy(
      avgVistorDensityPerKm2PerDay = Some(
        getAvgVistorDensityPerKm2PerDay(oFormDataMetricsAndVisitors, oFormDataSeason)
      )
    )
    Some(newMetricsAndVisitors)

  }

  def getAvgVistorDensityPerKm2PerDay(oFormDataMetricsAndVisitors: Option[MetricsAndVisitors],
                                      oFormDataSeason: Option[Season]): Double = {

    val formDataMetricsAndVisitors = oFormDataMetricsAndVisitors.get
    val formDataSeason = oFormDataSeason.get

    val startDate: Date = formDataSeason.lastAsObj.open.get
    val endDate: Date = formDataSeason.lastAsObj.closed.get
    val numOfDaysInThisSeason = formDataSeason.getDateDiff(startDate, endDate, TimeUnit.DAYS)

    val avgVistorDensityPerKm2PerDay = (formDataMetricsAndVisitors.avgAnnualVisitors / formDataMetricsAndVisitors.pisteArea_km2) / numOfDaysInThisSeason
    roundBy(avgVistorDensityPerKm2PerDay)(2).toDouble
  }

  def getNewLocation(oFormDataLocation: Option[Location]): Future[Option[Location]] = {

    val formDataLocation: Location = oFormDataLocation.get

    val ctyDocF = ctyService.getDocByField("varData_e.countryName", formDataLocation.countryName)
    val rgnDocF = rgnService.getDocById(formDataLocation.regionAsObj.id)

    for {
      rgnDocOpt <- rgnDocF
      ctyDocOpt <- ctyDocF
    } yield {

      val newRegion = formDataLocation.regionAsObj.copy(name = Some(rgnDocOpt.get.name))

      val ctyDoc = ctyDocOpt.get
      val newLocation = formDataLocation.copy(
        countryCode = ctyDoc.oVarDataAsObj.countryCode,
        continent = ctyDoc.oVarDataAsObj.continent,
        hemisphere = ctyDoc.oVarDataAsObj.hemisphere,
        region_ee = Some(newRegion)
      )

      Some(newLocation)
    }

  }


  // arrange select input options
  def getOpts: Future[Option[Map[String, Seq[(String, String)]]]] = {

    val oCountryNamesOptsF = ctyService.getCountryNameOpts
    val oRegionsOptsF = rgnService.getRegionOpts

    for {
      oCountryNamesOpts <- oCountryNamesOptsF
      oRegionsOpts <- oRegionsOptsF
    } yield {

      val optsMap = Map(
        "countryNames" -> oCountryNamesOpts.getOrElse(List()),
        "regions" -> oRegionsOpts.getOrElse(List())
      )

      val opts = getAllOpts(optsMap)
      Some(opts)

    }

  }

  // used to get seasons options for our, e.g. 2016/17
  def getSeasonOpts: Future[Option[Seq[(String, String)]]] = {

    getAllDocs.map(docs =>
      Some(
        docs.map(doc => {

          date2String(doc.seasonAsObj.lastAsObj.open) -> getNorthernHemisphereSeasonFormat(doc.seasonAsObj.lastAsObj.open)

        })
      )
    )

  }

  //--- mapping form data with model ---\\

  // todo - check if this is typesafe, add output in signature & store in ServiceClientDb OR model/Rst.scala as trait method
  def embedPostDataInModel(dbAction: String, thisData: RstMongo): Option[RstMongo] = {

    // todo - ** function to get rstData.scores_e here **
    //thisData.get.


    val getScores = thisData.scores_e // todo - formulate all scores into jsObject OR SHOULD THIS BE DONE IN FORM ??

    val oAdminModifiedId = Some(1) // todo - hook into session (log-in, etc) & remove
    val oAdminCreatedId = Some(1) // todo - hook into session (log-in, etc) & remove

    val dateCreated = dbAction match {
      case "create" => thisData.getNowDate
      case "update" => thisData.dateCreated
    }

    val dataModel = RstMongo(None, thisData.name, thisData.metricsAndVisitors_e, thisData.description_e, thisData.location_e,
      thisData.runsParksLifts_e, thisData.runTypes_e, thisData.liftTypes_e, thisData.season_e, thisData.liftPassPrices_e,
      thisData.localIataArr_e, thisData.localDomesticAirportArr_e, getScores, thisData.eventsProductsPromotionsThisSeason_e,
      oAdminCreatedId, dateCreated, oAdminModifiedId, thisData.getNowDate)

    Some(dataModel)

    // todo - JsSuccess, JsErrror HERE **
    //case otherData : _ => OtherFormData1(_, _, _, ..)
    //case e: _ => e

  }

  //--- ---//
  /*
  def getLocalIataAggregateCol = {
    serviceClientDb.getCollection(collectionName).flatMap(res => getLocalIataAggregate(res))
  }

  def getLocalIataAggregate(col: JSONCollection) = {

    import col.BatchCommands.AggregationFramework.UnwindField

    col.aggregate(UnwindField("localIataArr_e")).map(_.head[RstAggregate])

  }
  */

  // todo - test with coordinates [6.870129, 45.923733]
}

