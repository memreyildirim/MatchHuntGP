package com.emreyildirim.matchhuntv1.data.model

data class Review(
    val id: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewedUserId: String = "",
    val eventId: String = "",
    val skillRating: Float = 0f,      // Beceri/Yetenek puanı
    val behaviorRating: Float = 0f,   // Davranış/Saygı puanı
    val teamRating: Float = 0f,       // Uyum/Takım iletişimi puanı
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    // Toplam ortalama puanı hesapla
    val averageRating: Float
        get() = (skillRating + behaviorRating + teamRating) / 3f
} 