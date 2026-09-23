package com.solarmicrogrid.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
