package com.emreyildirim.matchhuntv1.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.data.repository.PostRepository
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {
    private val repository = PostRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _postCreated = MutableStateFlow(false)
    val postCreated: StateFlow<Boolean> = _postCreated.asStateFlow()

    fun createPost(
        imageUri: Uri,
        description: String,
        sportType: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _postCreated.value = false
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _error.value = "Kullanıcı oturumu bulunamadı. Lütfen tekrar giriş yapın."
                    return@launch
                }

                val userData = userRepository.getUserProfileData(currentUser.uid)
                if (userData == null) {
                    _error.value = "Kullanıcı profili bulunamadı. Lütfen profil bilgilerinizi tamamlayın."
                    return@launch
                }

                val userName = userData["username"] as? String ?: ""

                if (userName.isBlank()) {
                    _error.value = "Kullanıcı adı bulunamadı. Lütfen profil bilgilerinizi tamamlayın."
                    return@launch
                }

                repository.createPost(
                    userId = currentUser.uid,
                    userName = userName,
                    imageUri = imageUri,
                    description = description,
                    sportType = sportType
                ).onSuccess {
                    _postCreated.value = true
                }.onFailure { e ->
                    _error.value = "Post oluşturulurken bir hata oluştu: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Beklenmeyen bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 