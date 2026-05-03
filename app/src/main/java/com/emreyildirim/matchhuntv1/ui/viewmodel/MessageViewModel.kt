package com.emreyildirim.matchhuntv1.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.BuildConfig
import com.emreyildirim.matchhuntv1.data.model.Message
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.screens.Conversation
import com.emreyildirim.matchhuntv1.utils.ChatUtils
import com.emreyildirim.matchhuntv1.utils.NetworkUtils
import com.emreyildirim.matchhuntv1.utils.withNetworkTimeout
import com.google.firebase.auth.FirebaseAuth
import com.emreyildirim.matchhuntv1.data.model.Conversation as DbConversation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class MessageViewModel(private val context: Context? = null) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Gizlenen konuşmalar için SharedPreferences
    private val prefsName = "hidden_conversations"
    private val hiddenConversationsKey = "hidden_user_ids"

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    // Toplam okunmamış mesaj sayısı - sadece conversations değiştiğinde güncellenir
    val totalUnreadCount: StateFlow<Int> = _conversations
        .map { conversations -> conversations.sumOf { it.unreadCount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Listener referanslarını tutmak için
    private var messageListenerRegistration: ListenerRegistration? = null
    private var sentMessagesListener: ListenerRegistration? = null
    private var receivedMessagesListener: ListenerRegistration? = null
    private var conversationListenerRegistrations: MutableList<ListenerRegistration> = mutableListOf()
    
    // Chat mesajları için pagination state'leri
    private val pageSize = 20
    private var lastVisibleMessageSnapshot: DocumentSnapshot? = null
    private var hasMoreMessages: Boolean = true
    private var currentChatId: String? = null
    private var currentTargetUserId: String? = null

    private val TAG = "MessageViewModel"
    
    // Gizlenen konuşmaları yükle
    private fun getHiddenConversations(): Set<String> {
        return context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            ?.getStringSet(hiddenConversationsKey, emptySet())
            ?.toSet() ?: emptySet()
    }
    
    // Konuşmayı gizle
    fun hideConversation(userId: String) {
        viewModelScope.launch {
            try {
                val hidden = getHiddenConversations().toMutableSet()
                hidden.add(userId)
                context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    ?.edit()
                    ?.putStringSet(hiddenConversationsKey, hidden)
                    ?.apply()
                
                // UI'ı güncelle - gizlenen konuşmayı listeden çıkar
                val currentConversations = _conversations.value.toMutableList()
                _conversations.value = currentConversations.filter { it.userId != userId }
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Conversation hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding conversation", e)
            }
        }
    }
    
    // Konuşmayı tekrar göster (gizlemeyi kaldır)
    fun unhideConversation(userId: String) {
        viewModelScope.launch {
            try {
                val hidden = getHiddenConversations().toMutableSet()
                hidden.remove(userId)
                context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    ?.edit()
                    ?.putStringSet(hiddenConversationsKey, hidden)
                    ?.apply()
                
                // Konuşmaları yeniden yükle (gizlenen artık görünecek)
                loadConversations()
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Conversation unhidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error unhiding conversation", e)
            }
        }
    }

    fun sendMessage(text: String, targetUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val chatId = ChatUtils.getChatId(currentUserId, targetUserId)
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = currentUserId,
                    receiverId = targetUserId,
                    text = text,
                    timestamp = Date(),
                    isRead = false
                )

                // OPTIMISTIC UPDATE: Mesajı hemen local state'e ekle (UI anında güncellenir)
                if (currentChatId == chatId) {
                    val currentMessages = _messages.value.toMutableList()
                    _messages.value = currentMessages + message
                    if (BuildConfig.DEBUG) Log.d(TAG, "Message added optimistically")
                }

                // BATCH WRITE: Mesajı kaydet ve konuşma özetini güncelle
                val batch = firestore.batch()
                
                val messageRef = firestore.collection("messages").document(message.id)
                batch.set(messageRef, message)
                
                val conversationRef = firestore.collection("conversations").document(chatId)
                
                // 1. Önce dökümanın varlığını ve katılımcıları garanti et (merge ile)
                batch.set(conversationRef, mapOf(
                    "participants" to listOf(currentUserId, targetUserId).sorted()
                ), SetOptions.merge())
                
                // 2. Nokta notasyonu ile Map içindeki alanları GÜNCELLE (update ile nokta ayracı çalışır)
                batch.update(conversationRef, mapOf(
                    "lastMessage" to text,
                    "lastMessageSenderId" to currentUserId,
                    "lastMessageTimestamp" to message.timestamp,
                    "unreadCounts.$targetUserId" to FieldValue.increment(1),
                    "unreadCounts.$currentUserId" to 0
                ))

                withNetworkTimeout {
                    batch.commit().await()
                }

                if (BuildConfig.DEBUG) Log.d(TAG, "Message and conversation summary updated")
            } catch (e: Exception) {
                _error.value = NetworkUtils.getErrorMessage(e)
                if (BuildConfig.DEBUG) Log.e(TAG, "sendMessage error", e)
            }
        }
    }

    /**
     * Belirli bir kullanıcı ile olan konuşmada son pageSize kadar mesajı getirir.
     * Chat ekranı ilk açıldığında çağırılmalı.
     */
    fun loadInitialMessages(targetUserId: String) {
        viewModelScope.launch {
            try {
                if (BuildConfig.DEBUG) Log.d(TAG, "loadInitialMessages() called")

                val currentUserId = auth.currentUser?.uid ?: return@launch
                val chatId = ChatUtils.getChatId(currentUserId, targetUserId)
                
                // Eğer aynı chat zaten yüklüyse ve hafızada mesaj varsa, tekrar Firestore'a gitme
                if (currentChatId == chatId && _messages.value.isNotEmpty()) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "loadInitialMessages() skipped: already loaded")
                    return@launch
                }

                // FARKLI BİR CHAT'E GEÇİLDİYSE STATE'İ TEMİZLE
                if (currentChatId != null && currentChatId != chatId) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Chat changed, clearing cached messages")
                    _messages.value = emptyList()
                    // Eski listener'ı durdur
                    stopMessageListener()
                }
                
                currentTargetUserId = targetUserId

                _isLoading.value = true
                _error.value = null

                currentChatId = chatId
                currentTargetUserId = targetUserId
                lastVisibleMessageSnapshot = null
                hasMoreMessages = true

                // Firestore rules ile uyumlu olması için senderId ve receiverId kullan
                // İki ayrı query yapıp sonuçları birleştir (OR query yok)
                val sentMessagesQuery = firestore.collection("messages")
                    .whereEqualTo("senderId", currentUserId)
                    .whereEqualTo("receiverId", targetUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize.toLong())

                val receivedMessagesQuery = firestore.collection("messages")
                    .whereEqualTo("senderId", targetUserId)
                    .whereEqualTo("receiverId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize.toLong())

                // Her iki query'yi paralel olarak çalıştır
                val sentSnapshot = withNetworkTimeout { sentMessagesQuery.get().await() }
                val receivedSnapshot = withNetworkTimeout { receivedMessagesQuery.get().await() }

                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "loadInitialMessages() returned ${sentSnapshot.size()} sent + ${receivedSnapshot.size()} received"
                    )
                }

                // Tüm mesajları birleştir ve sırala
                val allMessages = (sentSnapshot.documents + receivedSnapshot.documents)
                    .mapNotNull { doc ->
                        Message.fromFirestore(doc)
                    }
                    .distinctBy { it.id } // Duplicate kontrolü
                    .sortedByDescending { it.timestamp }
                    .take(pageSize) // En son pageSize kadar mesaj al

                val messages = allMessages
                if (BuildConfig.DEBUG) Log.d(TAG, "loadInitialMessages() mapped ${messages.size} messages")

                // DESC ile aldık, UI'de eski → yeni göstermek için ters çeviriyoruz
                _messages.value = messages.reversed()

                // Pagination için son snapshot'ı kaydet (sent veya received'tan biri)
                lastVisibleMessageSnapshot = if (sentSnapshot.size() > 0) {
                    sentSnapshot.documents.lastOrNull()
                } else {
                    receivedSnapshot.documents.lastOrNull()
                }
                hasMoreMessages = (sentSnapshot.size() >= pageSize || receivedSnapshot.size() >= pageSize)
                if (BuildConfig.DEBUG) Log.d(TAG, "loadInitialMessages() hasMoreMessages=$hasMoreMessages")
            } catch (e: Exception) {
                _error.value = NetworkUtils.getErrorMessage(e)
                if (BuildConfig.DEBUG) Log.e(TAG, "loadInitialMessages() error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Kullanıcı yukarı kaydırdığında daha eski mesajları getirir.
     */
    fun loadMoreMessages() {
        viewModelScope.launch {
            try {
                if (BuildConfig.DEBUG) Log.d(TAG, "loadMoreMessages() called, hasMoreMessages=$hasMoreMessages")
                if (!hasMoreMessages) {
                    return@launch
                }
                val chatId = currentChatId ?: run {
                    if (BuildConfig.DEBUG) Log.w(TAG, "loadMoreMessages() aborted: chat id is null")
                    return@launch
                }
                // Mevcut mesajlardan en eski mesajın timestamp'ini kullan
                val oldestMessage = _messages.value.firstOrNull()
                if (oldestMessage == null) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "loadMoreMessages() aborted: no messages in list")
                    hasMoreMessages = false
                    return@launch
                }

                _isLoading.value = true

                val currentUserId = auth.currentUser?.uid ?: return@launch
                val targetUserId = currentTargetUserId ?: run {
                    if (BuildConfig.DEBUG) Log.w(TAG, "loadMoreMessages() aborted: targetUserId is null")
                    return@launch
                }

                if (BuildConfig.DEBUG) Log.d(TAG, "loadMoreMessages() querying older messages")

                // İki ayrı query yap (sent ve received)
                // Timestamp'e göre filtrele
                val sentMessagesQuery = firestore.collection("messages")
                    .whereEqualTo("senderId", currentUserId)
                    .whereEqualTo("receiverId", targetUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .whereLessThan("timestamp", oldestMessage.timestamp)
                    .limit(pageSize.toLong())

                val receivedMessagesQuery = firestore.collection("messages")
                    .whereEqualTo("senderId", targetUserId)
                    .whereEqualTo("receiverId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .whereLessThan("timestamp", oldestMessage.timestamp)
                    .limit(pageSize.toLong())

                // Her iki query'yi paralel olarak çalıştır
                val sentSnapshot = withNetworkTimeout { sentMessagesQuery.get().await() }
                val receivedSnapshot = withNetworkTimeout { receivedMessagesQuery.get().await() }

                // Tüm mesajları birleştir ve sırala
                val olderMessages = (sentSnapshot.documents + receivedSnapshot.documents)
                    .mapNotNull { doc ->
                        Message.fromFirestore(doc)
                    }
                    .distinctBy { it.id } // Duplicate kontrolü
                    .sortedByDescending { it.timestamp }
                    .take(pageSize) // En son pageSize kadar mesaj al

                if (BuildConfig.DEBUG) Log.d(TAG, "loadMoreMessages() mapped ${olderMessages.size} older messages")

                if (olderMessages.isEmpty()) {
                    hasMoreMessages = false
                    return@launch
                }

                // Eski mesajlar listenin başına ekleniyor (UI hala eski → yeni gösterir)
                val currentMessages = _messages.value
                val combinedMessages = (olderMessages.reversed() + currentMessages)
                    .distinctBy { it.id }
                
                _messages.value = combinedMessages

                // Pagination için kontrol et
                hasMoreMessages = (sentSnapshot.size() >= pageSize || receivedSnapshot.size() >= pageSize)
                if (BuildConfig.DEBUG) Log.d(TAG, "loadMoreMessages() hasMoreMessages=$hasMoreMessages")
            } catch (e: Exception) {
                _error.value = NetworkUtils.getErrorMessage(e)
                if (BuildConfig.DEBUG) Log.e(TAG, "loadMoreMessages() error", e)
            } finally {
                _isLoading.value = false
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
                if (BuildConfig.DEBUG) Log.e(TAG, "updateConversationList error", e)
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val currentUserId = auth.currentUser?.uid ?: return@launch
                
                val hiddenConversations = getHiddenConversations()
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Loading optimized conversations")

                // 1. Konuşma özetlerini getir (Tek sorgu!)
                val snapshot = withNetworkTimeout {
                    firestore.collection("conversations")
                        .whereArrayContains("participants", currentUserId)
                        .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                        .get()
                        .await()
                }

                val firestoreConvs = snapshot.documents.mapNotNull { 
                    DbConversation.fromFirestore(it)
                }

                // 2. UI modellerine dönüştür (Gerekli profilleri çek)
                val conversationList = firestoreConvs.mapNotNull { fConv ->
                    val otherUserId = fConv.participants.firstOrNull { it != currentUserId } ?: return@mapNotNull null
                    
                    if (otherUserId in hiddenConversations) return@mapNotNull null

                    // Profil bilgisini getir
                    val userDoc = firestore.collection("users").document(otherUserId).get().await()
                    val userProfile = userDoc.toObject(UserProfile::class.java) ?: return@mapNotNull null

                    Conversation(
                        userId = otherUserId,
                        userProfile = userProfile,
                        lastMessage = Message(
                            text = fConv.lastMessage,
                            timestamp = fConv.lastMessageTimestamp,
                            senderId = fConv.lastMessageSenderId,
                            chatId = fConv.id
                        ),
                        unreadCount = (fConv.unreadCounts[currentUserId] ?: 0L).toInt()
                    )
                }

                _conversations.value = conversationList.distinctBy { it.userId }
                if (BuildConfig.DEBUG) Log.d(TAG, "Loaded ${_conversations.value.size} conversations")
            } catch (e: Exception) {
                _error.value = NetworkUtils.getErrorMessage(e)
                if (BuildConfig.DEBUG) Log.e(TAG, "loadConversations error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Gerçek zamanlı mesaj dinleyicisi
    fun startMessageListener(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatId = ChatUtils.getChatId(currentUserId, targetUserId)
        
        // Eğer aynı chat için listener zaten varsa, yeni listener ekleme
        if (currentChatId == chatId && messageListenerRegistration != null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Listener already active")
            return
        }
        
        // Eski listener'ları durdur
        stopMessageListener()
        
        currentChatId = chatId
        currentTargetUserId = targetUserId
        if (BuildConfig.DEBUG) Log.d(TAG, "Starting message listener")

        try {
            // Firestore rules ile uyumlu olması için senderId ve receiverId kullan
            // İki ayrı listener kullan (OR query yok)
            var isInitialSnapshotSent = true
            var isInitialSnapshotReceived = true

            // Gönderilen mesajları dinle
            sentMessagesListener = firestore.collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", targetUserId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    handleMessageSnapshot(
                        snapshot, 
                        error, 
                        chatId, 
                        isInitialSnapshotSent,
                        "sent"
                    )
                    isInitialSnapshotSent = false
                }

            // Alınan mesajları dinle
            receivedMessagesListener = firestore.collection("messages")
                .whereEqualTo("senderId", targetUserId)
                .whereEqualTo("receiverId", currentUserId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    handleMessageSnapshot(
                        snapshot, 
                        error, 
                        chatId, 
                        isInitialSnapshotReceived,
                        "received"
                    )
                    isInitialSnapshotReceived = false
                }

            // Her iki listener'ı da kaydet (birleşik ListenerRegistration)
            messageListenerRegistration = object : ListenerRegistration {
                override fun remove() {
                    sentMessagesListener?.remove()
                    receivedMessagesListener?.remove()
                    sentMessagesListener = null
                    receivedMessagesListener = null
                }
            }
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Message listener started successfully")
        } catch (e: Exception) {
            _error.value = "Mesaj dinleyicisi başlatılırken bir hata oluştu: ${e.message}"
            if (BuildConfig.DEBUG) Log.e(TAG, "Error starting message listener", e)
        }
    }

    private fun handleMessageSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot?,
        error: com.google.firebase.firestore.FirebaseFirestoreException?,
        expectedChatId: String,
        isInitial: Boolean,
        type: String
    ) {
        if (error != null) {
            _error.value = "Mesajlar dinlenirken bir hata oluştu: ${error.message}"
            if (BuildConfig.DEBUG) Log.e(TAG, "Listener error ($type)", error)
            return
        }

        snapshot?.let { documents ->
            try {
                // Chat değişmişse listener'ı görmezden gel
                if (currentChatId != expectedChatId) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Ignoring listener update ($type): chat changed")
                    return
                }

                if (isInitial) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Initial snapshot received ($type), ${documents.size()} documents")
                    // İlk snapshot'ı sadece log'la, loadInitialMessages zaten yüklüyor
                    return
                }

                // Yalnızca yeni eklenen mesajları işle
                val documentChanges = documents.documentChanges
                val newMessages = documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .mapNotNull { change ->
                        Message.fromFirestore(change.document)
                    }
                    .filter { message ->
                        // ChatId kontrolü (ekstra güvenlik)
                        val isValid = message.chatId == currentChatId
                        if (!isValid && BuildConfig.DEBUG) Log.w(TAG, "Message chatId mismatch detected")
                        isValid
                    }

                if (newMessages.isNotEmpty()) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Listener received ${newMessages.size} new messages ($type)")
                    
                    // Duplicate kontrolü
                    val currentMessages = _messages.value.toMutableList()
                    val existingMessageIds = currentMessages.map { it.id }.toSet()
                    val uniqueNewMessages = newMessages.filter { it.id !in existingMessageIds }
                    
                    if (uniqueNewMessages.isNotEmpty()) {
                        _messages.value = currentMessages + uniqueNewMessages
                        if (BuildConfig.DEBUG) Log.d(TAG, "Added ${uniqueNewMessages.size} new messages ($type)")
                    }
                }
            } catch (e: Exception) {
                _error.value = "Mesajlar işlenirken bir hata oluştu: ${e.message}"
                if (BuildConfig.DEBUG) Log.e(TAG, "Error processing listener snapshot ($type)", e)
            }
        } ?: run {
            if (BuildConfig.DEBUG) Log.w(TAG, "Listener snapshot is null ($type)")
        }
    }

    // Listener'ı durdur
    fun stopMessageListener() {
        sentMessagesListener?.remove()
        receivedMessagesListener?.remove()
        messageListenerRegistration?.remove()
        sentMessagesListener = null
        receivedMessagesListener = null
        messageListenerRegistration = null
        if (BuildConfig.DEBUG) Log.d(TAG, "Message listener stopped")
    }

    // Konuşma listesi için gerçek zamanlı dinleyici
    fun startConversationListener() {
        val currentUserId = auth.currentUser?.uid ?: return
        stopConversationListeners()

        try {
            val registration = firestore.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Conversation listener error", error)
                        return@addSnapshotListener
                    }

                    snapshot?.let { docs ->
                        viewModelScope.launch {
                            val hiddenConversations = getHiddenConversations()
                            val currentList = _conversations.value.toMutableList()
                            var changed = false
                            
                            for (change in docs.documentChanges) {
                                val fConv = DbConversation.fromFirestore(change.document) ?: continue
                                val otherUserId = fConv.participants.firstOrNull { it != currentUserId } ?: continue
                                
                                if (otherUserId in hiddenConversations) continue

                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val existing = currentList.find { it.userId == otherUserId }
                                        val userProfile = existing?.userProfile ?: run {
                                            firestore.collection("users").document(otherUserId).get().await()
                                                .toObject(UserProfile::class.java)
                                        } ?: continue

                                        val updatedConv = Conversation(
                                            userId = otherUserId,
                                            userProfile = userProfile,
                                            lastMessage = Message(
                                                text = fConv.lastMessage,
                                                timestamp = fConv.lastMessageTimestamp,
                                                senderId = fConv.lastMessageSenderId,
                                                chatId = fConv.id
                                            ),
                                            unreadCount = (fConv.unreadCounts[currentUserId] ?: 0L).toInt()
                                        )
                                        
                                        val index = currentList.indexOfFirst { it.userId == otherUserId }
                                        if (index != -1) {
                                            currentList[index] = updatedConv
                                        } else {
                                            currentList.add(updatedConv)
                                        }
                                        changed = true
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        if (currentList.removeAll { it.userId == otherUserId }) {
                                            changed = true
                                        }
                                    }
                                }
                            }
                            if (changed) {
                                _conversations.value = currentList.distinctBy { it.userId }
                                    .sortedByDescending { it.lastMessage?.timestamp }
                            }
                        }
                    }
                }
            conversationListenerRegistrations.add(registration)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error starting conversation listener", e)
        }
    }

    // Konuşma listener'larını durdur
    fun stopConversationListeners() {
        conversationListenerRegistrations.forEach { it.remove() }
        conversationListenerRegistrations.clear()
        if (BuildConfig.DEBUG) Log.d(TAG, "Conversation listeners stopped")
    }



    // Mesajları okundu olarak işaretle
    fun markMessagesAsRead(targetUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val chatId = ChatUtils.getChatId(currentUserId, targetUserId)
                val currentTime = Date()

                // Okunmamış mesajları bul
                val unreadSnapshot = firestore.collection("messages")
                    .whereEqualTo("senderId", targetUserId)
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                val batch = firestore.batch()
                
                // Mesajları okundu işaretle (Eğer varsa)
                if (!unreadSnapshot.isEmpty) {
                    for (doc in unreadSnapshot.documents) {
                        batch.update(doc.reference, mapOf(
                            "isRead" to true,
                            "readAt" to currentTime
                        ))
                    }
                }

                // Konuşma özetindeki okunmamış sayısını HER DURUMDA sıfırla
                val conversationRef = firestore.collection("conversations").document(chatId)
                batch.update(conversationRef, "unreadCounts.$currentUserId", 0)

                withNetworkTimeout {
                    batch.commit().await()
                }

                if (BuildConfig.DEBUG) Log.d(TAG, "Unread count reset for $chatId")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "markMessagesAsRead error", e)
            }
        }
    }

    // Okunmamış mesaj sayısını hesapla




    // ViewModel temizlendiğinde listener'ları durdur
    override fun onCleared() {
        super.onCleared()
        stopMessageListener()
        stopConversationListeners()
    }
}