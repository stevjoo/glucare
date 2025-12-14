package com.example.mapmidtermproject.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapmidtermproject.repositories.FoodRepository
import com.example.mapmidtermproject.utils.FoodLog
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

class LogViewModel : ViewModel() {
    private val repository = FoodRepository()
    private val _logs = MutableLiveData<List<FoodLog>>()
    val logs: LiveData<List<FoodLog>> = _logs
    private var listener: ListenerRegistration? = null

    fun startListening() {
        listener = repository.getFoodLogs { newLogs ->
            _logs.value = newLogs
        }
    }

    fun stopListening() {
        listener?.remove()
    }

    fun saveLog(food: String, sugar: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.addFoodLog(food, sugar, onSuccess) { e ->
            onFailure(e.message ?: "Gagal menyimpan")
        }
    }

    fun deleteLog(logId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.deleteFoodLog(logId, onSuccess) { e ->
            onFailure(e.message ?: "Gagal menghapus")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    fun updateLog(logId: String, food: String, sugar: Int, date: Date, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        com.example.mapmidtermproject.utils.FirestoreHelper.updateFoodLog(logId, food, sugar, date, onSuccess) { e ->
            onFailure(e.message ?: "Gagal update")
        }
    }
}