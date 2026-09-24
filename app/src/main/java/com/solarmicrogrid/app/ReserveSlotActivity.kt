package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.api.SessionExpiredException
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.model.EnergyBookingSlot
import com.solarmicrogrid.app.model.Station

// lets a prosumer reserve an energy slot, the node and slot are picked from lists the api gives
class ReserveSlotActivity : AppCompatActivity() {

    companion object {
        private val TIME_FORMAT = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?Z$""")
    }

    private var stations = listOf<Station>()
    private var slots = listOf<EnergyBookingSlot>()
    private var token = ""
    private lateinit var stationSpinner: Spinner
    private lateinit var slotSpinner: Spinner
    private lateinit var errorText: TextView
    private lateinit var reserveButton: Button

    // sets up the reserve slot screen and loads the nodes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserve_slot)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_reserve
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> switchTo(ProsumerDashboardActivity::class.java)
                R.id.nav_bookings -> switchTo(ReservationListActivity::class.java)
                R.id.nav_profile -> switchTo(EditProfileActivity::class.java)
            }
            true
        }

        stationSpinner = findViewById(R.id.stationSpinner)
        slotSpinner = findViewById(R.id.slotSpinner)
        val scheduledTimeInput = findViewById<EditText>(R.id.scheduledTimeInput)
        errorText = findViewById(R.id.errorText)
        reserveButton = findViewById(R.id.reserveButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            errorText.text = getString(R.string.message_not_logged_in)
            errorText.visibility = TextView.VISIBLE
            reserveButton.isEnabled = false
            return
        }
        token = session.token

        stationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // loads the slots of the node that was picked
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadSlots(stations[position].id)
            }

            // nothing to do, the spinner always has a selection
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // checks a node, a slot and the time are filled, then sends the reservation request
        reserveButton.setOnClickListener {
            val station = stations.getOrNull(stationSpinner.selectedItemPosition)
            val slot = slots.getOrNull(slotSpinner.selectedItemPosition)
            val scheduledTime = scheduledTimeInput.text.toString().trim()

            if (station == null || slot == null || scheduledTime.isEmpty()) {
                showError(getString(R.string.error_fill_required))
                return@setOnClickListener
            }

            if (!TIME_FORMAT.matches(scheduledTime)) {
                showError(getString(R.string.error_time_format))
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            reserveSlot(station.id, slot.id, scheduledTime)
        }

        findViewById<Button>(R.id.modifyReservationButton).setOnClickListener {
            startActivity(Intent(this, ModifyReservationActivity::class.java))
        }
        findViewById<Button>(R.id.cancelReservationButton).setOnClickListener {
            startActivity(Intent(this, CancelReservationActivity::class.java))
        }
        findViewById<Button>(R.id.viewQrButton).setOnClickListener {
            startActivity(Intent(this, ReservationQrActivity::class.java))
        }

        loadStations()
    }

    // asks the api for the active nodes, off the main thread
    private fun loadStations() {
        Thread {
            try {
                val list = Station.listFromJson(ApiClient(authToken = token).get("/stations"))
                runOnUiThread { showStations(list) }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e) }
            }
        }.start()
    }

    // puts the node names in the first dropdown, picking one loads its slots
    private fun showStations(list: List<Station>) {
        stations = list
        stationSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list.map { it.name })

        if (list.isEmpty()) {
            showSlots(listOf())
            showError(getString(R.string.message_no_nodes))
        }
    }

    // asks the api for the upcoming slots of one node, off the main thread
    private fun loadSlots(stationId: String) {
        Thread {
            try {
                val list = EnergyBookingSlot.listFromJson(ApiClient(authToken = token).get("/stations/$stationId/slots"))
                runOnUiThread {
                    // ignores an answer for a node that is no longer the picked one
                    if (stations.getOrNull(stationSpinner.selectedItemPosition)?.id == stationId) {
                        showSlots(list)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e) }
            }
        }.start()
    }

    // puts the slot times in the second dropdown, the reserve button needs at least one slot
    private fun showSlots(list: List<EnergyBookingSlot>) {
        slots = list
        val labels = list.map {
            getString(R.string.slot_option, shortTime(it.startTime), shortTime(it.endTime), it.availableSlots, it.totalSlots)
        }
        slotSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        reserveButton.isEnabled = list.isNotEmpty()

        if (list.isEmpty() && stations.isNotEmpty()) {
            showError(getString(R.string.no_upcoming_slots))
        } else if (list.isNotEmpty()) {
            errorText.visibility = TextView.GONE
        }
    }

    // cuts a utc time from the api down to date and minutes
    private fun shortTime(utc: String): String {
        return utc.take(16).replace('T', ' ')
    }

    // sends the create reservation request off the main thread
    private fun reserveSlot(stationId: String, slotId: String, scheduledTime: String) {
        Thread {
            try {
                val reservation = ApiClient(authToken = token).create(stationId, slotId, scheduledTime)
                runOnUiThread {
                    val intent = Intent(this, ReservationSummaryActivity::class.java)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_MESSAGE, getString(R.string.message_reservation_saved))
                    intent.putExtra(ReservationSummaryActivity.EXTRA_RESERVATION_ID, reservation.id)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_STATE, reservation.state)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e) }
            }
        }.start()
    }

    // shows the line above the reserve button
    private fun showError(text: String) {
        errorText.text = text
        errorText.visibility = TextView.VISIBLE
    }

    // a session expiry goes back to login instead of showing the message inline
    private fun handleApiError(e: Exception) {
        if (e is SessionExpiredException) {
            AppDatabase(this).clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        showError(e.message ?: getString(R.string.error_could_not_load))
    }

    // moves to another tab, closes this one so the tabs do not stack up
    private fun switchTo(screen: Class<*>) {
        startActivity(Intent(this, screen))
        overridePendingTransition(0, 0)
        finish()
    }
}
