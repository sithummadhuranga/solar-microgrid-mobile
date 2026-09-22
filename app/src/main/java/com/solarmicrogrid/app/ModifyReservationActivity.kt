package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ReservationApi

// lets a prosumer change the scheduled time of a reservation, M-4, BR-5 checked by the api
class ModifyReservationActivity : AppCompatActivity() {

    private val api = ReservationApi()

    // sets up the modify reservation screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_reservation)

        val reservationIdInput = findViewById<EditText>(R.id.reservationIdInput)
        val scheduledTimeInput = findViewById<EditText>(R.id.scheduledTimeInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // checks the fields are filled, then sends the update request
        saveButton.setOnClickListener {
            val reservationId = reservationIdInput.text.toString().trim()
            val scheduledTime = scheduledTimeInput.text.toString().trim()

            if (reservationId.isEmpty() || scheduledTime.isEmpty()) {
                errorText.text = getString(R.string.error_fill_required)
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            saveChanges(reservationId, scheduledTime, errorText)
        }
    }

    // sends the update reservation request off the main thread
    private fun saveChanges(reservationId: String, scheduledTime: String, errorText: TextView) {
        Thread {
            try {
                val reservation = api.update(reservationId, scheduledTime)
                runOnUiThread {
                    val intent = Intent(this, ReservationSummaryActivity::class.java)
                    intent.putExtra(ReservationSummaryActivity.EXTRA_MESSAGE, "Reservation updated")
                    intent.putExtra(ReservationSummaryActivity.EXTRA_RESERVATION_ID, reservation.optString("id"))
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
