package com.emreyildirim.matchhuntv1.data.repository

import android.util.Log
import com.emreyildirim.matchhuntv1.BuildConfig
import com.emreyildirim.matchhuntv1.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EventRepository {
    private val tag = "EventRepository"
    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("events")

    suspend fun getEventById(eventId: String): Event? {
        return try {
            val eventDoc = eventsCollection.document(eventId).get().await()
            eventDoc.toObject(Event::class.java)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(tag, "Error getting event: ${e.message}")
            null
        }
    }
} 