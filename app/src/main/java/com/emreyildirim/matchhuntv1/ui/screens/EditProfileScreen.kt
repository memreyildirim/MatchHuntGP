package com.emreyildirim.matchhuntv1.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var selectedSports by remember { mutableStateOf<List<String>>(emptyList()) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCityDropdownExpanded by remember { mutableStateOf(false) }

    // Mevcut profil bilgilerini al
    val profileData = navController.currentBackStackEntry?.savedStateHandle?.get<Map<String, String>>("profileData")
    
    LaunchedEffect(Unit) {
        // Eğer profileData null ise, doğrudan Firestore'dan verileri al
        if (profileData == null) {
            try {
                val userId = auth.currentUser?.uid ?: return@LaunchedEffect
                val userData = userRepository.getUserProfileData(userId)
                userData?.let { data ->
                    username = (data["username"] as? String) ?: ""
                    age = (data["age"] as? Number)?.toString() ?: ""
                    selectedCity = (data["city"] as? String) ?: ""
                    about = (data["about"] as? String) ?: ""
                    selectedSports = (data["sports"] as? List<String>)?.filter { it.isNotEmpty() } ?: emptyList()
                    (data["profileImageUrl"] as? String)?.let { url ->
                        if (url.isNotEmpty()) {
                            profileImageUri = Uri.parse(url)
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "An error occurred while fetching user data: ${e.message}"
            }
        }
    }
    
    LaunchedEffect(profileData) {
        profileData?.let { data ->
            username = data["username"] ?: ""
            age = data["age"] ?: ""
            selectedCity = data["city"] ?: ""
            about = data["about"] ?: ""
            selectedSports = (data["sports"] ?: "").split(",").filter { it.isNotEmpty() }
            data["profileImageUrl"]?.let { url ->
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
                        text = "Edit Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
                    .size(140.dp)
                    .padding(vertical = 24.dp)
                    .clip(CircleShape)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
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
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
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
                        .padding(8.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add Photo",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Kullanıcı Adı
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    label = { 
                        Text(
                            "City",
                            style = MaterialTheme.typography.labelLarge
                        ) 
                    },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = isCityDropdownExpanded
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = isCityDropdownExpanded,
                    onDismissRequest = { isCityDropdownExpanded = false },
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Cities.list.forEach { city ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    city,
                                    style = MaterialTheme.typography.bodyLarge
                                ) 
                            },
                            onClick = {
                                selectedCity = city
                                isCityDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // İlgi Alanları Başlığı
            Text(
                text = "Interest",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
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
                    val label = sportInfo.name               // ekranda görünen: "Futbol"

                    FilterChip(
                        selected = selectedSports.contains(key),
                        onClick = {
                            selectedSports = if (selectedSports.contains(key)) {
                                selectedSports - key
                            } else {
                                selectedSports + key
                            }
                        },
                        label = {
                            Text(label)
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Kaydet Butonu
            Button(
                onClick = {
                    if (username.isBlank() || age.isBlank() || selectedCity.isBlank() || selectedSports.isEmpty()) {
                        errorMessage = "PLease fill the all space"
                        return@Button
                    }
                    
                    scope.launch {
                        try {
                            isLoading = true
                            val userId = auth.currentUser?.uid ?: return@launch
                            
                            // Mevcut profil fotoğrafı URL'sini al
                            var photoUrl = ""
                            val userData = userRepository.getUserProfileData(userId)
                            photoUrl = (userData?.get("profileImageUrl") as? String) ?: ""
                            
                            // Eğer yeni bir fotoğraf seçildiyse ve mevcut fotoğraftan farklıysa
                            if (profileImageUri != null && profileImageUri.toString() != photoUrl) {
                                try {
                                    // Yeni fotoğrafı yükle
                                    photoUrl = userRepository.uploadProfileImage(userId, profileImageUri!!)
                                } catch (e: Exception) {
                                    println("Fotoğraf güncellenirken hata: ${e.message}")
                                }
                            }
                            
                            // Profil bilgilerini güncelle
                            userRepository.createUserProfile(
                                userId = userId,
                                username = username,
                                age = age.toInt(),
                                city = selectedCity,
                                sports = selectedSports.map { it.lowercase() }, // push notif için lowercase save ettik
                                about = about
                            )
                            
                            // Profil fotoğrafını güncelle
                            if (photoUrl.isNotEmpty()) {
                                userRepository.updateProfileImage(userId, photoUrl)
                            }
                            
                            // ProfileScreen'e güncelleme bilgisi gönder
                            navController.previousBackStackEntry?.savedStateHandle?.set("profileUpdated", true)
                            navController.navigateUp()
                        } catch (e: Exception) {
                            errorMessage = "Profil güncellenirken bir hata oluştu: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
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
                        text = "Save the changes",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
            
            // Hata Mesajı
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }
        }
    }
}