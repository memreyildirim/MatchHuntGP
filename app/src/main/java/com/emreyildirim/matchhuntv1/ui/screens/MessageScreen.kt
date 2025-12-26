package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.emreyildirim.matchhuntv1.data.model.Message
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    onNavigateBack: () -> Unit,
    targetUserId: String? = null,
    viewModel: MessageViewModel
) {
    var messageText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var targetUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    val firestore = FirebaseFirestore.getInstance()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var hasInitialScrollDone by remember { mutableStateOf(false) }
    // Son görünen mesajın id'sini tutarak, sadece gerçekten yeni mesaj geldiğinde auto-scroll yapacağız
    var lastKnownLastMessageId by remember { mutableStateOf<String?>(null) }

    // Hedef kullanıcının profilini yükle
    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            try {
                firestore.collection("users")
                    .document(targetUserId)
                    .get()
                    .addOnSuccessListener { document ->
                        targetUserProfile = document.toObject(UserProfile::class.java)
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Mesajları yükle + listener başlat + okunmamışları güncelle
    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            Log.d("MessageScreen", "Setting up chat for userId=$targetUserId")
            viewModel.loadInitialMessages(targetUserId)
            viewModel.startMessageListener(targetUserId)
            viewModel.markMessagesAsRead(targetUserId)
        } else {
            // targetUserId null ise listener'ı durdur
            viewModel.stopMessageListener()
        }
    }
    
    // Ekran görünürken listener'ın aktif olduğundan emin ol
    DisposableEffect(targetUserId) {
        if (targetUserId != null) {
            Log.d("MessageScreen", "MessageScreen entered for userId=$targetUserId")
            // Listener zaten LaunchedEffect'te başlatılıyor, burada sadece kontrol ediyoruz
            onDispose {
                Log.d("MessageScreen", "MessageScreen disposed for userId=$targetUserId")
                // Ekran kapandığında listener'ı durdurma - ViewModel'de yönetiliyor
                // Sadece chat değiştiğinde durdurulacak
            }
        }
        onDispose { }
    }

    // Listenin tepesine ulaşıldığında daha eski mesajları yükle
    LaunchedEffect(listState, isLoading, messages.size, targetUserId) {
        if (targetUserId == null) return@LaunchedEffect

        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                // İlk açılışta henüz son mesaja scroll etmeden pagination tetikleme
                if (!hasInitialScrollDone) return@collectLatest

                // Listenin en üstündeysek (en eski mesajlar tarafı)
                if (index == 0 && !isLoading && messages.isNotEmpty()) {
                    viewModel.loadMoreMessages()
                    Log.d("MessageScreen", "Loading more messages")
                }
            }
    }

    // Mesajlar yüklendiğinde / değiştiğinde scroll davranışı
    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect

        try {
            val currentLastId = messages.last().id

            // 1) İlk yüklemede: her durumda en alta git, pagination'ı tetikleyene kadar hasInitialScrollDone=false idi
            if (!hasInitialScrollDone) {
                listState.scrollToItem(messages.lastIndex)
                hasInitialScrollDone = true
                lastKnownLastMessageId = currentLastId
                if (targetUserId != null) {
                    viewModel.markMessagesAsRead(targetUserId)
                }
                return@LaunchedEffect
            }

            // 2) Pagination: eski mesajlar liste başına eklenir, son mesaj aynı kalır → lastId değişmez.
            //    Bu durumda hiçbir şey yapma ki kullanıcı en üstte kalabilsin.
            if (lastKnownLastMessageId == currentLastId) {
                return@LaunchedEffect
            }

            // 3) Gerçekten yeni bir mesaj eklendi (gönderilen ya da alınan)
            //    Kullanıcı chat ekranındayken otomatik olarak en alta scroll et.
            listState.animateScrollToItem(messages.lastIndex)
            lastKnownLastMessageId = currentLastId

            if (targetUserId != null) {
                viewModel.markMessagesAsRead(targetUserId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (targetUserId != null) {
                        Text(targetUserProfile?.username ?: "Messages")
                    } else {
                        Text("Messages")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Eğer hiç mesaj yoksa ve yükleniyorsa: tam ekran loader
            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Mesajlar Listesi
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .padding(bottom = 80.dp)
            ) {
                // Pagination sırasında, listenin en üstünde küçük bir loader göster
                if (isLoading && messages.isNotEmpty()) {
                    item(key = "top_loader") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // messages zaten eski → yeni sıralı, en yeni mesaj en altta görünecek
                // key vererek, tepeye eski mesaj eklendiğinde scroll pozisyonunun korunmasını sağlıyoruz
                items(
                    items = messages,
                    key = { message -> message.id }
                ) { message ->
                    MessageItem(message = message)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Mesaj Girişi
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Write a message...") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && targetUserId != null) {
                                viewModel.sendMessage(messageText, targetUserId)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() && !isLoading && targetUserId != null,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val isCurrentUser = message.senderId == currentUserId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrentUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentUser) 
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
} 