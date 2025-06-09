package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.model.UserProfile
import com.emreyildirim.matchhuntv1.ui.viewmodel.ReviewViewModel
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRatingScreen(
    eventId: String,
    participantId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    var skillRating by remember { mutableStateOf(0f) }
    var behaviorRating by remember { mutableStateOf(0f) }
    var teamRating by remember { mutableStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val reviewSubmitted by viewModel.reviewSubmitted.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    // Debug için log ekleyelim
    LaunchedEffect(Unit) {
        Log.d("ProfileRatingScreen", "Screen opened. EventId: $eventId, ParticipantId: $participantId")
    }
    
    // Etkinlik ve kullanıcı bilgilerini yükle
    var event by remember { mutableStateOf<Event?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    val userRepository = remember { UserRepository() }
    
    LaunchedEffect(eventId, participantId) {
        scope.launch {
            // Etkinlik bilgilerini yükle
            userRepository.getEvent(eventId).onSuccess { loadedEvent ->
                event = loadedEvent
                // Değerlendirilecek kullanıcının bilgilerini yükle
                if (participantId != null && participantId.isNotEmpty()) {
                    Log.d("ProfileRatingScreen", "Kullanıcı profili yükleniyor. TargetUserId: $participantId")
                    userRepository.getUserProfile(participantId).onSuccess { profile ->
                        userProfile = profile
                        Log.d("ProfileRatingScreen", "Kullanıcı profili yüklendi: ${profile.username}")
                    }
                } else {
                    Log.d("ProfileRatingScreen", "Etkinlik sahibi profili yükleniyor. TargetUserId: ${loadedEvent.createdBy}")
                    userRepository.getUserProfile(loadedEvent.createdBy).onSuccess { profile ->
                        userProfile = profile
                        Log.d("ProfileRatingScreen", "Etkinlik sahibi profili yüklendi: ${profile.username}")
                    }
                }
            }
        }
    }

    LaunchedEffect(reviewSubmitted) {
        if (reviewSubmitted) {
            snackbarHostState.showSnackbar(
                message = "Your evaluation has been saved successfully",
                duration = SnackbarDuration.Short
            )
            onNavigateBack()
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            viewModel.resetReviewSubmitted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (participantId != null) "Rate Participant" else "Rate the Event Owner"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Etkinlik sahibi bilgileri
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profil fotoğrafı
                    AsyncImage(
                        model = userProfile?.profileImageUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Kullanıcı adı
                    Text(
                        text = userProfile?.username ?: "User",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    // Etkinlik bilgileri
                    event?.let { currentEvent ->
                        Text(
                            text = currentEvent.title,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${currentEvent.date} - ${currentEvent.time}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Beceri/Yetenek Puanı
            Text(
                text = "Skill/Ability Points",
                style = MaterialTheme.typography.titleMedium
            )
            RatingBar(
                rating = skillRating,
                onRatingChanged = { skillRating = it }
            )

            // Davranış/Saygı Puanı
            Text(
                text = "Behavior/Respect Score",
                style = MaterialTheme.typography.titleMedium
            )
            RatingBar(
                rating = behaviorRating,
                onRatingChanged = { behaviorRating = it }
            )

            // Uyum/Takım İletişimi Puanı
            Text(
                text = "Cohesion/Team Communication Score",
                style = MaterialTheme.typography.titleMedium
            )
            RatingBar(
                rating = teamRating,
                onRatingChanged = { teamRating = it }
            )

            // Yorum
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Comment") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Değerlendirme Gönder Butonu
            Button(
                onClick = {
                    if (participantId != null) {
                        viewModel.submitParticipantReview(
                            eventId = eventId,
                            participantId = participantId,
                            skillRating = skillRating,
                            behaviorRating = behaviorRating,
                            teamRating = teamRating,
                            comment = comment
                        )
                    } else {
                        viewModel.submitReview(
                            eventId = eventId,
                            skillRating = skillRating,
                            behaviorRating = behaviorRating,
                            teamRating = teamRating,
                            comment = comment
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && skillRating > 0 && behaviorRating > 0 && teamRating > 0
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit Review")
                }
            }

            // Hata mesajı
            error?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChanged(i.toFloat()) }
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Rating $i",
                    tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 