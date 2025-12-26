package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.emreyildirim.matchhuntv1.data.repository.ReviewRepository
import androidx.compose.foundation.clickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.Message
import com.emreyildirim.matchhuntv1.ui.components.RatingItem
import com.emreyildirim.matchhuntv1.ui.components.ReviewCard
import com.emreyildirim.matchhuntv1.utils.Sports
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileReviewScreen(
    userId: String,
    viewModel: EventViewModel,
    onNavigateBack: () -> Unit,
    navController: NavController
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var userReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var skillRating by remember { mutableStateOf(0f) }
    var behaviorRating by remember { mutableStateOf(0f) }
    var teamRating by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val reviewRepository = remember { ReviewRepository() }
    
    val events by viewModel.events.collectAsState()
    val createdEvents = remember(events) { events.filter { it.createdBy == userId } }
    val participatedEvents = remember(events) { events.filter { it.participants.contains(userId) } }

    val bottomSheetState = rememberModalBottomSheetState()

    // TODO: İlgi alanları, biyografi, yorumlar gibi ek alanlar UserProfile modeline eklenmeli
    // TODO: Kullanıcıyı takip etme fonksiyonları eklenmeli
    
    LaunchedEffect(userId) {
        try {

            viewModel.loadEventsForUserReviewScreen(userId = userId)
            // Kullanıcı profilini getir
            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val photoUrl = document.getString("profileImageUrl") ?: document.getString("photoUrl")
                    userProfile = document.toObject(UserProfile::class.java)?.copy(profileImageUrl = photoUrl ?: "")
                    Log.d("ProfileReview", "Photo URL: $photoUrl")
                    Log.d("ProfileReview", "User Profile: $userProfile")
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    error = exception.message ?: "An error occurred while loading profile information"
                    isLoading = false
                }

            // Kullanıcının değerlendirmelerini getir
            firestore.collection("reviews")
                .whereEqualTo("reviewedUserId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val reviews = documents.toObjects(Review::class.java)
                    userReviews = reviews
                    
                    // Ortalama puanları hesapla
                    if (reviews.isNotEmpty()) {
                        skillRating = reviews.map { it.skillRating }.average().toFloat()
                        behaviorRating = reviews.map { it.behaviorRating }.average().toFloat()
                        teamRating = reviews.map { it.teamRating }.average().toFloat()
                    }
                }

        } catch (e: Exception) {
            error = e.message ?: "An error occurred while loading profile information"
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error ?: "Bilinmeyen hata", color = Color.Red)
            }
        } else {
            userProfile?.let { profile ->
                Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profil Başlığı ve Bilgileri
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profil Fotoğrafı
                        if (profile.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                                model = profile.profileImageUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                    .size(100.dp)
                                .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.ic_profile_placeholder),
                                onError = { Log.e("ProfileReview", "Error loading image: ${profile.profileImageUrl}") }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                                modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                        // Kullanıcı Bilgileri
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.username,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { 
                                            navController.navigate("messages/${userId}")
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Message,
                                            contentDescription = "Send Message",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (profile.age != 0) {
                                    Text(
                                        text = "${profile.age} years old",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                if (profile.city.isNotEmpty()) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                    Text(
                                        text = profile.city,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                    )

                    // Biyografi
                    if (!profile.about.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "About",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = profile.about ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // İlgi Alanları
                    if (!profile.sports.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Area of Interest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            items(profile.sports) { sport ->
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
                    
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                    )


                    
                    // Değerlendirme Puanları
                    if (userReviews.isNotEmpty()) {
                        Text(
                            text = "Reviews",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Yatay düzende puanlar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RatingItem(
                                rating = skillRating,
                                label = "Beceri"
                            )
                            
                            RatingItem(
                                rating = behaviorRating,
                                label = "Davranış"
                            )
                            
                            RatingItem(
                                rating = teamRating,
                                label = "Uyum"
                            )
                        }

                        Text(
                            text = "${userReviews.size} reviews in total",
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

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Oluşturulan Etkinlikler
                    Text(
                        text = "My Created Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    if (createdEvents.isEmpty()) {
                            Text(
                                text = "No events have been created yet",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                        items(createdEvents) { event ->
                                SimpleEventCard(event = event)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Katıldığı Etkinlikler
                    Text(
                        text = "Participated Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    if (participatedEvents.isEmpty()) {
                            Text(
                                text = "No events participated yet",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                        items(participatedEvents) { event ->
                                SimpleEventCard(event = event)
                            }
                        }
                    }
                }
            }
        }

        if (showReviewsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showReviewsSheet = false },
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Reviews",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
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

@Composable
fun SimpleEventCard(event: Event) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Başlık
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Tarih
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = event.date,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Spor Türü
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sports,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = event.sportType,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// TODO: UserProfile modeline bio, interests gibi alanlar eklenmeli
// TODO: EventCard composable'ı güncellenmeli veya özelleştirilmeli 