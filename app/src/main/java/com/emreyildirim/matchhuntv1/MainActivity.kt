package com.emreyildirim.matchhuntv1

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.emreyildirim.matchhuntv1.navigation.AppNavigation
import com.emreyildirim.matchhuntv1.ui.theme.MatchHuntV1Theme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
            if (isGranted){
                // izin verildi
                Log.d("MainActivity", "Notification permission granted")
            } else {
                // izin reddedildi
                Log.d("MainActivity", "Notification permission denied")
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Android 13 (api33) ve sonrası için bildirim izni istemek zorunlu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MatchHuntV1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}