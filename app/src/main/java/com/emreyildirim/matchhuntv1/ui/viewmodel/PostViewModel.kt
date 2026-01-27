package com.emreyildirim.matchhuntv1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.data.model.Comment
import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.data.repository.PostRepository
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class PostViewModel : ViewModel() {
    private val repository = PostRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _deleteSuccess = MutableStateFlow<String?>(null)
    val deleteSuccess: StateFlow<String?> = _deleteSuccess.asStateFlow()

    private val _likeError = MutableStateFlow<String?>(null)
    val likeError: StateFlow<String?> = _likeError.asStateFlow()

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError.asStateFlow()

    fun deletePost(postId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _deleteError.value = "Kullanıcı oturumu bulunamadı"
                return@launch
            }

            _isDeleting.value = true
            _deleteError.value = null
            _deleteSuccess.value = null

            repository.deletePost(postId, currentUserId)
                .onSuccess {
                    _isDeleting.value = false
                    _deleteSuccess.value = postId
                    onSuccess(postId)
                }
                .onFailure { error ->
                    _isDeleting.value = false
                    _deleteError.value = error.message ?: "Post silinirken bir hata oluştu"
                }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun clearDeleteSuccess() {
        _deleteSuccess.value = null
    }

    /**
     * Post beğenme/beğenmeme işlemi
     * @param postId Beğenilecek post ID
     * @param onSuccess Başarılı olduğunda çağrılacak callback
     * @param onError Hata durumunda çağrılacak callback
     */
    fun likePost(
        postId: String, 
        onSuccess: () -> Unit = {}, 
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                val errorMsg = "Kullanıcı oturumu bulunamadı"
                _likeError.value = errorMsg
                onError(errorMsg)
                return@launch
            }

            _likeError.value = null

            repository.likePost(postId, currentUserId)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    val errorMsg = error.message ?: "Beğeni işlemi sırasında bir hata oluştu"
                    _likeError.value = errorMsg
                    onError(errorMsg)
                }
        }
    }

    /**
     * Yorum ekleme işlemi - content string ile
     * @param postId Yorum eklenecek post ID
     * @param content Yorum içeriği
     * @param onSuccess Başarılı olduğunda çağrılacak callback. Oluşturulan comment'i döner
     * @param onError Hata durumunda çağrılacak callback
     */
    fun addComment(
        postId: String, 
        content: String, 
        onSuccess: (Comment) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    val errorMsg = "Kullanıcı oturumu bulunamadı"
                    _commentError.value = errorMsg
                    onError(errorMsg)
                    return@launch
                }

                val currentUser = userRepository.getUserProfileData(currentUserId)
                val userName = currentUser?.get("username") as? String ?: "Unknown User"

                val comment = Comment(
                    id = UUID.randomUUID().toString(),
                    postId = postId,
                    userId = currentUserId,
                    userName = userName,
                    content = content,
                    createdAt = Date()
                )

                repository.addComment(postId, comment)
                _commentError.value = null
                onSuccess(comment)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Yorum eklenirken bir hata oluştu"
                _commentError.value = errorMsg
                onError(errorMsg)
            }
        }
    }

    fun clearLikeError() {
        _likeError.value = null
    }

    fun clearCommentError() {
        _commentError.value = null
    }
}

