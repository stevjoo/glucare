package com.example.mapmidtermproject.repositories

import com.example.mapmidtermproject.utils.FirestoreHelper
import com.example.mapmidtermproject.utils.FoodLog
import com.google.firebase.firestore.ListenerRegistration

class FoodRepository {
    fun addFoodLog(food: String, sugar: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        FirestoreHelper.saveFoodLog(food, sugar, onSuccess, onFailure)
    }

    fun getFoodLogs(onResult: (List<FoodLog>) -> Unit): ListenerRegistration? {
        return FirestoreHelper.listenToFoodLogs(onResult)
    }

    fun deleteFoodLog(logId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        FirestoreHelper.deleteFoodLog(logId, onSuccess, onFailure)
    }
}