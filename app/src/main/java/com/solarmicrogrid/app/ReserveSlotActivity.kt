package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase

// lets a prosumer reserve an energy slot
class ReserveSlotActivity : AppCompatActivity() {

    // sets up the reserve slot screen
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

        val stationIdInput = findViewById<EditText>(R.id.stationIdInput)
        val slotIdInput = findViewById<EditText>(R.id.slotIdInput)
        val scheduledTimeInput = findViewById<EditText>(R.id.scheduledTimeInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val reserveButton = findViewById<Button>(R.id.reserveButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            errorText.text = getString(R.string.message_not_logged_in)
            errorText.visibility = TextView.VISIBLE
            reserveButton.isEnabled = false
            return
        }

        // checks the fields are filled, then sends the reservation request
        reserveButton.setOnClickListener {
            val stationId = stationIdInput.text.toString().trim()
            val slotId = slotIdInput.text.toString().trim()
            val scheduledTime = scheduledTimeInput.text.toString().trim()

            if (stationId.isEmpty() || slotId.isEmpty() || scheduledTime.isEmpty()) {
                errorText.text = getString(R.string.error_fill_required)
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            reserveSlot(session.token, stationId, slotId, scheduledTime, errorText)
        }
    }

    // sends the create reservation request off the main thread
    private fun reserveSlot(token: String, stationId: String, slotId: String, scheduledTime: String, errorText: TextView) {
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
                runOnUiThread {
                    errorText.text = e.message
                    errorText.visibility = TextView.VISIBLE
                }
            }
        }.start()
    }

    // moves to another tab, closes this one so the tabs do not stack up
    private fun switchTo(screen: Class<*>) {
        startActivity(Intent(this, screen))
        overridePendingTransition(0, 0)
        finish()
    }
}
