package com.emreyildirim.matchhuntv1.data.model

import java.util.Date

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val sportType: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val maxParticipants: Int = 0,
    val participants: List<String> = emptyList(),
    val pendingRequests: List<String> = emptyList(),
    val createdBy: String = "",
    val creatorId: String = createdBy,
    val creatorUsername: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val endDate: Date = Date(),
    val creatorProfileImageUrl: String = ""
) 