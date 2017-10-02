package controllers

import javax.inject.Inject

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import play.api.mvc.{Action, _}
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import data.Pagelet
import models.{ApiData, SunData, TvData}
import play.twirl.api.HtmlFormat
import services.exClient.WeatherService

/**
  * Created by sambo on 15/08/2017.
  */
class JsonController @Inject() (sw: WeatherService)(ws: WSClient)(cc: ControllerComponents)(implicit exec: ExecutionContext, mat: Materializer) extends AbstractController(cc) {

  def getResponse[T <: ApiData[T]](api: String) = Action.async { request: Request[AnyContent] =>

    sw.makeServiceCall[T](api).map { response =>

      response match {
        case JsSuccess(r: T, path: JsPath) => r match {
          case s: SunData  => Ok(views.html.ex.sun(s))
          case m: TvData => Ok(views.html.ex.tv(m))
          case _ => Ok(s"Data not compatible with templates")
        }
        case e: JsError => Ok(s"Errors: ${JsError.toJson(e).toString()}")
      }

    }
  }

  // call weather API returning html or error string (logic in services/ServiceWeather)
  def readJson = getResponse[SunData]("SunData")//getSunResponse(-33.8830, 151.2167)

  def postJson = Action(parse.json) { request: Request[JsValue] =>
    (request.body \ "name").asOpt[String].map { name =>
      Ok("Hello " + name)
    }.getOrElse {
      BadRequest("Missing parameter [name]")
    }
  }

  def twoJsons(embed: Boolean = false) = Action.async { request: Request[AnyContent] =>

    val cdAsy1 = getResponse[SunData]("SunData")(request)
    val cdAsy2 = getResponse[TvData]("TvData")(request)

    for {
      cdAsync1Result <- cdAsy1
      cdAsync2Result <- cdAsy2

      cdAsync1Body <- Pagelet.readBody(cdAsync1Result)
      cdAsync2Body <- Pagelet.readBody(cdAsync2Result)
    } yield {
      Ok(views.html.ex.index(cdAsync1Body, cdAsync2Body))
    }
  }


}
