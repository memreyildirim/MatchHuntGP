package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.BuildConfig
import com.emreyildirim.matchhuntv1.R
import com.google.firebase.auth.FirebaseAuth
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.theme.Obsidian
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.emreyildirim.matchhuntv1.utils.LocationUtils
import com.emreyildirim.matchhuntv1.utils.Sports
import com.google.firebase.firestore.FirebaseFirestore
import com.emreyildirim.matchhuntv1.ui.components.LocationText
import com.google.android.gms.maps.model.LatLng
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

private const val SPORT_FILTER_ALL = "All"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindEventScreen(
    viewModel: EventViewModel,
    pagerState: PagerState,
    onNavigateToProfile: (String) -> Unit,
    initialEventId: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSportType by remember { mutableStateOf(SPORT_FILTER_ALL) }
    val displayedSportFilter =
        if (selectedSportType == SPORT_FILTER_ALL) stringResource(R.string.find_event_filter_all) else selectedSportType
    var expanded by remember { mutableStateOf(false) }
    var selectedEventId by remember(initialEventId) { mutableStateOf<String?>(initialEventId) }
    val sheetState = rememberModalBottomSheetState()
    var isScreenVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Refresh events when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadEvents()
    }

    // Get the current selected event from the events list
    val selectedEvent = remember(selectedEventId, events) {
        selectedEventId?.let { id -> events.find { it.id == id } }
    }

    // Handle screen visibility
    DisposableEffect(Unit) {
        isScreenVisible = true
        onDispose {
            isScreenVisible = false
            viewModel.resetError()
        }
    }

    // Show loading and error messages as Toast
    LaunchedEffect(isLoading, error, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect

        if (error != null) {
            Toast.makeText(
                context,
                error!!,
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetError() // Toast gösterildikten sonra error'ı temizle
        }
    }

    Scaffold(contentColor = MaterialTheme.colorScheme.secondaryContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp, 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchEvents(it, selectedSportType)
                },
                label = { Text(stringResource(R.string.find_event_search_label)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Obsidian,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Obsidian,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = displayedSportFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.find_event_sport_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Obsidian,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Obsidian,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                ExposedDropdownMenu(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.find_event_filter_all)) },
                        onClick = {
                            selectedSportType = SPORT_FILTER_ALL
                            expanded = false
                            viewModel.searchEvents(searchQuery, SPORT_FILTER_ALL)
                        }
                    )
                    Sports.allSports.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport.name) },
                            onClick = {
                                selectedSportType = sport.name
                                expanded = false
                                viewModel.searchEvents(searchQuery, sport.name.lowercase())
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Obsidian)
                }
            } else if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.find_event_empty_title),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        EventCard(
                            event = event,
                            onClick = { selectedEventId = event.id }
                        )
                    }
                }
            }
        }
    }

    // Show ModalBottomSheet when an event is selected
    if (selectedEvent != null) {
        EventDetailsSheet(
            event = selectedEvent,
            viewModel = viewModel,
            onJoinClick = {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                viewModel.sendJoinRequest(selectedEvent.id, uid)
            },
            onDismiss = { selectedEventId = null },
            onNavigateToProfile = onNavigateToProfile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit
) {
    val isFull = event.participants.size >= event.maxParticipants

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isFull)
                (Sports.getSportInfo(event.sportType)?.color ?: MaterialTheme.colorScheme.surface).copy(alpha = 0.5f)
            else
                Sports.getSportInfo(event.sportType)?.color ?: MaterialTheme.colorScheme.surface
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
                    color = if (isFull)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
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
                                text = Sports.getSportInfo(event.sportType)?.name ?: event.sportType,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isFull)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isFull)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = if (isFull)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFull)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
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
                        tint = if (isFull)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(
                            R.string.find_event_participate_summary,
                            event.participants.size,
                            event.maxParticipants
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFull)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (isFull) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.find_event_quota_full),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsSheet(
    event: Event,
    viewModel: EventViewModel,
    onJoinClick: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    var creatorProfile by remember { mutableStateOf<UserProfile?>(null) }
    val firestore = FirebaseFirestore.getInstance()
    val isFull = event.participants.size >= event.maxParticipants
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isRequested = event.pendingRequests.contains(currentUserId)
    val isParticipant = event.participants.contains(currentUserId)

    LaunchedEffect(event.createdBy) {
        firestore.collection("users")
            .document(event.createdBy)
            .get()
            .addOnSuccessListener { document ->
                creatorProfile = document.toObject(UserProfile::class.java)
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Event Title
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                color = if (isFull)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Creator Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (creatorProfile?.profileImageUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = creatorProfile?.profileImageUrl,
                            contentDescription = stringResource(R.string.find_event_cd_profile_photo),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = if (isFull)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = event.creatorUsername,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFull)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = { onNavigateToProfile(event.createdBy) }
                ) {
                    Text(stringResource(R.string.find_event_display_profile))
                }
            }

            // Description
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFull)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Event details
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
                        tint = if (isFull)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${event.date} - ${event.time}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFull)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
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
                        tint = if (isFull)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    LocationText(
                        latLng = LatLng(event.latitude, event.longitude),
                        apiKey = BuildConfig.MAPS_API_KEY,
                        format = LocationUtils.LocationDisplayFormat.MEDIUM,
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
                        tint = if (isFull)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(
                            R.string.find_event_participate_summary,
                            event.participants.size,
                            event.maxParticipants
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFull)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isFull && !isParticipant) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.find_event_quota_full),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (isParticipant) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "Bu Etkinliktesiniz",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Button(
                        onClick = onJoinClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isRequested) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                contentColor = Color.White
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    ) {
                        Text(
                            text = if (isRequested) "İsteği İptal Et" else stringResource(R.string.find_event_send_request),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "MatchHunt'ta harika bir spor etkinliği buldum! 🏃‍♂️🏆 Sen de katıl:\n\nhttps://matchhunt-17adf.web.app/events/${event.id}"
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Etkinliği Paylaş"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Obsidian
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Paylaş",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Etkinliği Paylaş",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}