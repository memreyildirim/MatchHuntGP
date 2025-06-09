package com.emreyildirim.matchhuntv1.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun LocationPicker(
    onLocationSelected: (LatLng) -> Unit,
    initialLocation: LatLng = LatLng(41.0082, 28.9784) // İstanbul koordinatları
) {
    var selectedLocation by remember { mutableStateOf(initialLocation) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 12f)
    }

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            mapToolbarEnabled = true
        )
    }

    val properties = remember {
        MapProperties(
            isMyLocationEnabled = true
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            properties = properties,
            onMapClick = { latLng ->
                selectedLocation = latLng
            }
        ) {
            Marker(
                state = MarkerState(position = selectedLocation),
                title = "Picked Location",
                snippet = "Approve for choose the location"
            )
        }

        Button(
            onClick = { onLocationSelected(selectedLocation) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Approve Location")
        }
    }
} 