package com.example.mapmidtermproject.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.mapmidtermproject.models.WoundAnalysis
import com.example.mapmidtermproject.utils.FirestoreHelper
import com.google.firebase.firestore.ListenerRegistration
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

            file.absolutePath // Kembalikan Lokasi File
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveAnalysisResult(label: String, confidence: Float, localPath: String) {
        val analysis = WoundAnalysis(
            label = label,
            confidence = confidence,
            localImagePath = localPath,
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