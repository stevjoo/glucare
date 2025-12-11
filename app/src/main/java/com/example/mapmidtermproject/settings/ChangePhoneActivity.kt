package com.example.mapmidtermproject.settings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.utils.FirestoreHelper

class ChangePhoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_phone)

        val etNewPhone = findViewById<EditText>(R.id.etNewPhone)
        val btnSave = findViewById<Button>(R.id.btnSavePhone)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val phone = etNewPhone.text.toString().trim()
            if (phone.isEmpty()) {
                Toast.makeText(this, "Nomor telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else {
                btnSave.isEnabled = false
                btnSave.text = "Menyimpan..."

                // Panggil function KHUSUS Phone Number
                FirestoreHelper.updatePhone(phone) {
                    Toast.makeText(this, "Nomor telepon berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}