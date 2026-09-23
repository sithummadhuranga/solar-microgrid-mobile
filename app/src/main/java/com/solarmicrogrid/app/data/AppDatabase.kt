package com.solarmicrogrid.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// the app's local database, keeps the logged in user so the app remembers them
class AppDatabase(context: Context) : SQLiteOpenHelper(context, "solarmicrogrid.db", null, 1) {

    // creates the tables the first time the app runs
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE session (user_id TEXT, nic TEXT, role TEXT, token TEXT, name TEXT)"
        )
    }

    // rebuilds the tables when the database version goes up
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS session")
        onCreate(db)
    }

    // saves the logged in user, only one is kept at a time
    fun saveSession(userId: String, nic: String, role: String, token: String, name: String) {
        val values = ContentValues()
        values.put("user_id", userId)
        values.put("nic", nic)
        values.put("role", role)
        values.put("token", token)
        values.put("name", name)

        val db = writableDatabase
        db.delete("session", null, null)
        db.insert("session", null, values)
    }

    // gets the logged in user, or null when nobody is logged in
    fun session(): Session? {
        val cursor = readableDatabase.query("session", null, null, null, null, null, null)
        var session: Session? = null

        if (cursor.moveToFirst()) {
            session = Session(
                cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("nic")),
                cursor.getString(cursor.getColumnIndexOrThrow("role")),
                cursor.getString(cursor.getColumnIndexOrThrow("token")),
                cursor.getString(cursor.getColumnIndexOrThrow("name"))
            )
        }

        cursor.close()
        return session
    }

    // clears the logged in user on logout
    fun clearSession() {
        writableDatabase.delete("session", null, null)
    }
}
