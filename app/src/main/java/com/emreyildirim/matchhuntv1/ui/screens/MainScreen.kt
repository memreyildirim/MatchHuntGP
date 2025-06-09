package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
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
