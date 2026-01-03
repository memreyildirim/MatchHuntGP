package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.ui.viewmodel.SocialFeedViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.utils.Sports
import com.google.firebase.auth.FirebaseAuth
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
    onNavigateToCreatePost: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: SocialFeedViewModel = viewModel(),
    messageViewModel: MessageViewModel
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMorePosts by viewModel.hasMorePosts.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val navigateToProfile by viewModel.navigateToProfile.collectAsState()
    
    // Sadece toplam okunmamış mesaj sayısını dinle - her render'da hesaplama yapılmaz
    val unreadMessageCount by messageViewModel.totalUnreadCount.collectAsState()

    val lazyListState = rememberLazyListState()
    val swipeRefreshState = rememberSwipeRefreshState(isLoading)

    // Konuşmaları yükle ve dinle - sadece bir kez
    LaunchedEffect(Unit) {
        messageViewModel.loadConversations()
        messageViewModel.startConversationListener()
    }

    LaunchedEffect(navigateToProfile) {
        navigateToProfile?.let { userId ->
            onNavigateToProfile(userId)
            viewModel.onProfileNavigationHandled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            lastVisibleItemIndex >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && hasMorePosts && !isLoadingMore) {
                viewModel.loadMorePosts()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Ekran arka planı
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Social Feed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            fontSize = 27.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToMessages) {
                        BadgedBox(
                            badge = {
                                if (unreadMessageCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                    ) { 
                                        Text(
                                            if (unreadMessageCount > 99) "99+" else unreadMessageCount.toString(),
                                            fontSize = 9.sp, 
                                            color = Color.White
                                        ) 
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Message,
                                contentDescription = "Messages",
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background // TopBar rengi arka planla aynı yapılarak uyumsuzluk giderildi
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePost,
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 85.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, "Create", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.loadPosts() },
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            indicator = { state, refreshTrigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = refreshTrigger,
                    contentColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        ) {
            if (isLoading && posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 120.dp,
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            obsidian = MaterialTheme.colorScheme.primary,
                            brandVolt = MaterialTheme.colorScheme.onPrimary,
                            onLikeClick = { viewModel.likePost(post.id) },
                            onCommentSubmit = { viewModel.addComment(post.id, it) },
                            onNavigateToProfile = { viewModel.navigateToProfile(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    obsidian: Color,
    brandVolt: Color,
    onLikeClick: (String) -> Unit,
    onCommentSubmit: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val userRepository = remember { UserRepository() }
    var profileImageUrl by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var showComments by remember { mutableStateOf(false) }

    var isLiked by remember(post.likedBy) { mutableStateOf(post.likedBy.contains(currentUserId)) }
    var likeCount by remember(post.likes) { mutableStateOf(post.likes) }

    var showLikeHeart by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showLikeHeart) { 1.2f } else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "HeartScale"
    )

    val scope = rememberCoroutineScope()
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val sportColor = remember(post.sportType) {
        Sports.getSportInfo(post.sportType)?.color ?: brandVolt
    }

    LaunchedEffect(post.userId) {
        val userData = userRepository.getUserProfileData(post.userId)
        profileImageUrl = (userData?.get("profileImageUrl") as? String) ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToProfile(post.userId) },
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(post.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = obsidian)
                    Text(
                        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(post.createdAt),
                        fontSize = 11.sp, color = Color.Gray
                    )
                }

                // Sport Type Alanı: Siyah zemin üzerine sporun kendi rengi
                Surface(
                    color = obsidian,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        post.sportType.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = sportColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            if (!isLiked) {
                                isLiked = true
                                likeCount++
                                onLikeClick(post.id)
                            }
                            showLikeHeart = true
                            scope.launch { delay(800); showLikeHeart = false }
                        }
                        lastTapTime = now
                    }
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = brandVolt.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center).size(100.dp).scale(heartScale)
                )
            }

            Row(
                modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    isLiked = !isLiked
                    likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                    onLikeClick(post.id)
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else obsidian,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text("${likeCount}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = obsidian)

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { showComments = !showComments }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = obsidian, modifier = Modifier.size(24.dp))
                }
                Text("${post.comments.size}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = obsidian)
            }

            if (post.description.isNotBlank()) {
                Text(
                    text = post.description,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = obsidian
                )
            }

            AnimatedVisibility(
                visible = showComments,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color(0xFFF9FAFB))
                        .padding(bottom = 12.dp)
                ) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        post.comments.forEach { comment ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = comment.userName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = obsidian
                                        ),
                                        modifier = Modifier.clickable { onNavigateToProfile(comment.userId) }
                                    )
                                    Text(
                                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(comment.createdAt),
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 10.sp)
                                    )
                                }
                                Text(
                                    text = comment.content,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = obsidian),
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (commentText.isEmpty()) {
                                    Text("Add comment...", color = Color.Gray, fontSize = 13.sp)
                                }
                                innerTextField()
                            }
                        )

                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onCommentSubmit(commentText)
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (commentText.isNotBlank()) obsidian else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}