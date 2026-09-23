package com.solarmicrogrid.app.data

// the logged in user, as stored in the session table
class Session(
    val userId: String,
    val nic: String,
    val role: String,
    val token: String,
    val name: String
)
