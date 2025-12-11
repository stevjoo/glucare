package com.example.mapmidtermproject.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.adapters.LocalImageAdapter
import com.example.mapmidtermproject.viewmodels.WoundViewModel

class LocalGalleryActivity : AppCompatActivity() {

    private lateinit var viewModel: WoundViewModel
    private lateinit var adapter: LocalImageAdapter
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var rvGallery: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_gallery)

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        rvGallery = findViewById(R.id.rvGallery)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // Setup RecyclerView
        rvGallery.layoutManager = GridLayoutManager(this, 2)
        adapter = LocalImageAdapter { localImage ->
            AlertDialog.Builder(this)
                .setTitle("Hapus Foto?")
                .setMessage("Foto ini akan dihapus permanen dari aplikasi.")
                .setPositiveButton("Hapus") { _, _ ->
                    viewModel.deleteImage(localImage.file)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        rvGallery.adapter = adapter

        viewModel = ViewModelProvider(this)[WoundViewModel::class.java]

        // Observe Data
        viewModel.woundImages.observe(this) { images ->
            if (images.isEmpty()) {
                layoutEmptyState.visibility = View.VISIBLE
                rvGallery.visibility = View.GONE
            } else {
                layoutEmptyState.visibility = View.GONE
                rvGallery.visibility = View.VISIBLE
                adapter.submitList(images)
            }
        }

        viewModel.loadImages()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}