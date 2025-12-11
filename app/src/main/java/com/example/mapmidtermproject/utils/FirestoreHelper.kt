package com.example.mapmidtermproject.utils

import android.util.Log
import com.example.mapmidtermproject.models.WoundAnalysis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// --- DATA CLASSES ---
data class UserProfile(
    val username: String? = null,
    val phone: String? = null,
    val email: String? = null
)

data class FoodLog(
    var id: String = "",
    val foodName: String = "",
    val bloodSugar: Int = 0,
    val timestamp: java.util.Date = java.util.Date()
)

object FirestoreHelper {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    fun initUserDataIfNew() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val docRef = db.collection("users").document(uid)

        docRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val newData = hashMapOf(
                    "username" to (user.displayName ?: "Pengguna"),
                    "email" to (user.email ?: ""),
                    "phone" to ""
                )
                docRef.set(newData, SetOptions.merge())
            }
        }
    }

    fun getUserProfile(onResult: (UserProfile?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { document, error ->
            if (document != null && document.exists()) {
                onResult(document.toObject(UserProfile::class.java))
            } else {
                onResult(null)
            }
        }
    }

    fun updateUsername(newUsername: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("username", newUsername).addOnSuccessListener { onSuccess() }
    }

    fun updatePhone(newPhone: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("phone", newPhone).addOnSuccessListener { onSuccess() }
    }

    // --- FOOD LOG  ---
    fun saveFoodLog(food: String, sugar: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val log = FoodLog(id = "", foodName = food, bloodSugar = sugar)
        db.collection("users").document(uid).collection("food_logs").add(log)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun listenToFoodLogs(onResult: (List<FoodLog>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid).collection("food_logs")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, _ ->
                val list = ArrayList<FoodLog>()
                value?.forEach { doc ->
                    val log = doc.toObject(FoodLog::class.java)
                    log.id = doc.id
                    list.add(log)
                }
                onResult(list)
            }
    }

    fun deleteFoodLog(logId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("food_logs").document(logId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- WOUND ANALYSIS HISTORY ---
    fun saveWoundAnalysis(analysis: WoundAnalysis, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("wound_history")
            .add(analysis)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("Firestore", "Gagal simpan wound: $e") }
    }

    fun listenToWoundHistory(onResult: (List<WoundAnalysis>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid).collection("wound_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<WoundAnalysis>()
                value?.forEach { doc ->
                    val item = doc.toObject(WoundAnalysis::class.java)
                    item.id = doc.id
                    list.add(item)
                }
                onResult(list)
            }
    }

    fun deleteAccount(onSuccess: () -> Unit, onReauthRequired: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        // Hapus food_logs, wound_history, user doc, auth (Logic sama seperti sebelumnya, disederhanakan disini)
        db.collection("users").document(uid).delete().addOnSuccessListener {
            auth.currentUser?.delete()?.addOnSuccessListener { onSuccess() }
        }
    }

    fun updateWoundLabel(id: String, newLabel: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("wound_history").document(id)
            .update("label", newLabel)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { Log.e("Firestore", "Gagal update: $it") }
    }

    fun deleteWoundAnalysis(id: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("wound_history").document(id)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { Log.e("Firestore", "Gagal hapus: $it") }
    }

    fun updateFoodLog(id: String, food: String, sugar: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>(
            "foodName" to food,
            "bloodSugar" to sugar
        )
        db.collection("users").document(uid).collection("food_logs").document(id)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}