package com.agon.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmberRedDark,
    onPrimary = NightBg,
    secondary = EmberOrangeDark,
    onSecondary = NightBg,
    tertiary = GoldTrophy,
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMuted,
    primaryContainer = EmberDeep,
    onPrimaryContainer = NightText,
    error = EmberRedDark,
)

private val LightColorScheme = lightColorScheme(
    primary = EmberRed,
    onPrimary = CreamSurface,
    secondary = EmberOrange,
    onSecondary = InkText,
    tertiary = EmberDeep,
    background = CreamBg,
    onBackground = InkText,
    surface = CreamSurface,
    onSurface = InkText,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkMuted,
    primaryContainer = EmberRed,
    onPrimaryContainer = CreamSurface,
    error = EmberRed,
)

@Composable
fun AgonAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        content = content,
    )
}
