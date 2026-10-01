package com.solarmicrogrid.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.api.SessionExpiredException
import com.solarmicrogrid.app.data.AppDatabase

// lets a prosumer cancel one of their reservations, the api checks the 12 hour notice rule, a grid operator can also cancel one
class CancelReservationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESERVATION_ID = "reservation_id"
        const val EXTRA_STATION_NAME = "station_name"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
    }

    private var reservationId = ""
    private var token = ""
    private lateinit var errorText: TextView
    private lateinit var cancelButton: Button

    // sets up the cancel reservation screen for the reservation picked on the bookings screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_reservation)

        reservationId = intent.getStringExtra(EXTRA_RESERVATION_ID) ?: ""
        val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: ""
        val currentTime = TimeHelper.display(intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: "")
        findViewById<TextView>(R.id.reservationInfo).text = stationName + "\n" + currentTime

        errorText = findViewById(R.id.errorText)
        cancelButton = findViewById(R.id.cancelButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            showError(getString(R.string.message_not_logged_in))
            cancelButton.isEnabled = false
            return
        }
        token = session.token

        // asks to confirm before cancelling
        cancelButton.setOnClickListener {
            errorText.visibility = TextView.GONE
            confirmCancel(reservationId)
        }
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
