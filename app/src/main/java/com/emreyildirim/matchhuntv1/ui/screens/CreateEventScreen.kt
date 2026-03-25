package com.emreyildirim.matchhuntv1.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import com.emreyildirim.matchhuntv1.R
import com.emreyildirim.matchhuntv1.ui.components.LocationPicker
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.emreyildirim.matchhuntv1.utils.Sports
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateEventScreen(
    viewModel: EventViewModel,
    pagerState: PagerState
) {
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }

    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var maxParticipants by remember { mutableStateOf("") }
    var showLocationPicker by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedSportKey by remember { mutableStateOf("") }

// Ekranda göstereceğimiz label:
    val selectedSportLabel =
        Sports.getSportInfo(selectedSportKey)?.name ?: ""
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val eventCreated by viewModel.eventCreated.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Sport Type Dropdown
    var expanded by remember { mutableStateOf(false) }
    
    // Date Picker Dialog
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Time Picker Dialog
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Event created effect
    LaunchedEffect(eventCreated) {
        if (eventCreated) {
            android.widget.Toast.makeText(
                context,
                "Event created successfully!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            eventTitle = ""
            eventDescription = ""
            selectedSportKey = ""
            eventDate = ""
            eventTime = ""
            eventLocation = ""
            maxParticipants = ""
            
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
            
            viewModel.resetEventCreated()
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Date(millis)
                            val currentDate = Date()
                            
                            val calendar = Calendar.getInstance()
                            calendar.time = selectedDate
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            
                            val currentCalendar = Calendar.getInstance()
                            currentCalendar.time = currentDate
                            currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
                            currentCalendar.set(Calendar.MINUTE, 0)
                            currentCalendar.set(Calendar.SECOND, 0)
                            currentCalendar.set(Calendar.MILLISECOND, 0)
                            
                            if (calendar.time.before(currentCalendar.time)) {
                                android.widget.Toast.makeText(
                                    context,
                                    "You cannot select a past date!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                eventDate = dateFormat.format(selectedDate)
                                showDatePicker = false
                            }
                        }
                    }
                ) {
                    Text("Okey")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timePickerState.hour
                        val minute = timePickerState.minute
                        eventTime = String.format("%02d:%02d", hour, minute)
                        showTimePicker = false
                    }
                ) {
                    Text("Okey")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Location Picker Dialog
    if (showLocationPicker) {
        AlertDialog(
            onDismissRequest = { showLocationPicker = false },
            title = { Text("Choose Location") },
            text = {
                LocationPicker(
                    onLocationSelected = { location ->
                        selectedLocation = location
                        eventLocation = "${location.latitude}, ${location.longitude}"
                        showLocationPicker = false
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showLocationPicker = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.secondaryContainer),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Text(
                text = "Create New Event",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Event Title
            OutlinedTextField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = { Text("Event Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Obsidian,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Obsidian,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            
            // Sport Type Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSportLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event Type") },
                    leadingIcon = {
                        val sportInfo = Sports.getSportInfo(selectedSportKey)
                        if (sportInfo != null) {
                            Icon(
                                painter = painterResource(id = sportInfo.iconResId),
                                contentDescription = selectedSportLabel,
                                modifier = Modifier.size(24.dp),
                                tint = null
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sports,
                                contentDescription = selectedSportLabel,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Obsidian,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Obsidian,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Sports.allSports.forEach { sportInfo ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sportInfo.nameEn) // label: İngilizce ad veya istersen sportInfo.name
                                    Icon(
                                        painter = painterResource(id = sportInfo.iconResId),
                                        contentDescription = sportInfo.nameEn,
                                        modifier = Modifier.size(24.dp),
                                        tint = null
                                    )
                                }
                            },
                            onClick = {
                                selectedSportKey = sportInfo.nameEn.lowercase()   // KEY: "football"
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Event Description
            OutlinedTextField(
                value = eventDescription,
                onValueChange = { eventDescription = it },
                label = { Text("Event Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Obsidian,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Obsidian,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            
            // Date and Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Event Date with Date Picker
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { },
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Choose Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Obsidian,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Obsidian,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                // Event Time with Time Picker
                OutlinedTextField(
                    value = eventTime,
                    onValueChange = { },
                    label = { Text("Time") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Choose Time",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Obsidian,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Obsidian,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
            
            // Event Location
            OutlinedTextField(
                value = eventLocation,
                onValueChange = { },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showLocationPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Choose Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Obsidian,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Obsidian,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            
            // Max Participants
            OutlinedTextField(
                value = maxParticipants,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        maxParticipants = newValue
                    }
                },
                label = { Text("Maximum Number of Participants") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Obsidian,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Obsidian,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            
            // Error message
            error?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Create Event Button
            Button(
                onClick = {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val selectedDateTime = dateFormat.parse("$eventDate $eventTime")
                    val currentDateTime = Date()
                    
                    if (selectedDateTime?.before(currentDateTime) == true) {
                        android.widget.Toast.makeText(
                            context,
                            "You cannot select a past date and time!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else if (selectedLocation == null) {
                        android.widget.Toast.makeText(
                            context,
                            "Please choose a location!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.createEvent(
                            title = eventTitle,
                            description = eventDescription,
                            sportType = selectedSportKey,
                            date = eventDate,
                            time = eventTime,
                            location = eventLocation,
                            latitude = selectedLocation!!.latitude,
                            longitude = selectedLocation!!.longitude,
                            maxParticipants = maxParticipants.toIntOrNull() ?: 0
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = eventTitle.isNotBlank() && 
                         selectedSportKey.isNotBlank() &&
                         eventDescription.isNotBlank() && 
                         eventDate.isNotBlank() && 
                         eventTime.isNotBlank() && 
                         selectedLocation != null && 
                         maxParticipants.isNotBlank() && 
                         !isLoading,
                shape = MaterialTheme.shapes.medium,
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
                        "Create Event",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
} 