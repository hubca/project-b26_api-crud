package services.dbClient

import java.time.Instant
import java.util.Date
import javax.inject.Inject

import play.api.libs.json.{Json, Reads}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.{AbstractController, ControllerComponents, PlayBodyParsers}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import models._
import services.exClient.WeatherService

/**
  * Created by sambo on 18/10/2017.
  */
class UpdateScores @Inject() (cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(rstService: ResortService, wthService: WthService) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  // todo - control types expected if using different models on output OR use unit
  def updateScore(cat: String): Future[Seq[Resort]] = cat match {

    case "ba" => updateScoreBA
    case "sf_def" => updateScoreSF_def
    //case "bg" => updateScoreBG,
    //case "fm_pre" => updateScoreFM_pre,
    //case "fm_defPre" => updateScoreFM_defPre,
    //case "lc_pre" => updateScoreLC_pre,
    //case "gr" => updateScoreGR,
    //case "ad" => updateScoreAD,
    //case "nl" => updateScoreNL,
    //case "fd" => updateScoreFD,
    //case "pr" => updateScorePR,

  }

  def getScore(attr: String, xThis: Double, xMax: Double, xMin: Double): Double = attr match {

    case "ba" | "sf_def" => calculateFixedScore("high", xThis, xMax, xMin) // e.g. ba, sf
    case "_" => calculateFixedScore("low", xThis, xMax, xMin)
    // ba, sf, bg, fm, lc, gr, ad, tt, nl, fd, pr

  }

  def calculateFixedScore(best: String, xThis: Double, xMax: Double, xMin: Double): Double = best match {

    case "high" => (1 / (xMax - xMin)) * (xThis - xMin)
    case "low" => (1 / (xMax - xMin)) * (xMax - xThis)
    case "_" => 0

  }

  def updateScoreBA: Future[Seq[Resort]] = {

    val docs = rstService.getAllDocs.map(r => r.sorted(Resort.orderingByBoardingArea))

    // todo add success/failure message?
    docs.map(r =>
      r.map(r2 =>
        rstService.updateOneField(r2._id, Json.obj("scoreBA" -> getScore("ba", r2.boardingArea_km2, r.last.boardingArea_km2, r.head.boardingArea_km2)))
      )
    )
    docs

  }


  def updateScoreSF_def: Future[Seq[Resort]] = {

    val format = new java.text.SimpleDateFormat("dd-MM-yyyy")

    val fromDate = new Date// today
    val durationDays = 3 // default duration
    val addDate = fromDate.toInstant.plusSeconds(60*60*24*durationDays)
    val toDate = format.format(Date.from(addDate))// today + durationDays

    val fromDateAsString = "14-09-2017"//format.format(fromDate)// e.g. 19-10-2017
    val toDateAsString = "24-09-2017"//format.format(toDate)// e.g. 22-10-2017

    val docs = wthService.getWeatherAggregateCol(Some(fromDateAsString), Some(toDateAsString)).map(r => r.sorted(WeatherAggregate.orderingByAvgSnowfall))

    docs.map(r =>
      r.map(r2 =>
        rstService.updateOneField(r2._id, Json.obj("scoreSF" -> getScore("sf_def", r2.avgSnowfall, r.last.avgSnowfall, r.head.avgSnowfall)))
      )
    )

    rstService.getAllDocs

  }

  def updateScoreBG: Future[Seq[Resort]] = {

    /*
    num of green (or green circle) runs
    num of button lifts

    --beginner ratings (OURs)--
    -beginner ratings in the most recent season score-
    avg beginner ratings in the most recent season
    num of beginner ratings in the most recent season

    -all other beginner ratings score-
    avg of all other beginner ratings
    num of all other beginner ratings



  */

    val docs = rstService.getAllDocs.map(r => r.sorted(Resort.orderingByBoardingArea))

    // todo add success/failure message?
    docs.map(r =>
      r.map(r2 =>
        rstService.updateOneField(r2._id, Json.obj("scoreBA" -> getScore("ba", r2.boardingArea_km2, r.last.boardingArea_km2, r.head.boardingArea_km2)))
      )
    )
    docs

  }

}
