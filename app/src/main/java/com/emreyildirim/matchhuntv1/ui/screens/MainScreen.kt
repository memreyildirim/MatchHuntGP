package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emreyildirim.matchhuntv1.data.model.BottomNavItem
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(rootNavController: NavController) {
    val bottomNavController = rememberNavController()
    val viewModel: EventViewModel = viewModel()

    val screens = listOf(
        BottomNavItem("social", Icons.Default.Home, "Social"),
        BottomNavItem("events", Icons.Default.Search, "Search"),
        BottomNavItem("my_events", Icons.Default.History, "My Events"),
        BottomNavItem("profile", Icons.Default.Person, "Profile")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    
                    NavigationBarItem(
                        icon = { 
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                                },
                                label = "icon_animation"
                            ) { selected ->
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.label,
                                    modifier = Modifier
                                        .size(if (selected) 28.dp else 24.dp)
                                        .scale(if (selected) 1.1f else 1f),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = { 
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                                },
                                label = "label_animation"
                            ) { selected ->
                                Text(
                                    screen.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "social",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("social") {
                SocialFeedScreen(
                    onNavigateToCreatePost = {
                        rootNavController.navigate("createPost")
                    },
                    onNavigateToMessages = {
                        rootNavController.navigate("messages")
                    },
                    onNavigateToProfile = { userId ->
                        rootNavController.navigate("user_profile/$userId")
                    }
                )
            }
            composable("events") {
                EventsScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = { userId ->
                        rootNavController.navigate("user_profile/$userId")
                    }
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
