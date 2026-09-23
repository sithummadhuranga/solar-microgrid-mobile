package com.solarmicrogrid.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase

// lets a prosumer cancel a reservation, the api checks the 12 hour notice rule, a grid operator can also cancel one
class CancelReservationActivity : AppCompatActivity() {

    // sets up the cancel reservation screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_reservation)

        val reservationIdInput = findViewById<EditText>(R.id.reservationIdInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            errorText.text = getString(R.string.message_not_logged_in)
            errorText.visibility = TextView.VISIBLE
            cancelButton.isEnabled = false
            return
        }

        // checks the reservation id is filled, then asks to confirm before cancelling
        cancelButton.setOnClickListener {
            val reservationId = reservationIdInput.text.toString().trim()

            if (reservationId.isEmpty()) {
                errorText.text = getString(R.string.error_fill_required)
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            confirmCancel(session.token, reservationId, errorText)
        }
    }

    // shows a confirm dialog before sending the cancel request
    private fun confirmCancel(token: String, reservationId: String, errorText: TextView) {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_cancel_reservation)
            .setPositiveButton(R.string.button_cancel) { _, _ -> cancelReservation(token, reservationId, errorText) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // sends the cancel reservation request off the main thread
    private fun cancelReservation(token: String, reservationId: String, errorText: TextView) {
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
                runOnUiThread {
                    errorText.text = e.message
                    errorText.visibility = TextView.VISIBLE
                }
            }
        }.start()
    }
}
