package com.example.mapmidtermproject.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.viewmodels.WoundViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class AnalysisActivity : AppCompatActivity() {

    private lateinit var ivWoundImage: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var btnStartAnalysis: MaterialButton
    private var currentImageUri: Uri? = null

    private lateinit var viewModel: WoundViewModel
    private lateinit var loadingDialog: ProgressDialog

    private val cameraActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uriStr = result.data?.getStringExtra("captured_uri")
                if (!uriStr.isNullOrEmpty()) onImageSelected(Uri.parse(uriStr))
            }
        }

    // Gallery Launcher untuk Input Gambar (bukan melihat galeri history)
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { onImageSelected(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        viewModel = ViewModelProvider(this)[WoundViewModel::class.java]

        loadingDialog = ProgressDialog(this).apply {
            setMessage("Menganalisis Luka...")
            setCancelable(false)
        }

        ivWoundImage = findViewById(R.id.ivWoundImage)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnStartAnalysis = findViewById(R.id.btnStartAnalysis)

        // BACK BUTTON
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        btnSelectImage.setOnClickListener { showImageSourceDialog() }
        findViewById<ViewGroup>(R.id.cardWoundImage).setOnClickListener { showImageSourceDialog() }

        btnStartAnalysis.setOnClickListener {
            if (currentImageUri != null) {
                loadingDialog.show()
                viewModel.analyzeImage(uri = currentImageUri!!)
            } else {
                Toast.makeText(this, "Mohon pilih gambar terlebih dahulu.", Toast.LENGTH_SHORT).show()
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.analysisResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { rawResult ->
                loadingDialog.dismiss()

                if (!rawResult.startsWith("Error") && !rawResult.startsWith("❓")) {
                    currentImageUri?.let { viewModel.saveResult(it) }
                }

                val cleanResult = rawResult
                    .replace("⚠️", "")
                    .replace("✅", "")
                    .replace("❓", "")
                    .trim()

                val isSevere = viewModel.isDiabeticDetected.value ?: false
                showResultDialog(cleanResult, isSevere)
            }
        }
    }

    private fun onImageSelected(uri: Uri) {
        currentImageUri = uri
        val layoutPlaceholder = findViewById<LinearLayout>(R.id.layoutPlaceholder)
        layoutPlaceholder?.visibility = View.GONE

        ivWoundImage.setImageURI(uri)
        ivWoundImage.alpha = 0f
        ivWoundImage.scaleX = 0.8f
        ivWoundImage.scaleY = 0.8f

        ivWoundImage.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(600).setInterpolator(OvershootInterpolator()).start()

        btnStartAnalysis.isEnabled = true
        btnStartAnalysis.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#009688"))
    }

    private fun showResultDialog(resultText: String, isSevere: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_result, null)
        val builder = AlertDialog.Builder(this)
        val dialog = builder.create()

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnPositive = dialogView.findViewById<MaterialButton>(R.id.btnDialogPositive)
        val btnNegative = dialogView.findViewById<MaterialButton>(R.id.btnDialogNegative)
        val ivIcon = dialogView.findViewById<ImageView>(R.id.ivResultIcon)

        if (isSevere) {
            tvTitle.text = "PERHATIAN MEDIS"
            tvTitle.setTextColor(Color.RED)
            tvMessage.text = "$resultText\n\nREKOMENDASI:\nLuka berisiko. Segera konsultasikan ke dokter."
            ivIcon.setColorFilter(Color.RED)
            ivIcon.setImageResource(android.R.drawable.stat_sys_warning)

            btnPositive.text = "Cari Rumah Sakit"
            btnPositive.backgroundTintList = ColorStateList.valueOf(Color.RED)
            btnPositive.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, LocationActivity::class.java))
            }

            btnNegative.visibility = ViewGroup.VISIBLE
            btnNegative.text = "Tutup"
            btnNegative.setOnClickListener { dialog.dismiss() }

        } else {
            tvTitle.text = "HASIL ANALISIS"
            tvTitle.setTextColor(Color.parseColor("#009688"))
            tvMessage.text = "$resultText\n\nSARAN:\nLuka tampak ringan. Pantau terus."
            ivIcon.setColorFilter(Color.parseColor("#009688"))

            btnPositive.text = "Oke, Disimpan"
            btnPositive.setOnClickListener { dialog.dismiss() }
            btnNegative.visibility = ViewGroup.GONE
        }

        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showImageSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_source, null)
        val builder = AlertDialog.Builder(this)
        val dialog = builder.create()

        dialogView.findViewById<MaterialButton>(R.id.btnOpenCamera).setOnClickListener {
            dialog.dismiss()
            cameraActivityLauncher.launch(Intent(this, CameraActivity::class.java))
        }

        dialogView.findViewById<MaterialButton>(R.id.btnOpenGallery).setOnClickListener {
            dialog.dismiss()
            galleryLauncher.launch("image/*")
        }

        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}