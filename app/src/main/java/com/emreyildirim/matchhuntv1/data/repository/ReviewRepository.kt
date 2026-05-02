package com.emreyildirim.matchhuntv1.data.repository

import android.util.Log
import com.emreyildirim.matchhuntv1.BuildConfig
import com.emreyildirim.matchhuntv1.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.firestore.Query

class ReviewRepository {
    private val tag = "ReviewRepository"
    private val db = FirebaseFirestore.getInstance()
    private val reviewsCollection = db.collection("reviews")
    private val usersCollection = db.collection("users")
    private val userRepository = UserRepository()

    suspend fun createReview(review: Review): Result<Unit> {
        return try {
            val reviewId = UUID.randomUUID().toString()
            val reviewWithId = review.copy(id = reviewId)
            
            // Review'ı kaydet
            reviewsCollection.document(reviewId).set(reviewWithId).await()
            
            // Kullanıcının ortalama puanlarını güncelle
            updateUserRatings(review.reviewedUserId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(tag, "Error creating review", e)
            Result.failure(e)
        }
    }

    suspend fun getUserReviews(userId: String): List<Review> {
        return try {
            val reviewsSnapshot = reviewsCollection
                .whereEqualTo("reviewedUserId", userId)
                .get()
                .await()

            // Tüm reviewer ID'lerini topla (unique)
            val reviewerIds = reviewsSnapshot.documents
                .mapNotNull { it.getString("reviewerId") }
                .distinct()

            // Tüm reviewer isimlerini paralel olarak al (N+1 query problemini çöz)
            val reviewerNames = userRepository.getUserNames(reviewerIds)

            // Review'ları oluştur
            val sortedReviews = reviewsSnapshot.documents.mapNotNull { reviewDoc ->
                val reviewerId = reviewDoc.getString("reviewerId") ?: return@mapNotNull null
                val reviewerName = reviewerNames[reviewerId] ?: "Anonim"
                val timestamp = reviewDoc.getLong("timestamp") ?: System.currentTimeMillis()

                Review(
                    id = reviewDoc.id,
                    reviewerId = reviewerId,
                    reviewerName = reviewerName,
                    reviewedUserId = reviewDoc.getString("reviewedUserId") ?: "",
                    eventId = reviewDoc.getString("eventId") ?: "",
                    skillRating = reviewDoc.getDouble("skillRating")?.toFloat() ?: 0f,
                    behaviorRating = reviewDoc.getDouble("behaviorRating")?.toFloat() ?: 0f,
                    teamRating = reviewDoc.getDouble("teamRating")?.toFloat() ?: 0f,
                    comment = reviewDoc.getString("comment") ?: "",
                    timestamp = timestamp
                )
            }.sortedByDescending { it.timestamp }

            sortedReviews
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(tag, "Error getting user reviews: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUserReviewForEvent(userId: String, eventId: String): Result<Review?> {
        return try {
            val review = reviewsCollection
                .whereEqualTo("reviewerId", userId)
                .whereEqualTo("eventId", eventId)
                .get()
                .await()
                .toObjects(Review::class.java)
                .firstOrNull()
            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRatings(userId: String) {
        try {
            val reviews = getUserReviews(userId).map { it.skillRating }
            if (reviews.isEmpty()) return

            val averageSkillRating = reviews.average().toFloat()
            val averageBehaviorRating = reviews.average().toFloat()
            val averageTeamRating = reviews.average().toFloat()
            val averageRating = reviews.average().toFloat()
            
            // Update the user document with the new average ratings
            usersCollection.document(userId)
                .set(
                    mapOf(
                        "averageSkillRating" to averageSkillRating,
                        "averageBehaviorRating" to averageBehaviorRating,
                        "averageTeamRating" to averageTeamRating,
                        "averageRating" to averageRating
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(tag, "Error updating user ratings", e)
        }
    }

    suspend fun getUserRatings(userId: String): Result<Map<String, Float>> {
        return try {
            // Kullanıcının tüm değerlendirmelerini al
            val reviews = getUserReviews(userId)
            if (reviews.isEmpty()) {
                return Result.success(
                    mapOf(
                        "skill" to 0f,
                        "behavior" to 0f,
                        "team" to 0f,
                        "average" to 0f
                    )
                )
            }

            // Değerlendirmelerden ortalama puanları hesapla
            val averageSkillRating = reviews.map { it.skillRating }.average().toFloat()
            val averageBehaviorRating = reviews.map { it.behaviorRating }.average().toFloat()
            val averageTeamRating = reviews.map { it.teamRating }.average().toFloat()
            val averageRating = reviews.map { it.averageRating }.average().toFloat()

            // Kullanıcı dokümanını güncelle
            usersCollection.document(userId)
                .set(
                    mapOf(
                        "averageSkillRating" to averageSkillRating,
                        "averageBehaviorRating" to averageBehaviorRating,
                        "averageTeamRating" to averageTeamRating,
                        "averageRating" to averageRating
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            Result.success(
                mapOf(
                    "skill" to averageSkillRating,
                    "behavior" to averageBehaviorRating,
                    "team" to averageTeamRating,
                    "average" to averageRating
                )
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(tag, "Error getting user ratings", e)
            Result.failure(e)
        }
    }

    // getUserRatings ve getTotalReviews'i birleştiren optimize edilmiş fonksiyon
    suspend fun getUserRatingsAndCount(userId: String): Result<Pair<Map<String, Float>, Int>> {
        return try {
            // Kullanıcının tüm değerlendirmelerini al (sadece bir kez)
            val reviews = getUserReviews(userId)
            
            if (reviews.isEmpty()) {
                return Result.success(
                    Pair(
                        mapOf(
                            "skill" to 0f,
                            "behavior" to 0f,
                            "team" to 0f,
                            "average" to 0f
                        ),
                        0
                    )
                )
            }

            // Değerlendirmelerden ortalama puanları hesapla
            val averageSkillRating = reviews.map { it.skillRating }.average().toFloat()
            val averageBehaviorRating = reviews.map { it.behaviorRating }.average().toFloat()
            val averageTeamRating = reviews.map { it.teamRating }.average().toFloat()
            val averageRating = reviews.map { it.averageRating }.average().toFloat()

            val ratings = mapOf(
                "skill" to averageSkillRating,
                "behavior" to averageBehaviorRating,
                "team" to averageTeamRating,
                "average" to averageRating
            )

            // Kullanıcı dokümanını güncelle
            usersCollection.document(userId)
                .set(
                    mapOf(
                        "averageSkillRating" to averageSkillRating,
                        "averageBehaviorRating" to averageBehaviorRating,
                        "averageTeamRating" to averageTeamRating,
                        "averageRating" to averageRating
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            Result.success(Pair(ratings, reviews.size))
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(tag, "Error getting user ratings and count", e)
            Result.failure(e)
        }
    }

    suspend fun getTotalReviews(userId: String): Result<Int> {
        return try {
            val reviews = getUserReviews(userId)
            Result.success(reviews.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 