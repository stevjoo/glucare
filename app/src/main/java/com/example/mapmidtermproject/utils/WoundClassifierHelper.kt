package com.example.mapmidtermproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WoundClassifierHelper(
    private val context: Context,
    private val classifierListener: ClassifierListener
) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    // ResNet50 WAJIB 299x299
    private val inputSize = 299

    // Nilai Rata-rata ImageNet (Standar ResNet50)
    // Model ini mengurangi setiap pixel dengan angka ini
    private val MEAN_B = 103.939f
    private val MEAN_G = 116.779f
    private val MEAN_R = 123.68f

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "wound_model.tflite")
            val options = Interpreter.Options()
            options.setNumThreads(2)
            interpreter = Interpreter(modelFile, options)

            // Pastikan labels.txt urutannya sesuai abjad (Abrasions, Bruises, ..., Venous)
            labels = FileUtil.loadLabels(context, "labels.txt")
            Log.d("WoundClassifier", "Model loaded. Labels: $labels")

        } catch (e: Exception) {
            classifierListener.onError("Init failed: ${e.message}")
            Log.e("WoundClassifier", "TFLite Init failed", e)
        }
    }

    fun classify(imageUri: Uri) {
        if (interpreter == null) {
            setupClassifier()
        }

        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) return

            // Resize Manual ke 299x299
            bitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

            // Siapin ByteBuffer untuk input model (Ukuran: 1 x 299 x 299 x 3 x 4bytes)
            val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            // Konversi Pixel Manual (RGB -> BGR & Mean Subtraction)
            val intValues = IntArray(inputSize * inputSize)
            bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

            // Loop semua pixel
            var pixel = 0
            for (i in 0 until inputSize) {
                for (j in 0 until inputSize) {
                    val value = intValues[pixel++]

                    // Ekstrak warna RGB dari hex code Android
                    val r = ((value shr 16) and 0xFF).toFloat()
                    val g = ((value shr 8) and 0xFF).toFloat()
                    val b = (value and 0xFF).toFloat()

                    // Masukin ke buffer dengan urutan BGR
                    // Dan kurangi dengan Mean (Standar ResNet)
                    inputBuffer.putFloat(b - MEAN_B)
                    inputBuffer.putFloat(g - MEAN_G)
                    inputBuffer.putFloat(r - MEAN_R)
                }
            }

            // Siapkan Output
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.FLOAT32)

            // Run Inference
            interpreter?.run(inputBuffer, outputBuffer.buffer.rewind())

            // Baca Hasil
            val rawScores = outputBuffer.floatArray

            // DEBUG: Liat skor mentah di Logcat
            Log.d("WoundDebug", "RAW SCORES: ${rawScores.joinToString()}")

            var maxIndex = 0
            var maxScore = -Float.MAX_VALUE // Mulai dari minus tak hingga karena skor bisa negatif/kecil

            for (i in rawScores.indices) {
                if (rawScores[i] > maxScore) {
                    maxScore = rawScores[i]
                    maxIndex = i
                }
            }

            // Ambil nama label
            val labelFound = if (labels.isNotEmpty() && maxIndex < labels.size) {
                labels[maxIndex]
            } else {
                "Unknown"
            }
            classifierListener.onResults(labelFound, maxScore)

        } catch (e: Exception) {
            classifierListener.onError("Error: ${e.message}")
            Log.e("WoundClassifier", "Error Process", e)
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(label: String, score: Float)
    }
}