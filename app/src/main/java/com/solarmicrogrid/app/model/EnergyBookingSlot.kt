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
                id = json.optString("id"),
                stationId = json.optString("stationId"),
                startTime = json.optString("startTime"),
                endTime = json.optString("endTime"),
                totalSlots = json.optInt("totalSlots"),
                availableSlots = json.optInt("availableSlots")
            )
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
