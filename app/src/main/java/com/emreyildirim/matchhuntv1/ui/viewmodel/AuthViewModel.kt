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
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.utils.NetworkUtils
import com.emreyildirim.matchhuntv1.utils.TokenUpdate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.util.Log

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

    private val _isProfileComplete = MutableStateFlow<Boolean?>(null)
    val isProfileComplete: StateFlow<Boolean?> = _isProfileComplete.asStateFlow()
    
    private fun handleError(e: Exception): String {
        return NetworkUtils.getErrorMessage(e)
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _isProfileComplete.value = null

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val signedInUser = result.user ?: auth.currentUser
                if (signedInUser == null) {
                    _error.value = "Giriş yapan kullanıcı bulunamadı"
                    return@launch
                }

                // Auth state'i hemen güncelle ki UI login ekranında takılı kalmasın
                _currentUser.value = signedInUser

                // Kullanıcı verilerini yenile (hata olursa login akışını kesme)
                try {
                    signedInUser.reload().await()
                } catch (e: Exception) {
                    Log.w("Auth", "User reload failed after sign in: ${e.message}")
                }

                // E-posta doğrulama bilgisini state'e hemen yansıt
                val emailVerified = auth.currentUser?.isEmailVerified == true
                _isEmailVerified.value = emailVerified

                // E-posta doğrulamasını kontrol et
                if (!emailVerified) {
                    auth.signOut() // Doğrulanmamış kullanıcıyı oturumdan çıkar
                    _currentUser.value = null
                    _isEmailVerified.value = false
                    _error.value = "Lütfen e-posta adresinizi doğrulayın"
                    return@launch
                }

                // FCM token güncellemesi başarısız olsa bile navigation akışını bloklama
                try {
                    TokenUpdate.updateUserFcmToken()
                } catch (e: Exception) {
                    Log.w("Auth", "FCM token update failed: ${e.message}")
                }

                // Profil tamamlama durumunu kontrol et
                val isComplete = userRepository.isProfileComplete(signedInUser.uid)
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
        viewModelScope.launch {
            try {
                // Çıkış yapmadan önce kullanıcının UID'sini al
                val uid = auth.currentUser?.uid
                
                // Firebase Auth'tan çıkış yap
                auth.signOut()
                _currentUser.value = null
                _isEmailVerified.value = false
                _isProfileComplete.value = false
                
                // Firestore'daki FCM token'ı sil
                if (uid != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmToken", FieldValue.delete())
                        .addOnSuccessListener {
                            Log.d("Auth", "FCM token deleted from Firestore on sign out")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Auth", "Error deleting FCM token: ${e.message}")
                        }
                }
            } catch (e: Exception) {
                Log.e("Auth", "Error during sign out: ${e.message}")
            }
        }
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

    fun isProfileComplete(): Boolean? {
        return _isProfileComplete.value
    }

    fun checkProfileCompletion() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                _isProfileComplete.value = userRepository.isProfileComplete(userId)
                //registerdan sonra email verified ve profile completed ise token updatei yapılıyor firebase
                TokenUpdate.updateUserFcmToken()
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