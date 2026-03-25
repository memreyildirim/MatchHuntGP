package com.emreyildirim.matchhuntv1.data.model

import java.util.Date

data class Report(
    val id: String = "",
    val type: String = "",          // "profile", "post", "message"
    val targetId: String = "",      // userId, postId veya messageId
    val targetUserId: String = "",  // şikayet edilen kullanıcının id'si
    val reporterId: String = "",    // şikayeti yapan kullanıcının id'si
    val chatId: String? = null,     // mesaj şikayetlerinde konuşma id'si
    val postSnippet: String? = null,
    val messageSnippet: String? = null,
    val profileSnippet: String? = null,
    val reasonCode: String = "",    // "spam", "abuse", "other" vb.
    val reasonText: String = "",
    val createdAt: Date = Date(),
    val status: String = "open"     // "open", "reviewed", "resolved" vb.
)

