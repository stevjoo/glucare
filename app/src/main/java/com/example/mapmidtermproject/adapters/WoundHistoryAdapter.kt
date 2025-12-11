package com.example.mapmidtermproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.models.WoundAnalysis
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.Locale

class WoundHistoryAdapter(
    private var list: List<WoundAnalysis>,
    private val onItemClick: (WoundAnalysis) -> Unit
) : RecyclerView.Adapter<WoundHistoryAdapter.ViewHolder>() {

    fun updateData(newList: List<WoundAnalysis>) {
        list = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLabel: TextView = itemView.findViewById(R.id.tvHistoryLabel)
        val tvDate: TextView = itemView.findViewById(R.id.tvHistoryDate)
        val progressConfidence: CircularProgressIndicator = itemView.findViewById(R.id.progressConfidence)
        val tvProgressText: TextView = itemView.findViewById(R.id.tvProgressText)

        init {
            itemView.setOnClickListener { onItemClick(list[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wound_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvLabel.text = item.label ?: "Tanpa Label"

        if (item.timestamp != null) {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            holder.tvDate.text = sdf.format(item.timestamp)
        } else {
            holder.tvDate.text = "-"
        }

        val percent = (item.confidence * 100).toInt()
        holder.progressConfidence.progress = percent
        holder.tvProgressText.text = "$percent%"

        val colorRes = if (percent > 80) R.color.blue else android.R.color.holo_orange_dark
        val color = holder.itemView.context.getColor(colorRes)

        holder.progressConfidence.setIndicatorColor(color)
        holder.tvProgressText.setTextColor(color)
    }

    override fun getItemCount() = list.size
}