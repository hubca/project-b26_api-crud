package services

import java.net.URL
import java.util.Date
import javax.swing.text.DateFormatter

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.data.format.Formats._
import play.api.libs.json.{JsNumber, JsObject, Json}
import play.api.mvc.QueryStringBindable
import reactivemongo.bson.BSONObjectID

/**
  * Created by sambo on 13/11/2017.
  */
object CustomBinders {

  implicit object queryStringBindableBSONObjectID
    extends play.api.mvc.QueryStringBindable.Parsing[BSONObjectID](
      BSONObjectID.parse(_).get,
      _.stringify,
      (key: String, e: Exception) =>
        "Cannot parse parameter %s as BSONObjectID: %s".format(key, e.getMessage)
    )

  implicit object UrlFormatter extends Formatter[URL] {
    override val format = Some(("format.url", Nil))
    override def bind(key: String, data: Map[String, String]) = parsing(new URL(_), "error.url", Nil)(key, data)
    override def unbind(key: String, value: URL) = Map(key -> value.toString)
  }

  def jodaDateTimeFormat(pattern: String, timeZone: DateTimeZone = DateTimeZone.getDefault): Formatter[DateTime] = new Formatter[DateTime] {

    val formatter = DateTimeFormat.forPattern(pattern).withZone(timeZone)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(formatter.parseDateTime, "error.date", Nil)(key, data)

    def unbind(key: String, value: DateTime) = Map(key -> value.withZone(timeZone).toString(pattern))
  }

  /*
  implicit object MongoJsFormat {
    private val dateFmt = ISODateTimeFormat.dateTime()

    implicit val dateTimeRead: Reads[DateTime] = (
      (__ \ "$date").read[Long].map { dateTime =>
        new DateTime(dateTime)
      }
      )

    implicit val dateTimeWrite: Writes[DateTime] = new Writes[DateTime] {
      def writes(dateTime: DateTime): JsValue = Json.obj(
        "$date" -> dateTime.getMillis
      )
    }
  }

  val dateFormat = ISODateTimeFormat.dateTime()

  implicit object DateFormatter extends Formatter[jodaDateTimeFormat] {
    //override val format = Some((ISODateTimeFormat.dateTime(), Nil))//Some(("format.url", Nil))
    //override def bind(key: String, data: Map[String, String]) = parsing(new URL(_), "error.url", Nil)(key, data)
    override def bind(key: String, data: Map[String, Seq[String]]): Option[Either[String, DateTime]] = {
      val dateString: Option[Seq[String]] = data.get(key)
      try {
        Some(Right(new DateTime(dateFormat.parseDateTime(dateString.get.head).getMillis)))
      } catch {
        case e: IllegalArgumentException => Option(Left(dateString.get.head))
      }
    }
    def unbind(key: String, value: DateTime): String = {
      dateFormat.print(value.toDate.getTime)
    }
  }
  */

  /*
  val dateFormat = ISODateTimeFormat.dateTime()

  implicit def dateBinder: QueryStringBindable[DateTime] = new QueryStringBindable[DateTime] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DateTime]] = {
      val dateString: Option[Seq[String]] = params.get(key)
      try {
        Some(Right(new DateTime(dateFormat.parseDateTime(dateString.get.head).getMillis)))
      } catch {
        case e: IllegalArgumentException => Option(Left(dateString.get.head))
      }
    }

    def unbind(key: String, value: DateTime): String = {
      dateFormat.print(value.toDate.getTime)
    }
  }
  */

  /*
  implicit object JsObjectFormatter extends Formatter[JsObject] {
    //override val format = Some(("format.url", Nil))
    override val format = ISODateTimeFormat.date()
    //override def bind(key: String, data: Map[String, String]) = parsing(new URL(_), "error.url", Nil)(key, data)


    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], JsObject] = {
      data.get(key).map(UUID.fromString(_)).toRight(Seq(FormError(key, "forms.invalid.uuid", data.get(key).getOrElse(key))))
    }

    def bind(date: Date, dateFieldName: String): JsObject = {

      val longDate = new DateTime(date.getTime())
        .withZoneRetainFields(DateTimeZone.UTC)
        .withZone(DateTimeZone.getDefault())
        .getMillis

      Json.obj(s"$$$dateFieldName" -> JsNumber(longDate))

    }

    //override def unbind(key: String, value: URL) = Map(key -> value.toString)
    override def unbind(key: String, value: JsObject): Map[String, String] = Map(key -> value.toString)
  }
*/
  // to be used for dates as options in routes?
/*
    implicit def dateBinder: QueryStringBindable[Date] = new QueryStringBindable[Date] {
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Date]] = {
        val dateString: Option[Seq[String]] = params.get(key)
        try {
          Some(Right(new Date(dateFormat.parseDateTime(dateString.get.head).getMillis)))
        } catch {
          case e: IllegalArgumentException => Option(Left(dateString.get.head))
        }
      }

      def unbind(key: String, value: Date): String = {
        dateFormat.print(value.getTime)
      }
    }
*/

}
