package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.BuildConfig
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.ReportViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.emreyildirim.matchhuntv1.utils.Sports
import com.emreyildirim.matchhuntv1.ui.components.ReviewCard
import com.emreyildirim.matchhuntv1.utils.RatingCard.RatingStatItem
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileReviewScreen(
    userId: String,
    viewModel: EventViewModel,
    onNavigateBack: () -> Unit,
    navController: NavController,
    reportViewModel: ReportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Veri State'leri
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var skillRating by remember { mutableStateOf(0f) }
    var behaviorRating by remember { mutableStateOf(0f) }
    var teamRating by remember { mutableStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReasonCode by remember { mutableStateOf("abuse") }
    var reasonText by remember { mutableStateOf("") }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid



    // Genel ortalama hesaplama
    val averageRating = remember(skillRating, behaviorRating, teamRating) {
        if (skillRating > 0 || behaviorRating > 0 || teamRating > 0) {
            (skillRating + behaviorRating + teamRating) / 3f
        } else 0f
    }

    val scrollState = rememberScrollState()
    val bottomSheetState = rememberModalBottomSheetState()
    var showReviewsSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val reportSubmittedText = stringResource(R.string.toast_report_submitted_thanks)

    // Etkinlik Verileri
    val createdEvents by viewModel.createdEventsForUser.collectAsState()
    val participatedEvents by viewModel.participatedPastEventsForUser.collectAsState()

    // Veri Çekme Mantığı
    LaunchedEffect(userId) {
        if (BuildConfig.DEBUG) Log.d("ProfileReviewScreen", "LaunchedEffect started")
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

    // Event listeleri gerçekten güncelleniyor mu görmek için ek log
    LaunchedEffect(createdEvents, participatedEvents) {
        if (BuildConfig.DEBUG) {
            Log.d(
                "ProfileReviewScreen",
                "Events updated: created=${createdEvents.size}, participated=${participatedEvents.size}"
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_review_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.report_action_report), color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            },
                        )
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
                        username = userProfile?.username ?: stringResource(R.string.profile_review_unknown_user),
                        location = userProfile?.city ?: stringResource(R.string.profile_review_location_unset),
                        age = userProfile?.age, // Yaş bilgisini buradan aktarıyoruz
                        onMessageClick =  if (currentUserId != null && userId != currentUserId) {
                            { navController.navigate("messages/${userId}") }
                        } else {
                            null // Kendi profilde mesaj butonu gösterme
                        }
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
                                        text = stringResource(R.string.profile_overall_rating),
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (averageRating > 0) String.format("%.1f", averageRating) else stringResource(R.string.profile_rating_not_available),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 32.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.profile_rating_suffix),
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { showReviewsSheet = true },
                                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                                            text = stringResource(R.string.profile_review_rating_count_label),
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
                                RatingStatItem(label = stringResource(R.string.profile_review_rating_label_skill), value = skillRating)
                                RatingStatItem(label = stringResource(R.string.profile_review_rating_label_behavior), value = behaviorRating)
                                RatingStatItem(label = stringResource(R.string.profile_review_rating_label_team), value = teamRating)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (!userProfile?.about.isNullOrEmpty()) {
                        SectionHeader(stringResource(R.string.profile_review_section_about))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface
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
                        SectionHeader(stringResource(R.string.profile_review_section_interests))
                        InterestsRow(userProfile?.sports ?: emptyList())
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    EventCarousel(stringResource(R.string.profile_review_carousel_created), createdEvents)
                    Spacer(modifier = Modifier.height(24.dp))
                    EventCarousel(stringResource(R.string.profile_review_carousel_joined), participatedEvents)

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        if (showReviewsSheet) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
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
                        text = stringResource(R.string.profile_reviews_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (userReviews.isEmpty()) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.profile_review_no_reviews), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            reportViewModel.reportProfile(
                                userId = userId,
                                about = userProfile?.about,
                                reasonCode = selectedReasonCode,
                                reasonText = reasonText
                            )
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
                title = { Text(stringResource(R.string.report_profile_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.report_select_reason_title), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReasonCode = "abuse"
                                }
                        ) {
                            RadioButton(
                                selected = selectedReasonCode == "abuse",
                                onClick = { selectedReasonCode = "abuse" }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.report_reason_harassment_behavior))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReasonCode = "spam"
                                }
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
                                .clickable {
                                    selectedReasonCode = "fake"
                                }
                        ) {
                            RadioButton(
                                selected = selectedReasonCode == "fake",
                                onClick = { selectedReasonCode = "fake" }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.report_reason_fake_profile))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReasonCode = "other"
                                }
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
}

@Composable
fun ProfileHeaderSection(
    profileImageUrl: String,
    username: String,
    location: String,
    age: Int? = null,
    onMessageClick: (() -> Unit)? = null  // null = mesaj butonu gösterme
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier
                    .size(130.dp)
                    .padding(4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = stringResource(R.string.cd_profile),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_profile_placeholder)
                )
            }
            if (onMessageClick != null) {
                SmallFloatingActionButton(
                    onClick = onMessageClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.offset(x = (-4).dp, y = (-4).dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = stringResource(R.string.cd_message), modifier = Modifier.size(20.dp))
                }
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
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.profile_review_no_events), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
        modifier = Modifier
            .width(260.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
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