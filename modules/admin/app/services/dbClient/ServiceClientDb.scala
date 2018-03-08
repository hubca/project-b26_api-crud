package services.dbClient

import java.time.Instant
import java.util.Date
import javax.inject.Inject
import java.util.concurrent.TimeUnit

import akka.stream.Materializer
import models.db.MongoField
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsObject, _}
import play.api.mvc.{AbstractController, ControllerComponents, PlayBodyParsers, Result}
import play.modules.reactivemongo.json.JSONSerializationPack.Writer
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceClientDb @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {

  override lazy val parse: PlayBodyParsers = cc.parsers

  def getCollection(collectionName: String): Future[JSONCollection] = database.map(_.collection[JSONCollection](collectionName))

  // todo 1) consider redesigning nested documents as data packages to the end point
  // todo 2) Polymorph db collections
  // todo 3) Is ReactiveMongo (aggregation framework) composable and an enabler to building queries?
  // todo 4) queries should just return data required at the time

  // -- generic queries -- \\

  def createDoc[T](collectionName: String, model: T)(implicit writes: Writer[T]) = {// todo - create a flatmap of admin collection model to insert data once
    getCollection(collectionName).flatMap(_.insert(model)).map(_ => Ok(s"Document inserted into $collectionName"))
  }

  def deleteDoc(collectionName: String, oId: Option[BSONObjectID]) = {

    val idSelector = Json.obj("_id" -> oId)

    // todo - is this the best way to delete?
    getCollection(collectionName).flatMap(coll =>
      coll.remove(idSelector).map {
        lastError =>
          //logger.debug(s"Successfully updated with LastError: $lastError")
          Created(s"Document from $collectionName successfully deleted")

      }
    )

  }

  // USE INSTEAD OF updateMultipleFields ??
  def updateDoc[T](collectionName: String, model: T, oId: Option[BSONObjectID])(implicit writes: Writer[T]): Future[Result] = {

    val idSelector = Json.obj("_id" -> oId)

    getCollection(collectionName).flatMap(coll =>
      coll.update(idSelector, model).map {
        lastError =>
          //logger.debug(s"Successfully updated with LastError: $lastError")
          Created(s"Document from $collectionName successfully updated")
      }
    )

  }

  def updateOneField(collectionName: String, oId: Option[BSONObjectID], mongoField: MongoField): Future[Result] = {

    val setObject = mongoField.fieldValue match {
      case s: String => Json.obj(mongoField.fieldName -> s.asInstanceOf[String])
      case i: Int => Json.obj(mongoField.fieldName -> i.asInstanceOf[Int])
      case d: Double => Json.obj(mongoField.fieldName -> d.asInstanceOf[Double])
      case n: Long => Json.obj(mongoField.fieldName -> n.asInstanceOf[Long])
      case ls: List[String] => Json.obj(mongoField.fieldName -> ls.asInstanceOf[List[String]])
      case li: List[Int] => Json.obj(mongoField.fieldName -> li.asInstanceOf[List[Int]])
      case ld: List[Double] => Json.obj(mongoField.fieldName -> ld.asInstanceOf[List[Double]])
      case ln: List[Long] => Json.obj(mongoField.fieldName -> ln.asInstanceOf[List[Long]])
      //case x: Any => Json.obj(mongoField.fieldName -> x.asInstanceOf[Any])
    }

    val idSelector = Json.obj("_id" -> oId)

    getCollection(collectionName).flatMap(collection =>
      collection.update(idSelector, Json.obj("$set" -> setObject)).map {
        lastError => Created(s"${mongoField.fieldName} updated")
      }
    )

  }

  def updateMultipleFields[T](collectionName: String, model: T, oId: Option[BSONObjectID])(implicit writes: Writer[T]): Future[Result] = {

    val idSelector = Json.obj("_id" -> oId)
    getCollection(collectionName).flatMap(collection =>
      //collection.update(idSelector, Json.obj("$set" -> model)).map {
      collection.update(idSelector, Json.obj("$set" -> model)).map {
        lastError =>
          Created("fields updated")
      }
    )

  }

  def getAllDocs[T](collectionName: String)(implicit reads: Reads[T]): Future[Seq[T]] = {

    getCollection(collectionName).flatMap(
      // find all
      _.find(Json.obj())
        // perform the query and get a cursor of JsObject
        .cursor[T](ReadPreference.primary)
        // Collect the results as a list
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[T]]())
    )

  }

  def getDocById[T](collectionName: String, oId: Option[BSONObjectID])(implicit reads: Reads[T]): Future[Option[T]] = {

    getCollection(collectionName).flatMap {
      _.find(Json.obj("_id" -> oId))
        .cursor[T](ReadPreference.primary)
        .headOption
    }

  }

  def getDocByField[T](fieldName: String, fieldValue: String, collectionName: String)(implicit reads: Reads[T]): Future[Option[T]] = {

    getCollection(collectionName).flatMap {
      _.find(Json.obj(fieldName -> fieldValue))
        .cursor[T](ReadPreference.primary)
        .headOption
    }

  }

  // testing json object model
  def getAllDocsJson(collectionName: String)(implicit reads: Reads[JsObject]): Future[Seq[JsObject]] = {

    getCollection(collectionName).flatMap(
      // find all
      _.find(Json.obj())
        // perform the query and get a cursor of JsObject
        .cursor[JsObject](ReadPreference.primary)
        // Collect the results as a list
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]())
    )

  }

  def getDocByIdJson(collectionName: String, oId: Option[BSONObjectID], oReturnField: Option[String])(implicit reads: Reads[JsObject]): Future[Option[JsObject]] = {

    getCollection(collectionName).flatMap {
      _.find(Json.obj("_id" -> oId), Json.obj(oReturnField.getOrElse("_id") -> 1)) // todo solve optional issues if not sent
        .cursor[JsObject](ReadPreference.primary)
        .headOption
    }

  }

  // -- date, time related -- //

  def format = new java.text.SimpleDateFormat("yyyy-MM-dd")

  //def date2String(oDate: Option[Date]): String = format.format(Date.from(Instant.ofEpochMilli(oDate.get.getTime)))

  // todo - DO WE NEED THE FOLLOWING ???
  def date2Long(date: Date): Long = {
    new DateTime(date.getTime())
      .withZoneRetainFields(DateTimeZone.UTC)
      .withZone(DateTimeZone.getDefault())
      .getMillis
  }

  // DELETE/MOVE THE FOLLOWING FUNCTIONS ???
  def now2MongoDate(dateFieldName: String): JsObject = {
    val now = new Date()
    val nowStr = format.format(now)
    str2MongoDate(nowStr, dateFieldName)
  }

  def str2Date(dateStr: String): Date = format.parse(dateStr)

  def dateStr2Long(dateStr: String): Long = {
    val parseDate = str2Date(dateStr)
    date2Long(parseDate)
  }

  def str2MongoDate(dateStr: String, dateFieldName: String): JsObject = {
    val longDate: Long = dateStr2Long(dateStr)
    Json.obj(s"$$$dateFieldName" -> JsNumber(longDate))
  }

  def getMongoDateRange(fromDate: String, toDate: String, dateFieldName: String): JsObject = {

    val fromDateJsObj = str2MongoDate(fromDate, dateFieldName)
    val toDateJsObj = str2MongoDate(toDate, dateFieldName)

    Json.obj("date" -> Json.obj("$gte" -> fromDateJsObj, "$lt" -> toDateJsObj))

  }

  def str2BSONObjectID(idString: String): BSONObjectID = BSONObjectID.parse(idString).get


  /*
  //--- possibly useful functions ---\\
  //--- aggregates ---\\

  def getWeatherAggregateCol(oFromDate: Option[String], oToDate: Option[String]): Future[Seq[WeatherAggregate]] = {
    serviceClientDb.getCollection(collectionName).flatMap(res => getWeatherAggregate(res, oFromDate, oToDate))
  }


    def getWeatherAggregate(col: JSONCollection, oFromDate: Option[String], oToDate: Option[String]): Future[Seq[WeatherAggregate]] = {

    import col.BatchCommands.AggregationFramework.{Group, Match, SumField, AvgField, MinField, MaxField}

    val aggr = oFromDate match {

      case Some(_) =>
        col.aggregate(
          Match(serviceClientDb.getMongoDateRange(oFromDate.get, oToDate.get, "date")), //  Json.obj("date" -> Json.obj("$gte" -> fromDate, "$lt" -> toDate))
          List(Group(JsString("$rstId"))(
            "totalSnowfall" -> SumField("snowfall"),
            "avgSnowfall" -> AvgField("snowfall"),
            "minSnowfall" -> MinField("snowfall"),
            "maxSnowfall" -> MaxField("snowfall")
          ))
        )

      case None =>
        col.aggregate(
          Group(JsString("$rstId"))(
            "totalSnowfall" -> SumField("snowfall"),
            "avgSnowfall" -> AvgField("snowfall"),
            "minSnowfall" -> MinField("snowfall"),
            "maxSnowfall" -> MaxField("snowfall")
          )
        )

    }

    aggr.map(_.head[WeatherAggregate])

  }

  def getLocalIataAggregateCol = {
    serviceClientDb.getCollection(collectionName).flatMap(res => getLocalIataAggregate(res))
  }

  def getLocalIataAggregate(col: JSONCollection) = {

    import col.BatchCommands.AggregationFramework.{UnwindField}

    col.aggregate(UnwindField("localIataArr_e")).map(_.head[TestRstAggregate])

  }

  //--- calculating total score (in embedPostDataInModel) ---\\
  val getTotalScore: Option[Double] = {
    val ratingsList = List(thisData.liftFacilitiesRating, thisData.atmosphereRating, thisData.funRating, thisData.suitabilityRating, thisData.valueRating)
    Some(ratingsList.map(_.getOrElse(0).toDouble).sum / ratingsList.length)
  }

   */
}