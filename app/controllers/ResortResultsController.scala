package controllers

import javax.inject._

import akka.actor.ActorSystem
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import data.Pagelet
import akka.stream.Materializer
import akka.stream.scaladsl.Source.fromFuture
import play.api.http.MediaRange.parse
import play.api.libs.json.JsValue
import services.ServiceClient
import ui.HtmlStreamImplicits._
import ui.HtmlStream

/**
  * Created by sambo on 14/08/2017.
  */
class ResortResultsController @Inject()(sc: ServiceClient)(parser: BodyParsers.Default)(cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext, mat: Materializer) extends AbstractController(cc) {

  override lazy val parse: PlayBodyParsers = cc.parsers

  def index(embed: Boolean = true) = Action(parser) { request: Request[AnyContent] =>

    val async1 = snowfall(embed = true)(request)
    val async2 = boardingArea(embed = true)(request)

    val async1Html = async1.flatMap(x => Pagelet.readBody(x))
    val async2Html = async2.flatMap(x => Pagelet.readBody(x))

    val source1 = HtmlStream(fromFuture(async1Html))
    val source2 = HtmlStream(fromFuture(async2Html))

    val merged = source1.andThen(source2)

    // @todo - correctly reference Assets Controller in result.scala.stream
    Ok.chunked(views.stream.resort.result(merged))
  }

  def snowfall(embed: Boolean = false) = Action.async { request: Request[AnyContent] =>

    val sf = sc.makeServiceCall("snowfall")
    val pos = "top right"
    val metric = "cm"

    for {
      sfStr <- sf
    } yield {
      //if(embed)
      Ok(views.html.resort.attribute(sfStr, pos, metric)(request))
    }
  }

  def boardingArea(embed: Boolean = false) = Action.async { request: Request[AnyContent] =>

    val ba = sc.makeServiceCall("boardingArea")
    val pos = "bottom left"
    val metric = "km2"

    for {
      baStr <- ba
    } yield {
      Ok(views.html.resort.attribute(baStr, pos, metric)(request))
    }
  }

}
