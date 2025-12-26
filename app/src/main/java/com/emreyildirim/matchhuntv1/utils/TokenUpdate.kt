package com.emreyildirim.matchhuntv1.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object TokenUpdate {

    //fcm token önceden kaydedilmemiş olabileeği için tekraradan bi girişte çağırmak için
    fun updateUserFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener

                val token = task.result ?: return@addOnCompleteListener

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(
                        mapOf("fcmToken" to token),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        Log.d("FCM", "Token updated from updateUserFcmToken")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Error updating token: ${e.message}")
                    }
            }
    }
}