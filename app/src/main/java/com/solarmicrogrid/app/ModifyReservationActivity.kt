package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.api.SessionExpiredException
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.model.EnergyReservation

// lets a prosumer change the scheduled time of one of their reservations, the api checks the 12 hour notice rule
class ModifyReservationActivity : AppCompatActivity() {

    companion object {
        private val TIME_FORMAT = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?Z$""")
    }

    private var reservations = listOf<EnergyReservation>()
    private var token = ""
    private lateinit var reservationSpinner: Spinner
    private lateinit var errorText: TextView
    private lateinit var saveButton: Button

    // sets up the modify reservation screen and loads the reservations to pick from
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_reservation)

        reservationSpinner = findViewById(R.id.reservationSpinner)
        val scheduledTimeInput = findViewById<EditText>(R.id.scheduledTimeInput)
        errorText = findViewById(R.id.errorText)
        saveButton = findViewById(R.id.saveButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            showError(getString(R.string.message_not_logged_in))
            saveButton.isEnabled = false
            return
        }
        token = session.token

        // checks a reservation and the time are filled, then sends the update request
        saveButton.setOnClickListener {
            val reservation = reservations.getOrNull(reservationSpinner.selectedItemPosition)
            val scheduledTime = scheduledTimeInput.text.toString().trim()

            if (reservation == null || scheduledTime.isEmpty()) {
                showError(getString(R.string.error_fill_required))
                return@setOnClickListener
            }

            if (!TIME_FORMAT.matches(scheduledTime)) {
                showError(getString(R.string.error_time_format))
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            saveChanges(reservation.id, scheduledTime)
        }

        loadReservations()
    }

    // asks the api for the caller's open reservations, off the main thread
    private fun loadReservations() {
        Thread {
            try {
                val list = EnergyReservation.listFromJson(
                    ApiClient(authToken = token).get("/reservations/mine?state=pending,approved")
                )
                val names = AppDatabase(this).stations().associate { it.id to it.name }
                runOnUiThread { showReservations(list, names) }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e) }
            }
        }.start()
    }

    // puts the reservations in the dropdown, the save button needs at least one
    private fun showReservations(list: List<EnergyReservation>, names: Map<String, String>) {
        reservations = list
        val labels = list.map {
            getString(R.string.reservation_option, names[it.stationId] ?: getString(R.string.label_node), shortTime(it.scheduledTime), it.state)
        }
        reservationSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        saveButton.isEnabled = list.isNotEmpty()

        if (list.isEmpty()) {
            showError(getString(R.string.message_no_reservations))
        }
    }

    // cuts a utc time from the api down to date and minutes
    private fun shortTime(utc: String): String {
        return utc.take(16).replace('T', ' ')
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
