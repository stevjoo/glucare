package com.example.mapmidtermproject.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.utils.FoodLog
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class FoodLogAdapter(
    private val onItemClick: (FoodLog) -> Unit
) : RecyclerView.Adapter<FoodLogAdapter.ViewHolder>() {

    private var logs = listOf<FoodLog>()

    fun submitList(newLogs: List<FoodLog>) {
        logs = newLogs.sortedByDescending { it.timestamp }
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFood: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvSugar: TextView = itemView.findViewById(R.id.tvSugar)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        val cvStatusIcon: MaterialCardView = itemView.findViewById(R.id.cvStatusIcon)
        val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)

        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(logs[adapterPosition])
                }
            }
            btnDelete.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(logs[adapterPosition])
                }
            }
        }

        fun bind(log: FoodLog) {
            tvFood.text = log.foodName
            tvSugar.text = "${log.bloodSugar}"

            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            tvDate.text = sdf.format(log.timestamp)

            // --- LOGIKA STATUS GULA DARAH ---
            val context = itemView.context

            // Default: Aman (Hijau)
            var iconRes = android.R.drawable.ic_input_add
            var colorRes = R.color.status_safe
            var bgRes = R.color.status_safe_bg

            if (log.bloodSugar >= 200) {
                // Bahaya (Merah)
                iconRes = android.R.drawable.ic_delete
                colorRes = R.color.status_danger
                bgRes = R.color.status_danger_bg
            } else if (log.bloodSugar >= 140) {
                // Hati-hati (Oranye)
                iconRes = android.R.drawable.stat_sys_warning
                colorRes = R.color.status_warning
                bgRes = R.color.status_warning_bg
            }

            ivStatusIcon.setImageResource(iconRes)
            ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
            cvStatusIcon.setCardBackgroundColor(ContextCompat.getColor(context, bgRes))

            tvSugar.setTextColor(ContextCompat.getColor(context, colorRes))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount() = logs.size
}