package com.solarmicrogrid.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.model.EnergyReservation
import org.json.JSONObject

// checks a scanned qr code against the server and finishes the energy transfer
class VerifyReservationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_DATA = "qr_data"
    }

    private var reservationId = ""
    private lateinit var detailsText: TextView
    private lateinit var messageText: TextView
    private lateinit var completeButton: Button

    // sets up the screen and sends the scanned code straight to the api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_reservation)

        detailsText = findViewById(R.id.detailsText)
        messageText = findViewById(R.id.messageText)
        completeButton = findViewById(R.id.completeButton)

        // stays off until the server says the code is good
        completeButton.isEnabled = false
        completeButton.setOnClickListener { completeTransfer() }

        verify(intent.getStringExtra(EXTRA_QR_DATA) ?: "")
    }

    // asks the api whether the scanned code belongs to a real reservation
    private fun verify(qrData: String) {
        val session = AppDatabase(this).session()
        if (session == null) {
            showMessage(getString(R.string.message_not_logged_in))
            return
        }

        val api = ApiClient(authToken = session.token)
        val body = JSONObject()
        body.put("qrData", qrData)

        Thread {
            try {
                val text = api.post("/reservations/verify", body)
                val reservation = EnergyReservation.fromJson(JSONObject(text))
                runOnUiThread {
                    reservationId = reservation.id
                    showReservation(reservation)
                    completeButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showMessage(e.message ?: getString(R.string.error_qr_not_valid))
                }
            }
        }.start()
    }

    // tells the api the transfer is finished so the job is marked done
    private fun completeTransfer() {
        val session = AppDatabase(this).session() ?: return
        val api = ApiClient(authToken = session.token)
        completeButton.isEnabled = false

        Thread {
            try {
                api.post("/reservations/$reservationId/complete", null)
                runOnUiThread { showMessage(getString(R.string.message_transfer_done)) }
            } catch (e: Exception) {
                runOnUiThread {
                    completeButton.isEnabled = true
                    showMessage(e.message ?: getString(R.string.error_could_not_complete))
                }
            }
        }.start()
    }

    // puts the verified reservation on the screen
    private fun showReservation(reservation: EnergyReservation) {
        detailsText.text = getString(R.string.label_station_prefix) + reservation.stationId + "\n" +
            getString(R.string.label_slot_prefix) + reservation.slotId + "\n" +
            getString(R.string.label_scheduled_prefix) + reservation.scheduledTime + "\n" +
            getString(R.string.label_state_prefix) + reservation.state
        messageText.visibility = View.GONE
    }

    // shows the line under the title, used for errors and for the done message
    private fun showMessage(text: String) {
        messageText.text = text
        messageText.visibility = View.VISIBLE
    }
}
