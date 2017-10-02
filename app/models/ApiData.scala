package models

import play.api.libs.json.Json

/**
  * Created by sambo on 17/06/2017.
  */

// _traits_
// - encapsulates method and field definitions, which can then be reused by mixing them into classes
// - unlike class inheritance, in which each class must inherit from just one superclass, a class can mix in any number of traits
// - used to define object types by specifying the signature of the supported methods
// - can be partially implemented but may not have constructor parameters

class ApiData[+T]
case class SunDataResults(sunrise: String, sunset: String, solar_noon: String, day_length: Long, civil_twilight_begin: String, civil_twilight_end: String, nautical_twilight_begin: String, nautical_twilight_end: String, astronomical_twilight_begin: String, astronomical_twilight_end: String)
case class SunData(results: SunDataResults, status: String) extends ApiData[SunData]
case class TvData(url: String, name: String, gender: String, culture: String, born: String, died: String, titles: Array[String], aliases: Array[String], father: String, mother: String, spouse: String, allegiances: Array[String], books: Array[String], povBooks: Array[String], tvSeries: Array[String], playedBy: Array[String]) extends ApiData[TvData]

object SunData {
  implicit val sunDataResultsReads = Json.reads[SunDataResults]
  implicit val sunDataReads = Json.reads[SunData]

  //val url =
}

object TvData {
  implicit val tvInfoReads = Json.reads[TvData]
}
