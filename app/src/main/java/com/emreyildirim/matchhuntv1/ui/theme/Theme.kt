package com.emreyildirim.matchhuntv1.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandVolt,          // Karanlıkta ana aksiyon rengi Neon Sarı
    onPrimary = Obsidian,         // Sarı üzerindeki metinler Siyah
    secondary = BrandVolt,
    onSecondary = Obsidian,
    background = DeepObsidian,    // Derin siyah arka plan
    surface = SurfaceDark,        // Kartlar için hafif aydınlatılmış yüzey
    onSurface = PureWhite,        // Kart üzerindeki metinler Beyaz
    onBackground = PureWhite,     // Arka plan metinleri Beyaz
    error = ErrorRed,
    outline = MutedTextDark.copy(alpha = 0.4f)
)

// Şu an sadece Light Theme aktif olduğu için sadece bu şemayı özelleştiriyoruz.
private val LightColorScheme = lightColorScheme(
    primary = Obsidian,       // Butonlar ve ana öğeler siyah
    onPrimary = BrandVolt,    // Siyah üzerindeki yazılar neon sarı
    primaryContainer = BrandVolt,
    onPrimaryContainer = Obsidian,
    secondary = Obsidian,
    background = SoftGray,    // Arka plan soft gri
    surface = PureWhite,      // Kartlar beyaz
    onSurface = Obsidian,     // Kart üzerindeki yazılar siyah
    surfaceVariant = IncomingGray,
    /* Varsayılan moru ezmek için diğer renkleri de tanımlıyoruz */
    secondaryContainer = SoftGray,
    onSecondaryContainer = Obsidian,
    tertiary = BrandVolt,
    onTertiary = Obsidian,
    error = ErrorRed,
)

@Composable
fun MatchHuntV1Theme(
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}