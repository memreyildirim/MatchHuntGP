package com.emreyildirim.matchhuntv1.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.data.model.Comment
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.data.repository.ReviewRepository
import com.emreyildirim.matchhuntv1.data.repository.PostRepository
import com.emreyildirim.matchhuntv1.ui.components.ReviewCard
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.PostViewModel
import com.emreyildirim.matchhuntv1.utils.RatingCard.RatingStatItem
import com.emreyildirim.matchhuntv1.utils.Sports
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    val reviewRepository = ReviewRepository()
    val postRepository = PostRepository()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var sports by remember { mutableStateOf<List<String>>(emptyList()) }
    var profileImageUrl by remember { mutableStateOf("") }
    var averageRating by remember { mutableStateOf(0f) }
    var skillRating by remember { mutableStateOf(0f) }
    var behaviorRating by remember { mutableStateOf(0f) }
    var teamRating by remember { mutableStateOf(0f) }
    var totalReviews by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var userReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var showPostDetailSheet by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var allPostsForSheet by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoadingAllPosts by remember { mutableStateOf(false) }

    // Animasyon State'i
    var isVisible by remember { mutableStateOf(false) }

    val userId = remember { auth.currentUser?.uid ?: "" }

    // Pull to Refresh State
    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // Veri yükleme fonksiyonu
    suspend fun loadProfileData() {
        if (userId.isNotEmpty()) {
            try {
                // Önce temel profil verilerini yükle (hızlı)
                val userData = userRepository.getUserProfileData(userId)
                userData?.let { data ->
                    username = (data["username"] as? String) ?: ""
                    age = (data["age"] as? Number)?.toString() ?: ""
                    city = (data["city"] as? String) ?: ""
                    about = (data["about"] as? String) ?: ""
                    sports = (data["sports"] as? List<String>)?.filter { it.isNotEmpty() } ?: emptyList()
                    profileImageUrl = (data["profileImageUrl"] as? String) ?: ""
                }

                // UI'ı göster (temel veriler yüklendi)
                isLoading = false
                delay(100)
                isVisible = true

                // Sonra rating verilerini paralel olarak yükle (arka planda)
                coroutineScope {
                    val ratingsAndCountDeferred = async {
                        reviewRepository.getUserRatingsAndCount(userId)
                    }
                    // Sadece grid için gerekli olan ilk 6 postu yükle (optimizasyon)
                    val postsDeferred = async {
                        postRepository.getUserPostsLimited(userId, limit = 6)
                    }

                    ratingsAndCountDeferred.await().getOrNull()?.let { result ->
                        val ratings = result.first
                        val count = result.second
                        skillRating = ratings["skill"] ?: 0f
                        behaviorRating = ratings["behavior"] ?: 0f
                        teamRating = ratings["team"] ?: 0f
                        averageRating = ratings["average"] ?: 0f
                        totalReviews = count
                    }

                    // Sadece ilk 6 postu yükle (grid için yeterli)
                    userPosts = postsDeferred.await()
                }
            } catch (e: Exception) {
                isLoading = false
                delay(100)
                isVisible = true
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            loadProfileData()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_screen_title), fontWeight = FontWeight.ExtraBold, fontSize = 27.sp) },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("editProfile") {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "profileData", mapOf(
                                    "username" to username,
                                    "age" to age,
                                    "city" to city,
                                    "sports" to sports.joinToString(","),
                                    "photoUrl" to profileImageUrl
                                )
                            )
                        }
                    }) {
                        Icon(Icons.Default.Edit,tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp),contentDescription = stringResource(R.string.cd_edit_profile))
                    }
                    IconButton(onClick = {
                        authViewModel.signOut()
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, modifier = Modifier.size(30.dp), contentDescription = stringResource(R.string.cd_logout_icon), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    loadProfileData()
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            indicator = { state, refreshTrigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = refreshTrigger,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // 1. Header Section (Hero)
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(600)) + expandVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .shadow(24.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                                .border(4.dp, Color.White, CircleShape)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(profileImageUrl.ifEmpty { R.drawable.ic_profile_placeholder })
                                    .crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = username.ifEmpty { stringResource(R.string.profile_default_display_name) },
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$city • $age Years", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                // 2. Stats Dashboard
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(800))
                ) {
                    StatsCard(
                        averageRating = averageRating,
                        totalReviews = totalReviews,
                        skillRating = skillRating,
                        behaviorRating = behaviorRating,
                        teamRating = teamRating,
                        onReviewsClick = {
                            scope.launch {
                                userReviews = reviewRepository.getUserReviews(userId)
                                showReviewsSheet = true
                            }
                        }
                    )
                }

                // 3. About Me Section
                if (about.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(initialOffsetY = { 200 }) + fadeIn(tween(1000))
                    ) {
                        Column {
                            SectionTitle(title = stringResource(R.string.profile_section_about_me), icon = Icons.Default.Info)
                            AboutCard(about = about)
                        }
                    }
                }

                // 4. Sports Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { 300 }) + fadeIn(tween(1200))
                ) {
                    Column {
                        SectionTitle(title = stringResource(R.string.profile_section_interests), icon = Icons.Default.Interests)
                        SportsRow(sports = sports)
                    }
                }

                // 5. Posts Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { 400 }) + fadeIn(tween(1400))
                ) {
                    Column {
                        SectionTitle(title = stringResource(R.string.profile_section_my_posts), icon = Icons.Default.GridOn)
                        UserPostsGrid(
                            posts = userPosts,
                            onPostClick = { post ->
                                selectedPost = post
                                showPostDetailSheet = true
                            },
                            onViewAllClick = {
                                // Tüm postları göster
                                selectedPost = null
                                showPostDetailSheet = true
                            }
                        )
                    }
                }

                // Minimal Footer Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.privacy_policy),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gist.github.com/memreyildirim/13ef72599604ee61e631d12781ad55bf"))
                                context.startActivity(intent)
                            }
                        )
                        Text("|", color = Color.LightGray)
                        Text(
                            text = stringResource(R.string.terms_of_service),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { /* Terms link */ }
                        )
                        Text("|", color = Color.LightGray)
                        Text(
                            text = stringResource(R.string.change_feedback),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@matchhunt.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "MatchHunt Geri Bildirim & Destek")
                                    putExtra(Intent.EXTRA_TEXT, "\n\n---\nKullanıcı: $username\nMatchHunt v1.0")
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "E-posta Gönder"))
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.profile_feedback_err_no_email_app),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "MatchHunt v1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    // Reviews BottomSheet
    if (showReviewsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReviewsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(stringResource(R.string.profile_reviews_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                Spacer(modifier = Modifier.height(16.dp))
                if (userReviews.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.profile_no_reviews), color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(userReviews) { ReviewCard(review = it) }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Posts BottomSheet - Lazy Loading ile tüm postları yükle
    if (showPostDetailSheet) {
        // Bottom sheet açıldığında tüm postları lazy olarak yükle
        LaunchedEffect(showPostDetailSheet) {
            if (showPostDetailSheet && allPostsForSheet.isEmpty() && !isLoadingAllPosts) {
                isLoadingAllPosts = true
                allPostsForSheet = postRepository.getUserPosts(userId)
                isLoadingAllPosts = false
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                showPostDetailSheet = false
                selectedPost = null
                // Bottom sheet kapandığında state'i temizle (bir sonraki açılışta tekrar yüklensin)
                allPostsForSheet = emptyList()
            },
            sheetState = rememberModalBottomSheetState(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    stringResource(R.string.profile_section_my_posts),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingAllPosts) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (allPostsForSheet.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.profile_no_posts), color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(allPostsForSheet) { post ->
                            PostDetailCard(
                                post = post,
                                postRepository = postRepository,
                                currentUserId = userId,
                                onNavigateToProfile = { targetUserId ->
                                    navController.navigate("user_profile/$targetUserId")
                                },
                                onPostDeleted = { deletedPostId ->
                                    // Optimistic update - hemen UI'dan kaldır
                                    userPosts = userPosts.filter { it.id != deletedPostId }
                                    allPostsForSheet = allPostsForSheet.filter { it.id != deletedPostId }

                                    // Seçili post silindiyse temizle
                                    if (selectedPost?.id == deletedPostId) {
                                        selectedPost = null
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatsCard(
    averageRating: Float,
    totalReviews: Int,
    skillRating: Float,
    behaviorRating: Float,
    teamRating: Float,
    onReviewsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.profile_overall_rating), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%.1f", averageRating),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 32.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(stringResource(R.string.profile_rating_suffix), color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp, start = 2.dp))
                    }
                }
                Surface(
                    onClick = onReviewsClick,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$totalReviews", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(R.string.profile_review_rating_count_label), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RatingStatItem(label = stringResource(R.string.profile_review_rating_label_skill), value = skillRating)
                RatingStatItem(label = stringResource(R.string.profile_review_rating_label_behavior), value = behaviorRating)
                RatingStatItem(label = stringResource(R.string.profile_review_rating_label_team), value = teamRating)
            }
        }
    }
}

@Composable
fun AboutCard(about: String) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = about,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = Color.DarkGray,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )
            if (about.length > 150) {
                Text(
                    text = if (isExpanded) stringResource(R.string.profile_about_show_less) else stringResource(R.string.profile_about_read_more),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp).clickable { isExpanded = !isExpanded }
                )
            }
        }
    }
}

