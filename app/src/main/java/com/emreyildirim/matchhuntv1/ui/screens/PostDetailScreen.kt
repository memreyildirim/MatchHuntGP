package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.ui.viewmodel.SocialFeedViewModel
import com.emreyildirim.matchhuntv1.ui.theme.SoftGray
import com.emreyildirim.matchhuntv1.ui.theme.Obsidian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavController,
    socialFeedViewModel: SocialFeedViewModel = viewModel(),
    onBackClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val posts by socialFeedViewModel.posts.collectAsState()
    val isLoading by socialFeedViewModel.isLoading.collectAsState()
    val post = remember(posts, postId) { posts.firstOrNull { it.id == postId } }

    LaunchedEffect(postId) {
        socialFeedViewModel.loadSinglePost(postId)
    }

    Scaffold(
        containerColor = SoftGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gönderi Detayı",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Obsidian
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Obsidian
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftGray
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && post == null) {
                CircularProgressIndicator(
                    color = Obsidian,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            } else if (post == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Gönderi bulunamadı veya silinmiş.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PostCard(
                            post = post,
                            onLikeClick = { socialFeedViewModel.likePost(post.id) },
                            onCommentSubmit = { socialFeedViewModel.addComment(post.id, it) },
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                }
            }
        }
    }
}
