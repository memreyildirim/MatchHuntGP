package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.R
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
    var lastKnownLastMessageId by remember { mutableStateOf<String?>(null) }

    // Hedef profil yükleme
    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            firestore.collection("users").document(targetUserId).get()
                .addOnSuccessListener { targetUserProfile = it.toObject(UserProfile::class.java) }
        }
    }

    // Listener ve Mesaj Başlatma
    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            viewModel.loadInitialMessages(targetUserId)
            viewModel.startMessageListener(targetUserId)
            viewModel.markMessagesAsRead(targetUserId)
        } else {
            viewModel.stopMessageListener()
        }
    }

    // Pagination: Listenin tepesine ulaşıldığında eski mesajları yükle
    LaunchedEffect(listState, isLoading, messages.size, targetUserId) {
        if (targetUserId == null) return@LaunchedEffect

        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                if (!hasInitialScrollDone) return@collectLatest

                // Listenin en üstündeysek ve yükleme yapılmıyorsa
                if (index == 0 && !isLoading && messages.isNotEmpty()) {
                    viewModel.loadMoreMessages()
                }
            }
    }

    // Akıllı Scroll Yönetimi
    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect

        try {
            val currentLastId = messages.last().id

            // 1) İlk yüklemede: her durumda en alta git
            if (!hasInitialScrollDone) {
                listState.scrollToItem(messages.lastIndex)
                hasInitialScrollDone = true
                lastKnownLastMessageId = currentLastId
                if (targetUserId != null) {
                    viewModel.markMessagesAsRead(targetUserId)
                }
                return@LaunchedEffect
            }

            // 2) Pagination: eski mesajlar eklendiyse (son mesaj aynıysa) scroll etme
            if (lastKnownLastMessageId == currentLastId) {
                return@LaunchedEffect
            }

            // 3) Yeni mesaj geldi: en alta kaydır
            listState.animateScrollToItem(messages.lastIndex)
            lastKnownLastMessageId = currentLastId

            if (targetUserId != null) {
                viewModel.markMessagesAsRead(targetUserId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val chatBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            AsyncImage(
                                model = targetUserProfile?.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier.clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.ic_profile_placeholder)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = targetUserProfile?.username ?: "Yükleniyor...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Sohbet Seçenekleri */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(chatBackground)
        ) {
            // Mesaj Listesi
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(
                    bottom = 96.dp,
                    top = 8.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Üst Pagination Loader'ı
                if (isLoading && messages.isNotEmpty()) {
                    item(key = "top_loader") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }

                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    ModernMessageItem(message = message)
                }
            }

            // --- YÜZER GİRİŞ ALANI (Floating Input) ---
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mesaj yaz...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank() && targetUserId != null) {
                                viewModel.sendMessage(messageText, targetUserId)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() && targetUserId != null && !isLoading,
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernMessageItem(message: Message) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isCurrentUser = message.senderId == currentUserId

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isCurrentUser) 20.dp else 4.dp,
                    bottomEnd = if (isCurrentUser) 4.dp else 20.dp
                ),
                tonalElevation = if (isCurrentUser) 0.dp else 2.dp,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}