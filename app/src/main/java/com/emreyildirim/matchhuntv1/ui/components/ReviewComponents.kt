package com.emreyildirim.matchhuntv1.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.data.repository.EventRepository
import com.emreyildirim.matchhuntv1.utils.Sports
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bireysel bir derecelendirme istatistiğini gösteren modernize edilmiş bileşen.
 */
@Composable
fun RatingDetailItem(
    rating: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300), // Altın sarısı yıldız
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", rating),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier
) {
    // Repository instanceları (Dışarıdan inject edilmesi önerilir ancak mevcut yapı korundu)
    val userRepository = remember { UserRepository() }
    val eventRepository = remember { EventRepository() }

    var reviewerProfileImageUrl by remember { mutableStateOf<String?>(null) }
    var sportType by remember { mutableStateOf<String?>(null) }

    // Reviewer verisini çek
    LaunchedEffect(review.reviewerId) {
        userRepository.getUserProfile(review.reviewerId).onSuccess { profile ->
            reviewerProfileImageUrl = profile.profileImageUrl
        }
    }

    // Etkinlik detayını çek (Spor türü ve ikon için)
    LaunchedEffect(review.eventId) {
        val event = eventRepository.getEventById(review.eventId)
        event?.let { sportType = it.sportType }
    }

    val sportInfo = sportType?.let { Sports.getSportInfo(it) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("tr")) }
    val reviewDate = dateFormat.format(Date(review.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Bölümü
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil Fotoğrafı
                AsyncImage(
                    model = reviewerProfileImageUrl,
                    contentDescription = "Reviewer Profile",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_profile_placeholder)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.reviewerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Spor Türü Etiketi
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        sportInfo?.let { info ->
                            Text(
                                text = info.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = info.color,
                                fontWeight = FontWeight.SemiBold
                            )
                        } ?: Text(
                            text = sportType ?: "Etkinlik",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = reviewDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Puanlama Grid Bölümü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingDetailItem(
                    rating = review.skillRating,
                    label = "Beceri",
                    modifier = Modifier.weight(1f)
                )
                RatingDetailItem(
                    rating = review.behaviorRating,
                    label = "Davranış",
                    modifier = Modifier.weight(1f)
                )
                RatingDetailItem(
                    rating = review.teamRating,
                    label = "Uyum",
                    modifier = Modifier.weight(1f)
                )
            }

            // Yorum Bölümü
            if (!review.comment.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = review.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}