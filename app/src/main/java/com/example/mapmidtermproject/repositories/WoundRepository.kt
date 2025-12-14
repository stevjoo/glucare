package com.example.mapmidtermproject.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.mapmidtermproject.models.WoundAnalysis
import com.example.mapmidtermproject.utils.FirestoreHelper
import com.google.firebase.firestore.ListenerRegistration
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalWoundImage(
    val file: File,
    val dateAdded: String
)

class WoundRepository(private val context: Context) {

    private fun getOutputDirectory(): File {
        val mediaDir = context.filesDir.let {
            File(it, "wound_gallery").apply { mkdirs() }
        }
        return mediaDir
    }

    // Fungsi helper untuk convert Bitmap ke Base64 String
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Kompresi kualitas 50% agar tidak memberatkan Firestore
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(getOutputDirectory(), "WOUND_$timestamp.jpg")

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun saveAnalysisResult(label: String, confidence: Float, localPath: String) {
        // Load bitmap dari local path untuk dikonversi
        val file = File(localPath)
        var base64String = ""

        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, true)
                base64String = bitmapToBase64(scaledBitmap)
            } catch (e: Exception) {
                Log.e("WoundRepo", "Gagal convert base64: ${e.message}")
            }
        }

        val analysis = WoundAnalysis(
            label = label,
            confidence = confidence,
            localImagePath = localPath,
            imageBase64 = base64String,
            timestamp = Date()
        )
        FirestoreHelper.saveWoundAnalysis(analysis) {}
    }

    fun getWoundHistory(onResult: (List<WoundAnalysis>) -> Unit): ListenerRegistration? {
        return FirestoreHelper.listenToWoundHistory(onResult)
    }

    fun getAllImages(): List<LocalWoundImage> {
        val directory = getOutputDirectory()
        val files = directory.listFiles()
        return files?.filter { it.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(it.lastModified()))
                LocalWoundImage(it, date)
            } ?: emptyList()
    }

    fun deleteImage(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    fun updateAnalysisLabel(id: String, newLabel: String) {
        FirestoreHelper.updateWoundLabel(id, newLabel) {}
    }

    fun deleteAnalysis(id: String) {
        FirestoreHelper.deleteWoundAnalysis(id) {}
    }
}