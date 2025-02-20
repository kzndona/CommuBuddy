package com.example.commubuddy.History

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.commubuddy.AlarmItem
import com.example.commubuddy.R

class HistoryAdapter(private val historyList: List<AlarmItem>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = historyList[position]
        holder.bind(historyItem)
    }

    override fun getItemCount(): Int = historyList.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.history_item_name)
        private val distanceTextView: TextView = itemView.findViewById(R.id.history_item_distance)

        fun bind(historyItem: AlarmItem) {
            nameTextView.text = historyItem.destinationName
            distanceTextView.text = "${historyItem.ringDistance} meters"  // Or any format you prefer
        }
    }
}
