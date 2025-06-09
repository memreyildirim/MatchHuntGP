package com.emreyildirim.matchhuntv1.data.repository

import com.emreyildirim.matchhuntv1.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EventRepository {
    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("events")

    suspend fun getEventById(eventId: String): Event? {
        return try {
            val eventDoc = eventsCollection.document(eventId).get().await()
            eventDoc.toObject(Event::class.java)
        } catch (e: Exception) {
            println("Error getting event: ${e.message}")
            null
        }
    }
} 