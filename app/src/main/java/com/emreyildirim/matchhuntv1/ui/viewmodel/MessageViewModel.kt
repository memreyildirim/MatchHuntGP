package com.emreyildirim.matchhuntv1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.data.model.Message
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.screens.Conversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class MessageViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Her kullanıcı için son okuma zamanını tut
    private val lastReadTimestamps = mutableMapOf<String, Date>()

    fun sendMessage(text: String, targetUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUserId,
                    receiverId = targetUserId,
                    text = text,
                    timestamp = Date(),
                    isRead = false
                )

                // Mesajı Firestore'a kaydet
                firestore.collection("messages")
                    .document(message.id)
                    .set(message)
                    .await()

                // Konuşma listesini güncelle
                updateConversationList(message)
            } catch (e: Exception) {
                _error.value = "Mesaj gönderilirken bir hata oluştu: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun updateConversationList(newMessage: Message) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val otherUserId = if (newMessage.senderId == currentUserId) newMessage.receiverId else newMessage.senderId

                // Kullanıcı profilini getir
                val userDoc = firestore.collection("users")
                    .document(otherUserId)
                    .get()
                    .await()

                val userProfile = userDoc.toObject(UserProfile::class.java)

                if (userProfile != null) {
                    val currentConversations = _conversations.value.toMutableList()
                    
                    // Mevcut konuşmayı bul veya yeni oluştur
                    val existingConversationIndex = currentConversations.indexOfFirst { it.userId == otherUserId }
                    
                    val updatedConversation = Conversation(
                        userId = otherUserId,
                        userProfile = userProfile,
                        lastMessage = newMessage
                    )

                    if (existingConversationIndex != -1) {
                        // Mevcut konuşmayı güncelle
                        currentConversations[existingConversationIndex] = updatedConversation
                    } else {
                        // Yeni konuşma ekle
                        currentConversations.add(updatedConversation)
                    }

                    // Konuşmaları son mesaj zamanına göre sırala
                    _conversations.value = currentConversations.sortedByDescending { it.lastMessage?.timestamp }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMessages(targetUserId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUserId = auth.currentUser?.uid ?: return@launch

                // İki kullanıcı arasındaki mesajları getir
                val messagesSnapshot = firestore.collection("messages")
                    .where(
                        Filter.or(
                            Filter.and(
                                Filter.equalTo("senderId", currentUserId),
                                Filter.equalTo("receiverId", targetUserId)
                            ),
                            Filter.and(
                                Filter.equalTo("senderId", targetUserId),
                                Filter.equalTo("receiverId", currentUserId)
                            )
                        )
                    )
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .get()
                    .await()

                val messages = messagesSnapshot.documents.mapNotNull { doc ->
                    Message.fromFirestore(doc)
                }
                _messages.value = messages
            } catch (e: Exception) {
                _error.value = "Mesajlar yüklenirken bir hata oluştu: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val currentUserId = auth.currentUser?.uid ?: return@launch

                // Kullanıcının tüm mesajlarını getir
                val messagesSnapshot = firestore.collection("messages")
                    .whereEqualTo("senderId", currentUserId)
                    .get()
                    .await()

                val sentMessages = messagesSnapshot.documents.mapNotNull { doc ->
                    Message.fromFirestore(doc)
                }

                val receivedMessagesSnapshot = firestore.collection("messages")
                    .whereEqualTo("receiverId", currentUserId)
                    .get()
                    .await()

                val receivedMessages = receivedMessagesSnapshot.documents.mapNotNull { doc ->
                    Message.fromFirestore(doc)
                }

                // Tüm mesajları birleştir
                val allMessages = sentMessages + receivedMessages

                // Her kullanıcı için son mesajı bul
                val conversationsMap = mutableMapOf<String, Conversation>()
                
                for (message in allMessages) {
                    val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
                    
                    if (!conversationsMap.containsKey(otherUserId)) {
                        // Kullanıcı profilini getir
                        val userDoc = firestore.collection("users")
                            .document(otherUserId)
                            .get()
                            .await()
                        
                        val userProfile = userDoc.toObject(UserProfile::class.java)
                        
                        if (userProfile != null) {
                            conversationsMap[otherUserId] = Conversation(
                                userId = otherUserId,
                                userProfile = userProfile,
                                lastMessage = message
                            )
                        }
                    } else {
                        // Son mesajı güncelle
                        val conversation = conversationsMap[otherUserId]
                        if (conversation != null && conversation.lastMessage?.timestamp?.before(message.timestamp) == true) {
                            conversationsMap[otherUserId] = conversation.copy(lastMessage = message)
                        }
                    }
                }

                // Konuşmaları son mesaj zamanına göre sırala
                _conversations.value = conversationsMap.values.sortedByDescending { it.lastMessage?.timestamp }
            } catch (e: Exception) {
                _error.value = "Konuşmalar yüklenirken bir hata oluştu: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Gerçek zamanlı mesaj dinleyicisi
    fun startMessageListener(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        try {
            firestore.collection("messages")
                .where(
                    Filter.or(
                        Filter.and(
                            Filter.equalTo("senderId", currentUserId),
                            Filter.equalTo("receiverId", targetUserId)
                        ),
                        Filter.and(
                            Filter.equalTo("senderId", targetUserId),
                            Filter.equalTo("receiverId", currentUserId)
                        )
                    )
                )
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _error.value = "Mesajlar dinlenirken bir hata oluştu: ${error.message}"
                        error.printStackTrace()
                        return@addSnapshotListener
                    }

                    snapshot?.let { documents ->
                        try {
                            val messages = documents.documents.mapNotNull { doc ->
                                Message.fromFirestore(doc)
                            }
                            _messages.value = messages
                        } catch (e: Exception) {
                            _error.value = "Mesajlar işlenirken bir hata oluştu: ${e.message}"
                            e.printStackTrace()
                        }
                    }
                }
        } catch (e: Exception) {
            _error.value = "Mesaj dinleyicisi başlatılırken bir hata oluştu: ${e.message}"
            e.printStackTrace()
        }
    }

    // Konuşma listesi için gerçek zamanlı dinleyici
    fun startConversationListener() {
        val currentUserId = auth.currentUser?.uid ?: return

        try {
            // Gönderilen mesajları dinle
            firestore.collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _error.value = "Konuşmalar dinlenirken bir hata oluştu: ${error.message}"
                        error.printStackTrace()
                        return@addSnapshotListener
                    }

                    snapshot?.let { documents ->
                        try {
                            val sentMessages = documents.documents.mapNotNull { doc ->
                                Message.fromFirestore(doc)
                            }
                            updateConversationsWithNewMessages(sentMessages)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            // Alınan mesajları dinle
            firestore.collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _error.value = "Konuşmalar dinlenirken bir hata oluştu: ${error.message}"
                        error.printStackTrace()
                        return@addSnapshotListener
                    }

                    snapshot?.let { documents ->
                        try {
                            val receivedMessages = documents.documents.mapNotNull { doc ->
                                Message.fromFirestore(doc)
                            }
                            updateConversationsWithNewMessages(receivedMessages)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        } catch (e: Exception) {
            _error.value = "Konuşma dinleyicisi başlatılırken bir hata oluştu: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun updateConversationsWithNewMessages(newMessages: List<Message>) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val currentConversations = _conversations.value.toMutableList()
                val conversationsMap = currentConversations.associateBy { it.userId }.toMutableMap()

                for (message in newMessages) {
                    val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
                    
                    if (!conversationsMap.containsKey(otherUserId)) {
                        // Kullanıcı profilini getir
                        val userDoc = firestore.collection("users")
                            .document(otherUserId)
                            .get()
                            .await()
                        
                        val userProfile = userDoc.toObject(UserProfile::class.java)
                        
                        if (userProfile != null) {
                            // Bu kullanıcıyla olan tüm mesajları getir
                            val userMessages = firestore.collection("messages")
                                .where(
                                    Filter.or(
                                        Filter.and(
                                            Filter.equalTo("senderId", currentUserId),
                                            Filter.equalTo("receiverId", otherUserId)
                                        ),
                                        Filter.and(
                                            Filter.equalTo("senderId", otherUserId),
                                            Filter.equalTo("receiverId", currentUserId)
                                        )
                                    )
                                )
                                .get()
                                .await()
                                .documents
                                .mapNotNull { doc -> Message.fromFirestore(doc) }

                            conversationsMap[otherUserId] = Conversation(
                                userId = otherUserId,
                                userProfile = userProfile,
                                lastMessage = message,
                                unreadCount = calculateUnreadCount(userMessages, currentUserId, otherUserId)
                            )
                        }
                    } else {
                        // Son mesajı güncelle
                        val conversation = conversationsMap[otherUserId]
                        if (conversation != null && conversation.lastMessage?.timestamp?.before(message.timestamp) == true) {
                            // Bu kullanıcıyla olan tüm mesajları getir
                            val userMessages = firestore.collection("messages")
                                .where(
                                    Filter.or(
                                        Filter.and(
                                            Filter.equalTo("senderId", currentUserId),
                                            Filter.equalTo("receiverId", otherUserId)
                                        ),
                                        Filter.and(
                                            Filter.equalTo("senderId", otherUserId),
                                            Filter.equalTo("receiverId", currentUserId)
                                        )
                                    )
                                )
                                .get()
                                .await()
                                .documents
                                .mapNotNull { doc -> Message.fromFirestore(doc) }

                            conversationsMap[otherUserId] = conversation.copy(
                                lastMessage = message,
                                unreadCount = calculateUnreadCount(userMessages, currentUserId, otherUserId)
                            )
                        }
                    }
                }

                // Konuşmaları son mesaj zamanına göre sırala
                _conversations.value = conversationsMap.values.sortedByDescending { it.lastMessage?.timestamp }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Mesajları okundu olarak işaretle
    fun markMessagesAsRead(targetUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val currentTime = Date()

                // Alınan ve okunmamış mesajları bul
                val unreadMessages = firestore.collection("messages")
                    .whereEqualTo("senderId", targetUserId)
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                // Her mesajı okundu olarak işaretle
                for (doc in unreadMessages.documents) {
                    firestore.collection("messages")
                        .document(doc.id)
                        .update(mapOf(
                            "isRead" to true,
                            "readAt" to currentTime
                        ))
                        .await()
                }

                // Son okuma zamanını lastRead koleksiyonunda güncelle
                firestore.collection("lastRead")
                    .document("${currentUserId}_${targetUserId}")
                    .set(mapOf(
                        "userId" to currentUserId,
                        "targetUserId" to targetUserId,
                        "timestamp" to currentTime
                    ))
                    .await()

                // Konuşma listesini güncelle
                updateConversationList(targetUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Okunmamış mesaj sayısını hesapla
    private suspend fun calculateUnreadCount(messages: List<Message>, currentUserId: String, targetUserId: String): Int {
        try {
            // Son okuma zamanını lastRead koleksiyonundan getir
            val lastReadDoc = firestore.collection("lastRead")
                .document("${currentUserId}_${targetUserId}")
                .get()
                .await()

            val lastReadTime = if (lastReadDoc.exists()) {
                lastReadDoc.getTimestamp("timestamp")?.toDate()
            } else {
                null
            }

            // Son okuma zamanından sonraki mesajları say
            return messages.count { message ->
                message.receiverId == currentUserId && 
                message.senderId == targetUserId &&
                (lastReadTime == null || message.timestamp.after(lastReadTime))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    private fun updateConversationList(targetUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch

                // Kullanıcı profilini getir
                val userDoc = firestore.collection("users")
                    .document(targetUserId)
                    .get()
                    .await()

                val userProfile = userDoc.toObject(UserProfile::class.java)

                if (userProfile != null) {
                    // Son okuma zamanını getir
                    val lastReadDoc = firestore.collection("lastRead")
                        .document("${currentUserId}_${targetUserId}")
                        .get()
                        .await()

                    val lastReadTime = if (lastReadDoc.exists()) {
                        lastReadDoc.getTimestamp("timestamp")?.toDate()
                    } else {
                        null
                    }

                    // Son okuma zamanından sonraki mesajları getir
                    val query = firestore.collection("messages")
                        .whereEqualTo("senderId", targetUserId)
                        .whereEqualTo("receiverId", currentUserId)
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)

                    if (lastReadTime != null) {
                        query.whereGreaterThan("timestamp", lastReadTime)
                    }

                    val messagesSnapshot = query.get().await()
                    val lastMessage = messagesSnapshot.documents.firstOrNull()?.let { doc ->
                        Message.fromFirestore(doc)
                    }

                    val unreadCount = messagesSnapshot.size()

                    val updatedConversation = Conversation(
                        userId = targetUserId,
                        userProfile = userProfile,
                        lastMessage = lastMessage,
                        unreadCount = unreadCount
                    )

                    val currentConversations = _conversations.value.toMutableList()
                    val index = currentConversations.indexOfFirst { it.userId == targetUserId }
                    
                    if (index != -1) {
                        currentConversations[index] = updatedConversation
                    } else {
                        currentConversations.add(updatedConversation)
                    }

                    _conversations.value = currentConversations.sortedByDescending { it.lastMessage?.timestamp }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}