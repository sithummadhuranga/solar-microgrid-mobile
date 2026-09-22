package com.solarmicrogrid.app.model

import org.json.JSONArray
import org.json.JSONObject

// holds one power trading reservation, matches the Energy Reservation collection
class EnergyReservation(
    val id: String,
    val nic: String,
    val stationId: String,
    val slotId: String,
    val scheduledTime: String,
    val state: String,
    val qrData: String?
) {
    companion object {
        // builds a reservation from the json the api returns
        fun fromJson(json: JSONObject): EnergyReservation {
            return EnergyReservation(
                id = json.optString("id"),
                nic = json.optString("nic"),
                stationId = json.optString("stationId"),
                slotId = json.optString("slotId"),
                scheduledTime = json.optString("scheduledTime"),
                state = json.optString("state"),
                qrData = if (json.has("qrData")) json.optString("qrData") else null
            )
        }

        // builds a list from the json array the list endpoints return
        fun listFromJson(text: String): List<EnergyReservation> {
            val array = JSONArray(text)
            val reservations = mutableListOf<EnergyReservation>()

            for (i in 0 until array.length()) {
                reservations.add(fromJson(array.getJSONObject(i)))
            }

            return reservations
        }
    }
}
