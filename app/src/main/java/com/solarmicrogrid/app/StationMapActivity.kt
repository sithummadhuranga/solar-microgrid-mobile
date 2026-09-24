package com.solarmicrogrid.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.model.EnergyBookingSlot
import com.solarmicrogrid.app.model.Station
import java.io.IOException

// map of microgrid nodes plotted from their stored latitude and longitude
class StationMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val slotAdapter = SlotAdapter()
    private var selectedStationId = ""
    private val colombo = LatLng(6.9271, 79.8612)
    private var hasAskedLocation = false
    private var stations: List<Station> = emptyList()
    private var userLocation: LatLng? = null
    private val nearbyCount = 3
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshMillis = 10_000L
    private var lastStationsJson = ""
    private var hasFramedCamera = false

    // after a screen rotation the answer can arrive before the map is ready, onMapReady then centres it
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (::map.isInitialized) {
            if (result.containsValue(true)) showMyLocation() else showColombo()
        }
    }

    // loads the layout, sets up the slot list and the camera buttons, and asks for the map
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_map)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        hasAskedLocation = savedInstanceState?.getBoolean("hasAskedLocation") ?: false

        setUpBottomNav()

        val slotList = findViewById<RecyclerView>(R.id.slot_list)
        slotList.layoutManager = LinearLayoutManager(this)
        slotList.adapter = slotAdapter

        findViewById<Button>(R.id.nearby_button).setOnClickListener { showNearby() }
        findViewById<Button>(R.id.all_nodes_button).setOnClickListener { showAllNodes() }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // the map is one of the operator's two tabs, but not one of the prosumer's, so a prosumer gets a back arrow instead
    private fun setUpBottomNav() {
        val role = AppDatabase(this).session()?.role
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (role == "GridOperator") {
            bottomNav.inflateMenu(R.menu.bottom_nav_operator)
            bottomNav.selectedItemId = R.id.nav_map
            bottomNav.setOnItemSelectedListener { item ->
                if (item.itemId == R.id.nav_scan) switchTo(OperatorHomeActivity::class.java)
                true
            }
        } else {
            bottomNav.visibility = View.GONE
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    // closes this screen when the back arrow in the toolbar is tapped, prosumer only, see setUpBottomNav
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // moves to another tab, closes this one so the tabs do not stack up
    private fun switchTo(screen: Class<*>) {
        startActivity(Intent(this, screen))
        overridePendingTransition(0, 0)
        finish()
    }

    // checks the api for node changes every 10 seconds while the map is on screen, and once straight away on return
    override fun onResume() {
        super.onResume()
        refreshNow()
        scheduleRefresh()
    }

    // stops the checks when the map is no longer on screen
    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacksAndMessages(null)
    }

    // runs refreshNow after the refresh interval, then queues the next one
    private fun scheduleRefresh() {
        refreshHandler.postDelayed({
            refreshNow()
            scheduleRefresh()
        }, refreshMillis)
    }

    // reloads the nodes and the slots of the selected node, without moving the camera or showing errors
    private fun refreshNow() {
        if (!::map.isInitialized) return
        loadStations(quiet = true)
        val selected = stations.find { it.id == selectedStationId }
        if (selected != null) loadSlots(selected, quiet = true)
    }

    // keeps whether the location question was asked, so a rotation does not ask again
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasAskedLocation", hasAskedLocation)
    }

    // keeps the map once it is ready, shows the zoom buttons, zooms in on a tapped node and loads the nodes
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener { marker ->
            val station = marker.tag as? Station ?: return@setOnMarkerClickListener false
            showStationDetails(station)
            loadSlots(station)
            marker.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 14f))
            true
        }
        centreMap()
        loadStations()
    }

    // uses the location permission if granted, otherwise asks for it once
    private fun centreMap() {
        if (hasLocationPermission()) {
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

    // checks whether the fine or coarse location permission was granted
    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    // reads the newest last known location from every provider, null when there is none or no permission
    @SuppressLint("MissingPermission")
    private fun readLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (provider in locationManager.getProviders(true)) {
            val location = locationManager.getLastKnownLocation(provider) ?: continue
            if (best == null || location.time > best.time) best = location
        }
        return best?.let { LatLng(it.latitude, it.longitude) }
    }

    // turns on the my-location layer and moves to the last known location, or colombo if there is none
    @SuppressLint("MissingPermission")
    private fun showMyLocation() {
        map.isMyLocationEnabled = true
        val here = readLocation()
        if (here == null) {
            showColombo()
            return
        }
        userLocation = here
        if (stations.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 12f))
        } else {
            fitCamera()
        }
    }

    // shows every node when there is no phone location, or colombo at zoom 8 before the nodes load
    private fun showColombo() {
        if (stations.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(colombo, 8f))
        } else {
            fitCamera()
        }
    }

    // fits the camera around the phone and its nearest node, or around every node when the location is unknown
    private fun fitCamera() {
        userLocation = readLocation() ?: userLocation
        val here = userLocation
        if (here != null) fitPoints(listOf(here) + nearestTo(here, 1)) else showAllNodes()
    }

    // fits the camera around the phone and its closest few nodes, the nearby button, reads the location again first
    private fun showNearby() {
        if (stations.isEmpty()) return
        userLocation = readLocation() ?: userLocation
        val here = userLocation
        if (here == null) {
            showError(getString(R.string.location_unknown))
            return
        }
        fitPoints(listOf(here) + nearestTo(here, nearbyCount))
    }

    // fits the camera around every node, the all nodes button
    private fun showAllNodes() {
        if (stations.isEmpty()) return
        fitPoints(stations.map { LatLng(it.latitude, it.longitude) })
    }

    // moves the camera so every point is on screen, or zooms on the point when there is only one
    private fun fitPoints(points: List<LatLng>) {
        if (points.distinct().size == 1) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(points[0], 12f))
            return
        }
        val bounds = LatLngBounds.Builder()
        for (point in points) bounds.include(point)
        val padding = (64 * resources.displayMetrics.density).toInt()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding))
    }

    // finds the positions of the nodes closest to a point, used only to frame the camera
    private fun nearestTo(point: LatLng, count: Int): List<LatLng> {
        val result = FloatArray(1)
        return stations
            .sortedBy { station ->
                Location.distanceBetween(point.latitude, point.longitude, station.latitude, station.longitude, result)
                result[0]
            }
            .take(count)
            .map { LatLng(it.latitude, it.longitude) }
    }

    // gets the active nodes from the api on a background thread, quiet skips the error message for background checks
    private fun loadStations(quiet: Boolean = false) {
        Thread {
            try {
                val json = getJson("/stations?active=true")
                val stations = Station.listFromJson(json)
                runOnUiThread { showStations(json, stations) }
            } catch (e: IOException) {
                if (!quiet) runOnUiThread { showError(getString(R.string.server_unreachable)) }
            } catch (e: Exception) {
                if (!quiet) runOnUiThread { showError(e.message ?: getString(R.string.server_unreachable)) }
            }
        }.start()
    }

    // redraws one marker per node when the nodes changed, skips a node without a location, frames the camera only once
    private fun showStations(json: String, list: List<Station>) {
        if (json == lastStationsJson) return
        lastStationsJson = json
        stations = list.filter { !it.latitude.isNaN() && !it.longitude.isNaN() }

        map.clear()
        for (station in stations) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(station.latitude, station.longitude))
                    .title(station.name)
            )
            marker?.tag = station
            if (station.id == selectedStationId) marker?.showInfoWindow()
        }
        updateSelectedStation()

        if (!hasFramedCamera && stations.isNotEmpty()) {
            hasFramedCamera = true
            fitCamera()
        }
    }

    // keeps the panel in step with the latest nodes, closes it when the selected node was deactivated or deleted
    private fun updateSelectedStation() {
        if (selectedStationId.isEmpty()) return
        val station = stations.find { it.id == selectedStationId }
        if (station == null) {
            selectedStationId = ""
            findViewById<View>(R.id.details_panel).visibility = View.GONE
            showError(getString(R.string.station_removed))
        } else {
            fillDetails(station)
        }
    }

    // opens the panel under the map for the tapped node
    private fun showStationDetails(station: Station) {
        selectedStationId = station.id
        fillDetails(station)
        slotAdapter.setItems(emptyList())
        findViewById<View>(R.id.no_slots_text).visibility = View.GONE
        findViewById<View>(R.id.details_panel).visibility = View.VISIBLE
    }

    // writes the node details into the panel
    private fun fillDetails(station: Station) {
        val capacity = station.capacityKwh.toBigDecimal().stripTrailingZeros().toPlainString()

        findViewById<TextView>(R.id.station_name).text = station.name
        findViewById<TextView>(R.id.station_address).text = station.address
        findViewById<TextView>(R.id.station_capacity).text =
            getString(R.string.station_capacity, capacity)
        findViewById<TextView>(R.id.station_slot_count).text =
            getString(R.string.station_slot_count, station.batterySlotCount)
        findViewById<TextView>(R.id.station_hours).text =
            getString(R.string.station_hours, station.openingTime, station.closingTime)
    }

    // gets the upcoming slots of a node from the api on a background thread, quiet skips the error message
    private fun loadSlots(station: Station, quiet: Boolean = false) {
        Thread {
            try {
                val json = getJson("/stations/${station.id}/slots?upcoming=true")
                val slots = EnergyBookingSlot.listFromJson(json)
                runOnUiThread { showSlots(station.id, slots) }
            } catch (e: IOException) {
                if (!quiet) runOnUiThread { showError(getString(R.string.server_unreachable)) }
            } catch (e: Exception) {
                if (!quiet) runOnUiThread { showError(e.message ?: getString(R.string.server_unreachable)) }
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

    // sends a GET with the logged in user's token and returns the body, throws the api message on failure
    private fun getJson(path: String): String {
        val token = AppDatabase(this).session()?.token
        return ApiClient(authToken = token).get(path)
    }
}
