package com.emreyildirim.matchhuntv1.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.data.repository.PostRepository
import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.data.model.Comment
import com.emreyildirim.matchhuntv1.utils.NetworkUtils
import com.emreyildirim.matchhuntv1.utils.withNetworkTimeout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.util.*

class SocialFeedViewModel(
    private val postViewModel: PostViewModel = PostViewModel()
) : ViewModel() {
    private val repository = PostRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _hasMorePosts = MutableStateFlow(true)
    val hasMorePosts: StateFlow<Boolean> = _hasMorePosts.asStateFlow()
    
    private val _navigateToProfile = MutableStateFlow<String?>(null)
    val navigateToProfile: StateFlow<String?> = _navigateToProfile.asStateFlow()
    
    // Yükleme işlemlerini takip etmek için
    private var initialLoadJob: Job? = null
    private var loadMoreJob: Job? = null
    
    // İlk yükleme yapıldı mı kontrolü
    private var isInitialLoadDone = false

    fun navigateToProfile(userId: String) {
        _navigateToProfile.value = userId
    }

    fun onProfileNavigationHandled() {
        _navigateToProfile.value = null
    }

    // İlk yükleme için
    fun loadPosts() {
        // Eğer zaten yükleme yapılıyorsa, işlem yapma
        if (_isLoading.value) return
        
        // Önceki işi iptal et
        initialLoadJob?.cancel()
        
        initialLoadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withNetworkTimeout {
                    repository.getPostsPaginated()
                }
                    .onSuccess { (posts, hasMore) ->
                        _posts.value = posts
                        _hasMorePosts.value = hasMore
                        isInitialLoadDone = true
                    }
                    .onFailure { e ->
                        _error.value = NetworkUtils.getErrorMessage(e)
                    }
            } catch (e: Exception) {
                _error.value = NetworkUtils.getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Daha fazla post yüklemek için
    fun loadMorePosts() {
        // Eğer zaten yükleme yapılıyorsa veya daha fazla post yoksa, işlem yapma
        if (_isLoadingMore.value || !_hasMorePosts.value || _posts.value.isEmpty()) return
        
        // Önceki işi iptal et
        loadMoreJob?.cancel()
        
        loadMoreJob = viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                // Son postu al
                val lastPost = _posts.value.lastOrNull()
                
                withNetworkTimeout {
                    repository.getPostsPaginated(lastPost)
                }
                    .onSuccess { (newPosts, hasMore) ->
                        // Yeni postları mevcut listeye ekle
                        _posts.value = _posts.value + newPosts
                        _hasMorePosts.value = hasMore
                    }
                    .onFailure { e ->
                        _error.value = NetworkUtils.getErrorMessage(e)
                    }
            } catch (e: Exception) {
                _error.value = NetworkUtils.getErrorMessage(e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }


    fun likePost(postId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Kullanıcı oturumu bulunamadı. Lütfen tekrar giriş yapın."
            return
        }

        // Optimistic update - önce UI'ı güncelle
        val originalPost = _posts.value.find { it.id == postId }
        val updatedPosts = _posts.value.map { post ->
            if (post.id == postId) {
                val likedBy = post.likedBy.toMutableList()
                val wasLiked = currentUser.uid in likedBy
                if (wasLiked) {
                    likedBy.remove(currentUser.uid)
                } else {
                    likedBy.add(currentUser.uid)
                }
                post.copy(likedBy = likedBy, likes = likedBy.size)
            } else {
                post
            }
        }
        _posts.value = updatedPosts

        // PostViewModel ile işlemi yap
        postViewModel.likePost(
            postId = postId,
            onSuccess = {
                // Başarılı olduğunda optimistic update zaten yapıldı
            },
            onError = { errorMsg ->
                // Hata durumunda optimistic update'i geri al
                originalPost?.let {
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) {
                            it // Orijinal post'u geri yükle
                        } else {
                            post
                        }
                    }
                }
                _error.value = errorMsg
            }
        )
    }
    
    fun addComment(postId: String, content: String) {
        // PostViewModel ile yorum ekle
        postViewModel.addComment(
            postId = postId,
            content = content,
            onSuccess = { comment ->
                // Başarılı olduğunda local state'i güncelle
                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(comments = post.comments + comment)
                    } else post
                }
            },
            onError = { errorMsg ->
                _error.value = errorMsg
            }
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        // ViewModel temizlendiğinde işleri iptal et
        initialLoadJob?.cancel()
        loadMoreJob?.cancel()
    }
}