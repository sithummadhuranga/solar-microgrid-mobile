package com.solarmicrogrid.app.model

import org.json.JSONArray
import org.json.JSONObject

// one microgrid node from GET /api/stations
class Station(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val capacityKwh: Double,
    val batterySlotCount: Int,
    val openingTime: String,
    val closingTime: String,
    val status: String
) {
    companion object {

        // reads one station from a json object
        fun fromJson(json: JSONObject): Station {
            return Station(
                id = json.optString("id"),
                name = json.optString("name"),
                address = json.optString("address"),
                latitude = json.optDouble("latitude"),
                longitude = json.optDouble("longitude"),
                capacityKwh = json.optDouble("capacityKwh"),
                batterySlotCount = json.optInt("batterySlotCount"),
                openingTime = json.optString("openingTime"),
                closingTime = json.optString("closingTime"),
                status = json.optString("status")
            )
        }

        // reads a list of stations from a json array string
        fun listFromJson(text: String): List<Station> {
            val array = JSONArray(text)
            val stations = mutableListOf<Station>()
            for (i in 0 until array.length()) {
                stations.add(fromJson(array.getJSONObject(i)))
            }
            return stations
        }
    }
}
