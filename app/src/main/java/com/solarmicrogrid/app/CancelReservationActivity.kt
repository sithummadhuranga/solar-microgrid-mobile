package com.solarmicrogrid.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.api.SessionExpiredException
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.model.EnergyReservation

// lets a prosumer cancel one of their reservations, the api checks the 12 hour notice rule, a grid operator can also cancel one
class CancelReservationActivity : AppCompatActivity() {

    private var reservations = listOf<EnergyReservation>()
    private var token = ""
    private lateinit var reservationSpinner: Spinner
    private lateinit var errorText: TextView
    private lateinit var cancelButton: Button

    // sets up the cancel reservation screen and loads the reservations to pick from
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_reservation)

        reservationSpinner = findViewById(R.id.reservationSpinner)
        errorText = findViewById(R.id.errorText)
        cancelButton = findViewById(R.id.cancelButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            showError(getString(R.string.message_not_logged_in))
            cancelButton.isEnabled = false
            return
        }
        token = session.token

        // checks a reservation is picked, then asks to confirm before cancelling
        cancelButton.setOnClickListener {
            val reservation = reservations.getOrNull(reservationSpinner.selectedItemPosition)

            if (reservation == null) {
                showError(getString(R.string.error_fill_required))
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            confirmCancel(reservation.id)
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

    // puts the reservations in the dropdown, the cancel button needs at least one
    private fun showReservations(list: List<EnergyReservation>, names: Map<String, String>) {
        reservations = list
        val labels = list.map {
            getString(R.string.reservation_option, names[it.stationId] ?: getString(R.string.label_node), shortTime(it.scheduledTime), it.state)
        }
        reservationSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        cancelButton.isEnabled = list.isNotEmpty()

        if (list.isEmpty()) {
            showError(getString(R.string.message_no_reservations))
        }
    }

    // cuts a utc time from the api down to date and minutes
    private fun shortTime(utc: String): String {
        return utc.take(16).replace('T', ' ')
    }

    // shows a confirm dialog before sending the cancel request
    private fun confirmCancel(reservationId: String) {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_cancel_reservation)
            .setPositiveButton(R.string.button_cancel) { _, _ -> cancelReservation(reservationId) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // sends the cancel reservation request off the main thread
    private fun cancelReservation(reservationId: String) {
        Thread {
            try {
                val reservation = ApiClient(authToken = token).cancel(reservationId)
                runOnUiThread {
                    val intent = Intent(this, ReservationSummaryActivity::class.java)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_MESSAGE, getString(R.string.message_reservation_cancelled))
                    intent.putExtra(ReservationSummaryActivity.EXTRA_RESERVATION_ID, reservationId)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_STATE, reservation.state)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e) }
            }
        }.start()
    }

    // shows the line above the cancel button
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
