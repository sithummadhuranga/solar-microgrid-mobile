package com.solarmicrogrid.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// shows the result after a reservation is created, modified or cancelled, M-4
class ReservationSummaryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_RESERVATION_ID = "reservation_id"
        const val EXTRA_STATE = "state"
    }

    // sets up the summary screen from the intent extras
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_summary)

        findViewById<TextView>(R.id.messageText).text = intent.getStringExtra(EXTRA_MESSAGE)
        findViewById<TextView>(R.id.reservationIdText).text =
            "Reservation id: " + intent.getStringExtra(EXTRA_RESERVATION_ID)
        findViewById<TextView>(R.id.stateText).text =
            "State: " + intent.getStringExtra(EXTRA_STATE)
    }
}
