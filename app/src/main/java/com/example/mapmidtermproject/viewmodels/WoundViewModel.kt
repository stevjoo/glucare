package com.example.mapmidtermproject.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mapmidtermproject.models.WoundAnalysis
import com.example.mapmidtermproject.repositories.LocalWoundImage
import com.example.mapmidtermproject.repositories.WoundRepository
import com.example.mapmidtermproject.utils.Event
import com.example.mapmidtermproject.utils.WoundClassifierHelper
import java.io.File

class WoundViewModel(application: Application) : AndroidViewModel(application), WoundClassifierHelper.ClassifierListener {

    private val repository = WoundRepository(application.applicationContext)
    private val woundClassifier = WoundClassifierHelper(application.applicationContext, this)

    // --- DATA UNTUK MAIN ACTIVITY ---
    private val _historyList = MutableLiveData<List<WoundAnalysis>>()
    val historyList: LiveData<List<WoundAnalysis>> = _historyList

    private val _statsData = MutableLiveData<Map<String, Int>>()
    val statsData: LiveData<Map<String, Int>> = _statsData

    // --- DATA ANALYSIS ACTIVITY ---
    private val _analysisResult = MutableLiveData<Event<String>>()
    val analysisResult: LiveData<Event<String>> get() = _analysisResult

    private val _isDiabeticDetected = MutableLiveData<Boolean>()
    val isDiabeticDetected: LiveData<Boolean> get() = _isDiabeticDetected

    // Local Gallery
    private val _woundImages = MutableLiveData<List<LocalWoundImage>>()
    val woundImages: LiveData<List<LocalWoundImage>> = _woundImages

    // Variabel sementara untuk menyimpan hasil scan terakhir sebelum user klik save
    var lastLabel: String = ""
    var lastConfidence: Float = 0f

    // LOAD HISTORY (Dipanggil di MainActivity)
    fun loadHistory() {
        repository.getWoundHistory { list ->
            _historyList.value = list
            calculateStats(list) // Hitung statistik untuk grafik
        }
    }

    // HITUNG STATISTIK (Untuk Bar Chart)
    private fun calculateStats(list: List<WoundAnalysis>) {
        // Mengelompokkan berdasarkan Label dan menghitung jumlahnya
        val stats = list.groupingBy { it.label }.eachCount()
        _statsData.value = stats
    }

    // ANALISIS GAMBAR (Dipanggil di AnalysisActivity)
    fun analyzeImage(uri: Uri) {
        woundClassifier.classify(uri)
    }

    // Callback dari ML Helper
    override fun onResults(label: String, score: Float) {
        // Simpan sementara di memori
        lastLabel = label
        lastConfidence = score

        val scorePercent = score * 100
        val isDiabetic = label.contains("Diabetic", ignoreCase = true) ||
                label.contains("Ulcer", ignoreCase = true)

        _isDiabeticDetected.postValue(isDiabetic)

        val textResult = if (score < 0.50f) {
            "❓ Tidak Yakin (${String.format("%.1f", scorePercent)}%)\nCoba foto ulang."
        } else {
            "$label\n(Akurasi: ${String.format("%.1f", scorePercent)}%)"
        }
        _analysisResult.postValue(Event(textResult))
    }

    override fun onError(error: String) {
        _analysisResult.postValue(Event("Error: $error"))
    }

    // SIMPAN HASIL (Local + Firestore)
    fun saveResult(uri: Uri) {
        // Simpan ke File Lokal
        val localPath = repository.saveImageToInternalStorage(uri)

        // Jika sukses simpan lokal, simpan metadatanya ke Firestore
        if (localPath != null && lastLabel.isNotEmpty()) {
            repository.saveAnalysisResult(lastLabel, lastConfidence, localPath)
        }
        // Refresh local gallery data
        loadImages()
    }

    fun loadImages() {
        _woundImages.postValue(repository.getAllImages())
    }

    fun deleteImage(file: File) {
        repository.deleteImage(file)
        loadImages()
    }

    fun updateWoundLabel(item: WoundAnalysis, newLabel: String) {
        repository.updateAnalysisLabel(item.id, newLabel)
    }

    fun deleteWoundItem(item: WoundAnalysis) {
        if (item.localImagePath.isNotEmpty()) {
            val file = File(item.localImagePath)
            if (file.exists()) {
                file.delete()
            }
        }

        repository.deleteAnalysis(item.id)
    }
}