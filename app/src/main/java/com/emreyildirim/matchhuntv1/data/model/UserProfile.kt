package com.emreyildirim.matchhuntv1.data.model

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val age: Int = 0,
    val city: String = "",
    val sports: List<String> = emptyList(),
    val profileImageUrl: String = "",
    val isProfileComplete: Boolean = false,
    val averageRating: Float = 0f,
    val about: String = "" // Kullanıcı hakkında bilgi
) 