package com.emreyildirim.matchhuntv1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.data.model.Message
import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.data.model.Report
import com.emreyildirim.matchhuntv1.data.repository.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    private val repository = ReportRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isReporting = MutableStateFlow(false)
    val isReporting: StateFlow<Boolean> = _isReporting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow<Boolean>(false)
    val success: StateFlow<Boolean> = _success

    private fun currentUserIdOrNull(): String? = auth.currentUser?.uid

    fun reportProfile(
        userId: String,
        about: String?,
        reasonCode: String,
        reasonText: String
    ) {
        viewModelScope.launch {
            val reporterId = currentUserIdOrNull() ?: return@launch

            _isReporting.value = true
            _error.value = null
            _success.value = false

            try {
                val report = Report(
                    type = "profile",
                    targetId = userId,
                    targetUserId = userId,
                    reporterId = reporterId,
                    profileSnippet = about?.take(120),
                    reasonCode = reasonCode,
                    reasonText = reasonText
                )
                repository.createReport(report)
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isReporting.value = false
            }
        }
    }

    fun reportPost(
        post: Post,
        reasonCode: String,
        reasonText: String
    ) {
        viewModelScope.launch {
            val reporterId = currentUserIdOrNull() ?: return@launch

            _isReporting.value = true
            _error.value = null
            _success.value = false

            try {
                val report = Report(
                    type = "post",
                    targetId = post.id,
                    targetUserId = post.userId,
                    reporterId = reporterId,
                    postSnippet = post.description.take(120),
                    reasonCode = reasonCode,
                    reasonText = reasonText
                )
                repository.createReport(report)
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isReporting.value = false
            }
        }
    }

    fun reportMessage(
        message: Message,
        reasonCode: String,
        reasonText: String
    ) {
        viewModelScope.launch {
            val reporterId = currentUserIdOrNull() ?: return@launch

            _isReporting.value = true
            _error.value = null
            _success.value = false

            try {
                val report = Report(
                    type = "message",
                    targetId = message.id,
                    targetUserId = message.senderId,
                    reporterId = reporterId,
                    chatId = message.chatId,
                    messageSnippet = message.text.take(120),
                    reasonCode = reasonCode,
                    reasonText = reasonText
                )
                repository.createReport(report)
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isReporting.value = false
            }
        }
    }

    fun clearStatus() {
        _success.value = false
        _error.value = null
    }
}

