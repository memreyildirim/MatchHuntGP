package com.emreyildirim.matchhuntv1.utils

import com.google.firebase.messaging.FirebaseMessaging

fun updateSportsTopics(oldSports: List<String>, newSports: List<String>) {
    val oldSet = oldSports.map { it.lowercase() }.toSet()
    val newSet = newSports.map { it.lowercase() }.toSet()

    val toUnsubscribe = oldSet - newSet
    val toSubscribe = newSet - oldSet

    val fcm = FirebaseMessaging.getInstance()

    // Eski ama artık seçili olmayan sporlar için unsubscribe
    toUnsubscribe.forEach { sport ->
        val topic = "events_$sport"
        fcm.unsubscribeFromTopic(topic)
    }

    // Yeni seçili sporlar için subscribe
    toSubscribe.forEach { sport ->
        val topic = "events_$sport"
        fcm.subscribeToTopic(topic)
    }
}