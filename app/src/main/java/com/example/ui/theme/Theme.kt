package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FamilyDarkGreenSecondary,
    secondary = FamilyGreenAccent,
    tertiary = Color(0xFF34B7F1),
    background = FamilyDarkBackground,
    surface = FamilyDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = FamilyDarkOnSurface,
    onSurface = FamilyDarkOnSurface,
    surfaceVariant = Color(0xFF2A3942),
    onSurfaceVariant = Color(0xFF8696A0)
)

private val LightColorScheme = lightColorScheme(
    primary = FamilyGreenSecondary,
    secondary = FamilyGreenPrimary,
    tertiary = Color(0xFF34B7F1),
    background = Color(0xFFF0F2F5),
    surface = FamilyLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111B21),
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF667781)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false to preserve the customized signature green WhatsApp look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
