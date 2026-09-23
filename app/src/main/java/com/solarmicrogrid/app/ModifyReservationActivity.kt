package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase

// lets a prosumer change the scheduled time of a reservation, the api checks the 12 hour notice rule
class ModifyReservationActivity : AppCompatActivity() {

    // sets up the modify reservation screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_reservation)

        val reservationIdInput = findViewById<EditText>(R.id.reservationIdInput)
        val scheduledTimeInput = findViewById<EditText>(R.id.scheduledTimeInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val session = AppDatabase(this).session()
        if (session == null) {
            errorText.text = getString(R.string.message_not_logged_in)
            errorText.visibility = TextView.VISIBLE
            saveButton.isEnabled = false
            return
        }

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
            saveChanges(session.token, reservationId, scheduledTime, errorText)
        }
    }

    // sends the update reservation request off the main thread
    private fun saveChanges(token: String, reservationId: String, scheduledTime: String, errorText: TextView) {
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
                runOnUiThread {
                    errorText.text = e.message
                    errorText.visibility = TextView.VISIBLE
                }
            }
        }.start()
    }
}
