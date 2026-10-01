// File: EditProfileActivity.kt
// Purpose: lets a prosumer edit their own profile and ask for their account to be deactivated
// Author: H.M.T.S.M.Dissanayake

package com.solarmicrogrid.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.api.SessionExpiredException
import com.solarmicrogrid.app.data.AppDatabase

class EditProfileActivity : AppCompatActivity() {

    private lateinit var api: ApiClient
    private lateinit var nicText: TextView
    private lateinit var fullNameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var messageText: TextView
    private lateinit var deactivateButton: Button
    private lateinit var currentPasswordInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmNewPasswordInput: EditText

    companion object {
        // shortest password accepted
        private const val MIN_PASSWORD_LENGTH = 8
    }

    // sets up the screen and loads the current profile
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val session = AppDatabase(this).session()
        if (session == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        api = ApiClient(authToken = session.token)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> switchTo(ProsumerDashboardActivity::class.java)
                R.id.nav_reserve -> switchTo(ReserveSlotActivity::class.java)
                R.id.nav_bookings -> switchTo(ReservationListActivity::class.java)
            }
            true
        }

        nicText = findViewById(R.id.nicText)
        fullNameInput = findViewById(R.id.fullNameInput)
        phoneInput = findViewById(R.id.phoneInput)
        addressInput = findViewById(R.id.addressInput)
        messageText = findViewById(R.id.messageText)
        deactivateButton = findViewById(R.id.deactivateButton)
        currentPasswordInput = findViewById(R.id.currentPasswordInput)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmNewPasswordInput = findViewById(R.id.confirmNewPasswordInput)

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveProfile() }
        findViewById<Button>(R.id.changePasswordButton).setOnClickListener { changePassword() }
        deactivateButton.setOnClickListener { confirmDeactivate() }

        loadProfile()
    }

    // asks the api for the current profile, off the main thread
    private fun loadProfile() {
        Thread {
            try {
                val profile = api.myProfile()
                runOnUiThread {
                    nicText.text = profile.getString("nic")
                    fullNameInput.setText(profile.getString("fullName"))
                    phoneInput.setText(profile.getString("phone"))
                    addressInput.setText(profile.getString("address"))
                    deactivateButton.isEnabled = !profile.getBoolean("deactivationRequested")
                }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e, getString(R.string.error_could_not_load)) }
            }
        }.start()
    }

    // checks the fields are filled, then saves the profile off the main thread
    private fun saveProfile() {
        val fullName = fullNameInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val address = addressInput.text.toString().trim()

        if (fullName.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showMessage(getString(R.string.error_fill_required))
            return
        }

        Thread {
            try {
                api.updateMyProfile(fullName, phone, address)
                runOnUiThread { showMessage(getString(R.string.message_profile_saved)) }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e, getString(R.string.error_could_not_save)) }
            }
        }.start()
    }

    // checks the new password is strong enough, then sends the change off the main thread
    private fun changePassword() {
        val currentPassword = currentPasswordInput.text.toString().trim()
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmNewPassword = confirmNewPasswordInput.text.toString().trim()

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            showMessage(getString(R.string.error_fill_required))
            return
        }

        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            showMessage(getString(R.string.error_password_too_short))
            return
        }

        if (newPassword != confirmNewPassword) {
            showMessage(getString(R.string.error_passwords_dont_match))
            return
        }

        Thread {
            try {
                api.changeMyPassword(currentPassword, newPassword)
                runOnUiThread {
                    currentPasswordInput.text.clear()
                    newPasswordInput.text.clear()
                    confirmNewPasswordInput.text.clear()
                    showMessage(getString(R.string.message_password_changed))
                }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e, getString(R.string.error_could_not_save)) }
            }
        }.start()
    }

    // shows a confirm dialog before asking backoffice to deactivate the account, only backoffice can reactivate it
    private fun confirmDeactivate() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_deactivate_account)
            .setPositiveButton(R.string.button_request_deactivation) { _, _ -> requestDeactivation() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // sends the deactivation request off the main thread
    private fun requestDeactivation() {
        Thread {
            try {
                api.requestMyDeactivation()
                runOnUiThread {
                    deactivateButton.isEnabled = false
                    showMessage(getString(R.string.message_deactivation_requested))
                }
            } catch (e: Exception) {
                runOnUiThread { handleApiError(e, getString(R.string.error_could_not_save)) }
            }
        }.start()
    }

    // shows the message line under the form
    private fun showMessage(text: String) {
        messageText.text = text
        messageText.visibility = TextView.VISIBLE
    }

    // closes this screen when the back arrow in the toolbar is tapped
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // moves to another tab, closes this one so the tabs do not stack up
    private fun switchTo(screen: Class<*>) {
        startActivity(Intent(this, screen))
        overridePendingTransition(0, 0)
        finish()
    }

    // a session expiry goes back to login instead of showing the message inline
    private fun handleApiError(e: Exception, fallback: String) {
        if (e is SessionExpiredException) {
            AppDatabase(this).clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        showMessage(e.message ?: fallback)
    }
}
