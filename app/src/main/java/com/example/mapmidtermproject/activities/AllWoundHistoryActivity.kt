package com.example.mapmidtermproject.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.adapters.WoundHistoryAdapter
import com.example.mapmidtermproject.models.WoundAnalysis
import com.example.mapmidtermproject.viewmodels.WoundViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AllWoundHistoryActivity : AppCompatActivity() {

    private lateinit var viewModel: WoundViewModel
    private lateinit var adapter: WoundHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_wound_history)

        viewModel = ViewModelProvider(this)[WoundViewModel::class.java]

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvAllHistory)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = WoundHistoryAdapter(emptyList()) { item ->
            showDetailDialog(item)
        }
        rv.adapter = adapter

        viewModel.historyList.observe(this) { list ->
            adapter.updateData(list)
        }
        viewModel.loadHistory()
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
        val btnEditDate = dialogView.findViewById<MaterialButton>(R.id.btnEditDate)

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
        
        val editCalendar = Calendar.getInstance()
        editCalendar.time = item.timestamp
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        tvDetailDate.text = sdf.format(editCalendar.time)

        btnEditDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->

                TimePickerDialog(this, { _, hour, minute ->
                    val checkCalendar = Calendar.getInstance()
                    checkCalendar.set(year, month, day, hour, minute)

                    if (checkCalendar.after(Calendar.getInstance())) {
                        Toast.makeText(this, "Tidak bisa memilih waktu masa depan!", Toast.LENGTH_SHORT).show()
                    } else {
                        editCalendar.set(year, month, day, hour, minute)
                        tvDetailDate.text = sdf.format(editCalendar.time)
                    }
                }, editCalendar.get(Calendar.HOUR_OF_DAY), editCalendar.get(Calendar.MINUTE), true).show()

            }, editCalendar.get(Calendar.YEAR), editCalendar.get(Calendar.MONTH), editCalendar.get(Calendar.DAY_OF_MONTH))

            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        val imgFile = File(item.localImagePath)

        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            ivImage.setImageBitmap(bitmap)
            tvStatus.visibility = View.GONE
        } else if (item.imageBase64.isNotEmpty()) {
            try {
                val decodedString = Base64.decode(item.imageBase64, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivImage.setImageBitmap(decodedBitmap)
                tvStatus.visibility = View.GONE

                try {
                    val fileOut = FileOutputStream(imgFile)
                    decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOut)
                    fileOut.flush()
                    fileOut.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                ivImage.setImageResource(R.drawable.ic_image_placeholder)
                tvStatus.text = "Gambar rusak"
                tvStatus.visibility = View.VISIBLE
            }
        } else {
            ivImage.setImageResource(R.drawable.ic_image_placeholder)
            ivImage.alpha = 0.5f
            tvStatus.visibility = View.VISIBLE
        }

        btnUpdate.setOnClickListener {
            val newLabel = etLabel.text.toString().trim()
            if (newLabel.isNotEmpty()) {
                viewModel.updateWoundLabel(item, newLabel)
                viewModel.updateWoundDate(item, editCalendar.time) // Update waktu baru
                Toast.makeText(this, "Data diperbarui", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Data?")
                .setMessage("Data ini akan dihapus permanen.")
                .setPositiveButton("Hapus") { _, _ ->
                    viewModel.deleteWoundItem(item)
                    Toast.makeText(this, "Data dihapus", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        dialog.show()
    }
}