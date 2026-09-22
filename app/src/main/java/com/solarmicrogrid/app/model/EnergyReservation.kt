package com.solarmicrogrid.app.model

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
    }
}
