package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    var isResending by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()

    // Check email verification status
    LaunchedEffect(Unit) {
        while (true) {
            authViewModel.checkEmailVerification()
            if (authViewModel.isEmailVerified()) {
                authViewModel.checkProfileCompletion()

                // Profil sonucunu kısa süre bekle.
                repeat(10) {
                    if (authViewModel.isProfileComplete() != null) return@repeat
                    delay(300)
                }

                val profileState = authViewModel.isProfileComplete()
                val targetRoute = when (profileState) {
                    true -> "main"
                    false -> "createProfile"
                    null -> "completeProfile" // Alan yok/null kullanıcılar için eski akış
                }
                navController.navigate(targetRoute) {
                    popUpTo("emailVerification") { inclusive = true }
                }
                break
            }
            delay(3000) // Check every 3 seconds
        }
    }

    // Countdown timer
    LaunchedEffect(isResending) {
        if (isResending) {
            countdown = 60
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            isResending = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logolastcircle),
                contentDescription = stringResource(R.string.cd_matchhunt_logo),
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = stringResource(R.string.email_ver_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.email_ver_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Resend Email Button
            Button(
                onClick = { 
                    if (!isResending) {
                        authViewModel.sendVerificationEmail()
                        isResending = true
                    }
                },
                enabled = !isLoading && !isResending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (isResending) stringResource(R.string.email_ver_resending, countdown) else stringResource(R.string.email_ver_resend),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Back to Login Button
            TextButton(
                onClick = { 
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("emailVerification") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.email_ver_back_to_login))
            }
        }
    }
} 