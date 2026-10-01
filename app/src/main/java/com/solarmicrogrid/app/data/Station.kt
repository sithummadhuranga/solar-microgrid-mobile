package com.solarmicrogrid.app.data

import org.json.JSONArray
import org.json.JSONObject

// one microgrid node, kept on the phone so the lists can show a name instead of an id
class Station(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val capacityKwh: Double,
    val batterySlotCount: Int
) {
    companion object {
        // builds the list from the json array the stations endpoint returns
        fun listFromJson(text: String): List<Station> {
            val array = JSONArray(text)
            val stations = mutableListOf<Station>()

            for (i in 0 until array.length()) {
                stations.add(fromJson(array.getJSONObject(i)))
            }

            return stations
        }

        // builds one node from the json the api returns
        fun fromJson(json: JSONObject): Station {
            return Station(
                json.optString("id"),
                json.optString("name"),
                json.optDouble("latitude"),
                json.optDouble("longitude"),
                json.optDouble("capacityKwh"),
                json.optInt("batterySlotCount")
            )
        }
    }
}
