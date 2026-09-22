package com.solarmicrogrid.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ReservationApi

// lets a prosumer cancel a reservation, M-4, BR-5 checked by the api, BR-9
class CancelReservationActivity : AppCompatActivity() {

    private val api = ReservationApi()

    // sets up the cancel reservation screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_reservation)

        val reservationIdInput = findViewById<EditText>(R.id.reservationIdInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        // checks the reservation id is filled, then asks to confirm before cancelling
        cancelButton.setOnClickListener {
            val reservationId = reservationIdInput.text.toString().trim()

            if (reservationId.isEmpty()) {
                errorText.text = getString(R.string.error_fill_required)
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            confirmCancel(reservationId, errorText)
        }
    }

    // shows a confirm dialog before sending the cancel request
    private fun confirmCancel(reservationId: String, errorText: TextView) {
        AlertDialog.Builder(this)
            .setMessage(R.string.button_cancel)
            .setPositiveButton(R.string.button_cancel) { _, _ -> cancelReservation(reservationId, errorText) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // sends the cancel reservation request off the main thread
    private fun cancelReservation(reservationId: String, errorText: TextView) {
        Thread {
            try {
                val reservation = api.cancel(reservationId)
                runOnUiThread {
                    val intent = Intent(this, ReservationSummaryActivity::class.java)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_MESSAGE, "Reservation cancelled")
                    intent.putExtra(ReservationSummaryActivity.EXTRA_RESERVATION_ID, reservationId)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_STATE, reservation.optString("state"))
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
