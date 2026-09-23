package com.solarmicrogrid.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.solarmicrogrid.app.model.EnergyBookingSlot
import com.solarmicrogrid.app.model.Station
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// map of microgrid nodes plotted from their stored latitude and longitude (M-8)
class StationMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val apiUrl = "http://10.0.2.2:5080/api"
    private lateinit var map: GoogleMap
    private val slotAdapter = SlotAdapter()
    private var selectedStationId = ""
    private val colombo = LatLng(6.9271, 79.8612)
    private var hasAskedLocation = false

    // after a screen rotation the answer can arrive before the map is ready, onMapReady then centres it
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (::map.isInitialized) {
            if (result.containsValue(true)) showMyLocation() else showColombo()
        }
    }

    // loads the layout, sets up the slot list and asks for the map
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_map)
        hasAskedLocation = savedInstanceState?.getBoolean("hasAskedLocation") ?: false

        val slotList = findViewById<RecyclerView>(R.id.slot_list)
        slotList.layoutManager = LinearLayoutManager(this)
        slotList.adapter = slotAdapter

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // keeps whether the location question was asked, so a rotation does not ask again
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasAskedLocation", hasAskedLocation)
    }

    // keeps the map once it is ready, listens for marker taps and loads the nodes
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener { marker ->
            val station = marker.tag as? Station ?: return@setOnMarkerClickListener false
            showStationDetails(station)
            loadSlots(station)
            false
        }
        centreMap()
        loadStations()
    }

    // uses the location permission if granted, otherwise asks for it once
    private fun centreMap() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            showMyLocation()
        } else if (hasAskedLocation) {
            showColombo()
        } else {
            hasAskedLocation = true
            locationPermission.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // turns on the my-location layer and moves to the last known location, or colombo if there is none
    @SuppressLint("MissingPermission")
    private fun showMyLocation() {
        map.isMyLocationEnabled = true
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (provider in locationManager.getProviders(true)) {
            val location = locationManager.getLastKnownLocation(provider) ?: continue
            if (best == null || location.time > best.time) best = location
        }
        if (best == null) {
            showColombo()
            return
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(best.latitude, best.longitude), 12f))
    }

    // moves the camera to colombo at zoom 8
    private fun showColombo() {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(colombo, 8f))
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

    // adds one marker per node, titled with the node name, skips a node without a location
    private fun showStations(stations: List<Station>) {
        for (station in stations) {
            if (station.latitude.isNaN() || station.longitude.isNaN()) continue
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(station.latitude, station.longitude))
                    .title(station.name)
            )
            marker?.tag = station
        }
    }

    // fills the panel under the map with the selected node
    private fun showStationDetails(station: Station) {
        selectedStationId = station.id
        val capacity = station.capacityKwh.toBigDecimal().stripTrailingZeros().toPlainString()

        findViewById<TextView>(R.id.station_name).text = station.name
        findViewById<TextView>(R.id.station_address).text = station.address
        findViewById<TextView>(R.id.station_capacity).text =
            getString(R.string.station_capacity, capacity)
        findViewById<TextView>(R.id.station_slot_count).text =
            getString(R.string.station_slot_count, station.batterySlotCount)
        findViewById<TextView>(R.id.station_hours).text =
            getString(R.string.station_hours, station.openingTime, station.closingTime)

        slotAdapter.setItems(emptyList())
        findViewById<View>(R.id.no_slots_text).visibility = View.GONE
        findViewById<View>(R.id.details_panel).visibility = View.VISIBLE
    }

    // gets the upcoming slots of a node from the api on a background thread
    private fun loadSlots(station: Station) {
        Thread {
            try {
                val json = getJson("/stations/${station.id}/slots")
                val slots = EnergyBookingSlot.listFromJson(json)
                runOnUiThread { showSlots(station.id, slots) }
            } catch (e: IOException) {
                runOnUiThread { showError(getString(R.string.server_unreachable)) }
            } catch (e: Exception) {
                runOnUiThread { showError(e.message ?: getString(R.string.server_unreachable)) }
            }
        }.start()
    }

    // shows the slots, skipped if another node was tapped while they loaded
    private fun showSlots(stationId: String, slots: List<EnergyBookingSlot>) {
        if (stationId != selectedStationId) return
        slotAdapter.setItems(slots)
        findViewById<View>(R.id.no_slots_text).visibility =
            if (slots.isEmpty()) View.VISIBLE else View.GONE
    }

    // shows a message from the api or the network
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // reads "message" from an api error body, falls back to the raw text or the status code
    private fun errorMessage(text: String, code: Int): String {
        if (text.isEmpty()) return getString(R.string.request_failed, code)
        return try {
            JSONObject(text).optString("message").ifEmpty { text }
        } catch (e: JSONException) {
            text
        }
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
                throw Exception(errorMessage(text, code))
            }
            return text
        } finally {
            connection.disconnect()
        }
    }
}
