package com.example.mapmidtermproject.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.adapters.WoundHistoryAdapter
import com.example.mapmidtermproject.models.WoundAnalysis
import com.example.mapmidtermproject.settings.SettingsActivity
import com.example.mapmidtermproject.viewmodels.UserViewModel
import com.example.mapmidtermproject.viewmodels.WoundViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var woundViewModel: WoundViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var historyAdapter: WoundHistoryAdapter
    private lateinit var barChart: BarChart
    private lateinit var tvGreeting: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MAPMidTermProject)

        auth = Firebase.auth
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        woundViewModel = ViewModelProvider(this)[WoundViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        tvGreeting = findViewById(R.id.tvGreeting)
        barChart = findViewById(R.id.barChart)

        setupHistoryList()
        setupChart()
        setupBottomNavigation()

        findViewById<TextView>(R.id.tvViewAllHistory).setOnClickListener {
            startActivity(Intent(this, AllWoundHistoryActivity::class.java))
        }

        findViewById<ExtendedFloatingActionButton>(R.id.btnFabScan).setOnClickListener {
            startActivity(Intent(this, AnalysisActivity::class.java))
        }

        woundViewModel.loadHistory()
        userViewModel.loadProfile()

        userViewModel.userProfile.observe(this) { profile ->
            val name = profile?.username ?: auth.currentUser?.displayName ?: "User"
            tvGreeting.text = "Halo, $name!"
        }

        woundViewModel.historyList.observe(this) { list ->
            // LIMIT 3 ITEM UNTUK MAIN PAGE
            val limitedList = list.take(3)
            historyAdapter.updateData(limitedList)
        }

        woundViewModel.statsData.observe(this) { stats ->
            updateChart(stats)
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        if (bottomNav.selectedItemId != R.id.nav_home) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun setupHistoryList() {
        val rvHistory = findViewById<RecyclerView>(R.id.rvWoundHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)

        historyAdapter = WoundHistoryAdapter(emptyList()) { item ->
            showDetailDialog(item)
        }
        rvHistory.adapter = historyAdapter
    }

    private fun showDetailDialog(item: WoundAnalysis) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wound_detail, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val ivImage = dialogView.findViewById<ImageView>(R.id.ivDetailImage)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvImageStatus)
        val etLabel = dialogView.findViewById<TextInputEditText>(R.id.etDetailLabel)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btnUpdate)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btnDelete)

        val progressDetail = dialogView.findViewById<CircularProgressIndicator>(R.id.progressDetail)
        val tvDetailPercent = dialogView.findViewById<TextView>(R.id.tvDetailPercent)
        val tvDetailDate = dialogView.findViewById<TextView>(R.id.tvDetailDate)

        etLabel.setText(item.label)

        val percent = (item.confidence * 100).toInt()
        progressDetail.progress = percent
        tvDetailPercent.text = "$percent%"

        if (percent > 80) {
            progressDetail.setIndicatorColor(getColor(R.color.blue))
            tvDetailPercent.setTextColor(getColor(R.color.blue))
        } else {
            progressDetail.setIndicatorColor(getColor(android.R.color.holo_orange_dark))
            tvDetailPercent.setTextColor(getColor(android.R.color.holo_orange_dark))
        }

        if (item.timestamp != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            tvDetailDate.text = sdf.format(item.timestamp)
        }

        val imgFile = File(item.localImagePath)
        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            ivImage.setImageBitmap(bitmap)
            tvStatus.visibility = View.GONE
        } else {
            ivImage.setImageResource(R.drawable.ic_image_placeholder)
            ivImage.alpha = 0.5f
            tvStatus.visibility = View.VISIBLE
        }

        btnUpdate.setOnClickListener {
            val newLabel = etLabel.text.toString().trim()
            if (newLabel.isNotEmpty()) {
                woundViewModel.updateWoundLabel(item, newLabel)
                Toast.makeText(this, "Data diperbarui", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Data?")
                .setMessage("Data ini akan dihapus permanen.")
                .setPositiveButton("Hapus") { _, _ ->
                    woundViewModel.deleteWoundItem(item)
                    Toast.makeText(this, "Data dihapus", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        dialog.show()
    }

    private fun setupChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawBorders(false)

        barChart.extraBottomOffset = 20f

        barChart.setTouchEnabled(true)
        barChart.isDragEnabled = true
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.DKGRAY

        xAxis.labelRotationAngle = 0f

        val axisLeft = barChart.axisLeft
        axisLeft.setDrawGridLines(true)
        axisLeft.axisMinimum = 0f
        axisLeft.granularity = 1f

        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false
    }

    private fun updateChart(stats: Map<String, Int>) {
        if (stats.isEmpty()) {
            barChart.clear()
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f
        for ((label, count) in stats) {
            entries.add(BarEntry(index, count.toFloat()))
            labels.add(label)
            index++
        }

        val dataSet = BarDataSet(entries, "Wound Types")
        dataSet.colors = listOf(getColor(R.color.blue), Color.parseColor("#4DB6AC"))
        dataSet.valueTextSize = 12f

        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val data = BarData(dataSet)
        data.barWidth = 0.5f

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.data = data
        barChart.setVisibleXRangeMaximum(3f)
        barChart.moveViewToX(0f)
        barChart.invalidate()
        barChart.animateY(1000)
    }

    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_log -> {
                    startActivity(Intent(this, LogActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                else -> false
            }
        }
    }
}