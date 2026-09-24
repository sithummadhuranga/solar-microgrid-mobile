// File: RegisterActivity.kt
// Purpose: registers a new prosumer with a nic, the account stays pending until backoffice activates it
// Author: H.M.T.S.M.Dissanayake

package com.solarmicrogrid.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.solarmicrogrid.app.api.ApiClient

class RegisterActivity : AppCompatActivity() {

    private val api = ApiClient()

    // sets up the registration screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val nicInput = findViewById<EditText>(R.id.nicInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val fullNameInput = findViewById<EditText>(R.id.fullNameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val addressInput = findViewById<EditText>(R.id.addressInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // checks the fields are filled, then sends the registration request
        registerButton.setOnClickListener {
            val nic = nicInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val fullName = fullNameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val address = addressInput.text.toString().trim()

            if (nic.isEmpty() || password.isEmpty() || fullName.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                errorText.text = getString(R.string.error_fill_required)
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            register(nic, password, fullName, phone, address, errorText)
        }
    }

    // closes this screen when the back arrow in the toolbar is tapped
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // sends the register request off the main thread, then sends the prosumer back to log in once activated
    private fun register(nic: String, password: String, fullName: String, phone: String, address: String, errorText: TextView) {
        Thread {
            try {
                api.register(nic, password, fullName, phone, address)
                runOnUiThread {
                    errorText.setTextColor(getColor(R.color.solar_green_dark))
                    errorText.text = getString(R.string.message_registered)
                    errorText.visibility = TextView.VISIBLE
                }
            } catch (e: Exception) {
                runOnUiThread {
                    errorText.text = e.message ?: getString(R.string.error_register_failed)
                    errorText.visibility = TextView.VISIBLE
                }
            }
        }.start()
    }
}
