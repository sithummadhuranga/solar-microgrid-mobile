package com.solarmicrogrid.app.api

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// calls the reservation endpoints on the web api
// base url is a placeholder until the api is hosted, see D-11 in PROJECT_SCOPE.md
class ReservationApi(private val baseUrl: String = "http://10.0.2.2:5000/api") {

    // creates a reservation, throws with the server message if it fails
    fun create(nic: String, stationId: String, slotId: String, scheduledTime: String): JSONObject {
        val body = JSONObject()
        body.put("nic", nic)
        body.put("stationId", stationId)
        body.put("slotId", slotId)
        body.put("scheduledTime", scheduledTime)
        return call("/reservations", "POST", body)
    }

    // updates a reservation, throws with the server message if it fails, including the 12 hour rule
    fun update(id: String, scheduledTime: String): JSONObject {
        val body = JSONObject()
        body.put("scheduledTime", scheduledTime)
        return call("/reservations/$id", "PUT", body)
    }

    // cancels a reservation, throws with the server message if it fails, including the 12 hour rule
    fun cancel(id: String): JSONObject {
        return call("/reservations/$id/cancel", "POST", null)
    }

    // sends the request and returns the parsed json, throws with the server text on a non ok status
    private fun call(path: String, method: String, body: JSONObject?): JSONObject {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Content-Type", "application/json")
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

        return if (text.isEmpty()) JSONObject() else JSONObject(text)
    }
}
