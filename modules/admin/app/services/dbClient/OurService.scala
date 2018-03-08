package services.dbClient

import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import models.db._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class OurService @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(serviceClientDb: ServiceClientDb, rstService: RstService) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport with SelectOptionsTrait with DateTransformTrait {

  override lazy val parse: PlayBodyParsers = cc.parsers

  protected val collectionName = "our"

  //--- database commands ---\\

  def createDoc(newDoc: OurMongo) = serviceClientDb.createDoc[OurMongo](collectionName, newDoc)

  def deleteDoc(oId: Option[BSONObjectID]) = serviceClientDb.deleteDoc(collectionName, oId)

  def updateDoc(oId: Option[BSONObjectID], formData: OurMongo): Future[Result] = serviceClientDb.updateDoc[OurMongo](collectionName, formData, oId) // todo - change Rst model?

  def updateOneField(oId: Option[BSONObjectID], mongoField: MongoField): Future[Result] = serviceClientDb.updateOneField(collectionName, oId, mongoField)

  def getAllDocs: Future[Seq[OurMongo]] = serviceClientDb.getAllDocs[OurMongo](collectionName)

  def updateMultipleFields(oId: Option[BSONObjectID], formData: OurMongo): Future[Result] = serviceClientDb.updateMultipleFields[OurMongo](collectionName, formData, oId)

  def getDocById(oId: Option[BSONObjectID]): Future[Option[OurMongo]] = serviceClientDb.getDocById[OurMongo](collectionName, oId)

  def getDocByField(fieldName: String, fieldValue: String): Future[Option[OurMongo]] = serviceClientDb.getDocByField[OurMongo](fieldName, fieldValue, collectionName)


  //--- arrange select input options  ---\\

  def getOpts: Future[Option[Map[String, Seq[(String, String)]]]] = {

    val oSeasonOptsF = rstService.getSeasonOpts

    oSeasonOptsF.map(lastVisitSeasonList => {

      val optsMap = Map(
        "lastVisits" -> OurMongo.lastVisitList,
        "lastVisitSeasons" -> getLastVisitSeasonOpts,//OurMongo.lastVisitSeasonList,//getLastVisitSeasonOpts.get,//OrElse(List()),
        "userSkillLevels" -> OurMongo.userSkillLevelList,
        "lastGroupTypes" -> OurMongo.lastGroupTypeList,
        "ratings10" -> getRatingsList10
      )

      val opts = getAllOpts(optsMap)
      Some(opts)

    })

  }

  def getLastVisitSeasonOpts: Seq[(String, String)] = {

    val seasonList: List[Int] = OurMongo.lastVisitSeasonList

    seasonList.map(startingYear => {

      val startingYearStr = startingYear.toString
      val startingYearDateFormatStr = startingYearStr + "-01-01"
      startingYearDateFormatStr -> startingYearStr

    })

  }

  //--- mapping form data with model ---\\

  // todo - check if this is typesafe, add output in signature & store in ServiceClientDb OR model/Cty.scala as trait method
  def embedPostDataInModel(dbAction: String, thisData: OurMongo): Option[OurMongo] = {

    val lastVisit: Option[LastVisit] = {

      val selection = thisData.lastVisitAsObj.selection
      val oDate = selection match {

        case "now" => thisData.getNowDate
        case "departure date" => thisData.lastVisitAsObj.date
        case "season starting"  => thisData.lastVisitAsObj.date // e.g. new Date("2018")
        case "undisclosed" => None

      }

      Some(LastVisit(selection, oDate))

    }

    val oAdminModifiedId = Some(1) // todo - hook into session (log-in, etc) & remove
    val oAdminCreatedId = Some(1) // todo - hook into session (log-in, etc) & remove

    val dateCreated = dbAction match {
      case "create" => thisData.getNowDate
      case "update" => thisData.dateCreated
    }

    val dataModel = OurMongo(None, thisData.usrId, thisData.sessionId, thisData.rstId, thisData.userSkillLevel,
      thisData.numOfResortVisits, lastVisit, thisData.lastGroupType, thisData.liftFacilitiesRating,
      thisData.atmosphereRating, thisData.funRating, thisData.suitabilityRating, thisData.valueRating,
      thisData.liklihoodToVisitAgain, thisData.overallRating, oAdminCreatedId, dateCreated, oAdminModifiedId, thisData.getNowDate)

    Some(dataModel)
    // todo - JsSuccess, JsErrror HERE **
    //case otherData : _ => OtherFormData1(_, _, _, ..)
    //case e: _ => e

  }

  /*
  def transformLastVisit(formData: OurMongo): Future[OurMongo] = {


    val dcxfss = formData.copy(lastVisit = Some(new Date()))

  }
  // todo - transform = if thisSeason & (now
  def getLastVisit2(formData: OurMongo) = {

    val oRstId: Option[BSONObjectID] = formData.rstId
    val oRstDocF: Future[Option[RstMongo]] = rstService.getDocById(oRstId)

    oRstDocF.map(oRstDoc => {

      val season = oRstDoc.get.seasonAsObj
      val lastSeason = season.lastAsObj
      val nextSeason = season.nextAsObj

      val year = lastSeason.open.get.
  })


    for {
      rgnDocOpt <- rgnDocF
      ctyDocOpt <- ctyDocF
    } yield {

      val newRegion = formDataLocation.regionAsObj.copy(name = Some(rgnDocOpt.get.name))

      val ctyDoc = ctyDocOpt.get
      val newLocation = formDataLocation.copy(
        countryCode = ctyDoc.countryCode,
        continent = ctyDoc.continent,
        hemisphere = ctyDoc.hemisphere,
        region_ee = Some(newRegion)
      )

      Some(newLocation)
    }


    val startDate: Date = formDataSeason.lastAsObj.open.get
    val endDate: Date = formDataSeason.lastAsObj.closed.get
    val numOfDaysInThisSeason = formDataSeason.getDateDiff(startDate, endDate, TimeUnit.DAYS)

    val avgVistorDensityPerKm2PerDay = (formDataMetricsAndVisitors.avgAnnualVisitors / formDataMetricsAndVisitors.pisteArea_km2) / numOfDaysInThisSeason
    roundBy(avgVistorDensityPerKm2PerDay)(2).toDouble
  }
  */

}

