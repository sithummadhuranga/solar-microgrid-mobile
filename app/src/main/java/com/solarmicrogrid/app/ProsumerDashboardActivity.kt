package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase
import com.solarmicrogrid.app.data.Station
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
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        pendingCountText = findViewById(R.id.pendingCountText)
        approvedFutureCountText = findViewById(R.id.approvedFutureCountText)
        messageText = findViewById(R.id.messageText)

        findViewById<Button>(R.id.historyButton).setOnClickListener {
            open(ReservationHistoryActivity::class.java)
        }

        findViewById<Button>(R.id.mapButton).setOnClickListener {
            open(StationMapActivity::class.java)
        }

        // switches tabs, dashboard itself needs no action since it is already showing
        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_reserve -> switchTab(ReserveSlotActivity::class.java)
                R.id.nav_bookings -> switchTab(ReservationListActivity::class.java)
                R.id.nav_profile -> switchTab(EditProfileActivity::class.java)
            }
            true
        }
    }

    // adds the log out action to the toolbar's overflow menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_logout, menu)
        return true
    }

    // clears the session and sends the prosumer back to the login screen
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            AppDatabase(this).clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // reloads the counts every time the screen comes back to the front, so a new booking shows up
    override fun onResume() {
        super.onResume()
        loadCounts()
        syncStations()
    }

    // pulls the microgrid nodes and keeps them on the phone for the booking lists to use
    private fun syncStations() {
        val database = AppDatabase(this)
        val session = database.session() ?: return
        val api = ApiClient(authToken = session.token)

        Thread {
            try {
                database.saveStations(Station.listFromJson(api.get("/stations")))
            } catch (e: Exception) {
                // keeps whatever was saved before, the lists fall back to showing the id
            }
        }.start()
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

    // moves to another tab, closes this one so the tabs do not stack up
    private fun switchTab(screen: Class<*>) {
        startActivity(Intent(this, screen))
        overridePendingTransition(0, 0)
        finish()
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
