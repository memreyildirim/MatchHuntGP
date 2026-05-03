package com.emreyildirim.matchhuntv1.data.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.*

data class Conversation(
    val id: String = "", // This will be the chatId
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Date = Date(),
    val unreadCounts: Map<String, Long> = emptyMap() // Map of userId to their unread count
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Conversation? {
            return try {
                val timestamp = doc.getTimestamp("lastMessageTimestamp")
                Conversation(
                    id = doc.id,
                    participants = doc.get("participants") as? List<String> ?: emptyList(),
                    lastMessage = doc.getString("lastMessage") ?: "",
                    lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
                    lastMessageTimestamp = timestamp?.toDate() ?: Date(),
                    unreadCounts = doc.get("unreadCounts") as? Map<String, Long> ?: emptyMap()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
