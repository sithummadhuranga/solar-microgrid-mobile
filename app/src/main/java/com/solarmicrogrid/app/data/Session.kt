package com.solarmicrogrid.app.data

// the logged in user as it is kept on the phone, see D-8 in PROJECT_SCOPE.md
class Session(
    val userId: String,
    val nic: String,
    val role: String,
    val token: String,
    val name: String
)