@Composable
fun SportsRow(sports: List<String>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sports) { sport ->
            val sportInfo = Sports.getSportInfo(sport)
            val color = sportInfo?.color ?: MaterialTheme.colorScheme.primary
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sportInfo != null) {
                        Image(
                            painter = painterResource(id = sportInfo.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = sportInfo?.name ?: sport,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = color
                    )
                }
            }
        }
    }
}


@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.Gray
        )
    }
}

@Composable
fun UserPostsGrid(
    posts: List<Post>,
    onPostClick: (Post) -> Unit,
    onViewAllClick: () -> Unit
) {
    if (posts.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.profile_posts_grid_empty),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // İlk 6 postu göster (2 satır x 3 sütun)
            val postsToShow = posts.take(6)

            repeat(2) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) { col ->
                        val index = row * 3 + col
                        if (index < postsToShow.size) {
                            val post = postsToShow[index]
                            PostThumbnail(
                                post = post,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onPostClick(post) }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (row < 1) Spacer(modifier = Modifier.height(2.dp))
            }

            // Eğer 6'dan fazla post varsa "View All" butonu
            if (posts.size > 6) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onViewAllClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.profile_view_all_posts, posts.size),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PostThumbnail(
    post: Post,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(post.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Overlay - beğeni ve yorum sayısı
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )

        // İstatistikler
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Favorite,
                null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${post.likes}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PostDetailCard(
    post: Post,
    postRepository: PostRepository,
    currentUserId: String,
    onNavigateToProfile: (String) -> Unit = {},
    postViewModel: PostViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onPostDeleted: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val postDeletedText = stringResource(R.string.toast_post_deleted)
    val userRepository = remember { UserRepository() }
    val auth = FirebaseAuth.getInstance()
    var profileImageUrl by remember { mutableStateOf("") }
    var isLiked by remember(post.likedBy) { mutableStateOf(post.likedBy.contains(currentUserId)) }
    var likeCount by remember(post.likes) { mutableStateOf(post.likes) }
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val sportColor = Sports.getSportInfo(post.sportType)?.color ?: MaterialTheme.colorScheme.primary

    // ViewModel state'lerini observe et
    val isDeleting by postViewModel.isDeleting.collectAsState()
    val deleteError by postViewModel.deleteError.collectAsState()
    val deleteSuccess by postViewModel.deleteSuccess.collectAsState()
    val postDeleteErrorText = stringResource(R.string.profile_toast_error_with_message, deleteError ?: "")

    // Toast mesajlarını göster
    LaunchedEffect(deleteSuccess) {
        deleteSuccess?.let {
            Toast.makeText(context, postDeletedText, Toast.LENGTH_SHORT).show()
            Log.d("PostDetailCard", "Post deleted successfully")
            postViewModel.clearDeleteSuccess()
        }
    }

    LaunchedEffect(deleteError) {
        deleteError?.let {
            Toast.makeText(context, postDeleteErrorText, Toast.LENGTH_SHORT).show()
            Log.e("PostDetailCard", "Error deleting post: $it")
            postViewModel.clearDeleteError()
        }
    }

    LaunchedEffect(post.userId) {
        val userData = userRepository.getUserProfileData(post.userId)
        profileImageUrl = (userData?.get("profileImageUrl") as? String) ?: ""
    }

    LaunchedEffect(currentUserId) {
        val userData = userRepository.getUserProfileData(currentUserId)
        currentUserName = (userData?.get("username") as? String) ?: ""
    }

    LaunchedEffect(showComments, post.id) {
        if (showComments && comments.isEmpty()) {
            comments = postRepository.getComments(post.id)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Header: User Info
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profileImageUrl.ifEmpty { R.drawable.ic_profile_placeholder })
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(post.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(post.createdAt),
                        fontSize = 11.sp, color = Color.Gray
                    )
                }
                Surface(
                    color = sportColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        post.sportType,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = sportColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        DropdownMenuItem(
                            text = { 
                                if (isDeleting) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.profile_deleting))
                                    }
                                } else {
                                    Text(stringResource(R.string.profile_delete_post))
                                }
                            },
                            onClick = {
                                if (!isDeleting) {
                                    showMenu = false
                                    postViewModel.deletePost(post.id) { deletedPostId ->
                                        onPostDeleted(deletedPostId)
                                    }
                                }
                            },
                            enabled = !isDeleting
                        )
