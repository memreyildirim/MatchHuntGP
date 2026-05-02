package com.emreyildirim.matchhuntv1.utils

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.util.Locale
import org.json.JSONObject

object LocationUtils {
    enum class LocationDisplayFormat {
        SHORT, // ilce, semt
        MEDIUM // ilce, semt, il
    }

    suspend fun getAddressFromLatLng(
        context: Context,
        latLng: LatLng,
        apiKey: String,
        format: LocationDisplayFormat = LocationDisplayFormat.SHORT
    ): String {
        return withContext(Dispatchers.IO) {
            val geocoderAddress = getAddressFromGeocoder(context, latLng, format)
            if (geocoderAddress.isNotEmpty()) return@withContext geocoderAddress

            val apiAddress = getAddressFromGoogleApi(latLng, apiKey, format)
            if (apiAddress.isNotEmpty()) apiAddress else "Konum bilgisi alınamadı"
        }
    }

    private fun getAddressFromGeocoder(
        context: Context,
        latLng: LatLng,
        format: LocationDisplayFormat
    ): String {
        return try {
            if (!Geocoder.isPresent()) return ""
            val geocoder = Geocoder(context, Locale("tr", "TR"))
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            formatFromAddress(results?.firstOrNull(), format)
        } catch (_: Exception) {
            ""
        }
    }

    private fun getAddressFromGoogleApi(
        latLng: LatLng,
        apiKey: String,
        format: LocationDisplayFormat
    ): String {
        if (apiKey.isBlank()) return ""
        return try {
            val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latLng.latitude},${latLng.longitude}&key=$apiKey&language=tr"
            val response = URL(url).readText()
            val jsonResponse = JSONObject(response)
            if (jsonResponse.optString("status") != "OK") return ""

            val results = jsonResponse.optJSONArray("results") ?: return ""
            if (results.length() == 0) return ""
            val result = results.getJSONObject(0)
            val addressComponents = result.optJSONArray("address_components")
            var district = ""
            var neighborhood = ""
            var city = ""

            if (addressComponents != null) {
                for (i in 0 until addressComponents.length()) {
                    val component = addressComponents.getJSONObject(i)
                    val types = component.optJSONArray("types") ?: continue
                    for (j in 0 until types.length()) {
                        when (types.getString(j)) {
                            "sublocality_level_1" -> district = component.optString("long_name")
                            "sublocality_level_2" -> neighborhood = component.optString("long_name")
                            "administrative_area_level_1" -> city = component.optString("long_name")
                        }
                    }
                }
            }

            val short = when {
                district.isNotEmpty() && neighborhood.isNotEmpty() -> "$district, $neighborhood"
                district.isNotEmpty() -> district
                else -> ""
            }

            when (format) {
                LocationDisplayFormat.SHORT -> {
                    if (short.isNotEmpty()) short else result.optString("formatted_address")
                }
                LocationDisplayFormat.MEDIUM -> {
                    if (short.isNotEmpty() && city.isNotEmpty() && !short.contains(city)) {
                        "$short, $city"
                    } else if (short.isNotEmpty()) {
                        short
                    } else {
                        result.optString("formatted_address")
                    }
                }
            }
        } catch (_: IOException) {
            ""
        }
    }

    private fun formatFromAddress(
        address: Address?,
        format: LocationDisplayFormat
    ): String {
        if (address == null) return ""
        val district = address.subAdminArea ?: address.locality ?: ""
        val neighborhood = address.subLocality ?: ""
        val city = address.adminArea ?: ""
        val short = when {
            district.isNotEmpty() && neighborhood.isNotEmpty() -> "$district, $neighborhood"
            district.isNotEmpty() -> district
            else -> ""
        }

        return when (format) {
            LocationDisplayFormat.SHORT -> {
                if (short.isNotEmpty()) short else (address.getAddressLine(0) ?: "")
            }
            LocationDisplayFormat.MEDIUM -> {
                if (short.isNotEmpty() && city.isNotEmpty() && !short.contains(city)) {
                    "$short, $city"
                } else if (short.isNotEmpty()) {
                    short
                } else {
                    address.getAddressLine(0) ?: ""
                }
            }
        }
    }

    fun openMapsWithLocation(context: Context, latLng: LatLng) {
        try {
            // Google Maps'i marker ile aç
            val gmmIntentUri = Uri.parse("geo:${latLng.latitude},${latLng.longitude}?q=${latLng.latitude},${latLng.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Google Maps yüklü değilse, web tarayıcıda aç
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${latLng.latitude},${latLng.longitude}"))
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            // Herhangi bir hata durumunda web tarayıcıda aç
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${latLng.latitude},${latLng.longitude}"))
            context.startActivity(webIntent)
        }
    }
} 