package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.emreyildirim.matchhuntv1.utils.Sports
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import com.emreyildirim.matchhuntv1.ui.components.LocationText
import com.google.android.gms.maps.model.LatLng
import com.emreyildirim.matchhuntv1.utils.Config


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindEventScreen(
    viewModel: EventViewModel,
    pagerState: PagerState,
    onNavigateToProfile: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSportType by remember { mutableStateOf("All") }
    var expanded by remember { mutableStateOf(false) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isScreenVisible by remember { mutableStateOf(false) }

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

    // Handle screen visibility and Snackbar cleanup
    DisposableEffect(Unit) {
        isScreenVisible = true
        onDispose {
            isScreenVisible = false
            snackbarHostState.currentSnackbarData?.dismiss()
            viewModel.resetError() // Add this function to your ViewModel if it doesn't exist
        }
    }

    // Show loading and error messages as Snackbar
    LaunchedEffect(isLoading, error, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect

        if (isLoading) {
            snackbarHostState.showSnackbar(
                message = "Processing...",
                duration = SnackbarDuration.Indefinite,
                actionLabel = "Close"
            )
        } else if (error != null) {
            snackbarHostState.showSnackbar(
                message = error!!,
                duration = SnackbarDuration.Short,
                actionLabel = "Close"
            )
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(6.dp)
            ) { data ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    containerColor = if (isLoading) 
                        MaterialTheme.colorScheme.surfaceVariant 
                    else 
                        MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (isLoading)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(
                            onClick = { data.dismiss() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isLoading)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Close")
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(data.visuals.message)
                    }
                }
            }
        }
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
                label = { Text("Search Event") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSportType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            selectedSportType = "All"
                            expanded = false
                            viewModel.searchEvents(searchQuery, "All")
                        }
                    )
                    Sports.allSports.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport.nameEn) },
                            onClick = {
                                selectedSportType = sport.nameEn
                                expanded = false
                                viewModel.searchEvents(searchQuery, sport.nameEn.lowercase())
                            }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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

    // Show ModalBottomSheet when an event is selected
    if (selectedEvent != null) {
        EventDetailsSheet(
            event = selectedEvent,
            viewModel = viewModel,
            onJoinClick = {
                viewModel.sendJoinRequest(selectedEvent.id, "currentUserId")
                selectedEventId = null
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
                                text = Sports.getSportInfo(event.sportType)?.nameEn ?: event.sportType,
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
                        text = "${event.participants.size}/${event.maxParticipants}",
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
                    text = "Quota Full",
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
        modifier = Modifier.fillMaxWidth()
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
                            contentDescription = "Profile Photo",
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
                    Text("Display Profile")
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
                        apiKey = Config.MAPS_API_KEY,
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
                        text = "${event.participants.size}/${event.maxParticipants} participate",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFull)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isFull) {
                Text(
                    text = "Quota Full",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                // Join/Cancel button
                Button(
                    onClick = onJoinClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    enabled = !event.participants.contains("currentUserId")
                ) {
                    Text("Send Request/Cancel Request")
                }
            }
        }
    }
} 