package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase
import org.json.JSONObject

// the prosumer home screen, shows the reservation counts and opens the other screens
class ProsumerDashboardActivity : AppCompatActivity() {

    private lateinit var pendingCountText: TextView
    private lateinit var approvedFutureCountText: TextView
    private lateinit var messageText: TextView

    // sets up the screen and points the buttons at the other prosumer screens
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prosumer_dashboard)

        pendingCountText = findViewById(R.id.pendingCountText)
        approvedFutureCountText = findViewById(R.id.approvedFutureCountText)
        messageText = findViewById(R.id.messageText)

        findViewById<Button>(R.id.myReservationsButton).setOnClickListener {
            open(ReservationListActivity::class.java)
        }

        findViewById<Button>(R.id.historyButton).setOnClickListener {
            open(ReservationHistoryActivity::class.java)
        }

        findViewById<Button>(R.id.reserveButton).setOnClickListener {
            open(ReserveSlotActivity::class.java)
        }
    }

    // reloads the counts every time the screen comes back to the front, so a new booking shows up
    override fun onResume() {
        super.onResume()
        loadCounts()
    }

    // asks the api for the two counts, off the main thread
    private fun loadCounts() {
        val session = AppDatabase(this).session()
        if (session == null) {
            showMessage(getString(R.string.message_not_logged_in))
            return
        }

        val api = ApiClient(authToken = session.token)

        Thread {
            try {
                val summary = JSONObject(api.get("/reservations/summary"))
                runOnUiThread {
                    pendingCountText.text = summary.optInt("pending").toString()
                    approvedFutureCountText.text = summary.optInt("approvedFuture").toString()
                    hideMessage()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showMessage(e.message ?: getString(R.string.error_could_not_load_counts))
                }
            }
        }.start()
    }

    // opens another prosumer screen
    private fun open(screen: Class<*>) {
        startActivity(Intent(this, screen))
    }

    // shows the line under the title, used for errors
    private fun showMessage(text: String) {
        messageText.text = text
        messageText.visibility = View.VISIBLE
    }

    // hides the line under the title
    private fun hideMessage() {
        messageText.visibility = View.GONE
    }
}
