package com.solarmicrogrid.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

// shows the transaction qr code for an approved reservation, M-5, BR-7
class ReservationQrActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_DATA = "qr_data"
    }

    // draws the qr code from the data passed in, or shows why it cannot yet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_qr)

        val qrImage = findViewById<ImageView>(R.id.qrImage)
        val qrErrorText = findViewById<TextView>(R.id.qrErrorText)
        val qrData = intent.getStringExtra(EXTRA_QR_DATA)

        if (qrData.isNullOrEmpty()) {
            qrErrorText.text = "No QR code yet, the reservation is not approved"
            return
        }

        val bitmap = BarcodeEncoder().encodeBitmap(qrData, BarcodeFormat.QR_CODE, 600, 600)
        qrImage.setImageBitmap(bitmap)
    }
}
