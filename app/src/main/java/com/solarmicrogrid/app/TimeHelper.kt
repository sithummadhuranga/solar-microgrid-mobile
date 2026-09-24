package com.solarmicrogrid.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// small helpers for showing api times in the phone's time zone and picking a time without typing it
object TimeHelper {

    // turns a utc time from the api into a date
    private fun parseUtc(utc: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(utc.take(16)) ?: Date()
    }

    // shows a utc time from the api as a short local text like Sat 26 Sep, 10:00
    fun display(utc: String): String {
        return SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(parseUtc(utc))
    }

    // shows a slot as a day and a time range like Sat 26 Sep, 10:00 to 12:00
    fun range(startUtc: String, endUtc: String): String {
        val day = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(parseUtc(startUtc))
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return day + ", " + timeFormat.format(parseUtc(startUtc)) + " to " + timeFormat.format(parseUtc(endUtc))
    }

    // shows a picked local time as a short text like Sat 26 Sep, 10:00
    fun display(picked: Calendar): String {
        return SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(picked.time)
    }

    // turns a picked local time into the utc text the api wants
    fun toUtc(picked: Calendar): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(picked.time)
    }

    // asks for a date and then a time, then hands back the picked local time
    fun pick(context: Context, onPicked: (Calendar) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, hour, minute, 0)
                onPicked(picked)
            }, now.get(Calendar.HOUR_OF_DAY), 0, true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }
}
