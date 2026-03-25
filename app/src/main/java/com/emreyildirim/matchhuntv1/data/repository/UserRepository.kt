package com.emreyildirim.matchhuntv1.data.repository

import android.net.Uri
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.utils.updateSportsTopics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    internal val usersCollection = db.collection("users")
    private val storageRef = storage.reference.child("profile_images")
    private val eventsCollection = db.collection("events")



    suspend fun isProfileComplete(userId: String): Boolean? {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (!userDoc.exists()) {
                null
            } else {
                val data = userDoc.data ?: return null
                when {

                    // Geriye dönük uyumluluk için eski alan adı
                    data["isProfileComplete"] is Boolean -> data["isProfileComplete"] as Boolean
                    // Alan yoksa yönlendirme kararı verme (yanlışlıkla createProfile'e gitmesin)
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createUserProfile(
        userId: String,
        username: String,
        age: Int,
        city: String,
        sports: List<String>,
        about: String = ""
    ): Boolean {
        return try {
            // 1) Eski spor listesini al (varsa)
            val existingDoc = usersCollection.document(userId).get().await()
            val oldSports = (existingDoc.get("sports") as? List<String>) ?: emptyList()

            // 2) Yeni spor listesini normalize et (lowercase)
            val normalizedSports = sports.map { it.lowercase() }

            println("Creating user profile with about: $about") // Debug log
            val userData = hashMapOf(
                "username" to username,
                "age" to age,
                "city" to city,
                "sports" to normalizedSports,
                "about" to about,
                "isProfileComplete" to true
            )
            println("User data to be saved: $userData") // Debug log

            // 3) Firestore'a yaz
            usersCollection.document(userId).set(userData, SetOptions.merge()).await()
            println("User profile created/updated successfully") // Debug log

            // 4) Topic aboneliklerini güncelle (unsubscribe + subscribe)
            updateSportsTopics(
                oldSports = oldSports,
                newSports = normalizedSports
            )

            true
        } catch (e: Exception) {
            println("Error creating/updating user profile: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun getUserProfileData(userId: String): Map<String, Any>? {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            userDoc.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        try {
            // Eski fotoğrafı sil
            val userDoc = usersCollection.document(userId).get().await()
            val oldPhotoUrl = userDoc.data?.get("profileImageUrl") as? String
            if (!oldPhotoUrl.isNullOrEmpty()) {
                try {
                    deleteProfileImage(oldPhotoUrl)
                } catch (e: Exception) {
                    // Eski fotoğraf silinirken hata oluşursa devam et
                    println("Eski fotoğraf silinirken hata: ${e.message}")
                }
            }

            // Yeni fotoğrafı yükle
            val imageRef = storageRef.child("$userId/${UUID.randomUUID()}")
            val uploadTask = imageRef.putFile(imageUri).await()
            return imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Profil fotoğrafı yüklenirken bir hata oluştu: ${e.message}")
        }
    }

    suspend fun updateProfileImage(userId: String, imageUrl: String) {
        usersCollection.document(userId)
            .update("profileImageUrl", imageUrl)
            .await()
    }

    suspend fun deleteProfileImage(imageUrl: String) {
        try {
            // Firebase Storage'dan fotoğrafı sil
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
        } catch (e: Exception) {
            // Hata durumunda loglama yapılabilir
            println("Error deleting profile image: ${e.message}")
        }
    }

    suspend fun getEventOwnerId(eventId: String): Result<String> {
        return try {
            val eventDoc = eventsCollection.document(eventId).get().await()
            val creatorId = eventDoc.getString("createdBy")
            if (creatorId != null) {
                Result.success(creatorId)
            } else {
                Result.failure(Exception("Etkinlik sahibi bulunamadı"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserName(userId: String): Result<String> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val username = userDoc.getString("username")
            if (username != null) {
                Result.success(username)
            } else {
                Result.failure(Exception("Kullanıcı adı bulunamadı"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Batch olarak birden fazla kullanıcı adını al (N+1 query problemini çözmek için)
    // Paralel olarak tüm kullanıcı adlarını alır
    suspend fun getUserNames(userIds: List<String>): Map<String, String> {
        return try {
            if (userIds.isEmpty()) return emptyMap()
            
            // Tüm kullanıcı adlarını paralel olarak al
            coroutineScope {
                userIds.map { userId ->
                    async {
                        userId to (getUserName(userId).getOrNull() ?: "Anonim")
                    }
                }.map { it.await() }.toMap()
            }
        } catch (e: Exception) {
            // Hata durumunda fallback: her birini tek tek al (sequential)
            userIds.associateWith { userId ->
                getUserName(userId).getOrNull() ?: "Anonim"
            }
        }
    }

    suspend fun getEvent(eventId: String): Result<Event> {
        return try {
            val eventDoc = eventsCollection.document(eventId).get().await()
            val event = eventDoc.toObject(Event::class.java)
            if (event != null) {
                Result.success(event)
            } else {
                Result.failure(Exception("Etkinlik bulunamadı"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val userProfile = userDoc.toObject(UserProfile::class.java)
            if (userProfile != null) {
                Result.success(userProfile)
            } else {
                Result.failure(Exception("Kullanıcı profili bulunamadı"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmailVerificationStatus(userId: String, isVerified: Boolean) {
        try {
            db.collection("users")
                .document(userId)
                .update("isEmailVerified", isVerified)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }
} 