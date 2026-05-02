package com.emreyildirim.matchhuntv1.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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

                // Mesajı Firestore'a kaydet
                withNetworkTimeout {
                    firestore.collection("messages")
                        .document(message.id)
                        .set(message)
                        .await()
                }

                // Konuşma listesini güncelle
                updateConversationList(message)
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
                val currentMessages = _messages.value.toMutableList()
                _messages.value = olderMessages.reversed() + currentMessages

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
                
                // Gizlenen konuşmaları yükle
                val hiddenConversations = getHiddenConversations()
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Loading conversations, hidden count: ${hiddenConversations.size}")

                // Kullanıcının tüm mesajlarını getir
                val messagesSnapshot = withNetworkTimeout {
                    firestore.collection("messages")
                        .whereEqualTo("senderId", currentUserId)
                        .get()
                        .await()
                }

                val sentMessages = messagesSnapshot.documents.mapNotNull { doc ->
                    Message.fromFirestore(doc)
                }

                val receivedMessagesSnapshot = withNetworkTimeout {
                    firestore.collection("messages")
                        .whereEqualTo("receiverId", currentUserId)
                        .get()
                        .await()
                }

                val receivedMessages = receivedMessagesSnapshot.documents.mapNotNull { doc ->
                    Message.fromFirestore(doc)
                }

                // Tüm mesajları birleştir ve timestamp'e göre sırala
                val allMessages = (sentMessages + receivedMessages).sortedBy { it.timestamp }

                // Her kullanıcı için son mesajı bul
                val conversationsMap = mutableMapOf<String, Conversation>()
                
                for (message in allMessages) {
                    val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
                    
                    // GİZLENEN KONUŞMALARI ATLA - Profil sorgusu yapma, işleme alma
                    if (otherUserId in hiddenConversations) {
                        continue // Bu kullanıcıyla ilgili hiçbir şey yapma
                    }
                    
                    if (!conversationsMap.containsKey(otherUserId)) {
                        // Sadece gizlenmemiş kullanıcılar için profil sorgusu yap
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
                        // Son mesajı güncelle - mesajlar sıralı olduğu için her zaman en son mesajı al
                        val conversation = conversationsMap[otherUserId]
                        if (conversation != null) {
                            conversationsMap[otherUserId] = conversation.copy(lastMessage = message)
                        }
                    }
                }

                // Her konuşma için okunmamış mesaj sayısını hesapla
                // (Gizlenenler zaten conversationsMap'te yok, bu yüzden otomatik filtrelenmiş)
                val conversationsWithUnread = conversationsMap.values.map { conversation ->
                    val userMessages = allMessages.filter { message ->
                        val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
                        otherUserId == conversation.userId
                    }
                    val unreadCount = calculateUnreadCount(userMessages, currentUserId, conversation.userId)
                    conversation.copy(unreadCount = unreadCount)
                }

                // Konuşmaları son mesaj zamanına göre sırala
                _conversations.value = conversationsWithUnread.sortedByDescending { it.lastMessage?.timestamp }

                if (BuildConfig.DEBUG) Log.d(TAG, "Loaded ${_conversations.value.size} visible conversations")
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

        // Önce mevcut listener'ları temizle
        stopConversationListeners()

        try {
            var isInitialSnapshotSent = true
            var isInitialSnapshotReceived = true
            
            // Gönderilen mesajları dinle
            val sentListener = firestore.collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _error.value = "Konuşmalar dinlenirken bir hata oluştu: ${error.message}"
                        if (BuildConfig.DEBUG) Log.e(TAG, "Conversation listener error (sent)", error)
                        return@addSnapshotListener
                    }

                    snapshot?.let { documents ->
                        try {
                            // İlk snapshot'ı atla (tüm mesajlar zaten yüklü)
                            if (isInitialSnapshotSent) {
                                isInitialSnapshotSent = false
                                return@addSnapshotListener
                            }
                            
                            // Sadece yeni eklenen mesajları işle
                            val newMessages = documents.documentChanges
                                .filter { it.type == DocumentChange.Type.ADDED }
                                .mapNotNull { change ->
                                    Message.fromFirestore(change.document)
                                }
                            
                            if (newMessages.isNotEmpty()) {
                                if (BuildConfig.DEBUG) Log.d(TAG, "Got ${newMessages.size} new sent messages")
                                updateConversationsWithNewMessages(newMessages)
                            }
                        } catch (e: Exception) {
                            if (BuildConfig.DEBUG) Log.e(TAG, "Error processing sent messages", e)
                        }
                    }
                }
            conversationListenerRegistrations.add(sentListener)

            // Alınan mesajları dinle
            val receivedListener = firestore.collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _error.value = "Konuşmalar dinlenirken bir hata oluştu: ${error.message}"
                        if (BuildConfig.DEBUG) Log.e(TAG, "Conversation listener error (received)", error)
                        return@addSnapshotListener
                    }

                    snapshot?.let { documents ->
                        try {
                            // İlk snapshot'ı atla (tüm mesajlar zaten yüklü)
                            if (isInitialSnapshotReceived) {
                                isInitialSnapshotReceived = false
                                return@addSnapshotListener
                            }
                            
                            // Sadece yeni eklenen mesajları işle
                            val newMessages = documents.documentChanges
                                .filter { it.type == DocumentChange.Type.ADDED }
                                .mapNotNull { change ->
                                    Message.fromFirestore(change.document)
                                }
                            
                            if (newMessages.isNotEmpty()) {
                                if (BuildConfig.DEBUG) Log.d(TAG, "Got ${newMessages.size} new received messages")
                                updateConversationsWithNewMessages(newMessages)
                            }
                        } catch (e: Exception) {
                            if (BuildConfig.DEBUG) Log.e(TAG, "Error processing received messages", e)
                        }
                    }
                }
            conversationListenerRegistrations.add(receivedListener)

            if (BuildConfig.DEBUG) Log.d(TAG, "Conversation listeners started")
        } catch (e: Exception) {
            _error.value = "Konuşma dinleyicisi başlatılırken bir hata oluştu: ${e.message}"
            if (BuildConfig.DEBUG) Log.e(TAG, "Error starting conversation listeners", e)
        }
    }

    // Konuşma listener'larını durdur
    fun stopConversationListeners() {
        conversationListenerRegistrations.forEach { it.remove() }
        conversationListenerRegistrations.clear()
        if (BuildConfig.DEBUG) Log.d(TAG, "Conversation listeners stopped")
    }

    private fun updateConversationsWithNewMessages(newMessages: List<Message>) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val hiddenConversations = getHiddenConversations()
                val currentConversations = _conversations.value.toMutableList()
                val conversationsMap = currentConversations.associateBy { it.userId }.toMutableMap()

                // Yeni mesajları timestamp'e göre sırala
                val sortedNewMessages = newMessages.sortedBy { it.timestamp }

                for (message in sortedNewMessages) {
                    val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
                    
                    // YENİ MESAJ GELEN KONUŞMALAR İÇİN GİZLEMEYİ KALDIR
                    if (otherUserId in hiddenConversations) {
                        // Yeni mesaj geldi, gizlemeyi kaldır
                        unhideConversation(otherUserId)
                        // hiddenConversations'ı güncelle
                        val updatedHidden = getHiddenConversations()
                        // Artık gizlenmemiş, devam et
                    }
                    
                    // Gizlenen konuşmalar için profil sorgusu yapma
                    if (otherUserId in getHiddenConversations()) {
                        continue
                    }
                    
                    if (!conversationsMap.containsKey(otherUserId)) {
                        // Yeni konuşma - kullanıcı profilini getir
                        val userDoc = firestore.collection("users")
                            .document(otherUserId)
                            .get()
                            .await()
                        
                        val userProfile = userDoc.toObject(UserProfile::class.java)
                        
                        if (userProfile != null) {
                            // Firestore rules ile uyumlu: senderId ve receiverId kullan
                            val sentMessagesQuery = firestore.collection("messages")
                                .whereEqualTo("senderId", currentUserId)
                                .whereEqualTo("receiverId", otherUserId)
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .get()
                                .await()

                            val receivedMessagesQuery = firestore.collection("messages")
                                .whereEqualTo("senderId", otherUserId)
                                .whereEqualTo("receiverId", currentUserId)
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .get()
                                .await()

                            // Tüm mesajları birleştir
                            val userMessages = (sentMessagesQuery.documents + receivedMessagesQuery.documents)
                                .mapNotNull { doc -> Message.fromFirestore(doc) }
                                .distinctBy { it.id }
                                .sortedBy { it.timestamp }

                            // En son mesajı bul
                            val lastMessage = userMessages.maxByOrNull { it.timestamp }

                            conversationsMap[otherUserId] = Conversation(
                                userId = otherUserId,
                                userProfile = userProfile,
                                lastMessage = lastMessage,
                                unreadCount = calculateUnreadCount(userMessages, currentUserId, otherUserId)
                            )
                        }
                    } else {
                        // Mevcut konuşma - sadece son mesajı güncelle (performans için)
                        val conversation = conversationsMap[otherUserId]
                        if (conversation != null) {
                            val shouldUpdate = conversation.lastMessage == null || 
                                conversation.lastMessage?.timestamp?.before(message.timestamp) == true
                            
                            if (shouldUpdate) {
                                // Sadece yeni mesajı kullan, tüm mesajları tekrar çekme
                                val existingUnreadCount = conversation.unreadCount
                                val newUnreadCount = if (message.receiverId == currentUserId && !message.isRead) {
                                    existingUnreadCount + 1
                                } else if (message.senderId == currentUserId) {
                                    // Kendi gönderdiğimiz mesaj - unread count'u sıfırla (okundu sayılır)
                                    0
                                } else {
                                    existingUnreadCount
                                }
                                
                                conversationsMap[otherUserId] = conversation.copy(
                                    lastMessage = message,
                                    unreadCount = newUnreadCount
                                )
                                if (BuildConfig.DEBUG) Log.d(TAG, "Conversation updated with a new message")
                            }
                        }
                    }
                }

                // Gizlenen konuşmaları filtrele (güncel listeyi al)
                val updatedHiddenConversations = getHiddenConversations()
                val visibleConversations = conversationsMap.values.filter { 
                    it.userId !in updatedHiddenConversations 
                }

                // Konuşmaları son mesaj zamanına göre sırala
                _conversations.value = visibleConversations.sortedByDescending { it.lastMessage?.timestamp }
                if (BuildConfig.DEBUG) Log.d(TAG, "Conversations updated: ${_conversations.value.size}")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error updating conversations", e)
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
                if (BuildConfig.DEBUG) Log.e(TAG, "markMessagesAsRead error", e)
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
            if (BuildConfig.DEBUG) Log.e(TAG, "calculateUnreadCount error", e)
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
                if (BuildConfig.DEBUG) Log.e(TAG, "updateConversationList(targetUserId) error", e)
            }
        }
    }

    // ViewModel temizlendiğinde listener'ları durdur
    override fun onCleared() {
        super.onCleared()
        stopMessageListener()
        stopConversationListeners()
    }
}