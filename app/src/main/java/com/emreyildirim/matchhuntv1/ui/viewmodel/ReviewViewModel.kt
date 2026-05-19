package com.emreyildirim.matchhuntv1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.data.repository.ReviewRepository
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ReviewViewModel : ViewModel() {
    private val reviewRepository = ReviewRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _reviewSubmitted = MutableStateFlow(false)
    val reviewSubmitted: StateFlow<Boolean> = _reviewSubmitted.asStateFlow()

    fun submitReview(eventId: String, skillRating: Float, behaviorRating: Float, teamRating: Float, comment: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Kullanıcı oturumu bulunamadı"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Etkinlik sahibinin ID'sini al
                val eventOwnerId = userRepository.getEventOwnerId(eventId).getOrNull()
                if (eventOwnerId == null) {
                    _error.value = "Etkinlik sahibi bulunamadı"
                    return@launch
                }

                // Önce kullanıcının bu etkinlik için daha önce değerlendirme yapıp yapmadığını kontrol et
                val existingReview = reviewRepository.getUserReviewForEventAndUser(currentUser.uid, eventOwnerId, eventId).getOrNull()
                if (existingReview != null) {
                    _error.value = "Bu etkinlik için zaten bir değerlendirme yapmışsınız"
                    return@launch
                }

                // Kullanıcı kendisini değerlendiremez
                if (eventOwnerId == currentUser.uid) {
                    _error.value = "Kendinizi değerlendiremezsiniz"
                    return@launch
                }

                // Kullanıcı adını al
                val userName = userRepository.getUserName(currentUser.uid).getOrNull()
                    ?: "Anonim Kullanıcı"

                // Review oluştur
                val review = Review(
                    eventId = eventId,
                    reviewerId = currentUser.uid,
                    reviewerName = userName,
                    reviewedUserId = eventOwnerId,
                    skillRating = skillRating,
                    behaviorRating = behaviorRating,
                    teamRating = teamRating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )

                reviewRepository.createReview(review)
                    .onSuccess {
                        _reviewSubmitted.value = true
                    }
                    .onFailure { e ->
                        _error.value = "Değerlendirme kaydedilemedi: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetReviewSubmitted() {
        _reviewSubmitted.value = false
    }

    fun createReview(
        reviewerId: String,
        reviewedUserId: String,
        eventId: String,
        skillRating: Float,
        behaviorRating: Float,
        teamRating: Float,
        comment: String
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val review = Review(
                    reviewerId = reviewerId,
                    reviewedUserId = reviewedUserId,
                    eventId = eventId,
                    skillRating = skillRating,
                    behaviorRating = behaviorRating,
                    teamRating = teamRating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )
                reviewRepository.createReview(review)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Bir hata oluştu"
                _isLoading.value = false
            }
        }
    }

    fun submitParticipantReview(
        eventId: String,
        participantId: String,
        skillRating: Float,
        behaviorRating: Float,
        teamRating: Float,
        comment: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Kullanıcı oturumu bulunamadı"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Önce kullanıcının bu etkinlik için daha önce değerlendirme yapıp yapmadığını kontrol et
                val existingReview = reviewRepository.getUserReviewForEventAndUser(currentUser.uid, participantId, eventId).getOrNull()
                if (existingReview != null) {
                    _error.value = "Bu katılımcıyı bu etkinlik için zaten değerlendirmişsiniz"
                    return@launch
                }

                // Kullanıcı kendisini değerlendiremez
                if (participantId == currentUser.uid) {
                    _error.value = "Kendinizi değerlendiremezsiniz"
                    return@launch
                }

                // Kullanıcı adını al
                val userName = userRepository.getUserName(currentUser.uid).getOrNull()
                    ?: "Anonim Kullanıcı"

                // Review oluştur
                val review = Review(
                    eventId = eventId,
                    reviewerId = currentUser.uid,
                    reviewerName = userName,
                    reviewedUserId = participantId,
                    skillRating = skillRating,
                    behaviorRating = behaviorRating,
                    teamRating = teamRating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )

                reviewRepository.createReview(review)
                    .onSuccess {
                        _reviewSubmitted.value = true
                    }
                    .onFailure { e ->
                        _error.value = "Değerlendirme kaydedilemedi: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 