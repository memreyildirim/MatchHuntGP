package com.emreyildirim.matchhuntv1.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.utils.Cities
import com.emreyildirim.matchhuntv1.utils.Sports
import com.emreyildirim.matchhuntv1.utils.NetworkUtils
import com.emreyildirim.matchhuntv1.utils.withNetworkTimeout
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.ui.theme.BrandVolt
import com.emreyildirim.matchhuntv1.ui.theme.Obsidian
import com.emreyildirim.matchhuntv1.ui.theme.SoftGray
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val networkUnavailableShortText = stringResource(R.string.error_network_unavailable_short)
    val fillRequiredFieldsText = stringResource(R.string.edit_profile_fill_required_fields)
    val networkUnavailableDetailText = stringResource(R.string.error_network_unavailable_detail)
    val profileUpdatedText = stringResource(R.string.toast_profile_updated)

    // State Variables
    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var selectedSports by remember { mutableStateOf<List<String>>(emptyList()) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCityDropdownExpanded by remember { mutableStateOf(false) }

    // Navigation Argument Handling
    val profileData = navController.currentBackStackEntry?.savedStateHandle?.get<Map<String, String>>("profileData")

    // Initial Data Loading
    LaunchedEffect(Unit) {
        if (profileData == null) {
            try {
                // Network kontrolü
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    errorMessage = networkUnavailableShortText
                    return@LaunchedEffect
                }
                
                val userId = auth.currentUser?.uid ?: return@LaunchedEffect
                val userData = withNetworkTimeout {
                    userRepository.getUserProfileData(userId)
                }
                userData?.let { data ->
                    username = (data["username"] as? String) ?: ""
                    age = (data["age"] as? Number)?.toString() ?: ""
                    selectedCity = (data["city"] as? String) ?: ""
                    about = (data["about"] as? String) ?: ""
                    selectedSports = (data["sports"] as? List<String>)?.filter { it.isNotEmpty() } ?: emptyList()
                    (data["profileImageUrl"] as? String)?.let { url ->
                        if (url.isNotEmpty()) profileImageUri = Uri.parse(url)
                    }
                }
            } catch (e: Exception) {
                errorMessage = NetworkUtils.getErrorMessage(e)
            }
        }
    }

    // Handle Profile Data from Arguments
    LaunchedEffect(profileData) {
        profileData?.let { data ->
            username = data["username"] ?: ""
            age = data["age"] ?: ""
            selectedCity = data["city"] ?: ""
            about = data["about"] ?: ""
            selectedSports = (data["sports"] ?: "").split(",").filter { it.isNotEmpty() }
            data["photoUrl"]?.let { url -> // "profileImageUrl" instead of "photoUrl" depends on your nav logic, fixed to common usage
                if (url.isNotEmpty()) profileImageUri = Uri.parse(url)
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) profileImageUri = uri
    }

    Scaffold(
        containerColor = SoftGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_profile_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Obsidian
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftGray),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Obsidian
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image Section
            Box(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 32.dp)
                    .size(130.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, BrandVolt, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Image(
                        painter = if (profileImageUri != null) {
                            rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(profileImageUri)
                                    .crossfade(true)
                                    .build()
                            )
                        } else {
                            painterResource(id = R.drawable.ic_profile_placeholder)
                        },
                        contentDescription = stringResource(R.string.cd_profile_image),
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // Camera Action Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(40.dp)
                        .background(Obsidian, CircleShape)
                        .border(2.dp, BrandVolt, CircleShape)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.cd_add_photo),
                        tint = BrandVolt,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.profile_field_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Obsidian,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Obsidian,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                // Age
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text(stringResource(R.string.profile_field_age)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Obsidian,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Obsidian,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                // City Selection
                ExposedDropdownMenuBox(
                    expanded = isCityDropdownExpanded,
                    onExpandedChange = { isCityDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.profile_field_city)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCityDropdownExpanded) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Obsidian) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Obsidian,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Obsidian,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = isCityDropdownExpanded,
                        onDismissRequest = { isCityDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
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

                // About
                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it },
                    label = { Text(stringResource(R.string.profile_field_about_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Obsidian,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Obsidian,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // Interests Header
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Interests, null, modifier = Modifier.size(18.dp), tint = Obsidian)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.edit_profile_interests_header),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.Gray
                    )
                )
            }

            // Sports Horizontal List
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(Sports.allSports) { sportInfo ->
                    val key = sportInfo.nameEn.lowercase()
                    val isSelected = selectedSports.contains(key)
                    val sportColor = sportInfo.color

                    Surface(
                        modifier = Modifier.clickable {
                            selectedSports = if (isSelected) selectedSports - key else selectedSports + key
                        },
                        color = if (isSelected) Obsidian else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) Obsidian else Color.LightGray.copy(alpha = 0.5f)),
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = sportInfo.iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sportInfo.nameEn.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (isSelected) sportColor else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Error Message
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Save Button
            Button(
                onClick = {
                    if (username.isBlank() || age.isBlank() || selectedCity.isBlank() || selectedSports.isEmpty()) {
                        errorMessage = fillRequiredFieldsText
                        return@Button
                    }

                    scope.launch {
                        try {
                            isLoading = true
                            errorMessage = null
                            
                            // Network kontrolü
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                errorMessage = networkUnavailableDetailText
                                isLoading = false
                                return@launch
                            }
                            
                            val userId = auth.currentUser?.uid ?: return@launch

                            var photoUrl = ""
                            val userData = withNetworkTimeout {
                                userRepository.getUserProfileData(userId)
                            }
                            photoUrl = (userData?.get("profileImageUrl") as? String) ?: ""

                            // Image Upload Logic - fotoğraf yükleme için daha uzun timeout
                            if (profileImageUri != null && !profileImageUri.toString().startsWith("http")) {
                                try {
                                    photoUrl = withNetworkTimeout(NetworkUtils.UPLOAD_TIMEOUT_MS) {
                                        userRepository.uploadProfileImage(userId, profileImageUri!!)
                                    }
                                } catch (e: Exception) {
                                    errorMessage = NetworkUtils.getErrorMessage(e)
                                    isLoading = false
                                    return@launch
                                }
                            }

                            // Create/Update Profile
                            withNetworkTimeout {
                                userRepository.createUserProfile(
                                    userId = userId,
                                    username = username,
                                    age = age.toIntOrNull() ?: 0,
                                    city = selectedCity,
                                    sports = selectedSports,
                                    about = about
                                )
                            }

                            if (photoUrl.isNotEmpty()) {
                                withNetworkTimeout {
                                    userRepository.updateProfileImage(userId, photoUrl)
                                }
                            }

                            Toast.makeText(context, profileUpdatedText, Toast.LENGTH_SHORT).show()
                            navController.previousBackStackEntry?.savedStateHandle?.set("profileUpdated", true)
                            navController.navigateUp()
                        } catch (e: Exception) {
                            errorMessage = NetworkUtils.getErrorMessage(e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(if (isLoading) 0.dp else 4.dp, RoundedCornerShape(16.dp)),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Obsidian,
                    contentColor = BrandVolt,
                    disabledContainerColor = Obsidian.copy(alpha = 0.7f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandVolt)
                } else {
                    Text(
                        text = stringResource(R.string.edit_profile_save_changes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}