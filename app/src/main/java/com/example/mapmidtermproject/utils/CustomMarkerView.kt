package com.example.mapmidtermproject.utils

import android.content.Context
import android.widget.TextView
import com.example.mapmidtermproject.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(context: Context, layoutResource: Int, private val logs: List<FoodLog>) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvMarkerContent)
    private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        try {
            val index = e?.x?.toInt() ?: return
            if (index in logs.indices) {
                val log = logs[index]
                tvContent.text = "${log.foodName}\n${log.bloodSugar} mg/dL\nJam: ${sdf.format(log.timestamp)}"
            }
        } catch (ex: Exception) {
            tvContent.text = ""
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}