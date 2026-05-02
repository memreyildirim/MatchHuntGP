package com.emreyildirim.matchhuntv1.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import com.emreyildirim.matchhuntv1.utils.LocationUtils
import com.google.android.gms.maps.model.LatLng

@Composable
fun LocationText(
    latLng: LatLng,
    apiKey: String,
    format: LocationUtils.LocationDisplayFormat = LocationUtils.LocationDisplayFormat.SHORT,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf("Konum yükleniyor...") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(latLng) {
        isLoading = true
        try {
            val address = LocationUtils.getAddressFromLatLng(context, latLng, apiKey, format)
            if (address.isNotEmpty() && address != "Konum bilgisi alınamadı") {
                locationText = address
            } else {
                locationText = "${latLng.latitude}, ${latLng.longitude}"
            }
        } catch (e: Exception) {
            locationText = "${latLng.latitude}, ${latLng.longitude}"
        } finally {
            isLoading = false
        }
    }

    Text(
        text = if (isLoading) "Konum yükleniyor..." else locationText,
        modifier = modifier.clickable {
            LocationUtils.openMapsWithLocation(context, latLng)
        },
        textDecoration = TextDecoration.Underline
    )
} 