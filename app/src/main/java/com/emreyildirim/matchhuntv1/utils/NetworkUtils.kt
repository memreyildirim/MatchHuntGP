package com.emreyildirim.matchhuntv1.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Network ve timeout işlemleri için utility class
 */
object NetworkUtils {
    
    // Default timeout değerleri (milisaniye)
    const val DEFAULT_TIMEOUT_MS = 15_000L      // 15 saniye - normal işlemler
    const val UPLOAD_TIMEOUT_MS = 60_000L       // 60 saniye - dosya yükleme
    const val SHORT_TIMEOUT_MS = 10_000L        // 10 saniye - hızlı işlemler
    
    /**
     * Cihazın internet bağlantısı olup olmadığını kontrol eder
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Exception/Throwable türüne göre kullanıcı dostu hata mesajı döndürür
     */
    fun getErrorMessage(e: Throwable): String {
        return when (e) {
            is TimeoutCancellationException -> 
                "İşlem zaman aşımına uğradı. İnternet bağlantınızı kontrol edip tekrar deneyin."
            is UnknownHostException -> 
                "İnternet bağlantısı bulunamadı. Lütfen bağlantınızı kontrol edin."
            is SocketTimeoutException -> 
                "Sunucuya bağlanılamadı. Lütfen tekrar deneyin."
            is IOException -> 
                "Bağlantı hatası oluştu. İnternet bağlantınızı kontrol edin."
            else -> e.message ?: "Bir hata oluştu, lütfen tekrar deneyin."
        }
    }
    
    /**
     * Network hatası olup olmadığını kontrol eder
     */
    fun isNetworkError(e: Throwable): Boolean {
        return e is UnknownHostException || 
               e is SocketTimeoutException || 
               e is IOException ||
               e is TimeoutCancellationException
    }
}

/**
 * Firebase işlemlerini timeout ile saran extension fonksiyon
 * Kullanım: withNetworkTimeout { firestore.collection("x").get().await() }
 */
suspend fun <T> withNetworkTimeout(
    timeoutMs: Long = NetworkUtils.DEFAULT_TIMEOUT_MS,
    block: suspend () -> T
): T {
    return withTimeout(timeoutMs) { block() }
}

/**
 * Firebase işlemlerini timeout ile sarıp Result döndüren extension fonksiyon
 * Hata durumunda kullanıcı dostu mesaj içeren Result.failure döner
 * Kullanım: withNetworkTimeoutResult { firestore.collection("x").get().await() }
 */
suspend fun <T> withNetworkTimeoutResult(
    timeoutMs: Long = NetworkUtils.DEFAULT_TIMEOUT_MS,
    block: suspend () -> T
): Result<T> {
    return try {
        val result = withTimeout(timeoutMs) { block() }
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(Exception(NetworkUtils.getErrorMessage(e)))
    }
}
