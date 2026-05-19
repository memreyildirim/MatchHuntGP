package com.emreyildirim.matchhuntv1.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.BuildConfig
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.data.repository.ReviewRepository
import com.emreyildirim.matchhuntv1.ui.components.LocationText
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.maps.model.LatLng
import java.util.*
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.emreyildirim.matchhuntv1.utils.Sports
import com.emreyildirim.matchhuntv1.ui.viewmodel.ReviewViewModel
import android.net.Uri
import android.util.Log
import com.emreyildirim.matchhuntv1.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(
    navController: NavController,
    viewModel: EventViewModel
) {
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // Load user's events when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadUserEvents()
    }

    // Filter events for each tab
    val myCreatedEvents = remember(events) {
        events.filter { it.createdBy == currentUserId }
    }

    val myParticipatedEvents = remember(events) {
        events.filter { it.participants.contains(currentUserId) }
    }

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.resetToastMessage()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(stringResource(R.string.my_events_tab_created)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(stringResource(R.string.my_events_tab_participated)) }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) { page ->
            when (page) {
                0 -> MyCreatedEventsList(
                    events = myCreatedEvents,
                    isLoading = isLoading,
                    error = error,
                    viewModel = viewModel,
                    onNavigateToProfile = { userId ->
                        navController.navigate("user_profile/$userId")
                    },
                    navController = navController
                )
                1 -> MyParticipatedEventsList(
                    events = myParticipatedEvents,
                    isLoading = isLoading,
                    error = error,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun MyCreatedEventsList(
    events: List<Event>,
    isLoading: Boolean,
    error: String?,
    viewModel: EventViewModel,
    onNavigateToProfile: (String) -> Unit,
    navController: NavController
) {
    val swipeRefreshState = rememberSwipeRefreshState(isLoading)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.refreshUserEvents() }
    ) {
        if (isLoading && events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null && events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_event))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    MyCreatedEventCard(
                        event = event,
                        viewModel = viewModel,
                        onNavigateToProfile = onNavigateToProfile,
                        onEditEvent = { event ->
                            navController.navigate("edit_event/${event.id}")
                        },
                        onNavigateToProfileEvaluation = { participantId ->
                            // Debug için log ekleyelim
                            Log.d("MyCreatedEventsList", "Değerlendirme ekranına yönlendiriliyor. EventId: ${event.id}, ParticipantId: $participantId")
                            // Katılımcıyı değerlendirme ekranına yönlendirme
                            val encodedParticipantId = Uri.encode(participantId)
                            navController.navigate("rate_event/${event.id}/$encodedParticipantId")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyCreatedEventCard(
    event: Event,
    viewModel: EventViewModel,
    onNavigateToProfile: (String) -> Unit,
    onEditEvent: (Event) -> Unit,
    onNavigateToProfileEvaluation: (String) -> Unit
) {
    var requestersProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var participantsProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var expandedRequesterId by remember { mutableStateOf<String?>(null) }
    val reviewViewModel = remember { ReviewViewModel() }
    val reviewRepository = remember { ReviewRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var reviewedParticipants by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Check if event date and time has passed
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val eventDate = dateFormat.parse("${event.date} ${event.time}")
    val isEventPassed = eventDate?.before(Date()) ?: false

    // Load reviewed participants
    LaunchedEffect(event.id) {
        reviewRepository.getUserReviewsForEvent(currentUserId, event.id)
            .onSuccess { reviews ->
                reviewedParticipants = reviews.map { it.reviewedUserId }.toSet()
            }
    }

    // Load profiles of users who sent join requests
    LaunchedEffect(event.pendingRequests) {
        val profiles = mutableMapOf<String, UserProfile>()
        event.pendingRequests.forEach { userId ->
            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    document.toObject(UserProfile::class.java)?.let { profile ->
                        profiles[userId] = profile
                        requestersProfiles = profiles
                    }
                }
        }
    }

    // Load profiles of participants
    LaunchedEffect(event.participants) {
        val profiles = mutableMapOf<String, UserProfile>()
        event.participants.forEach { userId ->
            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    document.toObject(UserProfile::class.java)?.let { profile ->
                        profiles[userId] = profile
                        participantsProfiles = profiles
                    }
                }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Sports.getSportInfo(event.sportType)?.color ?: MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and sport type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                AssistChip(
                    onClick = { },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sport type icon
                            val sportInfo = Sports.getSportInfo(event.sportType)

                            if (sportInfo != null) {
                                Icon(
                                    painter = painterResource(id = sportInfo.iconResId),
                                    contentDescription = event.sportType,
                                    modifier = Modifier.size(16.dp)
                                        .padding(end = 8.dp),
                                    tint = null
                                )
                            }

                            Text(
                                text = Sports.getSportInfo(event.sportType)?.nameEn ?: event.sportType,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.widthIn(min = 80.dp, max = 120.dp)
                )
            }

            // Event details
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${event.date} ${event.time}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LocationText(
                        latLng = LatLng(event.latitude, event.longitude),
                        apiKey = BuildConfig.MAPS_API_KEY,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isEventPassed) {
                    Button(
                        onClick = { onEditEvent(event) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.my_events_edit))
                    }
                }

                if (event.pendingRequests.isNotEmpty()) {
                    Button(
                        onClick = { expandedRequesterId = event.id },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.my_events_requests_count, event.pendingRequests.size))
                    }
                }
            }

            // Katılımcılar Listesi
            if (event.participants.isNotEmpty()) {
                var showParticipants by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showParticipants = !showParticipants }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.my_events_participants_header,
                            event.participants.size,
                            event.maxParticipants
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (showParticipants) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (showParticipants) R.string.my_events_cd_hide_participants
                            else R.string.my_events_cd_show_participants
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = showParticipants,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Önce etkinlik sahibini göster
                        participantsProfiles[event.createdBy]?.let { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToProfile(event.createdBy) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AsyncImage(
                                        model = profile.profileImageUrl,
                                        contentDescription = stringResource(R.string.my_events_cd_creator_profile),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column {
                                        Text(
                                            text = profile.username,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.my_events_event_owner),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Sonra diğer katılımcıları göster
                        event.participants.filter { it != event.createdBy }.forEach { participantId ->
                            participantsProfiles[participantId]?.let { profile ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onNavigateToProfile(participantId) },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AsyncImage(
                                            model = profile.profileImageUrl,
                                            contentDescription = stringResource(R.string.my_events_cd_participant_profile),
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column {
                                            Text(
                                                text = profile.username,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = stringResource(R.string.my_events_participants_label),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Değerlendirme butonu - sadece diğer katılımcılar için
                                    if (isEventPassed && !reviewedParticipants.contains(participantId)) {
                                        Button(
                                            onClick = {
                                                // Debug için log ekleyelim
                                                Log.d("MyCreatedEventCard", "The evaluation button was clicked. ParticipantId: $participantId")
                                                // Katılımcıyı değerlendirme ekranına yönlendirme
                                                onNavigateToProfileEvaluation(participantId)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text(stringResource(R.string.my_events_evaluate))
                                        }
                                    } else if (isEventPassed && reviewedParticipants.contains(participantId)) {
                                        Text(
                                            text = stringResource(R.string.my_events_evaluated),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show join requests dialog
    if (expandedRequesterId == event.id) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            onDismissRequest = { expandedRequesterId = null },
            title = { Text(stringResource(R.string.my_events_participation_requests)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    event.pendingRequests.forEach { requesterId ->
                        val requesterProfile = requestersProfiles[requesterId]
                        requesterProfile?.let { profile ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToProfile(requesterId) }
                                ) {
                                    AsyncImage(
                                        model = profile.profileImageUrl,
                                        contentDescription = profile.username,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = android.R.drawable.ic_menu_gallery)
                                    )
                                    Column {
                                        Text(
                                            text = profile.username,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.my_events_age_format, profile.age),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.approveJoinRequest(event.id, requesterId)
                                            expandedRequesterId = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.my_events_cd_approve),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.rejectJoinRequest(event.id, requesterId)
                                            expandedRequesterId = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.my_events_cd_reject),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { expandedRequesterId = null }
                ) {
                    Text(stringResource(R.string.my_events_dialog_close))
                }
            }
        )
    }
}

@Composable
fun MyParticipatedEventsList(
    events: List<Event>,
    isLoading: Boolean,
    error: String?,
    viewModel: EventViewModel,
    navController: NavController
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val filteredEvents = events.filter { event ->
        // Sadece katıldığımız ve kendimizin oluşturmadığı etkinlikleri göster
        event.participants.contains(currentUserId) && event.createdBy != currentUserId
    }

    val swipeRefreshState = rememberSwipeRefreshState(isLoading)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.refreshUserEvents() }
    ) {
        if (isLoading && filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null && filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.my_events_no_attended))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEvents) { event ->
                    MyParticipatedEventCard(
                        event = event,
                        onNavigateToReview = { event ->
                            navController.navigate("rate_event/${event.id}")
                        },
                        navController = navController
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyParticipatedEventCard(
    event: Event,
    onNavigateToReview: (Event) -> Unit,
    navController: NavController
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isEventOwner = event.creatorId == currentUserId
    val isEventEnded = event.endDate.before(Date())
    var hasReviewed by remember { mutableStateOf(false) }
    val reviewRepository = remember { ReviewRepository() }
    val scope = rememberCoroutineScope()
    var showEventDetails by remember { mutableStateOf(false) }
    var creatorProfile by remember { mutableStateOf<UserProfile?>(null) }
    val firestore = FirebaseFirestore.getInstance()
    val isFull = event.participants.size >= event.maxParticipants

    // Check if user has already reviewed this event creator
    LaunchedEffect(event.id) {
        scope.launch {
            reviewRepository.getUserReviewForEventAndUser(currentUserId, event.createdBy, event.id)
                .onSuccess { review ->
                    hasReviewed = review != null
                }
        }
    }

    // Load creator profile
    LaunchedEffect(event.createdBy) {
        firestore.collection("users")
            .document(event.createdBy)
            .get()
            .addOnSuccessListener { document ->
                creatorProfile = document.toObject(UserProfile::class.java)
            }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { showEventDetails = true },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Sports.getSportInfo(event.sportType)?.color ?: MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and sport type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                AssistChip(
                    onClick = { },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sport type icon
                            val sportInfo = Sports.getSportInfo(event.sportType)

                            if (sportInfo != null) {
                                Icon(
                                    painter = painterResource(id = sportInfo.iconResId),
                                    contentDescription = event.sportType,
                                    modifier = Modifier.size(16.dp)
                                        .padding(end = 8.dp),
                                    tint = null
                                )
                            }

                            Text(
                                text = Sports.getSportInfo(event.sportType)?.nameEn ?: event.sportType,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.widthIn(min = 80.dp, max = 120.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Basic info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${event.participants.size}/${event.maxParticipants}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Event Status Chip
                AssistChip(
                    onClick = { },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isEventEnded) Icons.Default.History else Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isEventEnded) R.string.my_events_status_past
                                    else R.string.my_events_status_active
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isEventEnded)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        labelColor = if (isEventEnded)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Review Status (if event is ended)
                if (isEventEnded && !isEventOwner) {
                    AssistChip(
                        onClick = {
                            if (!hasReviewed) {
                                onNavigateToReview(event)
                            }
                        },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasReviewed) Icons.Default.Star else Icons.Default.StarBorder,
                                    tint = if (hasReviewed) Color(0xFFD58F04) else Color.Unspecified,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (hasReviewed) R.string.my_events_reviewed
                                        else R.string.my_events_review
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (hasReviewed)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = if (hasReviewed)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }

    // Event Details Bottom Sheet
    if (showEventDetails) {
        ModalBottomSheet(
            onDismissRequest = { showEventDetails = false },
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Title and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Event Title
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Event Status
                    AssistChip(
                        onClick = { },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isEventEnded) Icons.Default.History else Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (isEventEnded) R.string.my_events_status_past
                                        else R.string.my_events_status_active
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isEventEnded)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            labelColor = if (isEventEnded)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Creator Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("user_profile/${event.createdBy}")
                            showEventDetails = false
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = creatorProfile?.profileImageUrl,
                            contentDescription = stringResource(R.string.my_events_cd_creator_profile),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = creatorProfile?.username ?: stringResource(R.string.loading),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.my_events_event_owner),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.my_events_cd_view_profile),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Event Description
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Event Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date and Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${event.date} - ${event.time}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        LocationText(
                            latLng = LatLng(event.latitude, event.longitude),
                            apiKey = BuildConfig.MAPS_API_KEY,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Participants
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(
                                R.string.my_events_participant_summary,
                                event.participants.size,
                                event.maxParticipants
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Review Button (only show if event is ended, user is not the owner, and hasn't reviewed yet)
                if (isEventEnded && !isEventOwner && !hasReviewed) {
                    Button(
                        onClick = {
                            onNavigateToReview(event)
                            showEventDetails = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.my_events_review_owner))
                    }
                } else if (isEventEnded && !isEventOwner && hasReviewed) {
                    Text(
                        text = stringResource(R.string.my_events_already_reviewed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
} 