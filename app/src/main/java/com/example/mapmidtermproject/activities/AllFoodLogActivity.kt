package com.example.mapmidtermproject.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.adapters.FoodLogAdapter
import com.example.mapmidtermproject.utils.FoodLog
import com.example.mapmidtermproject.viewmodels.LogViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AllFoodLogActivity : AppCompatActivity() {

    private lateinit var viewModel: LogViewModel
    private lateinit var adapter: FoodLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_food_log)

        viewModel = ViewModelProvider(this)[LogViewModel::class.java]

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvAllFoodLogs)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = FoodLogAdapter { log -> showEditDialog(log) }
        rv.adapter = adapter

        viewModel.logs.observe(this) { logs ->
            adapter.submitList(logs)
        }
        viewModel.startListening()
    }

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

        val calendar = Calendar.getInstance()
        calendar.time = log.timestamp
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale("id", "ID"))

        tvEditTimeLabel.text = sdf.format(calendar.time)

        btnEditTime.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(this, { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)

                    tvEditTimeLabel.text = sdf.format(calendar.time)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnUpdate.setOnClickListener {
            val newFood = etEditFood.text.toString()
            val newSugar = etEditSugar.text.toString().toIntOrNull()

            if (newFood.isNotEmpty() && newSugar != null) {
                viewModel.updateLog(log.id, newFood, newSugar, calendar.time,
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
}