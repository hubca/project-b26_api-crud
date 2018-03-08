$(function() {
    $('table.datatable').DataTable({
        "columnDefs": [ {
        "targets": 'no-sort',
        "orderable": false,
        } ],
        "order": [[ 1, "asc" ]]
     });

    prepRadioPartnerInputs();
    $('span.buttonset input.form-control-radio').change(function(event) {
        prepRadioPartnerInputs($(this));
    });

});

function camelize(str) {
    return str.replace(/\W+(.)/g, function(match, chr) {
        return chr.toUpperCase();
    });
}

function prepRadioPartnerInputs(elem = $('span.buttonset input.form-control-radio:checked')) {

    if(elem.length < 1) return false;

    var uncleElem = elem.parents("dl").next();
    uncleElem.find("input:enabled").prop({ "required": false, "disabled": true });
    uncleElem.find("select:enabled").prop({ "required": false, "disabled": true });

    var accompanyingInputId = camelize(elem.val());
    uncleElem.find("dl").not("#"+accompanyingInputId+"_field").hide();
    uncleElem.find("#"+accompanyingInputId).prop({ "required": true, "disabled": false });
    uncleElem.find("dl#"+accompanyingInputId+"_field").show();

}

//db.rst.insert({ "name": "Whistler/Blackcomb", "description_e": { "short": "A very short description for Whistler/Blackcomb", "full": "Write up of Whistler/Blackcomb" }, "location_e": { "countryCode": "CA", "countryName": "Canada", "continent": "North America", "hemisphere": "Northern", "language": "English", "geoLocation_ee": { "type": "Point", "coordinates": [50.115198, -122.948647] }, "region_ee": { "id": 5, "name": "Rockies" }}, "metricsAndVisitors_e": { "highestAltitude_m" : 1423, "avgAnnualVisitors" : 1553700, "pisteArea_km2" : 31.2 }, "localIataArr_e": [{ "iataCode": "YVR", "distance_km": 1900, "travelTime_mins": 500, "roundTripCosts_usd": 80 }], "localDomesticAirportArr_e": [], "runsParksLifts_e": { "runsTotal": 41, "parksTotal": 7, "liftsTotal": 50 }, "runTypes_e": { "greenRunsNum": 12, "blueRunsNum": 24, "redRunsNum": 21, "blackRunsNum": 10, "greenCircleRunsNum": 0, "blueSquareRunsNum": 0, "blackDiamondRunsNum": 0, "blackDoubleDiamondRunsNum": 0, "blackTripleDiamondRunsNum": 0 }, "liftTypes_e": { "gondolaLiftsNum": 3, "buttonLiftsNum": 12, "chairLiftsNum": 75, "cableCarLiftsNum": 11, "movingCarpetLiftsNum": 4 }, "openingClosingDates_e" : { "last_ee": { "open": new Date("2016-12-01T00:00:00Z"), "closed": new Date("2017-05-22T00:00:00Z")}, "next_ee": { "open": new Date("2017-12-02T00:00:00Z"), "closed": new Date("2018-05-25T00:00:00Z") }}, "liftPassPrices_e": { "regionIdArr": [1, 2], "pricesValidFrom": new Date("2017-11-20T00:00:00Z"), "pricesValidUntil": new Date("2017-04-24T00:00:00Z"), "priceChildWeekDay_usd": 20.0, "priceAdultWeekDay_usd": 40.0, "priceChild6Day_usd": 80.0, "priceAdult6Day_usd": 200, "priceAverage_usd": 85.0, "priceDeepLinkArr": [ "http: //www.google.com" ] }, "scores_e": { "scoreBA": 0.1, "scoreSFdef": 0.3, "scoreBG": 0.1, "scoreFMpre": 0, "scoreFMdefPre": 0.1, "scoreLCpre": 0.2, "scoreGR": 0.1, "scoreAD": 0, "scoreNL": 0.1, "scoreFDpre": 0.3 }, "eventsProductsPromotionsThisSeason_e": { "totalEventsThisSeason": 24, "totalProductItemsThisSeason": 43, "totalPromotionsThisSeason": 15 }, "admin_e": { "adminModifiedId": 1, "dateCreated": new Date(), "lastModified": new Date() }})
//{ "iataCode" : "GVA", "distance_km" : 105, "travelTime_mins" : 90, "roundTripCosts_usd" : 55.2 }, { "iataCode" : "LYS", "distance_km" : 140.4, "travelTime_mins" : 140, "roundTripCosts_usd" : 65.3 }, { "iataCode" : "CMF", "distance_km" : 165.1, "travelTime_mins" : 120, "roundTripCosts_usd" : 80.9 }