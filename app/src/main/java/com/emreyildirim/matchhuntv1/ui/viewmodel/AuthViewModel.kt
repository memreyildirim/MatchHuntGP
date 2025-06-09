package com.emreyildirim.matchhuntv1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException
import com.emreyildirim.matchhuntv1.data.repository.UserRepository

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _isProfileComplete = MutableStateFlow(false)
    val isProfileComplete: StateFlow<Boolean> = _isProfileComplete.asStateFlow()
    
    private fun handleError(e: Exception): String {
        return when (e) {
            is UnknownHostException, is SocketTimeoutException, is IOException -> 
                "İnternet bağlantınızı kontrol edin ve tekrar deneyin"
            else -> e.message ?: "Bir hata oluştu, lütfen tekrar deneyin"
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val result = auth.signInWithEmailAndPassword(email, password).await()
                
                // Kullanıcı verilerini yenile
                auth.currentUser?.reload()?.await()
                
                // E-posta doğrulamasını kontrol et
                if (auth.currentUser?.isEmailVerified != true) {
                    auth.signOut() // Doğrulanmamış kullanıcıyı oturumdan çıkar
                    _error.value = "Lütfen e-posta adresinizi doğrulayın"
                    return@launch
                }
                
                _currentUser.value = auth.currentUser
                _isEmailVerified.value = true
                
                // Profil tamamlama durumunu kontrol et
                val isComplete = userRepository.isProfileComplete(auth.currentUser!!.uid)
                _isProfileComplete.value = isComplete
            } catch (e: Exception) {
                _error.value = handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                _currentUser.value = result.user
                sendVerificationEmail()
            } catch (e: Exception) {
                _error.value = handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _isEmailVerified.value = false
        _isProfileComplete.value = false
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                auth.currentUser?.sendEmailVerification()?.await()
            } catch (e: Exception) {
                _error.value = handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()?.await()
                _isEmailVerified.value = auth.currentUser?.isEmailVerified ?: false
                
                // Check profile completion when email is verified
                if (_isEmailVerified.value) {
                    _isProfileComplete.value = userRepository.isProfileComplete(auth.currentUser!!.uid)
                }
            } catch (e: Exception) {
                _error.value = handleError(e)
            }
        }
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    fun isProfileComplete(): Boolean {
        return _isProfileComplete.value
    }

    fun checkProfileCompletion() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                _isProfileComplete.value = userRepository.isProfileComplete(userId)
            } catch (e: Exception) {
                _error.value = handleError(e)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                auth.sendPasswordResetEmail(email).await()
                _error.value = "Şifre sıfırlama e-postası gönderildi. Lütfen e-posta kutunuzu kontrol edin."
            } catch (e: Exception) {
                _error.value = handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 