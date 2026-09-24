package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.api.SessionExpiredException
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.model.EnergyReservation

// shows the transaction qr code for an approved reservation, M-5, BR-7
class ReservationQrActivity : AppCompatActivity() {

    private var reservations = listOf<EnergyReservation>()
    private lateinit var reservationSpinner: Spinner
    private lateinit var qrImage: ImageView
    private lateinit var qrErrorText: TextView

    // sets up the screen and loads the approved reservations to pick from
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_qr)

        reservationSpinner = findViewById(R.id.reservationSpinner)
        qrImage = findViewById(R.id.qrImage)
        qrErrorText = findViewById(R.id.qrErrorText)

        val session = AppDatabase(this).session()
        if (session == null) {
            qrErrorText.text = getString(R.string.message_not_logged_in)
            return
        }

        reservationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // draws the qr code of the reservation that was picked
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                drawQr(reservations[position])
            }

            // nothing to do, the spinner always has a selection
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        loadReservations(session.token)
    }

    // asks the api for the caller's approved reservations, off the main thread
    private fun loadReservations(token: String) {
        Thread {
            try {
                val list = EnergyReservation.listFromJson(
                    ApiClient(authToken = token).get("/reservations/mine?state=approved")
                ).filter { !it.qrData.isNullOrEmpty() }
                val names = AppDatabase(this).stations().associate { it.id to it.name }
                runOnUiThread { showReservations(list, names) }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e) }
            }
        }.start()
    }

    // puts the reservations in the dropdown, or says there is no qr code yet
    private fun showReservations(list: List<EnergyReservation>, names: Map<String, String>) {
        reservations = list
        val labels = list.map {
            getString(R.string.reservation_option, names[it.stationId] ?: getString(R.string.label_node), shortTime(it.scheduledTime), it.state)
        }
        reservationSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        if (list.isEmpty()) {
            qrImage.setImageBitmap(null)
            qrErrorText.text = getString(R.string.error_no_qr_yet)
        }
    }

    // cuts a utc time from the api down to date and minutes
    private fun shortTime(utc: String): String {
        return utc.take(16).replace('T', ' ')
    }

    // draws the qr code from the reservation's qr data
    private fun drawQr(reservation: EnergyReservation) {
        qrErrorText.text = ""
        qrImage.setImageBitmap(BarcodeEncoder().encodeBitmap(reservation.qrData, BarcodeFormat.QR_CODE, 600, 600))
    }

    // a session expiry goes back to login instead of showing the message inline
    private fun handleApiError(e: Exception) {
        if (e is SessionExpiredException) {
            AppDatabase(this).clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        qrErrorText.text = e.message ?: getString(R.string.error_could_not_load)
    }
}
