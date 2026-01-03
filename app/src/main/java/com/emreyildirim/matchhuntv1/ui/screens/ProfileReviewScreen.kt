package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.emreyildirim.matchhuntv1.utils.Sports
import com.emreyildirim.matchhuntv1.ui.components.ReviewCard
import com.emreyildirim.matchhuntv1.utils.RatingCard.RatingStatItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileReviewScreen(
    userId: String,
    viewModel: EventViewModel,
    onNavigateBack: () -> Unit,
    navController: NavController
) {
    // Veri State'leri
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var skillRating by remember { mutableStateOf(0f) }
    var behaviorRating by remember { mutableStateOf(0f) }
    var teamRating by remember { mutableStateOf(0f) }

    // Genel ortalama hesaplama
    val averageRating = remember(skillRating, behaviorRating, teamRating) {
        if (skillRating > 0 || behaviorRating > 0 || teamRating > 0) {
            (skillRating + behaviorRating + teamRating) / 3f
        } else 0f
    }

    val scrollState = rememberScrollState()
    val bottomSheetState = rememberModalBottomSheetState()
    var showReviewsSheet by remember { mutableStateOf(false) }

    // Etkinlik Verileri
    val events by viewModel.events.collectAsState()
    val createdEvents = remember(events, userId) { events.filter { it.createdBy == userId } }
    val participatedEvents = remember(events, userId) { events.filter { it.participants.contains(userId) } }

    // Veri Çekme Mantığı
    LaunchedEffect(userId) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            viewModel.loadEventsForUserReviewScreen(userId = userId)

            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val photoUrl = document.getString("profileImageUrl") ?: document.getString("photoUrl")
                    userProfile = document.toObject(UserProfile::class.java)?.copy(profileImageUrl = photoUrl ?: "")
                    isLoading = false
                }

            firestore.collection("reviews").whereEqualTo("reviewedUserId", userId).get()
                .addOnSuccessListener { documents ->
                    val reviews = documents.toObjects(Review::class.java)
                    userReviews = reviews
                    if (reviews.isNotEmpty()) {
                        skillRating = reviews.map { it.skillRating }.average().toFloat()
                        behaviorRating = reviews.map { it.behaviorRating }.average().toFloat()
                        teamRating = reviews.map { it.teamRating }.average().toFloat()
                    }
                }
        } catch (e: Exception) {
            isLoading = false
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Kullanıcı Profili", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileHeaderSection(
                        profileImageUrl = userProfile?.profileImageUrl ?: "",
                        username = userProfile?.username ?: "Bilinmeyen Kullanıcı",
                        location = userProfile?.city ?: "Konum belirtilmedi",
                        age = userProfile?.age, // Yaş bilgisini buradan aktarıyoruz
                        onMessageClick = { navController.navigate("messages/${userId}") }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Rating Kartı Tasarımı
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Genel Puan",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (averageRating > 0) String.format("%.1f", averageRating) else "—",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 32.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "/5.0",
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { showReviewsSheet = true },
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${userReviews.size}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Değerlendirme",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                RatingStatItem(label = "Beceri", value = skillRating)
                                RatingStatItem(label = "Davranış", value = behaviorRating)
                                RatingStatItem(label = "Uyum", value = teamRating)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (!userProfile?.about.isNullOrEmpty()) {
                        SectionHeader("Hakkımda")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = userProfile?.about ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                                lineHeight = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!userProfile?.sports.isNullOrEmpty()) {
                        SectionHeader("İlgi Alanları")
                        InterestsRow(userProfile?.sports ?: emptyList())
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    EventCarousel("Oluşturulan Etkinlikler", createdEvents)
                    Spacer(modifier = Modifier.height(24.dp))
                    EventCarousel("Katılınan Etkinlikler", participatedEvents)

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        if (showReviewsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showReviewsSheet = false },
                sheetState = bottomSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Değerlendirmeler",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (userReviews.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text("Henüz değerlendirme yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
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
}

@Composable
fun ProfileHeaderSection(
    profileImageUrl: String,
    username: String,
    location: String,
    age: Int? = null,
    onMessageClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(130.dp).padding(4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profil",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_profile_placeholder)
                )
            }
            SmallFloatingActionButton(
                onClick = onMessageClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.offset(x = (-4).dp, y = (-4).dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Mesaj", modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))

            // Konum ve Yaş Bilgisi
            val headerSubText = buildString {
                append(location)
                if (age != null) {
                    append(" · ")
                    append(age)
                }
            }

            Text(
                text = headerSubText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
}

@Composable
fun InterestsRow(interests: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(interests) { sport ->
            val sportInfo = Sports.getSportInfo(sport)
            Surface(
                color = sportInfo?.color?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, sportInfo?.color?.copy(alpha = 0.3f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sportInfo != null) {
                        Icon(
                            painter = painterResource(id = sportInfo.iconResId),
                            contentDescription = sport,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                    }
                    Text(
                        text = sportInfo?.nameEn ?: sport,
                        style = MaterialTheme.typography.labelLarge,
                        color = sportInfo?.color ?: MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EventCarousel(title: String, events: List<Event>) {
    Column {
        SectionHeader(title)
        if (events.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Henüz etkinlik yok", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(events) { event ->
                    ModernEventCard(event)
                }
            }
        }
    }
}

@Composable
fun ModernEventCard(event: Event) {
    Card(
        modifier = Modifier.width(260.dp).shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                Text(text = event.sportType, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = event.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}