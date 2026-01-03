package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emreyildirim.matchhuntv1.data.model.BottomNavItem
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel

// --- LIGHT THEME RENK PALETİ ---
val ActionYellow = Color(0xFFDFFF00)
val AppBackground = Color(0xFFF8F9FA)
val ContentBlack = Color(0xFF1A1A1B)
val MutedGray = Color(0xFF95A5A6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(rootNavController: NavController) {
    val bottomNavController = rememberNavController()
    val viewModel: EventViewModel = viewModel()
    val messageViewModel: MessageViewModel = viewModel() // Paylaşılan instance

    val screens = listOf(
        BottomNavItem("social", Icons.Outlined.Public, "Social"),
        BottomNavItem("events", Icons.Outlined.Search, "Search"),
        // Tavsiye: EmojiEvents (Kupa) spor ve başarı hissi verir
        BottomNavItem("my_events", Icons.Outlined.EmojiEvents, "Events"),
        BottomNavItem("profile", Icons.Outlined.Person, "Profile")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppBackground,
            bottomBar = {
                ModernLightSlidingBar(bottomNavController, screens)
            }
        ) { innerPadding ->
            NavHost(
                navController = bottomNavController,
                startDestination = "social",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("social") {
                    SocialFeedScreen(
                        onNavigateToCreatePost = { rootNavController.navigate("createPost") },
                        onNavigateToMessages = { rootNavController.navigate("messages") },
                        onNavigateToProfile = { userId -> rootNavController.navigate("user_profile/$userId") },
                        messageViewModel = messageViewModel // Paylaşılan instance'ı geç
                    )
                }
                composable("events") {
                    EventsScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { userId -> rootNavController.navigate("user_profile/$userId") }
                    )
                }
                composable("my_events") {
                    MyEventsScreen(rootNavController, viewModel = viewModel)
                }
                composable("profile") {
                    ProfileScreen(rootNavController)
                }
            }
        }
    }
}

@Composable
fun ModernLightSlidingBar(
    navController: NavController,
    screens: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = ContentBlack.copy(alpha = 0.15f)
                    ),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(22.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val itemWidth = maxWidth / screens.size

                    val animatedOffset by animateDpAsState(
                        targetValue = itemWidth * selectedIndex,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "pill_offset"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffset)
                            .width(itemWidth)
                            .fillMaxHeight()
                            .padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                                    )
                                )
                        )
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        screens.forEachIndexed { index, screen ->
                            val isSelected = selectedIndex == index

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) getFilledIcon(screen.label) else screen.icon,
                                        contentDescription = screen.label,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSelected) ContentBlack else MutedGray
                                    )

                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Text(
                                            text = screen.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ContentBlack,
                                            modifier = Modifier.padding(top = 2.dp)
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
}

fun getFilledIcon(label: String): ImageVector {
    return when(label) {
        "Social" -> Icons.Filled.Public
        "Search" -> Icons.Filled.Search
        "Events" -> Icons.Filled.EmojiEvents // Tavsiye edilen kupa ikonu
        "Profile" -> Icons.Filled.Person
        else -> Icons.Filled.Home
    }
}