//                        DropdownMenuItem(
//                            text = { Text("Paylaş") },
//                            onClick = {
//                                showMenu = false
//                                // TODO: Paylaş işlemi
//                            }
//                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.report_action_report)) },
                            onClick = {
                                showMenu = false
                                // TODO: Şikayet etme işlemi
                            }
                        )
                    }
                }
            }

            // Body: Main Content (Image)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(post.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            // Actions Bar
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    // Optimistic update
                    val wasLiked = isLiked
                    val originalLikeCount = likeCount
                    isLiked = !isLiked
                    likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                    
                    // ViewModel ile like işlemi
                    postViewModel.likePost(
                        postId = post.id,
                        onSuccess = {
                            // Başarılı olduğunda optimistic update zaten yapıldı
                        },
                        onError = { errorMsg ->
                            // Hata durumunda optimistic update'i geri al
                            isLiked = wasLiked
                            likeCount = originalLikeCount
                        }
                    )
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_like),
                        tint = if (isLiked) Color.Red else Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text("${likeCount}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { showComments = !showComments }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(24.dp))
                }
                Text("${if (comments.isNotEmpty()) comments.size else post.comments.size}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            // Description
            if (post.description.isNotBlank()) {
                Text(
                    text = post.description,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Comments Section
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

                    // Comment List
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        comments.forEach { comment ->
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
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        ),
                                        modifier = Modifier.clickable { onNavigateToProfile(comment.userId) }
                                    )
                                    Text(
                                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(comment.createdAt),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.content,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }

                    // Comment Input
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
                                    Text(stringResource(R.string.profile_placeholder_comment), color = Color.Gray, fontSize = 13.sp)
                                }
                                innerTextField()
                            }
                        )

                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    val commentTextToAdd = commentText
                                    // Önce text'i temizle (optimistic update için)
                                    commentText = ""
                                    
                                    // ViewModel ile yorum ekleme
                                    postViewModel.addComment(
                                        postId = post.id,
                                        content = commentTextToAdd,
                                        onSuccess = { comment ->
                                            // Başarılı olduğunda optimistic update
                                            comments = comments + comment
                                        },
                                        onError = { errorMsg ->
                                            // Hata durumunda text'i geri yükle
                                            commentText = commentTextToAdd
                                        }
                                    )
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send),
                                tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}