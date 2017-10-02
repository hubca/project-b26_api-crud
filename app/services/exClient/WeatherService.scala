package services.exClient

import javax.inject.Inject

import models.{ApiData, SunData, TvData}
import play.api.libs.json._
import services.ServiceClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by sambo on 15/08/2017.
  */
class WeatherService @Inject() (sc: ServiceClient)(implicit exec: ExecutionContext) {

  def makeServiceCall[T <: ApiData[T]](api: String) = {

    // implicit values would go here if were not placed in companion objects

    api match {
      case "SunData" => mapCall[SunData](s"http://api.sunrise-sunset.org/json?lat=-33.8830&lng=151.2167&formatted=0")
      case "TvData" => mapCall[TvData](s"http://anapioficeandfire.com/api/characters/583")
    }

  }

  def mapCall[T](url: String)(implicit reads: Reads[T]) = {
    sc.makeCall(url).map { response =>
      val jsonString = response.json
      Json.fromJson[T](jsonString)
    }
  }

}