package com.emreyildirim.matchhuntv1.navigation

import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.ui.screens.*
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    val scope = rememberCoroutineScope()
    // Mesajlaşma ile ilgili tüm ekranlar için paylaşılan ViewModel (Context ile)
    val messageViewModel: MessageViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MessageViewModel(context.applicationContext) as T
            }
        }
    )

    // Profil tamamlama kontrolü ve yönlendirme
    suspend fun checkAndNavigateToNextScreen() {
        val isProfileComplete = userRepository.isProfileComplete(auth.currentUser!!.uid)
            ?: run {
                // Eski kullanıcılar (alan yok/null): önce CompleteProfile ekranı
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != "completeProfile") {
                    navController.navigate("completeProfile") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
                return
            }
        if (!isProfileComplete) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != "createProfile") {
                navController.navigate("createProfile") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        } else {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == null || !currentRoute.startsWith("main")) {
                navController.navigate("main") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                // Zaten deep link ile "main" rotasındayız. Ekranı korumak için hiçbir şey yapmıyoruz.
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen()

            LaunchedEffect(Unit) {
                delay(800) // Splash ekranı için şık bir 800ms bekleme süresi
                if (auth.currentUser != null) {
                    try {
                        // Kullanıcının e-posta doğrulama durumunu Firestore'dan kontrol et
                        val userDoc = userRepository.getUserProfileData(auth.currentUser!!.uid)
                        val isEmailVerified = userDoc?.get("isEmailVerified") as? Boolean ?: false
                        
                        if (!isEmailVerified) {
                            // E-posta doğrulanmamışsa, Firebase'den kontrol et
                            val firebaseEmailVerified = auth.currentUser?.isEmailVerified ?: false
                            
                            if (!firebaseEmailVerified) {
                                val currentRoute = navController.currentBackStackEntry?.destination?.route
                                if (currentRoute != "emailVerification") {
                                    navController.navigate("emailVerification") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            } else {
                                // Firebase'de doğrulanmış ama Firestore'da güncellenmemiş
                                userRepository.updateEmailVerificationStatus(auth.currentUser!!.uid, true)
                                checkAndNavigateToNextScreen()
                            }
                        } else {
                            checkAndNavigateToNextScreen()
                        }
                    } catch (e: Exception) {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != "createProfile") {
                            // Kullanıcı profili henüz oluşturulmamışsa direkt profil oluşturma ekranına yönlendir
                            navController.navigate("createProfile") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                } else {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute != "login") {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }
        }
        composable("login") {
            LoginScreen(navController)
        }
        composable("register") {
            RegisterScreen(navController)
        }
        composable("emailVerification") {
            EmailVerificationScreen(navController)
        }
        composable("completeProfile") {
            CompleteProfileScreen(navController)
        }
        composable("createProfile") {
            CreateProfileScreen(navController)
        }
        composable("editProfile") {
            EditProfileScreen(navController)
        }
        composable(
            route = "main?eventId={eventId}&postId={postId}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("postId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://matchhunt-17adf.web.app/events/{eventId}" },
                navDeepLink { uriPattern = "https://matchhunt-17adf.firebaseapp.com/events/{eventId}" },
                navDeepLink { uriPattern = "https://matchhunt-17adf.web.app/posts/{postId}" },
                navDeepLink { uriPattern = "https://matchhunt-17adf.firebaseapp.com/posts/{postId}" }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            val postId = backStackEntry.arguments?.getString("postId")

            val auth = FirebaseAuth.getInstance()

            if (auth.currentUser == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("main?eventId={eventId}&postId={postId}") { inclusive = true }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xDFFF00)) // BrandVolt Neon Yellow
                }
            } else {
                var isCheckingProfile by remember { mutableStateOf(true) }
                var isProfileCompleteState by remember { mutableStateOf(false) }

                LaunchedEffect(auth.currentUser?.uid) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val isComplete = userRepository.isProfileComplete(uid) ?: false
                        isProfileCompleteState = isComplete
                        isCheckingProfile = false
                        if (!isComplete) {
                            navController.navigate("createProfile") {
                                popUpTo("main?eventId={eventId}&postId={postId}") { inclusive = true }
                            }
                        }
                    } else {
                        isCheckingProfile = false
                    }
                }

                if (isCheckingProfile) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xDFFF00))
                    }
                } else if (isProfileCompleteState) {
                    if (postId != null) {
                        PostDetailScreen(
                            postId = postId,
                            navController = navController,
                            onBackClick = {
                                val hasBack = navController.previousBackStackEntry != null && 
                                              navController.previousBackStackEntry?.destination?.route != "splash"
                                if (hasBack) {
                                    navController.navigateUp()
                                } else {
                                    navController.navigate("main") {
                                        popUpTo("main?eventId={eventId}&postId={postId}") { inclusive = true }
                                    }
                                }
                            },
                            onNavigateToProfile = { userId ->
                                navController.navigate("user_profile/$userId")
                            }
                        )
                    } else {
                        MainScreen(
                            rootNavController = navController,
                            initialEventId = eventId,
                            initialPostId = postId
                        )
                    }
                }
            }
        }
        composable("profile") {
            ProfileScreen(navController)
        }
        composable("socialFeed") {
            SocialFeedScreen(
                onNavigateToCreatePost = {
                    navController.navigate("createPost")
                },
                onNavigateToMessages = {
                    navController.navigate("messages")
                },
                onNavigateToProfile = { userId ->
                    navController.navigate("user_profile/$userId")
                },
                messageViewModel = messageViewModel // Paylaşılan instance'ı geç
            )
        }
        composable("createPost") {
            CreatePostScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        composable("messages") {
            MessageListScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToChat = { userId ->
                    navController.navigate("messages/$userId")
                },
                viewModel = messageViewModel
            )
        }
        composable(
            route = "messages/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            MessageScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToProfile = { targetUserId ->
                    navController.navigate("user_profile/$targetUserId")
                },
                targetUserId = userId,
                viewModel = messageViewModel
            )
        }
        composable(
            route = "user_profile/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ProfileReviewScreen(
                userId = userId,
                viewModel = viewModel(),
                onNavigateBack = {
                    val previousDestination = navController.previousBackStackEntry?.destination?.route
                    if (previousDestination == "main") {
                        navController.navigateUp()
                    } else {
                        navController.popBackStack("main", false)
                    }
                },
                navController = navController
            )
        }
        composable(
            route = "edit_event/{eventId}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            val viewModel: EventViewModel = viewModel()
            val events = viewModel.events.collectAsState().value
            val event = events.find { it.id == eventId }

            if (event != null) {
                EditMyEventScreen(
                    navController = navController,
                    viewModel = viewModel,
                    event = event
                )
            }
        }
        // Etkinlik sahibini değerlendirme route'u
        composable(
            route = "rate_event/{eventId}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            ProfileRatingScreen(
                eventId = eventId,
                participantId = null,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        // Katılımcıyı değerlendirme route'u
        composable(
            route = "rate_event/{eventId}/{participantId}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("participantId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            val participantId = backStackEntry.arguments?.getString("participantId") ?: return@composable
            ProfileRatingScreen(
                eventId = eventId,
                participantId = participantId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}