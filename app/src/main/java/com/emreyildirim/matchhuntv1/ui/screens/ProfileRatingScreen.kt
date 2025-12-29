package com.emreyildirim.matchhuntv1.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.emreyildirim.matchhuntv1.R
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

    // Veri yükleme state'leri
    var event by remember { mutableStateOf<Event?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    val userRepository = remember { UserRepository() }

    // Gradyan Arka Plan (Diğer ekranlarla uyumlu)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    )

    LaunchedEffect(eventId, participantId) {
        scope.launch {
            userRepository.getEvent(eventId).onSuccess { loadedEvent ->
                event = loadedEvent
                val targetId = participantId?.takeIf { it.isNotEmpty() } ?: loadedEvent.createdBy
                userRepository.getUserProfile(targetId).onSuccess { profile ->
                    userProfile = profile
                }
            }
        }
    }

    LaunchedEffect(reviewSubmitted) {
        if (reviewSubmitted) {
            snackbarHostState.showSnackbar("Değerlendirmeniz kaydedildi.")
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (participantId != null) "Katılımcıyı Puanla" else "Düzenleyeni Puanla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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

                // --- HEDEF KULLANICI KARTI ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = userProfile?.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_profile_placeholder)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userProfile?.username ?: "Yükleniyor...",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        event?.let { currentEvent ->
                            Text(
                                text = currentEvent.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            // Tarih ve Saat Bilgisi Eklendi
                            Text(
                                text = "${currentEvent.date} • ${currentEvent.time}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- PUANLAMA ALANI ---
                RatingSectionCard(
                    title = "Beceri & Yetenek",
                    rating = skillRating,
                    onRatingChanged = { skillRating = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                RatingSectionCard(
                    title = "Davranış & Saygı",
                    rating = behaviorRating,
                    onRatingChanged = { behaviorRating = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                RatingSectionCard(
                    title = "Uyum & Takım İletişimi",
                    rating = teamRating,
                    onRatingChanged = { teamRating = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- YORUM ALANI ---
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Yorumunuz (İsteğe bağlı)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- GÖNDER BUTONU ---
                Button(
                    onClick = {
                        if (participantId != null) {
                            viewModel.submitParticipantReview(eventId, participantId, skillRating, behaviorRating, teamRating, comment)
                        } else {
                            viewModel.submitReview(eventId, skillRating, behaviorRating, teamRating, comment)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && skillRating > 0 && behaviorRating > 0 && teamRating > 0
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Değerlendirmeyi Gönder", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun RatingSectionCard(
    title: String,
    rating: Float,
    onRatingChanged: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            ModernRatingBar(
                rating = rating,
                onRatingChanged = onRatingChanged
            )
        }
    }
}

@Composable
fun ModernRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..5) {
            val isSelected = i <= rating
            IconButton(
                onClick = { onRatingChanged(i.toFloat()) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFFFB400) else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}