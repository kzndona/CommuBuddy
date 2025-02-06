package com.example.commubuddy.Place

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.commubuddy.R

class PlacePredictionAdapter (
    private val predictions : List<PlacePredictionModel>,
    private val onItemClicked: (PlacePredictionModel) -> Unit
) : RecyclerView.Adapter<PlacePredictionAdapter.ViewHolder>() {

    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val primaryText: TextView = itemView.findViewById(R.id.item_place_prediction_primary_text)
        val secondaryText: TextView = itemView.findViewById(R.id.item_place_prediction_secondary_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_prediction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val prediction = predictions[position]
        holder.primaryText.text = prediction.primaryText
        holder.secondaryText.text = prediction.secondaryText

        holder.itemView.setOnClickListener {
            onItemClicked(prediction)
        }
    }

    override fun getItemCount(): Int = predictions.size
}