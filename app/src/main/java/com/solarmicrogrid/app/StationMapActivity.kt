package com.solarmicrogrid.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.solarmicrogrid.app.model.Station
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// map of microgrid nodes plotted from their stored latitude and longitude (M-8)
class StationMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val apiUrl = "http://10.0.2.2:5080/api"
    private lateinit var map: GoogleMap

    // loads the layout and asks for the map
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_map)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // keeps the map once it is ready and loads the nodes
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        loadStations()
    }

    // gets the nodes from the api on a background thread
    private fun loadStations() {
        Thread {
            try {
                val json = getJson("/stations")
                val stations = Station.listFromJson(json)
                runOnUiThread { showStations(stations) }
            } catch (e: IOException) {
                runOnUiThread { showError(getString(R.string.server_unreachable)) }
            } catch (e: Exception) {
                runOnUiThread { showError(e.message ?: getString(R.string.server_unreachable)) }
            }
        }.start()
    }

    // adds one marker per node, titled with the node name
    private fun showStations(stations: List<Station>) {
        for (station in stations) {
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(station.latitude, station.longitude))
                    .title(station.name)
            )
        }
    }

    // shows a message from the api or the network
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // sends a GET with the bearer token and returns the body, throws the api message on failure
    // temporary, switches to ApiClient.get once member 4's branch is merged
    private fun getJson(path: String): String {
        val connection = URL(apiUrl + path).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer " + BuildConfig.API_TOKEN)
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            val code = connection.responseCode
            val isOk = code in 200..299
            val stream = if (isOk) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (!isOk) {
                throw Exception(text.ifEmpty { getString(R.string.request_failed, code) })
            }
            return text
        } finally {
            connection.disconnect()
        }
    }
}
