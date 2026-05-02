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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emreyildirim.matchhuntv1.R
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
    val pastDateTimeText = stringResource(R.string.edit_my_event_err_past_datetime)
    val invalidDateTimeText = stringResource(R.string.edit_my_event_err_invalid_datetime_format)
    val pastDateText = stringResource(R.string.edit_my_event_err_past_date)
    val pastTimeText = stringResource(R.string.edit_my_event_err_past_time)
    
    // Sport Type Dropdown
    var expanded by remember { mutableStateOf(false) }
    
    // Date Picker Dialog
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Time Picker Dialog
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_my_event_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                label = { Text(stringResource(R.string.edit_my_event_field_title)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Description
            OutlinedTextField(
                value = eventDescription,
                onValueChange = { eventDescription = it },
                label = { Text(stringResource(R.string.edit_my_event_field_description)) },
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
                    label = { Text(stringResource(R.string.edit_my_event_field_sport)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Sports.allSports.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport.nameEn) },
                            onClick = {
                                selectedSportType = sport.nameEn.lowercase()
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
                    label = { Text(stringResource(R.string.edit_my_event_field_date)) },
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.cd_choose_date))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = eventTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.edit_my_event_field_time)) },
                    leadingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.cd_choose_time))
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
                label = { Text(stringResource(R.string.edit_my_event_field_location)) },
                leadingIcon = {
                    IconButton(onClick = { showLocationPicker = true }) {
                        Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.cd_choose_location))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Max Participants
            OutlinedTextField(
                value = maxParticipants,
                onValueChange = { maxParticipants = it },
                label = { Text(stringResource(R.string.edit_my_event_field_max_participants)) },
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
                    
                    // Validate that date and time are not in the past
                    try {
                        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                        
                        val selectedDate = dateFormatter.parse(eventDate)
                        val selectedTime = timeFormatter.parse(eventTime)
                        
                        val selectedCalendar = Calendar.getInstance()
                        selectedCalendar.time = selectedDate
                        val timeParts = eventTime.split(":")
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        selectedCalendar.set(Calendar.MINUTE, timeParts[1].toInt())
                        selectedCalendar.set(Calendar.SECOND, 0)
                        selectedCalendar.set(Calendar.MILLISECOND, 0)
                        
                        val currentCalendar = Calendar.getInstance()
                        
                        if (selectedCalendar.time.before(currentCalendar.time)) {
                            android.widget.Toast.makeText(
                                context,
                                pastDateTimeText,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            invalidDateTimeText,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
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
                Text(stringResource(R.string.edit_my_event_update))
            }

            //Delete Button
            Button(onClick = {
                viewModel.deleteEvent(eventId = event.id)
                             navController.navigateUp()
            },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorLight),
                modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.edit_my_event_delete))
            }
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
                                    pastDateText,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                eventDate = formatter.format(selectedDate)
                                showDatePicker = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
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
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // Parse the selected date to check if it's today
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val selectedDate = try {
            dateFormatter.parse(eventDate)
        } catch (e: Exception) {
            null
        }
        
        val isToday = selectedDate?.let {
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.time = it
            val currentCalendar = Calendar.getInstance()
            selectedCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
            selectedCalendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)
        } ?: false
        
        // If the selected date is today, use current time as initial time
        val initialHour = if (isToday) currentHour else 0
        val initialMinute = if (isToday) currentMinute else 0

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                // Check if selected time is in the past (only if date is today)
                if (isToday) {
                    val selectedTimeInMinutes = selectedHour * 60 + selectedMinute
                    val currentTimeInMinutes = currentHour * 60 + currentMinute
                    
                    if (selectedTimeInMinutes < currentTimeInMinutes) {
                        android.widget.Toast.makeText(
                            context,
                            pastTimeText,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@TimePickerDialog
                    }
                }
                
                eventTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                showTimePicker = false
            },
            initialHour,
            initialMinute,
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