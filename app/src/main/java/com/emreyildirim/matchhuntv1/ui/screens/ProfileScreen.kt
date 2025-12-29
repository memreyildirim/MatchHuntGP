package com.emreyildirim.matchhuntv1.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.data.repository.ReviewRepository
import com.emreyildirim.matchhuntv1.ui.components.ReviewCard
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import com.emreyildirim.matchhuntv1.utils.RatingCard.RatingStatItem
import com.emreyildirim.matchhuntv1.utils.Sports
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    val reviewRepository = ReviewRepository()
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

    // Animasyon State'i
    var isVisible by remember { mutableStateOf(false) }

    val userId = remember { auth.currentUser?.uid ?: "" }

    LaunchedEffect(userId) {
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
                val ratingsAndCountDeferred = async { 
                    reviewRepository.getUserRatingsAndCount(userId) 
                }
                
                ratingsAndCountDeferred.await().getOrNull()?.let { (ratings, count) ->
                    skillRating = ratings["skill"] ?: 0f
                    behaviorRating = ratings["behavior"] ?: 0f
                    teamRating = ratings["team"] ?: 0f
                    averageRating = ratings["average"] ?: 0f
                    totalReviews = count
                }
            } catch (e: Exception) {
                isLoading = false
                delay(100)
                isVisible = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
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
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = {
                        authViewModel.signOut()
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                            text = username.ifEmpty { "MatchHunter" },
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
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
                            SectionTitle(title = "About Me", icon = Icons.Default.Info)
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
                        SectionTitle(title = "Areas of Interest", icon = Icons.Default.Interests)
                        SportsRow(sports = sports)
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }

    // Reviews BottomSheet
    if (showReviewsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReviewsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Reviews", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                Spacer(modifier = Modifier.height(16.dp))
                if (userReviews.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No reviews yet", color = Color.Gray)
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
                    Text("Overall Rating", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%.1f", averageRating),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 32.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("/5.0", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp, start = 2.dp))
                    }
                }
                Surface(
                    onClick = onReviewsClick,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$totalReviews", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Reviews", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RatingStatItem(label = "Skill", value = skillRating)
                RatingStatItem(label = "Behavior", value = behaviorRating)
                RatingStatItem(label = "Cohesion", value = teamRating)
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
                    text = if (isExpanded) "Show Less" else "Read More",
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
                        text = sportInfo?.nameEn ?: sport,
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