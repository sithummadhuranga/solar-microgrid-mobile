package com.solarmicrogrid.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.solarmicrogrid.app.model.EnergyReservation

// fills the rows of the reservation lists, M-7
class ReservationAdapter : RecyclerView.Adapter<ReservationAdapter.ReservationHolder>() {

    private var reservations = listOf<EnergyReservation>()

    // holds the views of one row
    class ReservationHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stationText: TextView = view.findViewById(R.id.stationText)
        val slotText: TextView = view.findViewById(R.id.slotText)
        val scheduledTimeText: TextView = view.findViewById(R.id.scheduledTimeText)
        val stateText: TextView = view.findViewById(R.id.stateText)
    }

    // replaces the list that is shown
    fun setReservations(list: List<EnergyReservation>) {
        reservations = list
        notifyDataSetChanged()
    }

    // creates one empty row
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ReservationHolder(view)
    }

    // puts one reservation into a row
    override fun onBindViewHolder(holder: ReservationHolder, position: Int) {
        val reservation = reservations[position]
        val context = holder.itemView.context

        holder.stationText.text =
            context.getString(R.string.label_station_prefix) + reservation.stationId
        holder.slotText.text =
            context.getString(R.string.label_slot_prefix) + reservation.slotId
        holder.scheduledTimeText.text =
            context.getString(R.string.label_scheduled_prefix) + reservation.scheduledTime
        holder.stateText.text =
            context.getString(R.string.label_state_prefix) + reservation.state
    }

    // how many rows the list has
    override fun getItemCount(): Int {
        return reservations.size
    }
}
