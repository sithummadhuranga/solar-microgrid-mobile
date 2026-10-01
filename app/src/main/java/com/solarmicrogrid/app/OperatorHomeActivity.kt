package com.solarmicrogrid.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.solarmicrogrid.app.data.AppDatabase

// the grid operator home screen, starts the qr scan for an energy transfer
class OperatorHomeActivity : AppCompatActivity() {

    // takes what the scanner read and passes it to the verify screen
    private val scan = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val intent = Intent(this, VerifyReservationActivity::class.java)
            intent.putExtra(VerifyReservationActivity.EXTRA_QR_DATA, result.contents)
            startActivity(intent)
        }
    }

    // handles the camera permission answer, the scanner cannot open without it
    private val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startScan()
        } else {
            Toast.makeText(this, getString(R.string.error_camera_needed), Toast.LENGTH_SHORT).show()
        }
    }

    // sets up the screen and shows which operator is logged in
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_home)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        val session = AppDatabase(this).session()
        findViewById<TextView>(R.id.operatorNameText).text = session?.name ?: ""

        // asks for the camera first, then scans
        findViewById<Button>(R.id.scanButton).setOnClickListener {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (granted == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                askCamera.launch(Manifest.permission.CAMERA)
            }
        }

        // switches to the map, scan is already this screen so it just re-runs the scan flow
        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_map) {
                startActivity(Intent(this, StationMapActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            } else {
                findViewById<Button>(R.id.scanButton).performClick()
            }
            true
        }
    }

    // adds the log out action to the toolbar's overflow menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_logout, menu)
        return true
    }

    // clears the session and sends the operator back to the login screen
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

    // opens the camera scanner, qr codes only
    private fun startScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt(getString(R.string.label_scan_prompt))
        options.setBeepEnabled(false)
        scan.launch(options)
    }
}
