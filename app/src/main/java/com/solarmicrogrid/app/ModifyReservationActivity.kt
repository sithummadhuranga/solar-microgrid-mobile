package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.api.SessionExpiredException
import com.solarmicrogrid.app.data.AppDatabase
import java.util.Calendar

// lets a prosumer change the scheduled time of one of their reservations, the api checks the 12 hour notice rule
class ModifyReservationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESERVATION_ID = "reservation_id"
        const val EXTRA_STATION_NAME = "station_name"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
    }

    private var reservationId = ""
    private var token = ""
    private var pickedTime: Calendar? = null
    private lateinit var errorText: TextView
    private lateinit var saveButton: Button

    // sets up the modify reservation screen for the reservation picked on the bookings screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_reservation)

        reservationId = intent.getStringExtra(EXTRA_RESERVATION_ID) ?: ""
        val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: ""
        val currentTime = TimeHelper.display(intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: "")
        findViewById<TextView>(R.id.reservationInfo).text = stationName + "\n" + currentTime

        val scheduledTimeInput = findViewById<EditText>(R.id.scheduledTimeInput)

        // opens the date and time pickers instead of typing the time
        scheduledTimeInput.setOnClickListener {
            TimeHelper.pick(this) { picked ->
                pickedTime = picked
                scheduledTimeInput.setText(TimeHelper.display(picked))
            }
        }
        errorText = findViewById(R.id.errorText)
        saveButton = findViewById(R.id.saveButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            showError(getString(R.string.message_not_logged_in))
            saveButton.isEnabled = false
            return
        }
        token = session.token

        // checks the time is filled, then sends the update request
        saveButton.setOnClickListener {
            val picked = pickedTime

            if (picked == null) {
                showError(getString(R.string.error_fill_required))
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            saveChanges(reservationId, TimeHelper.toUtc(picked))
        }
    }

    // sends the update reservation request off the main thread
    private fun saveChanges(reservationId: String, scheduledTime: String) {
        Thread {
            try {
                val reservation = ApiClient(authToken = token).update(reservationId, scheduledTime)
                runOnUiThread {
                    val intent = Intent(this, ReservationSummaryActivity::class.java)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_MESSAGE, getString(R.string.message_reservation_updated))
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

    // shows the line above the save button
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
}
