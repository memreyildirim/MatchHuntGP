package com.emreyildirim.matchhuntv1.utils

object ChatUtils {
    fun getChatId(user1Id: String, user2Id: String): String {
        return if (user1Id < user2Id) {
            "${user1Id}_${user2Id}"
        } else {
            "${user2Id}_${user1Id}"
        }
    }
}