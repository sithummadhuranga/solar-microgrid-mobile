package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient

// lets a prosumer reserve an energy slot, M-4
// the nic field is temporary until login supplies it from the session
class ReserveSlotActivity : AppCompatActivity() {

    private val api = ApiClient()

    // sets up the reserve slot screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserve_slot)

        val nicInput = findViewById<EditText>(R.id.nicInput)
        val stationIdInput = findViewById<EditText>(R.id.stationIdInput)
        val slotIdInput = findViewById<EditText>(R.id.slotIdInput)
        val scheduledTimeInput = findViewById<EditText>(R.id.scheduledTimeInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val reserveButton = findViewById<Button>(R.id.reserveButton)

        // checks the fields are filled, then sends the reservation request
        reserveButton.setOnClickListener {
            val nic = nicInput.text.toString().trim()
            val stationId = stationIdInput.text.toString().trim()
            val slotId = slotIdInput.text.toString().trim()
            val scheduledTime = scheduledTimeInput.text.toString().trim()

            if (nic.isEmpty() || stationId.isEmpty() || slotId.isEmpty() || scheduledTime.isEmpty()) {
                errorText.text = getString(R.string.error_fill_required)
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            reserveSlot(nic, stationId, slotId, scheduledTime, errorText)
        }
    }

    // sends the create reservation request off the main thread
    private fun reserveSlot(nic: String, stationId: String, slotId: String, scheduledTime: String, errorText: TextView) {
        Thread {
            try {
                val reservation = api.create(nic, stationId, slotId, scheduledTime)
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
}
