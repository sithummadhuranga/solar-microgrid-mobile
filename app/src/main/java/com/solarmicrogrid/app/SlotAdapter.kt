package com.solarmicrogrid.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.solarmicrogrid.app.model.EnergyBookingSlot
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// list of upcoming slots of the selected node on the map screen
class SlotAdapter : RecyclerView.Adapter<SlotAdapter.SlotViewHolder>() {

    private var slots: List<EnergyBookingSlot> = emptyList()

    class SlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.slot_time)
        val freeText: TextView = view.findViewById(R.id.slot_free)
    }

    // replaces the slots and redraws the list
    fun setItems(items: List<EnergyBookingSlot>) {
        slots = items
        notifyDataSetChanged()
    }

    // makes a row view for one slot
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_slot, parent, false)
        return SlotViewHolder(view)
    }

    // fills a row with the slot times in local time and the free count
    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        val context = holder.itemView.context
        holder.timeText.text = context.getString(
            R.string.slot_time, toLocalTime(slot.startTime), toLocalTime(slot.endTime)
        )
        holder.freeText.text = context.getString(
            R.string.slot_free, slot.availableSlots, slot.totalSlots
        )
    }

    // returns the number of slots in the list
    override fun getItemCount(): Int {
        return slots.size
    }

    // turns a utc time from the api into the phone's local time, keeps the text if it cannot be read
    private fun toLocalTime(utc: String): String {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        input.timeZone = TimeZone.getTimeZone("UTC")
        val date = try {
            input.parse(utc.take(19))
        } catch (e: ParseException) {
            null
        } ?: return utc
        val output = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
        return output.format(date)
    }
}
