package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import com.emreyildirim.matchhuntv1.utils.PasswordValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var passwordErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()

    // Email format kontrolü için Regex
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}\$".toRegex()
    val isEmailValid = email.matches(emailRegex)

    LaunchedEffect(authViewModel.currentUser) {
        authViewModel.currentUser.collect { user ->
            if (user != null) {
                navController.navigate("emailVerification") {
                    popUpTo("register") { inclusive = true }
                }
            }
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
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Logo Bölümü
                Box(
                    modifier = Modifier
                        .size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logolastcircle),
                        contentDescription = "MatchHunt Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Başlık
                Text(
                    text = "YENİ HESAP",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Obsidian
                    )
                )

                Text(
                    text = "Aramıza katıl ve partnerini bul",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Kayıt Alanları
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email Alanı
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-posta Adresi") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Obsidian) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = email.isNotBlank() && !isEmailValid,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Obsidian,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Obsidian,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        singleLine = true
                    )

                    // Şifre Alanı
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordErrors = PasswordValidator.validate(it).errors
                        },
                        label = { Text("Şifre") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Obsidian) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = passwordErrors.isNotEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Obsidian,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Obsidian,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        singleLine = true
                    )

                    // Şifre Hataları Listesi
                    AnimatedVisibility(visible = passwordErrors.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            passwordErrors.forEach { errorMsg ->
                                Text(
                                    text = "• $errorMsg",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Şifre Tekrar Alanı
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Şifre Tekrar") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Obsidian) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = confirmPassword.isNotBlank() && password != confirmPassword,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Obsidian,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Obsidian,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        singleLine = true
                    )

                    if (confirmPassword.isNotBlank() && password != confirmPassword) {
                        Text(
                            text = "Şifreler uyuşmuyor",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Genel Hata Mesajı Paneli (E-posta doğrulama dahil)
                val emailErrorMessage = if (email.isNotBlank() && !isEmailValid) "Geçersiz e-posta formatı." else null
                val displayMessage = error ?: emailErrorMessage

                AnimatedVisibility(visible = displayMessage != null) {
                    Text(
                        text = displayMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Kayıt Butonu
                Button(
                    onClick = {
                        if (password == confirmPassword && passwordErrors.isEmpty() && isEmailValid) {
                            authViewModel.signUp(email, password)
                        }
                    },
                    enabled = !isLoading &&
                            email.isNotBlank() &&
                            password.isNotBlank() &&
                            confirmPassword.isNotBlank() &&
                            password == confirmPassword &&
                            passwordErrors.isEmpty() &&
                            isEmailValid,
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
                            text = "KAYIT OL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Giriş Yap Yönlendirmesi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zaten hesabın var mı?",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { navController.navigate("login") }) {
                        Text(
                            "Giriş Yap",
                            color = Obsidian,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}