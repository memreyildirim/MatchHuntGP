package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.ui.theme.AcceptGreen
import com.emreyildirim.matchhuntv1.ui.theme.BrandVolt
import com.emreyildirim.matchhuntv1.ui.theme.MutedText
import com.emreyildirim.matchhuntv1.ui.theme.Obsidian
import com.emreyildirim.matchhuntv1.ui.theme.PureWhite
import com.emreyildirim.matchhuntv1.ui.theme.SoftGray
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val errorEnterEmailFirst = stringResource(R.string.login_error_enter_email_first)
    val errorInvalidEmailFormat = stringResource(R.string.login_error_invalid_email_format)
    val resetSentText = stringResource(R.string.login_reset_sent)
    val enterValidEmailText = stringResource(R.string.login_enter_valid_email)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Yerel geri bildirim mesajı (Başarı veya format hataları için)
    var localFeedback by remember { mutableStateOf<String?>(null) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()
    val isProfileComplete by authViewModel.isProfileComplete.collectAsState()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var profileCheckRequested by remember { mutableStateOf(false) }

    // Email format kontrolü için Regex
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}\$".toRegex()
    val isEmailValid = email.matches(emailRegex)

    LaunchedEffect(currentUser, isEmailVerified, isProfileComplete, isLoading) {
        if (currentUser == null) return@LaunchedEffect
        // Giriş işlemi tamamlanmadan yönlendirme kararı verme
        if (isLoading) return@LaunchedEffect
        if (!isEmailVerified) {
            navController.navigate("emailVerification") { popUpTo("login") { inclusive = true } }
            return@LaunchedEffect
        }
        // isEmailVerified == true ama isProfileComplete null ise DB'den tekrar sorgu yap.
        if (isProfileComplete == null) {
            if (!profileCheckRequested) {
                profileCheckRequested = true
                authViewModel.checkProfileCompletion()
                return@LaunchedEffect
            }
            // Alan yok/null kullanıcılar için eski akış: önce CompleteProfile ekranı
            navController.navigate("completeProfile") { popUpTo("login") { inclusive = true } }
            return@LaunchedEffect
        }

        if (isProfileComplete == false) {
            navController.navigate("createProfile") { popUpTo("login") { inclusive = true } }
        } else {
            navController.navigate("main") { popUpTo("login") { inclusive = true } }
        }
    }

    // Kullanıcı çıkış yaptığında bir sonraki giriş için flag'i sıfırla
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            profileCheckRequested = false
        }
    }

    Scaffold(
        containerColor = SoftGray
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo Bölümü
                Box(
                    modifier = Modifier
                        .size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logolastcircle),
                        contentDescription = stringResource(R.string.cd_matchhunt_logo),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Karşılama Metni
                Text(
                    text = stringResource(R.string.login_title_welcome),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Obsidian
                    )
                )

                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MutedText,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Giriş Alanları Kartı
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email Alanı
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            localFeedback = null // Yazmaya başlayınca mesajı temizle
                        },
                        label = { Text(stringResource(R.string.auth_field_email_address)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Obsidian) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                            .testTag("emailTextField"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Obsidian,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Obsidian,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        ),
                        singleLine = true
                    )

                    // Şifre Alanı
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Obsidian) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MutedText
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Obsidian,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Obsidian,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        ),
                        singleLine = true
                    )
                }

                // Şifremi Unuttum
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            localFeedback = errorEnterEmailFirst
                        } else if (!isEmailValid) {
                            localFeedback = errorInvalidEmailFormat
                        } else {
                            authViewModel.resetPassword(email)
                            localFeedback = resetSentText
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.login_forgot_password),
                        color = Obsidian,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bilgi ve Hata Mesajı Paneli
                val displayMessage = error ?: localFeedback
                AnimatedVisibility(
                    visible = displayMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = displayMessage ?: "",
                        color = if (error != null || (localFeedback?.contains("geçersiz", ignoreCase = true) == true || localFeedback?.contains("önce", ignoreCase = true) == true))
                            MaterialTheme.colorScheme.error
                        else
                            AcceptGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Giriş Butonu
                Button(
                    onClick = {
                        if (isEmailValid) {
                            authViewModel.signIn(email, password)
                        } else {
                            localFeedback = enterValidEmailText
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(if (isLoading) 0.dp else 4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Obsidian,
                        contentColor = BrandVolt,
                        disabledContainerColor = Obsidian.copy(alpha = 0.7f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BrandVolt,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.login_button_caps),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Kayıt Ol Yönlendirmesi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.no_account),
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { navController.navigate("register") }) {
                        Text(
                            stringResource(R.string.register),
                            color = Obsidian,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}