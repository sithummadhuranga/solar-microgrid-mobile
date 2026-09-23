package com.solarmicrogrid.app.model

import org.json.JSONArray
import org.json.JSONObject

// one battery storage slot from GET /api/stations/{id}/slots, times are utc
class EnergyBookingSlot(
    val id: String,
    val stationId: String,
    val startTime: String,
    val endTime: String,
    val totalSlots: Int,
    val availableSlots: Int
) {
    companion object {

        // reads one slot from a json object
        fun fromJson(json: JSONObject): EnergyBookingSlot {
            return EnergyBookingSlot(
                id = text(json, "id"),
                stationId = text(json, "stationId"),
                startTime = text(json, "startTime"),
                endTime = text(json, "endTime"),
                totalSlots = json.optInt("totalSlots"),
                availableSlots = json.optInt("availableSlots")
            )
        }

        // reads a text field, gives an empty string for a json null instead of the word "null"
        private fun text(json: JSONObject, name: String): String {
            return if (json.isNull(name)) "" else json.optString(name)
        }

        // reads a list of slots from a json array string
        fun listFromJson(text: String): List<EnergyBookingSlot> {
            val array = JSONArray(text)
            val slots = mutableListOf<EnergyBookingSlot>()
            for (i in 0 until array.length()) {
                slots.add(fromJson(array.getJSONObject(i)))
            }
            return slots
        }
    }
}
