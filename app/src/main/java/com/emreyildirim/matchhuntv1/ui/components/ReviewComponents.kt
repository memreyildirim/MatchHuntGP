package com.emreyildirim.matchhuntv1.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emreyildirim.matchhuntv1.data.model.Review
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.data.repository.EventRepository
import com.emreyildirim.matchhuntv1.utils.Sports
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RatingItem(
    rating: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
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
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier
) {
    val userRepository = remember { UserRepository() }
    val eventRepository = remember { EventRepository() }
    var reviewerProfileImageUrl by remember { mutableStateOf<String?>(null) }
    var sportType by remember { mutableStateOf<String?>(null) }
    
    // Reviewer'ın profil fotoğrafını yükle
    LaunchedEffect(review.reviewerId) {
        userRepository.getUserProfile(review.reviewerId).onSuccess { profile ->
            reviewerProfileImageUrl = profile.profileImageUrl
        }
    }
    
    // Etkinliğin spor türünü yükle
    LaunchedEffect(review.eventId) {
        val event = eventRepository.getEventById(review.eventId)
        event?.let {
            sportType = it.sportType
        }
    }
    
    // Tarih formatını ayarla
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))
    val reviewDate = dateFormat.format(Date(review.timestamp))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Üst kısım: Profil fotoğrafı, kullanıcı adı ve tarih
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol taraf: Profil fotoğrafı ve kullanıcı adı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profil fotoğrafı (Circle)
                    AsyncImage(
                        model = reviewerProfileImageUrl,
                        contentDescription = "Reviewer Profile",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = review.reviewerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Spor türü
                        sportType?.let { sport ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = sport,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Sağ taraf: Tarih
                Text(
                    text = reviewDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Puanlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RatingItem(
                    rating = review.skillRating,
                    label = "Beceri",
                    modifier = Modifier.weight(1f)
                )
                
                RatingItem(
                    rating = review.behaviorRating,
                    label = "Davranış",
                    modifier = Modifier.weight(1f)
                )
                
                RatingItem(
                    rating = review.teamRating,
                    label = "Uyum",
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (!review.comment.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 