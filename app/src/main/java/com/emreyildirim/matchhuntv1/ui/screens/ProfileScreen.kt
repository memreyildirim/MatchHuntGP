package com.emreyildirim.matchhuntv1.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.data.repository.ReviewRepository
import com.emreyildirim.matchhuntv1.data.repository.EventRepository
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.emreyildirim.matchhuntv1.data.model.Review
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material3.HorizontalDivider
import com.emreyildirim.matchhuntv1.ui.components.ReviewCard
import com.emreyildirim.matchhuntv1.utils.Sports
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    val reviewRepository = ReviewRepository()
    val eventRepository = EventRepository()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var sports by remember { mutableStateOf<List<String>>(emptyList()) }
    var profileImageUrl by remember { mutableStateOf("") }
    var skillRating by remember { mutableStateOf(0f) }
    var behaviorRating by remember { mutableStateOf(0f) }
    var teamRating by remember { mutableStateOf(0f) }
    var averageRating by remember { mutableStateOf(0f) }
    var totalReviews by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var userReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    
    // Kullanıcı ID'sini bir kez al ve sakla
    val userId = remember { auth.currentUser?.uid ?: "" }
    
    // Profil verilerini yükle
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            try {
                val userData = userRepository.getUserProfileData(userId)
                userData?.let { data ->
                    username = (data["username"] as? String) ?: ""
                    age = (data["age"] as? Number)?.toString() ?: ""
                    city = (data["city"] as? String) ?: ""
                    about = (data["about"] as? String) ?: ""
                    sports = (data["sports"] as? List<String>)?.filter { it.isNotEmpty() } ?: emptyList()
                    profileImageUrl = (data["profileImageUrl"] as? String) ?: ""
                }
            } catch (e: Exception) {
                println("Error loading profile data: ${e.message}")
            }
        }
    }
    
    // Ratingleri yükle
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            try {
                // Get ratings and total reviews
                val ratingsResult = reviewRepository.getUserRatings(userId)
                val totalReviewsResult = reviewRepository.getTotalReviews(userId)
                
                ratingsResult.getOrNull()?.let { ratings ->
                    skillRating = ratings["skill"] ?: 0f
                    behaviorRating = ratings["behavior"] ?: 0f
                    teamRating = ratings["team"] ?: 0f
                    averageRating = ratings["average"] ?: 0f
                }
                totalReviews = totalReviewsResult.getOrNull() ?: 0
            } catch (e: Exception) {
                println("Error loading ratings: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // EditProfileScreen'den döndüğünde verileri yeniden yükle
    LaunchedEffect(navController.currentBackStackEntry) {
        val profileData = navController.currentBackStackEntry?.savedStateHandle?.get<Map<String, String>>("profileData")
        if (profileData != null) {
            // Profil verilerini güncelle
            username = profileData["username"] ?: ""
            age = profileData["age"] ?: ""
            city = profileData["city"] ?: ""
            sports = profileData["sports"]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            profileImageUrl = profileData["photoUrl"] ?: ""

            // Ratingleri yeniden yükle
            if (userId.isNotEmpty()) {
                try {
                    val ratingsResult = reviewRepository.getUserRatings(userId)
                    val totalReviewsResult = reviewRepository.getTotalReviews(userId)
                    
                    ratingsResult.getOrNull()?.let { ratings ->
                        skillRating = ratings["skill"] ?: 0f
                        behaviorRating = ratings["behavior"] ?: 0f
                        teamRating = ratings["team"] ?: 0f
                        averageRating = ratings["average"] ?: 0f
                    }
                    totalReviews = totalReviewsResult.getOrNull() ?: 0
                } catch (e: Exception) {
                    println("Error reloading ratings: ${e.message}")
                }
            }

            // SavedStateHandle'ı temizle
            navController.currentBackStackEntry?.savedStateHandle?.remove<Map<String, String>>("profileData")
        }
    }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    isUploading = true
                    val userId = auth.currentUser?.uid ?: return@launch
                    val imageUrl = userRepository.uploadProfileImage(userId, it)
                    userRepository.updateProfileImage(userId, imageUrl)
                    profileImageUrl = imageUrl
                } catch (e: Exception) {
                    print(e)
                } finally {
                    isUploading = false
                }
            }
        }
    }
    
    LaunchedEffect(authViewModel.currentUser) {
        authViewModel.currentUser.collect { user ->
            if (user == null) {
                navController.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(
                        onClick = {
                            // Mevcut profil bilgilerini EditProfileScreen'e aktar
                            navController.navigate("editProfile") {
                                launchSingleTop = true
                                // Profil bilgilerini argüman olarak gönder
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "profileData",
                                    mapOf(
                                        "username" to username,
                                        "age" to age,
                                        "city" to city,
                                        "sports" to sports.joinToString(","),
                                        "photoUrl" to profileImageUrl
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { 
                            authViewModel.signOut()
                            navController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profil Fotoğrafı
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                ) {
                    if (profileImageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(profileImageUrl)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_profile_placeholder),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                // Kullanıcı Adı
                Text(
                    text = username.ifEmpty { "Username" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Kullanıcı Bilgileri Kartı
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Hakkında Bilgisi
                        if (about.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = about,
                                    minLines = 1,
                                    maxLines = 3,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        // Yaş Bilgisi
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "$age years old",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Şehir Bilgisi
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Kullanıcı Puanı Kartı
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Genel Ortalama
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = String.format("%.1f", averageRating),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "Overall Average",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                        )

                        // Yatay düzende puanlar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Beceri/Yetenek Puanı
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f", skillRating),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Skill",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }

                            // Davranış/Saygı Puanı
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f", behaviorRating),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Behavior",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }

                            // Uyum/Takım İletişimi Puanı
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f", teamRating),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Cohesion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Text(
                            text = "$totalReviews reviews in total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        try {
                                            val reviews = reviewRepository.getUserReviews(userId)
                                            userReviews = reviews
                                            showReviewsSheet = true
                                        } catch (e: Exception) {
                                            println("Error loading reviews: ${e.message}")
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // İlgi Alanları Kartı
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Interests,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Area of interest",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Spor türleri listesi
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(sports) { sport ->
                                val sportInfo = Sports.getSportInfo(sport)
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (sportInfo != null) {
                                                Icon(
                                                    painter = painterResource(id = sportInfo.iconResId),
                                                    contentDescription = sport,
                                                    modifier = Modifier.size(16.dp)
                                                        .padding(end = 8.dp),
                                                    tint = null
                                                )
                                            }
                                            Text(Sports.getSportInfo(sport)?.nameEn ?: sport)
                                        }
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = sportInfo?.color ?: MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reviews BottomSheet
    if (showReviewsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReviewsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Reviews",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (userReviews.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No reviews yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(userReviews) { review ->
                            ReviewCard(review = review)
                        }
                    }
                }
            }
        }
    }
}


