// File: LoginActivity.kt
// Purpose: the launcher screen, logs a prosumer or a grid operator in and routes to their home screen
// Author: H.M.T.S.M.Dissanayake

package com.solarmicrogrid.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.solarmicrogrid.app.api.ApiClient
import com.solarmicrogrid.app.data.AppDatabase

class LoginActivity : AppCompatActivity() {

    private val api = ApiClient()

    // skips straight to the right home screen if a session is already saved, otherwise shows the form
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = AppDatabase(this).session()
        if (session != null) {
            openHome(session.role)
            return
        }

        setContentView(R.layout.activity_login)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        val identifierInput = findViewById<EditText>(R.id.identifierInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val errorText = findViewById<TextView>(R.id.errorText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // checks the fields are filled, then sends the login request
        loginButton.setOnClickListener {
            val identifier = identifierInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                errorText.text = getString(R.string.error_fill_required)
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = TextView.GONE
            login(identifier, password, errorText)
        }

        // opens the prosumer registration screen
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // sends the login request off the main thread, a backoffice account cannot log in on the phone
    private fun login(identifier: String, password: String, errorText: TextView) {
        Thread {
            try {
                val response = api.login(identifier, password, "mobile")
                AppDatabase(this).saveSession(
                    response.getString("id"),
                    "",
                    response.getString("role"),
                    response.getString("token"),
                    response.getString("fullName")
                )
                runOnUiThread { openHome(response.getString("role")) }
            } catch (e: Exception) {
                runOnUiThread {
                    errorText.text = e.message ?: getString(R.string.error_login_failed)
                    errorText.visibility = TextView.VISIBLE
                }
            }
        }.start()
    }

    // opens the prosumer dashboard or the operator home, depending on the logged in role
    private fun openHome(role: String) {
        val target = if (role == "GridOperator") OperatorHomeActivity::class.java else ProsumerDashboardActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }
}
