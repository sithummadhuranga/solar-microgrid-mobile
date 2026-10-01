package com.solarmicrogrid.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

// shows the transaction qr code of the approved reservation picked on the bookings screen, M-5, BR-7
class ReservationQrActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_DATA = "qr_data"
        const val EXTRA_STATION_NAME = "station_name"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
    }

    // sets up the screen and draws the qr code that came with the reservation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_qr)

        val qrData = intent.getStringExtra(EXTRA_QR_DATA)
        val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: ""
        val scheduledTime = TimeHelper.display(intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: "")
        findViewById<TextView>(R.id.reservationInfo).text = stationName + "\n" + scheduledTime

        if (qrData.isNullOrEmpty()) {
            findViewById<TextView>(R.id.qrErrorText).text = getString(R.string.error_no_qr_yet)
            return
        }

        findViewById<ImageView>(R.id.qrImage)
            .setImageBitmap(BarcodeEncoder().encodeBitmap(qrData, BarcodeFormat.QR_CODE, 600, 600))
    }
}
