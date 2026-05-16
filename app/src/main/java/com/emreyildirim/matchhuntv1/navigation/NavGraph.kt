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
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.ui.screens.*
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider

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
                navController.navigate("completeProfile") {
                    popUpTo("splash") { inclusive = true }
                }
                return
            }
        if (!isProfileComplete) {
            navController.navigate("createProfile") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser != null) {
            scope.launch {
                try {
                    // Kullanıcının e-posta doğrulama durumunu Firestore'dan kontrol et
                    val userDoc = userRepository.getUserProfileData(auth.currentUser!!.uid)
                    val isEmailVerified = userDoc?.get("isEmailVerified") as? Boolean ?: false
                    
                    if (!isEmailVerified) {
                        // E-posta doğrulanmamışsa, Firebase'den kontrol et
                        val firebaseEmailVerified = auth.currentUser?.isEmailVerified ?: false
                        
                        if (!firebaseEmailVerified) {
                            navController.navigate("emailVerification") {
                                popUpTo("splash") { inclusive = true }
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
                    // Kullanıcı profili henüz oluşturulmamışsa direkt profil oluşturma ekranına yönlendir
                    navController.navigate("createProfile") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen()
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
        composable("main") {
            MainScreen(navController)
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