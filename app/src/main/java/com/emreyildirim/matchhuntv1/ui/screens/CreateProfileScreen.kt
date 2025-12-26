package com.emreyildirim.matchhuntv1.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.utils.Cities
import com.emreyildirim.matchhuntv1.utils.Sports
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    val storage = FirebaseStorage.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var selectedSports by remember { mutableStateOf(setOf<String>()) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCityDropdownExpanded by remember { mutableStateOf(false) }
    
    // Mevcut profil bilgilerini al
    val profileData = navController.currentBackStackEntry?.savedStateHandle?.get<Map<String, String>>("profileData")
    
    LaunchedEffect(profileData) {
        profileData?.let { data ->
            username = data["username"] ?: ""
            age = data["age"] ?: ""
            selectedCity = data["city"] ?: ""
            about = data["about"] ?: ""
            selectedSports = data["sports"]?.split(",")?.map { it.trim() }?.toSet() ?: setOf()
            data["photoUrl"]?.let { url ->
                if (url.isNotEmpty()) {
                    profileImageUri = Uri.parse(url)
                }
            }
        }
    }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileImageUri = uri
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (profileData != null) "Edit Profile" else "Create Profil",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profil Fotoğrafı
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(vertical = 24.dp)
            ) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(profileImageUri)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
                
                IconButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add Image",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            // Kullanıcı Adı
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                }
            )
            
            // Yaş
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                label = { Text("Age") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null
                    )
                }
            )
            
            // Şehir Seçimi (Dropdown)
            ExposedDropdownMenuBox(
                expanded = isCityDropdownExpanded,
                onExpandedChange = { isCityDropdownExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedCity,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("City") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCityDropdownExpanded) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = isCityDropdownExpanded,
                    onDismissRequest = { isCityDropdownExpanded = false }
                ) {
                    Cities.list.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city) },
                            onClick = {
                                selectedCity = city
                                isCityDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Hakkında
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                minLines = 3,
                maxLines = 5
            )
            
            // İlgi Alanları Başlığı
            Text(
                text = "Interest ",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            
            // İlgi Alanları Listesi
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Sports.allSports) { sportInfo ->
                    val key = sportInfo.nameEn.lowercase()   // "Football" -> "football"
                    val label = sportInfo.name              // Görünen isim: "Futbol"

                    FilterChip(
                        selected = selectedSports.contains(key),
                        onClick = {
                            selectedSports = if (selectedSports.contains(key)) {
                                selectedSports - key
                            } else {
                                selectedSports + key
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Kaydet Butonu
            Button(
                onClick = {
                    if (username.isBlank() || age.isBlank() || selectedCity.isBlank() || selectedSports.isEmpty()) {
                        errorMessage = "Please fill in all fields."
                        return@Button
                    }
                    
                    scope.launch {
                        try {
                            isLoading = true
                            val userId = auth.currentUser?.uid ?: return@launch
                            
                            // Profil fotoğrafını yükle
                            var photoUrl = ""
                            if (profileImageUri != null) {
                                photoUrl = userRepository.uploadProfileImage(userId, profileImageUri!!)
                            }

                            // Profil bilgilerini kaydet
                            val ageInt = age.toIntOrNull()
                            if (ageInt == null || ageInt <= 0 || ageInt >= 120) {
                                throw Exception("Enter the valid age")
                            }
                            val success = userRepository.createUserProfile(
                                userId = userId,
                                username = username,
                                age = ageInt,
                                city = selectedCity,
                                sports = selectedSports.toList(),
                                about = about
                            )
                            
                            // Profil fotoğrafı varsa güncelle
                            if (photoUrl.isNotEmpty()) {
                                userRepository.updateProfileImage(userId, photoUrl)
                            }
                            
                            if (!success) {
                                throw Exception("An error occurred while creating the profile")
                            }
                            
                            navController.navigate("main") {
                                popUpTo("createProfile") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            errorMessage = "An error occurred while creating the profile: ${e.message}"
                            Log.e("CreateProfileScreen", "Error creating profile", e )
                            Toast.makeText(context, " ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Create Profile",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            // Hata Mesajı
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
} 