package com.emreyildirim.matchhuntv1.data.model

import java.util.Date

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val createdAt: Date = Date()
) 