package com.emreyildirim.matchhuntv1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.*

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Date = Date(),
    val isRead: Boolean = false
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Message? {
            return try {
                val timestamp = doc.getTimestamp("timestamp")
                Message(
                    id = doc.id,
                    senderId = doc.getString("senderId") ?: "",
                    receiverId = doc.getString("receiverId") ?: "",
                    text = doc.getString("text") ?: "",
                    timestamp = timestamp?.toDate() ?: Date(),
                    isRead = doc.getBoolean("isRead") ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
} 