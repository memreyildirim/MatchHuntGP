package com.emreyildirim.matchhuntv1.data.model

import java.util.Date

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val sportType: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val createdAt: Date = Date()
)