package com.solarmicrogrid.app.api

import com.solarmicrogrid.app.model.EnergyReservation
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// thrown for a signed in call the api no longer accepts, the account may have been deactivated
// or the token expired, the screen should clear the session and go back to the login screen
class SessionExpiredException(message: String) : Exception(message)

// calls the web api, the one class every android screen uses for network calls
// base url points at the api on the host machine from the emulator
class ApiClient(
    private val baseUrl: String = "http://10.0.2.2:5080/api",
    private val authToken: String? = null
) {

    // sends a get request, returns the raw response text
    fun get(path: String): String = call(path, "GET", null)

    // sends a post request, returns the raw response text
    fun post(path: String, body: JSONObject?): String = call(path, "POST", body)

    // sends a put request, returns the raw response text
    fun put(path: String, body: JSONObject?): String = call(path, "PUT", body)

    // logs in with the identifier and password, platform is web or mobile, the api checks nic then email
    fun login(identifier: String, password: String, platform: String): JSONObject {
        val body = JSONObject()
        body.put("identifier", identifier)
        body.put("password", password)
        body.put("platform", platform)
        return toJson(post("/auth/login", body))
    }

    // registers a new prosumer, the account stays pending until backoffice activates it
    fun register(nic: String, password: String, fullName: String, phone: String, address: String): JSONObject {
        val body = JSONObject()
        body.put("nic", nic)
        body.put("password", password)
        body.put("fullName", fullName)
        body.put("phone", phone)
        body.put("address", address)
        return toJson(post("/prosumers/register", body))
    }

    // reads the logged in prosumer's own profile
    fun myProfile(): JSONObject = toJson(get("/prosumers/me"))

    // saves changes to the logged in prosumer's own profile, the nic cannot change here
    fun updateMyProfile(fullName: String, phone: String, address: String): JSONObject {
        val body = JSONObject()
        body.put("fullName", fullName)
        body.put("phone", phone)
        body.put("address", address)
        return toJson(put("/prosumers/me", body))
    }

    // asks for the logged in prosumer's account to be deactivated
    fun requestMyDeactivation() {
        post("/prosumers/me/deactivate-request", null)
    }

    // changes the logged in prosumer's own password, the api checks the current password first
    fun changeMyPassword(currentPassword: String, newPassword: String) {
        val body = JSONObject()
        body.put("currentPassword", currentPassword)
        body.put("newPassword", newPassword)
        post("/prosumers/me/change-password", body)
    }

    // creates a reservation for the logged in prosumer, the nic comes from the token on the api side
    fun create(stationId: String, slotId: String, scheduledTime: String): EnergyReservation {
        val body = JSONObject()
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

    // pulls the plain message field out of an api error body, falls back to the raw text
    private fun errorMessage(text: String): String {
        return try {
            JSONObject(text).getString("message")
        } catch (e: Exception) {
            text
        }
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
                val message = errorMessage(text)
                // only a signed in call can mean the session is no longer valid, an anonymous call
                // like login just failed with the wrong password or platform
                if ((status == 401 || status == 403) && authToken != null) {
                    throw SessionExpiredException(message)
                }
                throw Exception(message)
            }

            return text
        } finally {
            connection.disconnect()
        }
    }
}
