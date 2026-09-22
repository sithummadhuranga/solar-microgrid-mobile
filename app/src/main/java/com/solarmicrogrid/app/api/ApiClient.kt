package com.solarmicrogrid.app.api

import com.solarmicrogrid.app.model.EnergyReservation
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// calls the web api, the one class every android screen uses for network calls
// base url points at the api on the host machine from the emulator, see D-11 in PROJECT_SCOPE.md
// authToken is null until login (M-2) stores a session to read it from
class ApiClient(
    private val baseUrl: String = "http://10.0.2.2:5080/api",
    private val authToken: String? = null
) {

    // sends a get request, returns the raw response text
    fun get(path: String): String = call(path, "GET", null)

    // sends a post request, returns the raw response text
    fun post(path: String, body: JSONObject?): String = call(path, "POST", body)

    // creates a reservation, throws with the server message if it fails
    fun create(nic: String, stationId: String, slotId: String, scheduledTime: String): EnergyReservation {
        val body = JSONObject()
        body.put("nic", nic)
        body.put("stationId", stationId)
        body.put("slotId", slotId)
        body.put("scheduledTime", scheduledTime)
        return EnergyReservation.fromJson(toJson(post("/reservations", body)))
    }

    // updates a reservation, throws with the server message if it fails, including the 12 hour rule
    fun update(id: String, scheduledTime: String): EnergyReservation {
        val body = JSONObject()
        body.put("scheduledTime", scheduledTime)
        return EnergyReservation.fromJson(toJson(call("/reservations/$id", "PUT", body)))
    }

    // cancels a reservation, throws with the server message if it fails, including the 12 hour rule
    fun cancel(id: String): EnergyReservation {
        return EnergyReservation.fromJson(toJson(post("/reservations/$id/cancel", null)))
    }

    // turns response text into a json object, empty text means an empty object
    private fun toJson(text: String): JSONObject {
        return if (text.isEmpty()) JSONObject() else JSONObject(text)
    }

    // sends the request and returns the raw response text, throws with the server text on a non ok status
    private fun call(path: String, method: String, body: JSONObject?): String {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Content-Type", "application/json")
            if (authToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $authToken")
            }
            connection.doInput = true

            if (body != null) {
                connection.doOutput = true
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(body.toString())
                writer.flush()
                writer.close()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = BufferedReader(InputStreamReader(stream)).readText()

            if (status !in 200..299) {
                throw Exception(text)
            }

            return text
        } finally {
            connection.disconnect()
        }
    }
}
