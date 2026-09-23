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
                id = text(json, "id"),
                name = text(json, "name"),
                address = text(json, "address"),
                latitude = json.optDouble("latitude"),
                longitude = json.optDouble("longitude"),
                capacityKwh = json.optDouble("capacityKwh", 0.0),
                batterySlotCount = json.optInt("batterySlotCount"),
                openingTime = text(json, "openingTime"),
                closingTime = text(json, "closingTime"),
                status = text(json, "status")
            )
        }

        // reads a text field, gives an empty string for a json null instead of the word "null"
        private fun text(json: JSONObject, name: String): String {
            return if (json.isNull(name)) "" else json.optString(name)
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
