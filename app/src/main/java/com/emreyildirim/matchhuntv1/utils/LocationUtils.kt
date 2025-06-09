package com.emreyildirim.matchhuntv1.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import org.json.JSONObject

object LocationUtils {
    suspend fun getAddressFromLatLng(latLng: LatLng, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latLng.latitude},${latLng.longitude}&key=$apiKey&language=tr"
                val response = URL(url).readText()
                val jsonResponse = JSONObject(response)
                
                if (jsonResponse.getString("status") == "OK") {
                    val results = jsonResponse.getJSONArray("results")
                    if (results.length() > 0) {
                        val result = results.getJSONObject(0)
                        val formattedAddress = result.getString("formatted_address")
                        val addressComponents = result.getJSONArray("address_components")
                        var district = ""
                        var neighborhood = ""
                        
                        for (i in 0 until addressComponents.length()) {
                            val component = addressComponents.getJSONObject(i)
                            val types = component.getJSONArray("types")
                            
                            for (j in 0 until types.length()) {
                                when (types.getString(j)) {
                                    "sublocality_level_1" -> district = component.getString("long_name")
                                    "sublocality_level_2" -> neighborhood = component.getString("long_name")
                                }
                            }
                        }
                        
                        if (neighborhood.isNotEmpty()) {
                            "$district, $neighborhood"
                        } else if (district.isNotEmpty()) {
                            district
                        } else {
                            formattedAddress
                        }
                    } else {
                        "Konum bilgisi alınamadı"
                    }
                } else {
                    "Konum bilgisi alınamadı!"
                }
            } catch (e: IOException) {
                "Konum bilgisi alınamadı"
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