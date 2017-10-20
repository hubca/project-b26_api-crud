package models

import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.api.data.format.Formats._
import play.api.http.MediaRange.parse
import play.api.libs.json
import play.api.libs.json.{JsArray, JsNumber, JsObject, Json}

/* Created by sambo on 17/08/2017 */

class CollectionClass[+T] // covariant (parent type) of all models mapping to Mongo JSON Collctions - http://blog.kamkor.me/Covariance-And-Contravariance-In-Scala/

case class Resort(_id: Option[BSONObjectID] = None, name: String = "", country: String = "", continent: String = "", countryPrefix: String = "", boardingArea_km2: Double = 0.0, scoreBA: Double = 0.0, scoreSF: Double = 0.0, localIataArr_e: Option[JsArray] = None) extends CollectionClass[Resort] {
  def idAsBsonId = _id.get//.getOrElse("")
  def idAsString = idAsBsonId.stringify
}

case class ResortAggregate(_id: Option[BSONObjectID], name: String, localIataArr_e: LocalIata) {
  def idAsBsonId = _id.get
  def idAsString = idAsBsonId.stringify
}


case class ResortFormData(name: String, country: String, continent: String, countryPrefix: String, boardingArea_km2: Double, scoreBA: Double, scoreSF: Double)



object Resort {
  implicit val resortFormat = Json.format[Resort]

  val orderingByBoardingArea: Ordering[Resort] = Ordering.by(e => e.boardingArea_km2)
  val orderingByResortName: Ordering[Resort] = Ordering.by(e => e.name)
  val orderingByScoreBA: Ordering[Resort] = Ordering.by(e => e.scoreBA)
  val orderingByScoreSF: Ordering[Resort] = Ordering.by(e => e.scoreSF)
}

object ResortAggregate {
  implicit val resortAggregateFormat = Json.format[ResortAggregate]
  //implicit val LocalIataListFormat = Json.format[List[LocalIata]]

}

object ResortForm {

  val form = Form(
    mapping(
      "name" -> nonEmptyText,
      "country" -> nonEmptyText,
      "continent" -> nonEmptyText,
      "countryPrefix" -> nonEmptyText,
      "boardingArea_km2" -> of(doubleFormat),
      "scoreBA" -> of(doubleFormat),
      "scoreSF" -> of(doubleFormat)
    )(ResortFormData.apply)(ResortFormData.unapply)
  )

}

// todo - rename: resortInfoStr -> localIata_arr to journey

