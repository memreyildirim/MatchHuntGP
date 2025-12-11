package com.emreyildirim.matchhuntv1.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.ui.components.LocationPicker
import com.emreyildirim.matchhuntv1.ui.theme.ErrorDark
import com.emreyildirim.matchhuntv1.ui.theme.ErrorLight
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
fun EditMyEventScreen(
    navController: NavController,
    viewModel: EventViewModel,
    event: Event
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

    var eventTitle by remember { mutableStateOf(event.title) }
    var eventDescription by remember { mutableStateOf(event.description) }
    var selectedSportType by remember { mutableStateOf(event.sportType) }
    var eventDate by remember { mutableStateOf(event.date) }
    var eventTime by remember { mutableStateOf(event.time) }
    var eventLocation by remember { mutableStateOf(event.location) }
    var maxParticipants by remember { mutableStateOf(event.maxParticipants.toString()) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(LatLng(event.latitude, event.longitude)) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Sport Type Dropdown
    var expanded by remember { mutableStateOf(false) }
    
    // Date Picker Dialog
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Time Picker Dialog
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit My Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = { Text("Event Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Description
            OutlinedTextField(
                value = eventDescription,
                onValueChange = { eventDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            // Sport Type
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSportType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Sports.list.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport) },
                            onClick = {
                                selectedSportType = sport
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Date and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Choose Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = eventTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time") },
                    leadingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Choose Time")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Location
            OutlinedTextField(
                value = eventLocation,
                onValueChange = {},
                readOnly = true,
                label = { Text("Location") },
                leadingIcon = {
                    IconButton(onClick = { showLocationPicker = true }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Choose Location")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Max Participants
            OutlinedTextField(
                value = maxParticipants,
                onValueChange = { maxParticipants = it },
                label = { Text("Maximum Participants") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Update Button
            Button(
                onClick = {
                    if (eventTitle.isBlank() || eventDescription.isBlank() || 
                        selectedSportType.isBlank() || eventDate.isBlank() || 
                        eventTime.isBlank() || eventLocation.isBlank() || 
                        maxParticipants.isBlank()) {
                        // Show error
                        return@Button
                    }
                    
                    val maxParticipantsInt = maxParticipants.toIntOrNull() ?: return@Button
                    val latitude = selectedLocation?.latitude ?: return@Button
                    val longitude = selectedLocation?.longitude ?: return@Button
                    
                    viewModel.updateEvent(
                        eventId = event.id,
                        title = eventTitle,
                        description = eventDescription,
                        sportType = selectedSportType,
                        date = eventDate,
                        time = eventTime,
                        location = eventLocation,
                        latitude = latitude,
                        longitude = longitude,
                        maxParticipants = maxParticipantsInt
                    )
                    
                    navController.navigateUp()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Event")
            }

            //Delete Button
            Button(onClick = {
                viewModel.deleteEvent(eventId = event.id)
                             navController.navigateUp()
            },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorLight),
                modifier = Modifier.fillMaxWidth()) {
                Text("Delete Event")
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            eventDate = formatter.format(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Okey")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                eventTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                showTimePicker = false
            },
            hour,
            minute,
            true
        ).show()
    }
    
    // Location Picker Dialog
    if (showLocationPicker) {
        LocationPicker(
            onLocationSelected = { latLng ->
                selectedLocation = latLng
                // You might want to get the address from the latLng here
                // For now, we'll just use the existing location
                showLocationPicker = false
            }
        )
    }
} 