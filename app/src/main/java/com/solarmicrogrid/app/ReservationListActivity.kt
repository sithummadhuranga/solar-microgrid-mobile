package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.model.EnergyReservation
import java.net.URLEncoder

// shows the reservations that have not happened yet, with search and a state filter
class ReservationListActivity : AppCompatActivity() {

    private val adapter = ReservationAdapter()
    private lateinit var searchInput: EditText
    private lateinit var stateFilter: Spinner
    private lateinit var messageText: TextView

    // sets up the screen, the list loads when the spinner reports its first selection
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_list)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_bookings
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> switchTo(ProsumerDashboardActivity::class.java)
                R.id.nav_reserve -> switchTo(ReserveSlotActivity::class.java)
                R.id.nav_profile -> switchTo(EditProfileActivity::class.java)
            }
            true
        }

        searchInput = findViewById(R.id.searchInput)
        stateFilter = findViewById(R.id.stateFilter)
        messageText = findViewById(R.id.messageText)

        val reservationList = findViewById<RecyclerView>(R.id.reservationList)
        reservationList.layoutManager = LinearLayoutManager(this)
        reservationList.adapter = adapter

        stateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // reloads when the state filter changes
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadReservations()
            }

            // nothing to do, the spinner always has a selection
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // reloads when the user presses search or done on the keyboard
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                loadReservations()
                true
            } else {
                false
            }
        }
    }

    // asks the api for the matching reservations, off the main thread
    private fun loadReservations() {
        val database = AppDatabase(this)
        val session = database.session()
        if (session == null) {
            showMessage(getString(R.string.message_not_logged_in))
            return
        }

        val api = ApiClient(authToken = session.token)
        val path = buildPath()

        Thread {
            try {
                val reservations = EnergyReservation.listFromJson(api.get(path))
                val names = database.stations().associate { it.id to it.name }
                runOnUiThread {
                    adapter.setStationNames(names)
                    adapter.setReservations(reservations)
                    if (reservations.isEmpty()) {
                        showMessage(getString(R.string.message_no_reservations))
                    } else {
                        hideMessage()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    adapter.setReservations(listOf())
                    showMessage(e.message ?: getString(R.string.error_could_not_load))
                }
            }
        }.start()
    }

    // builds the query, this screen only asks for reservations that are still open
    private fun buildPath(): String {
        val state = when (stateFilter.selectedItemPosition) {
            1 -> "pending"
            2 -> "approved"
            else -> "pending,approved"
        }
        val search = URLEncoder.encode(searchInput.text.toString().trim(), "UTF-8")
        return "/reservations/mine?state=$state&search=$search"
    }

    // shows the line above the list, used for errors and for an empty list
    private fun showMessage(text: String) {
        messageText.text = text
        messageText.visibility = View.VISIBLE
    }

    // hides the line above the list
    private fun hideMessage() {
        messageText.visibility = View.GONE
    }

    // moves to another tab, closes this one so the tabs do not stack up
    private fun switchTo(screen: Class<*>) {
        startActivity(Intent(this, screen))
        overridePendingTransition(0, 0)
        finish()
    }
}
