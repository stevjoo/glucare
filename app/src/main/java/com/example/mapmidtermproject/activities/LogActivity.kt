package com.example.mapmidtermproject.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog // Tambahkan import ini
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.adapters.FoodLogAdapter
import com.example.mapmidtermproject.settings.SettingsActivity
import com.example.mapmidtermproject.utils.CustomMarkerView
import com.example.mapmidtermproject.utils.FoodLog
import com.example.mapmidtermproject.viewmodels.LogViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class LogActivity : AppCompatActivity() {

    private lateinit var viewModel: LogViewModel
    private lateinit var etFoodName: TextInputEditText
    private lateinit var etSugarLevel: TextInputEditText
    private lateinit var lineChart: LineChart
    private lateinit var tvSelectedDate: TextView
    private lateinit var adapter: FoodLogAdapter

    private var allLogs: List<FoodLog> = listOf()
    private var selectedDate: Calendar = Calendar.getInstance()
    private var currentFilterType = "DAY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        viewModel = ViewModelProvider(this)[LogViewModel::class.java]

        etFoodName = findViewById(R.id.etFoodName)
        etSugarLevel = findViewById(R.id.etSugarLevel)
        lineChart = findViewById(R.id.lineChart)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)

        val btnSave = findViewById<MaterialButton>(R.id.btnSaveLog)
        val btnPickDate = findViewById<MaterialButton>(R.id.btnPickDate)
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val rvLogs = findViewById<RecyclerView>(R.id.rvFoodLogs)

        val tvViewAllLogs = findViewById<TextView>(R.id.tvViewAllLogs)
        tvViewAllLogs.setOnClickListener {
            startActivity(Intent(this, AllFoodLogActivity::class.java))
        }

        rvLogs.layoutManager = LinearLayoutManager(this)

        adapter = FoodLogAdapter { log -> showEditDialog(log) }
        rvLogs.adapter = adapter

        viewModel.logs.observe(this) { logs ->
            allLogs = logs
            applyFilter()
            val limitedLogs = logs.sortedByDescending { it.timestamp }.take(5)
            adapter.submitList(limitedLogs)
        }

        setupChart()
        updateDateLabel()

        btnSave.setOnClickListener {
            val food = etFoodName.text.toString()
            val sugarStr = etSugarLevel.text.toString()

            if (food.isNotEmpty() && sugarStr.isNotEmpty()) {
                val sugar = sugarStr.toIntOrNull()
                if (sugar != null) {
                    viewModel.saveLog(food, sugar,
                        onSuccess = {
                            Toast.makeText(this, "Data tersimpan!", Toast.LENGTH_SHORT).show()
                            etFoodName.text?.clear()
                            etSugarLevel.text?.clear()
                        },
                        onFailure = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
            } else {
                Toast.makeText(this, "Mohon lengkapi data", Toast.LENGTH_SHORT).show()
            }
        }

        btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateLabel()
                applyFilter()
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnSortDay -> currentFilterType = "DAY"
                    R.id.btnSortWeek -> currentFilterType = "WEEK"
                    R.id.btnSortMonth -> currentFilterType = "MONTH"
                }
                applyFilter()
            }
        }

        setupBottomNavigation()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startListening()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopListening()
    }

    override fun onResume() {
        super.onResume()
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        if (bottomNav.selectedItemId != R.id.nav_log) {
            bottomNav.selectedItemId = R.id.nav_log
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                R.id.nav_log -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                else -> false
            }
        }
    }

    // --- DIALOG EDIT LOG (REVISI) ---
    private fun showEditDialog(log: FoodLog) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_food_log, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etEditFood = dialogView.findViewById<TextInputEditText>(R.id.etEditFoodName)
        val etEditSugar = dialogView.findViewById<TextInputEditText>(R.id.etEditSugarLevel)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btnUpdateLog)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btnDeleteLog)
        val tvEditTimeLabel = dialogView.findViewById<TextView>(R.id.tvEditTimeLabel)
        val btnEditTime = dialogView.findViewById<MaterialButton>(R.id.btnEditTime)

        etEditFood.setText(log.foodName)
        etEditSugar.setText(log.bloodSugar.toString())

        val editCalendar = Calendar.getInstance()
        editCalendar.time = log.timestamp
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale("id", "ID"))
        tvEditTimeLabel.text = sdf.format(editCalendar.time)

        btnEditTime.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->

                    val checkCalendar = Calendar.getInstance()
                    checkCalendar.set(year, month, day, hour, minute)

                    if (checkCalendar.after(Calendar.getInstance())) {
                        Toast.makeText(this, "Tidak bisa memilih waktu masa depan!", Toast.LENGTH_SHORT).show()
                    } else {
                        editCalendar.set(year, month, day, hour, minute)
                        tvEditTimeLabel.text = sdf.format(editCalendar.time)
                    }

                }, editCalendar.get(Calendar.HOUR_OF_DAY), editCalendar.get(Calendar.MINUTE), true).show()

            }, editCalendar.get(Calendar.YEAR), editCalendar.get(Calendar.MONTH), editCalendar.get(Calendar.DAY_OF_MONTH))

            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        btnUpdate.setOnClickListener {
            val newFood = etEditFood.text.toString()
            val newSugar = etEditSugar.text.toString().toIntOrNull()

            if (newFood.isNotEmpty() && newSugar != null) {
                viewModel.updateLog(log.id, newFood, newSugar, editCalendar.time,
                    onSuccess = {
                        Toast.makeText(this, "Update berhasil", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    },
                    onFailure = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                )
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus?")
                .setMessage("Yakin hapus data ini?")
                .setPositiveButton("Ya") { _, _ ->
                    viewModel.deleteLog(log.id,
                        onSuccess = { dialog.dismiss() },
                        onFailure = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                    )
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        dialog.show()
    }


    // --- CHART & FILTER FUNCTIONS ---

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setExtraOffsets(0f, 0f, 0f, 10f)
    }

    private fun updateDateLabel() {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID"))
        tvSelectedDate.text = "Basis: ${sdf.format(selectedDate.time)}"
    }

    private fun applyFilter() {
        if (allLogs.isEmpty()) {
            lineChart.clear()
            return
        }
        val filteredLogs = mutableListOf<FoodLog>()
        val calLog = Calendar.getInstance()
        allLogs.forEach { log ->
            calLog.time = log.timestamp
            val sameDay = calLog.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR) && calLog.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
            when (currentFilterType) {
                "DAY" -> if (sameDay) filteredLogs.add(log)
                "WEEK" -> {
                    val diff = selectedDate.timeInMillis - log.timestamp.time
                    val daysDiff = diff / (1000 * 60 * 60 * 24)
                    if (daysDiff in 0..7) filteredLogs.add(log)
                }
                "MONTH" -> {
                    if (calLog.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) && calLog.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)) filteredLogs.add(log)
                }
            }
        }
        updateChart(filteredLogs)
    }

    private fun updateChart(logs: List<FoodLog>) {
        val sortedLogs = logs.sortedBy { it.timestamp }
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        val dateFormat = if (currentFilterType == "DAY") SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("dd/MM", Locale.getDefault())

        sortedLogs.forEachIndexed { index, log ->
            entries.add(Entry(index.toFloat(), log.bloodSugar.toFloat()))
            labels.add(dateFormat.format(log.timestamp))
        }

        if (entries.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("Tidak ada data pada periode ini.")
            return
        }

        val dataSet = LineDataSet(entries, "Gula Darah (mg/dL)")
        dataSet.color = getColor(R.color.blue)
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setCircleColor(getColor(R.color.blue))
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawValues(false)

        val marker = CustomMarkerView(this, R.layout.custom_marker_view, sortedLogs)
        marker.chartView = lineChart
        lineChart.marker = marker

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.granularity = 1f
        lineChart.data = LineData(dataSet)
        lineChart.animateY(800)
        lineChart.invalidate()
    }
}