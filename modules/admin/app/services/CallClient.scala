package services

import javax.inject.Inject

import play.api.libs.json.{JsResult, Json, Reads}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws._

class CallClient @Inject() (ws: WSClient)(implicit exec: ExecutionContext) {

  def makeCall(url: String) = ws.url(url).get()

  // todo - make admin and main site separation
  def makeServiceCall(inputSource: String)(serviceName: String): Future[String] = {
    makeCall(s"http://localhost:9000/admin/api/$inputSource/$serviceName").map(_.body)
  }

  def mapCall[T](url: String)(implicit reads: Reads[T]): Future[JsResult[T]] = {
    makeCall(url).map { response =>
      val jsonString = response.json
      Json.fromJson[T](jsonString)
    }
  }

}
