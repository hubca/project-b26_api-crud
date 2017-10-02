package services.dbClient

import java.util.Date
import javax.inject.Inject

import models.{Resort, CollectionClass}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents, PlayBodyParsers, Result}
import play.modules.reactivemongo.json.JSONSerializationPack.Writer
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by sambo on 29/09/2017.
  */
class ServiceClientDb @Inject() (cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  def getCollection(collectionName: String): Future[JSONCollection] = database.map(_.collection[JSONCollection](collectionName))

  def createDoc[T](collectionName: String, model: T)(implicit writes: Writer[T]) = {
    getCollection(collectionName).flatMap(_.insert(model)).map(_ => Ok(s"Document inserted into $collectionName"))
  }

  def deleteDoc(collectionName: String, id: BSONObjectID) = {

    val idSelector = Json.obj("_id" -> id)

    // todo - is this the best way to delete?
    getCollection(collectionName).flatMap(coll =>
      coll.remove(idSelector).map {
        lastError =>
          //logger.debug(s"Successfully updated with LastError: $lastError")
          Created(s"Document from $collectionName successfully deleted")

      }
    )

  }

  def updateDoc[T](collectionName: String, model: T, id: Option[BSONObjectID])(implicit writes: Writer[T]): Future[Result] = {

    val idSelector = Json.obj("_id" -> id)

    getCollection(collectionName).flatMap(coll =>
      coll.update(idSelector, model).map {
        lastError =>
          //logger.debug(s"Successfully updated with LastError: $lastError")
          Created(s"Document from $collectionName successfully updated")
      }
    )

  }

  def updateOneField(collectionName: String, id: Option[BSONObjectID], field: JsObject): Future[Result] = {

    val idSelector = Json.obj("_id" -> id)

    getCollection(collectionName).flatMap(collection =>
      collection.update(idSelector, Json.obj("$set" -> field)).map {
        lastError =>
          Created(s"$field updated")
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

  def getDocById[T](collectionName: String, id: BSONObjectID)(implicit reads: Reads[T]): Future[Option[T]] = {

    getCollection(collectionName).flatMap {
      _.find(Json.obj("_id" -> id))
        .cursor[T](ReadPreference.primary)
        .headOption
    }

  }

  def getDocByString[T](column: String, value: String, collectionName: String)(implicit reads: Reads[T]): Future[Option[T]] = {

    getCollection(collectionName).flatMap {
      _.find(Json.obj(column -> value))
        .cursor[T](ReadPreference.primary)
        .headOption
    }

  }


  def getDateLong(date: String): Long = {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
    val parseDate: Date = format.parse(date)
    parseDate.getTime
  }

  def convert2MongoDate(date: String, dateFieldName: String): JsObject = {
    val longDate: Long = getDateLong(date)
    Json.obj(s"$$$dateFieldName" -> JsNumber(longDate))
  }

  def getMongoDateRange(fromDate: String, toDate: String, dateFieldName: String): JsObject = {

    val fromDateJsObj = convert2MongoDate(fromDate, dateFieldName)
    val toDateJsObj = convert2MongoDate(toDate, dateFieldName)

    Json.obj("date" -> Json.obj("$gte" -> fromDateJsObj, "$lt" -> toDateJsObj))

  }

  /*

// todo - move to generic - clientServices?
def insertDocument[T <: collectionClass[T]](jsonString: JsValue, collection: JSONCollection)(implicit reads: Reads[T], exec: ExecutionContext) = {

  val infoFromJson: JsResult[T] = Json.fromJson[T](jsonString)

  infoFromJson match {
    case JsSuccess(t: T, path: JsPath) =>

      collection.insert(t)
      /*
      for {
        //collectionData <- collection
        lastError <- collection.insert(t)
      } yield {
        Logger.debug(s"Successfully inserted with LastError: $lastError")
        Created("Created 1 resort")
      }
      */
    case JsError(e) => Future.successful(BadRequest("Could not create a resort from the json provided."))

  }
}


def findUsing[T](column: String, value: String)(collection: JSONCollection)(implicit reads: Reads[T]): Future[Option[T]] = {

  collection.flatMap {
    _.find(Json.obj(column -> value))
      .cursor[T](ReadPreference.primary)
      .headOption
  }

}

def findById[T](id: BSONObjectID)(collection: JSONCollection)(implicit reads: Reads[T]): Future[Option[T]] = {

  collection.flatMap {
    _.find(Json.obj("_id" -> id))
      .cursor[T](ReadPreference.primary)
      .headOption
  }

}


def updateAllFields[T](id: Option[BSONObjectID], cClass: T)(collection: JSONCollection)(implicit reads: Reads[T]): Future[T] = {

  val idSelector = Json.obj("_id" -> id)

  collection.flatMap(coll =>
    coll.update(idSelector, cClass).map {
      lastError =>
        //logger.debug(s"Successfully updated with LastError: $lastError")
        Created(s"Updated")
    }
  )

  //val editedResort = Resort(id, resort.resortName, resort.resortCountry, resort.resortContinent, resort.resortCountryPrefix, resort.resortMiles, resort.scoreBA)
}

def updateOneField[T](id: Option[BSONObjectID], field: JsObject)(collection: JSONCollection)(implicit reads: Reads[T]): Future[Result] = {

  val idSelector = Json.obj("_id" -> id)

  collection.flatMap(coll =>
    coll.update(idSelector, Json.obj("$set" -> field)).map {
      lastError =>
        Created(s"Updated")
    }
  )


}
*/
}
