package com.emreyildirim.matchhuntv1.ui.viewmodel

import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emreyildirim.matchhuntv1.data.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


class EventViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _eventCreated = MutableStateFlow(false)
    val eventCreated: StateFlow<Boolean> = _eventCreated.asStateFlow()
    
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()
    
    init {
        loadEvents()
    }
    
    fun loadEvents() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentDate = Date()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                
                val snapshot = firestore.collection("events")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val eventsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    // Etkinlik tarihini parse et
                    val eventDate = dateFormat.parse("${event.date} ${event.time}")
                    // Sadece gelecek etkinlikleri filtrele
                    eventDate?.after(currentDate) ?: false
                }
                
                _events.value = eventsList
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Etkinlikler yüklenirken bir hata oluştu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPastEvents() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentDate = Date()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val currentUser = auth.currentUser
                
                if (currentUser == null) {
                    _error.value = "Kullanıcı girişi yapılmamış"
                    _isLoading.value = false
                    return@launch
                }
                
                val snapshot = firestore.collection("events")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val eventsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    // Etkinlik tarihini parse et
                    val eventDate = dateFormat.parse("${event.date} ${event.time}")
                    // Sadece geçmiş etkinlikleri filtrele
                    val isPastEvent = eventDate?.before(currentDate) ?: false
                    // Kullanıcının oluşturduğu veya katıldığı etkinlikleri göster
                    isPastEvent && (event.createdBy == currentUser.uid || event.participants.contains(currentUser.uid))
                }
                
                _events.value = eventsList
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Geçmiş etkinlikler yüklenirken bir hata oluştu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserEvents() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentUser = auth.currentUser
                
                if (currentUser == null) {
                    _error.value = "Kullanıcı girişi yapılmamış"
                    _isLoading.value = false
                    return@launch
                }
                
                val snapshot = firestore.collection("events")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val eventsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    // Kullanıcının oluşturduğu veya katıldığı tüm etkinlikleri göster
                    event.createdBy == currentUser.uid || event.participants.contains(currentUser.uid)
                }
                
                _events.value = eventsList
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Etkinlikler yüklenirken bir hata oluştu"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshEvents() {
        loadEvents()
    }

    fun refreshPastEvents() {
        loadPastEvents()
    }

    fun refreshUserEvents() {
        loadUserEvents()
    }
    
    fun searchEvents(query: String, sportType: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentDate = Date()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                
                var queryRef = eventsCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                
                // Apply sport type filter if provided and not "All"
                if (sportType.isNotEmpty() && sportType != "All") {
                    queryRef = queryRef.whereEqualTo("sportType", sportType)
                }
                
                val snapshot = queryRef.get().await()
                
                var eventsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    // Etkinlik tarihini parse et
                    val eventDate = dateFormat.parse("${event.date} ${event.time}")
                    // Sadece gelecek etkinlikleri filtrele
                    eventDate?.after(currentDate) ?: false
                }
                
                // Apply text search filter if provided
                if (query.isNotEmpty()) {
                    eventsList = eventsList.filter { event ->
                        event.title.contains(query, ignoreCase = true) || 
                        event.description.contains(query, ignoreCase = true)
                    }
                }
                
                _events.value = eventsList
            } catch (e: Exception) {
                _error.value = "Etkinlikler aranırken bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createEvent(
        title: String,
        description: String,
        sportType: String,
        date: String,
        time: String,
        location: String,
        latitude: Double,
        longitude: Double,
        maxParticipants: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser
                
                if (currentUser != null) {
                    // Get current user's profile information
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: ""
                            
                            // Parse date and time to create endDate
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val endDate = dateFormat.parse("$date $time") ?: Date()
                            
                            val newEvent = Event(
                                title = title,
                                description = description,
                                sportType = sportType,
                                date = date,
                                time = time,
                                location = location,
                                latitude = latitude,
                                longitude = longitude,
                                maxParticipants = maxParticipants,
                                createdBy = currentUser.uid,
                                creatorId = currentUser.uid,
                                creatorUsername = username,
                                participants = listOf(currentUser.uid),
                                endDate = endDate
                            )
                            
                            firestore.collection("events")
                                .add(newEvent)
                                .addOnSuccessListener { documentRef ->
                                    // Yeni oluşturulan etkinliği ID'si ile birlikte güncelle
                                    val eventWithId = newEvent.copy(id = documentRef.id)
                                    documentRef.set(eventWithId)
                                        .addOnSuccessListener {
                                            _isLoading.value = false
                                            _error.value = null
                                            _eventCreated.value = true
                                            loadEvents() // Etkinlikleri yeniden yükle
                                        }
                                        .addOnFailureListener { exception ->
                                            _error.value = exception.message ?: "Etkinlik oluşturulurken bir hata oluştu"
                                            _isLoading.value = false
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    _error.value = exception.message ?: "Etkinlik oluşturulurken bir hata oluştu"
                                    _isLoading.value = false
                                }
                        }
                        .addOnFailureListener { exception ->
                            _error.value = exception.message ?: "Kullanıcı bilgileri alınırken bir hata oluştu"
                            _isLoading.value = false
                        }
                } else {
                    _error.value = "Kullanıcı girişi yapılmamış"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Etkinlik oluşturulurken bir hata oluştu"
                _isLoading.value = false
            }
        }
    }

    fun resetEventCreated() {
        _eventCreated.value = false
    }

    fun resetError() {
        _error.value = null
    }

    fun resetToastMessage() {
        _toastMessage.value = null
    }

    fun sendJoinRequest(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _error.value = "Etkinliğe katılmak için giriş yapmalısınız"
                    _isLoading.value = false
                    return@launch
                }
                
                val eventDoc = eventsCollection.document(eventId).get().await()
                val event = eventDoc.toObject(Event::class.java)
                
                if (event == null) {
                    _error.value = "Etkinlik bulunamadı"
                    _isLoading.value = false
                    return@launch
                }

                // Etkinliği oluşturan kişi kontrolü
                if (event.createdBy == currentUser.uid) {
                    _error.value = "Etkinliği oluşturan kişi zaten katılımcıdır"
                    _isLoading.value = false
                    return@launch
                }
                
                if (event.participants.contains(currentUser.uid)) {
                    _error.value = "Zaten bu etkinliğe katıldınız"
                    _isLoading.value = false
                    return@launch
                }
                
                if (event.pendingRequests.contains(currentUser.uid)) {
                    // If already requested, cancel the request
                    cancelJoinRequest(eventId, currentUser.uid)
                    return@launch
                }
                
                // Add the current user to the pending requests list
                val updatedPendingRequests = event.pendingRequests.toMutableList()
                updatedPendingRequests.add(currentUser.uid)
                
                // Update the event in Firestore
                eventsCollection.document(eventId).update("pendingRequests", updatedPendingRequests).await()
                
                // Update the local events list
                val updatedEvent = event.copy(pendingRequests = updatedPendingRequests)
                val currentEvents = _events.value.map { 
                    if (it.id == eventId) updatedEvent else it 
                }
                _events.value = currentEvents
                
                // Show success message
                _error.value = "Katılma isteği başarıyla gönderildi"
                
            } catch (e: Exception) {
                _error.value = "İstek gönderilirken bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun cancelJoinRequest(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val eventDoc = eventsCollection.document(eventId).get().await()
                val event = eventDoc.toObject(Event::class.java)
                
                if (event == null) {
                    _error.value = "Etkinlik bulunamadı"
                    _isLoading.value = false
                    return@launch
                }
                
                // Remove the user from pending requests list
                val updatedPendingRequests = event.pendingRequests.toMutableList()
                updatedPendingRequests.remove(userId)
                
                // Update the event in Firestore
                eventsCollection.document(eventId).update("pendingRequests", updatedPendingRequests).await()
                
                // Update the local events list
                val updatedEvent = event.copy(pendingRequests = updatedPendingRequests)
                val currentEvents = _events.value.map { 
                    if (it.id == eventId) updatedEvent else it 
                }
                _events.value = currentEvents
                
                // Show success message
                _error.value = "Katılma isteği başarıyla iptal edildi"
                
            } catch (e: Exception) {
                _error.value = "İstek iptal edilirken bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveJoinRequest(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val eventDoc = eventsCollection.document(eventId).get().await()
                val event = eventDoc.toObject(Event::class.java)
                
                if (event == null) {
                    _toastMessage.value = "Etkinlik bulunamadı"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if the current user is the event creator
                if (event.createdBy != FirebaseAuth.getInstance().currentUser?.uid) {
                    _toastMessage.value = "Bu işlem için yetkiniz yok"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if the user is in pending requests
                if (!event.pendingRequests.contains(userId)) {
                    _toastMessage.value = "Kullanıcı isteği bulunamadı"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if the event is full
                if (event.participants.size >= event.maxParticipants) {
                    _toastMessage.value = "Etkinlik kontenjanı dolu"
                    _isLoading.value = false
                    return@launch
                }
                
                // Remove from pending requests and add to participants
                val updatedPendingRequests = event.pendingRequests.toMutableList()
                updatedPendingRequests.remove(userId)
                
                val updatedParticipants = event.participants.toMutableList()
                updatedParticipants.add(userId)
                
                // Update the event in Firestore
                eventsCollection.document(eventId).update(
                    mapOf(
                        "pendingRequests" to updatedPendingRequests,
                        "participants" to updatedParticipants
                    )
                ).await()
                
                // Update the local events list
                val updatedEvent = event.copy(
                    pendingRequests = updatedPendingRequests,
                    participants = updatedParticipants
                )
                val currentEvents = _events.value.map { 
                    if (it.id == eventId) updatedEvent else it 
                }
                _events.value = currentEvents
                
                // Show success message
                _toastMessage.value = "Katılım isteği onaylandı"
                
            } catch (e: Exception) {
                _toastMessage.value = "İstek onaylanırken bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectJoinRequest(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val eventDoc = eventsCollection.document(eventId).get().await()
                val event = eventDoc.toObject(Event::class.java)
                
                if (event == null) {
                    _toastMessage.value = "Etkinlik bulunamadı"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if the current user is the event creator
                if (event.createdBy != FirebaseAuth.getInstance().currentUser?.uid) {
                    _toastMessage.value = "Bu işlem için yetkiniz yok"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if the user is in pending requests
                if (!event.pendingRequests.contains(userId)) {
                    _toastMessage.value = "Kullanıcı isteği bulunamadı"
                    _isLoading.value = false
                    return@launch
                }
                
                // Remove from pending requests
                val updatedPendingRequests = event.pendingRequests.toMutableList()
                updatedPendingRequests.remove(userId)
                
                // Update the event in Firestore
                eventsCollection.document(eventId).update(
                    "pendingRequests", updatedPendingRequests
                ).await()
                
                // Update the local events list
                val updatedEvent = event.copy(pendingRequests = updatedPendingRequests)
                val currentEvents = _events.value.map { 
                    if (it.id == eventId) updatedEvent else it 
                }
                _events.value = currentEvents
                
                // Show success message
                _toastMessage.value = "Katılım isteği reddedildi"
                
            } catch (e: Exception) {
                _toastMessage.value = "İstek reddedilirken bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        sportType: String,
        date: String,
        time: String,
        location: String,
        latitude: Double,
        longitude: Double,
        maxParticipants: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _error.value = "Kullanıcı girişi yapılmamış"
                    _isLoading.value = false
                    return@launch
                }
                
                // Get the existing event
                val eventDoc = eventsCollection.document(eventId).get().await()
                val existingEvent = eventDoc.toObject(Event::class.java)
                
                if (existingEvent == null) {
                    _error.value = "Etkinlik bulunamadı"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if the current user is the event creator
                if (existingEvent.createdBy != currentUser.uid) {
                    _error.value = "Bu etkinliği düzenleme yetkiniz yok"
                    _isLoading.value = false
                    return@launch
                }
                
                // Parse date and time to create endDate
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val endDate = dateFormat.parse("$date $time") ?: Date()
                
                // Create updated event
                val updatedEvent = existingEvent.copy(
                    title = title,
                    description = description,
                    sportType = sportType,
                    date = date,
                    time = time,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    maxParticipants = maxParticipants,
                    endDate = endDate
                )
                
                // Update the event in Firestore
                eventsCollection.document(eventId).set(updatedEvent).await()
                
                _toastMessage.value = "Etkinlik başarıyla güncellendi"
                
                // Reload all events to ensure consistency
                loadEvents()
                
            } catch (e: Exception) {
                _error.value = "Etkinlik güncellenirken bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 