/* todo - integrate into project
MongoDB Journey requests

db.airports2.createIndex({ "airLocation_e": "2dsphere" })

var leg1Arr =
  db.airports2.aggregate([
   { "$geoNear": {
      "near": { "type": "Point", "coordinates": [ -0.149899, 51.468986 ] },
      "distanceField": "leg1Distance_km",
      "distanceMultiplier": 0.001
      "maxDistance": 400000, // in radians?
      "num": 3,
      "spherical": true
     }
  },
  { "$unwind": "$airDirectFlightsArr_e" }
  ])

var getUniqueArrayFunc = function(value, index, self) {
  return self.indexOf(value) === index;
}

var dAirportsArr = leg1Arr.map(
 function(u)
  { return u.airDirectFlightsArr_e.dfIataCode
  }
 ).filter(getUniqueArrayFunc)


[ "GVA", "INN", "YVR" ]

// get resorts with journey arrays
db.resortInfoStr.aggregate([
 { "$unwind": "$localIataArr_e" },
 { "$match":
  { "localIataArr_e.iataCode":
    { "$in": dAirportsArr }
  }
 },
 { "$addFields":
  { "localIataArr_e.uIataArr_e":
    { "$filter":
      { "input": leg1Arr,
        "as": "leg1",
        "cond":
          { "$eq":
            [ "$$leg1.airDirectFlightsArr_e.dfIataCode", "$localIataArr_e.iataCode" ]
          }
      }
    }
  }
 },
 { "$group":
  { "_id": "$_id",
    "name": { "$addToSet": "$name" },
    "journeyArr_e":
      { "$push": "$localIataArr_e"
      }
  }
 },
 { "$unwind": "$journeyArr_e" },
 { "$unwind": "$journeyArr_e.uIataArr_e" },
 { "$unwind": "$name" },
 { "$addFields": {
   "preJnyArr_e.jIataCodeArr": ["$journeyArr_e.uIataArr_e.airIataCode", "$journeyArr_e.iataCode" ],
   "preJnyArr_e.jTotalDistance_km": {
      "$add": [
          "$journeyArr_e.distance_km",
          "$journeyArr_e.uIataArr_e.airDirectFlightsArr_e.dfDistance_km",
          "$journeyArr_e.uIataArr_e.leg1Distance_km"
        ]
    },
   "preJnyArr_e.jTotalTravelTime_mins": {
      "$add": [
        "$journeyArr_e.travelTime_mins",
        "$journeyArr_e.uIataArr_e.airDirectFlightsArr_e.dfTravelTime_mins",
        {
          "$multiply":
            [ "$journeyArr_e.uIataArr_e.leg1Distance_km",
              { "$divide": [ 45, 60 ]}
            ]
        }
      ]
    },
    "preJnyArr_e.jTotalRoundTripCosts_usd": {
      "$add": [
        "$journeyArr_e.roundTripCosts_usd",
        "$journeyArr_e.uIataArr_e.airDirectFlightsArr_e.dfRoundTripCost_usd"
      ]
    }
   }
 },
 { "$group": {
      "_id": "$_id",
      "name": { "$addToSet": "$name" },
      "jnyArr_e": { "$addToSet": "$preJnyArr_e" }
   }
 },
 { "$unwind": "$name" }, // after this point is for aggregating min, max, avg for each resort
 { "$unwind": "$jnyArr_e" },
 { "$group": {
      "_id": "$_id",
      "name": { "$addToSet": "$name" },
      "minTravelDistance_km": { "$min": "$jnyArr_e.jTotalDistance_km" },
      "maxTravelDistance_km": { "$max": "$jnyArr_e.jTotalDistance_km" },
      "avgTravelDistance_km": { "$avg": "$jnyArr_e.jTotalDistance_km" },
      "minTravelTime_mins": { "$min": "$jnyArr_e.jTotalTravelTime_mins" },
      "maxTravelTime_mins": { "$max": "$jnyArr_e.jTotalTravelTime_mins" },
      "avgTravelTime_mins": { "$avg": "$jnyArr_e.jTotalTravelTime_mins" },
      "minTravelCosts_usd": { "$min": "$jnyArr_e.jTotalRoundTripCosts_usd" },
      "maxTravelCosts_usd": { "$max": "$jnyArr_e.jTotalRoundTripCosts_usd" },
      "avgTravelCosts_usd": { "$avg": "$jnyArr_e.jTotalRoundTripCosts_usd" }
   }
 },
 { "$unwind": "$name" }
]).pretty()


---

db.resortInfoStr.aggregate([
 { "$unwind": "$localIataArr_e" },
 { "$match":
  { "localIataArr_e.iataCode":
    { "$in": dAirportsArr }
  }
 },
 { "$addFields":
  { "localIataArr_e.uIataArr_e":
    { "$filter":
      { "input": leg1Arr,
        "as": "leg1",
        "cond":
          { "$eq":
            [ "$$leg1.airDirectFlightsArr_e.dfIataCode", "$localIataArr_e.iataCode" ]
          }
      }
    }
  }
 },
 { "$group":
  { "_id": "$_id",
    "name": { "$addToSet": "$name" },
    "journeyArr_e":
      { "$push": "$localIataArr_e"
      }
  }
 },
 { "$unwind": "$journeyArr_e" },
 { "$unwind": "$journeyArr_e.uIataArr_e" },
 { "$unwind": "$name" },
 { "$addFields": {
   "preJnyArr_e.jIataCodeArr": ["$journeyArr_e.uIataArr_e.airIataCode", "$journeyArr_e.iataCode" ],
   "preJnyArr_e.jTotalDistance_km": {
      "$add": [
          "$journeyArr_e.distance_km",
          "$journeyArr_e.uIataArr_e.airDirectFlightsArr_e.dfDistance_km",
          "$journeyArr_e.uIataArr_e.leg1Distance_km"
        ]
    },
   "preJnyArr_e.jTotalTravelTime_mins": {
      "$add": [
        "$journeyArr_e.travelTime_mins",
        "$journeyArr_e.uIataArr_e.airDirectFlightsArr_e.dfTravelTime_mins",
        {
          "$multiply":
            [ "$journeyArr_e.uIataArr_e.leg1Distance_km",
              { "$divide": [ 45, 60 ]}
            ]
        }
      ]
    },
    "preJnyArr_e.jTotalRoundTripCosts_usd": {
      "$add": [
        "$journeyArr_e.roundTripCosts_usd",
        "$journeyArr_e.uIataArr_e.airDirectFlightsArr_e.dfRoundTripCost_usd"
      ]
    }
   }
 },
 { "$group": {
      "_id": "$_id",
      "name": { "$addToSet": "$name" },
      "jnyArr_e": { "$addToSet": "$preJnyArr_e" }
   }
 },
 { "$unwind": "$name" },
 { "$unwind": "$jnyArr_e" },
 { "$group": {
      "_id": "$_id",
      "name": { "$addToSet": "$name" },
      "minTravelDistance_km": { "$min": "$jnyArr_e.jTotalDistance_km" },
      "maxTravelDistance_km": { "$max": "$jnyArr_e.jTotalDistance_km" },
      "avgTravelDistance_km": { "$avg": "$jnyArr_e.jTotalDistance_km" },
      "minTravelTime_mins": { "$min": "$jnyArr_e.jTotalTravelTime_mins" },
      "maxTravelTime_mins": { "$max": "$jnyArr_e.jTotalTravelTime_mins" },
      "avgTravelTime_mins": { "$avg": "$jnyArr_e.jTotalTravelTime_mins" },
      "minTravelCosts_usd": { "$min": "$jnyArr_e.jTotalRoundTripCosts_usd" },
      "maxTravelCosts_usd": { "$max": "$jnyArr_e.jTotalRoundTripCosts_usd" },
      "avgTravelCosts_usd": { "$avg": "$jnyArr_e.jTotalRoundTripCosts_usd" }
   }
 },
 { "$unwind": "$name" }
]).pretty()


 */