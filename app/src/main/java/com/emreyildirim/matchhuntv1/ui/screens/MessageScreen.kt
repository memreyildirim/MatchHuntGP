package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Message
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.ReportViewModel
import com.emreyildirim.matchhuntv1.ui.theme.* // Tema renklerini buradan import ediyoruz
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

sealed class MessageListItem {
    data class MessageItem(val message: Message) : MessageListItem()
    data class DateHeader(val date: Date, val label: String) : MessageListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    targetUserId: String? = null,
    viewModel: MessageViewModel,
    reportViewModel: ReportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var targetUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    val firestore = FirebaseFirestore.getInstance()
    val listState = rememberLazyListState()

    var hasInitialScrollDone by remember { mutableStateOf(false) }
    var lastKnownLastMessageId by remember { mutableStateOf<String?>(null) }

    val messageListItems = remember(messages) {
        buildMessageListWithHeaders(messages)
    }

    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReasonCode by remember { mutableStateOf("abuse") }
    var reasonText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val reportSubmittedText = stringResource(R.string.toast_report_submitted_thanks)

    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            // Konuşmayı gizlenen listeden çıkar (kullanıcı chat'e girdi)
            viewModel.unhideConversation(targetUserId)
            
            firestore.collection("users").document(targetUserId).get()
                .addOnSuccessListener { targetUserProfile = it.toObject(UserProfile::class.java) }
        }
    }

    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            viewModel.loadInitialMessages(targetUserId)
            viewModel.startMessageListener(targetUserId)
            viewModel.markMessagesAsRead(targetUserId)
        } else {
            viewModel.stopMessageListener()
        }
    }

    LaunchedEffect(listState, isLoading, messages.size, targetUserId) {
        if (targetUserId == null) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }.collectLatest { index ->
            if (!hasInitialScrollDone) return@collectLatest
            if (index == 0 && !isLoading && messages.isNotEmpty()) {
                viewModel.loadMoreMessages()
            }
        }
    }

    LaunchedEffect(messageListItems.size) {
        if (messageListItems.isEmpty()) return@LaunchedEffect
        try {
            val lastMessageItem = messageListItems.lastOrNull() as? MessageListItem.MessageItem
            val currentLastId = lastMessageItem?.message?.id ?: return@LaunchedEffect

            if (!hasInitialScrollDone) {
                listState.scrollToItem(messageListItems.lastIndex)
                hasInitialScrollDone = true
                lastKnownLastMessageId = currentLastId
                return@LaunchedEffect
            }

            if (lastKnownLastMessageId != currentLastId) {
                listState.animateScrollToItem(messageListItems.lastIndex)
                lastKnownLastMessageId = currentLastId
                if (targetUserId != null) viewModel.markMessagesAsRead(targetUserId)
            }
        } catch (_: Exception) { /* scroll/markRead failure is non-critical */ }
    }

    val chatBackground = Brush.verticalGradient(
        colors = listOf(Color.White, SoftGray)
    )

    Scaffold(
        containerColor = SoftGray,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { targetUserId?.let { onNavigateToProfile(it) } }
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = SoftGray
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
                            text = targetUserProfile?.username ?: stringResource(R.string.loading),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Obsidian
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = Obsidian)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Obsidian)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = MaterialTheme.colorScheme.background
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.report_chat_title)) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(chatBackground)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoading && messages.isNotEmpty()) {
                    item { Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), Obsidian) } }
                }

                items(
                    items = messageListItems,
                    key = { item ->
                        when (item) {
                            is MessageListItem.MessageItem -> item.message.id
                            is MessageListItem.DateHeader -> "date_${item.date.time}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is MessageListItem.MessageItem -> ModernMessageItem(message = item.message)
                        is MessageListItem.DateHeader -> DateHeaderItem(label = item.label)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                shape = RoundedCornerShape(30.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.message_placeholder), color = Color.Gray, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Obsidian
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && targetUserId != null) {
                                viewModel.sendMessage(messageText.trim(), targetUserId)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Obsidian, CircleShape),
                        enabled = messageText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send),
                            modifier = Modifier.size(18.dp),
                            tint = BrandVolt
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        val lastMessage = messages.lastOrNull()

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (lastMessage != null) {
                            reportViewModel.reportMessage(
                                message = lastMessage,
                                reasonCode = selectedReasonCode,
                                reasonText = reasonText
                            )
                        }
                        Toast.makeText(
                            context,
                            reportSubmittedText,
                            Toast.LENGTH_SHORT
                        ).show()
                        showReportDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_send))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.report_chat_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.report_select_reason_title), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReasonCode = "abuse" }
                    ) {
                        RadioButton(
                            selected = selectedReasonCode == "abuse",
                            onClick = { selectedReasonCode = "abuse" }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.report_reason_harassment_content))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReasonCode = "spam" }
                    ) {
                        RadioButton(
                            selected = selectedReasonCode == "spam",
                            onClick = { selectedReasonCode = "spam" }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.report_reason_spam))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReasonCode = "other" }
                    ) {
                        RadioButton(
                            selected = selectedReasonCode == "other",
                            onClick = { selectedReasonCode = "other" }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.report_reason_other))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text(stringResource(R.string.report_optional_detail)) },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
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
        Surface(
            color = if (isCurrentUser) Obsidian else IncomingGray,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isCurrentUser) 18.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = if (isCurrentUser) BrandVolt else Obsidian
                )
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isCurrentUser) BrandVolt else Obsidian).copy(alpha = 0.5f)
                    )
                    
                    if (isCurrentUser) {
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isRead) "Okundu" else "Gönderildi",
                            tint = if (message.isRead) BrandVolt else BrandVolt.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeaderItem(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.05f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun buildMessageListWithHeaders(messages: List<Message>): List<MessageListItem> {
    if (messages.isEmpty()) return emptyList()
    val items = mutableListOf<MessageListItem>()
    var lastDate: Calendar? = null
    for (message in messages) {
        val messageDate = Calendar.getInstance().apply { time = message.timestamp }
        if (lastDate == null || !isSameDay(lastDate, messageDate)) {
            items.add(MessageListItem.DateHeader(message.timestamp, formatDateHeader(message.timestamp)))
            lastDate = messageDate
        }
        items.add(MessageListItem.MessageItem(message))
    }
    return items
}

fun formatDateHeader(timestamp: Date): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = timestamp }
    if (isSameDay(now, messageTime)) return "Bugün"
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (isSameDay(yesterday, messageTime)) return "Dün"
    val daysDiff = TimeUnit.MILLISECONDS.toDays(now.timeInMillis - messageTime.timeInMillis)
    if (daysDiff < 7) return SimpleDateFormat("EEEE", Locale("tr", "TR")).format(timestamp)
    val dateFormat = if (now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)) SimpleDateFormat("d MMMM", Locale("tr", "TR"))
    else SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))
    return dateFormat.format(timestamp)
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}