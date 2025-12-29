package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Message
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: MessageViewModel
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Profil ekranındaki gradyan arka plan ile uyumluluk
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    )

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
        viewModel.startConversationListener()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- ARAMA ÇUBUĞU ---
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = { Text("Search messages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- İÇERİK DURUMLARI ---
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        ErrorState(message = error ?: "", onRetry = { viewModel.loadConversations() })
                    }
                    conversations.isEmpty() -> {
                        EmptyState()
                    }
                    else -> {
                        val filteredConversations = if (searchQuery.isEmpty()) {
                            conversations
                        } else {
                            conversations.filter {
                                it.userProfile.username.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(filteredConversations) { conversation ->
                                ModernConversationItem(
                                    conversation = conversation,
                                    onClick = { onNavigateToChat(conversation.userId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profil Resmi (Gölge ve Border ile)
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = conversation.userProfile.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_profile_placeholder)
                )
                // Online statüsü gibi ek bilgiler buraya gelebilir
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Kullanıcı Adı ve Mesaj Önizlemesi
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.userProfile.username,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Akıllı Zaman Formatı
                    Text(
                        text = formatTimestamp(conversation.lastMessage?.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (conversation.unreadCount > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.lastMessage?.text ?: "No message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.unreadCount > 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(conversation.unreadCount.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Start a conversation to see it here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

private fun formatTimestamp(timestamp: Date?): String {
    if (timestamp == null) return ""
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { time = timestamp }

    return when {
        now.get(Calendar.DATE) == time.get(Calendar.DATE) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)
        }
        now.get(Calendar.DATE) - time.get(Calendar.DATE) == 1 -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(timestamp)
        }
    }
}

data class Conversation(
    val userId: String,
    val userProfile: UserProfile,
    val lastMessage: Message?,
    val unreadCount: Int = 0
)