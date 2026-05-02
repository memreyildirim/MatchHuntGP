package com.emreyildirim.matchhuntv1.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationService : FirebaseMessagingService() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (BuildConfig.DEBUG) {
            Log.d("FCM", "New token received")
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            if (BuildConfig.DEBUG) {
                Log.d("FCM", "No user logged in, skipping token save")
            }
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf("fcmToken" to token),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                if (BuildConfig.DEBUG) {
                    Log.d("FCM", "Token saved to Firestore")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error saving token: ${e.message}")
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "MatchHunt"
        val body  = message.notification?.body  ?: message.data["body"]  ?: "Bildirim içeriği"
        val eventId = message.data["eventId"]
        val type = message.data["type"]

        if (BuildConfig.DEBUG) {
            Log.d(
                "FCM",
                "Push received. titlePresent=${title.isNotBlank()}, bodyPresent=${body.isNotBlank()}, dataSize=${message.data.size}"
            )
        }

        // Eğer bildirim bir etkinlik bildirimi ise ya da etkinlik güncelleme bildirimi ise, oluşturucuyu kontrol et
        if ((type == "event" || type == "event_update") && eventId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val shouldShow = checkIfShouldShowNotification(eventId)
                if (shouldShow) {
                    showNotification(title, body)
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d("FCM", "Notification filtered for event creator")
                    }
                }
            }
        } else {
            // Etkinlik bildirimi değilse normal şekilde göster
            showNotification(title, body)
        }
    }

    private suspend fun checkIfShouldShowNotification(eventId: String): Boolean {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                // Kullanıcı giriş yapmamışsa bildirimi göster
                return true
            }

            val eventDoc = db.collection("events").document(eventId).get().await()
            if (!eventDoc.exists()) {
                // Etkinlik bulunamazsa bildirimi göster (güvenlik için)
                return true
            }

            val eventData = eventDoc.data
            val creatorId = eventData?.get("createdBy") as? String
                ?: eventData?.get("creatorId") as? String

            // Eğer mevcut kullanıcı oluşturucu ise bildirimi gösterme
            creatorId != currentUserId
        } catch (e: Exception) {
            Log.e("FCM", "Error checking event creator: ${e.message}")
            // Hata durumunda bildirimi göster (güvenlik için)
            true
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "default_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Genel Bildirimler",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notif =  NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logolastcircle)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        // Android 13+ cihazlarda bildirim izni runtime'da reddedilebildigi icin
        // notify cagrisindan once izin kontrolu zorunlu.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (BuildConfig.DEBUG) Log.w("FCM", "Notification permission not granted; skipping notify")
            return
        }

        try {
            NotificationManagerCompat.from(this).notify(1001, notif)
        } catch (se: SecurityException) {
            Log.w("FCM", "Notification permission missing at runtime", se)
        }
    }
}