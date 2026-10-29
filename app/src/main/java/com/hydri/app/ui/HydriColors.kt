package com.hydri.app.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.hydri.app.data.Settings

/** Shared by the app and Glance: both resolve the user's appearance, including OLED. */
fun usesDarkTheme(settings: Settings, systemDark: Boolean): Boolean =
    settings.theme == "Dark" || (settings.theme == "System" && systemDark)

fun hydriColors(
    context: Context,
    s: Settings,
    systemDark: Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES,
): ColorScheme {
    val dark = usesDarkTheme(s, systemDark)
    if (s.dynamicColors && Build.VERSION.SDK_INT >= 31) {
        val dynamic =
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return if (dark && s.oled) dynamic.copy(background = Color.Black, surface = Color.Black)
        else dynamic
    }
    val accent = accents[if (s.dynamicColors) 0 else s.accent]
    return if (dark)
        darkColorScheme(
            primary = lerp(accent, Color.White, .25f),
            onPrimary = Color(0xFF08121C),
            primaryContainer = lerp(accent, Color.Black, .65f),
            onPrimaryContainer = lerp(accent, Color.White, .75f),
            secondary = lerp(accent, Color.White, .4f),
            onSecondary = Color(0xFF08121C),
            secondaryContainer = lerp(accent, Color.Black, .65f),
            onSecondaryContainer = lerp(accent, Color.White, .75f),
            tertiary = lerp(accent, Color.White, .45f),
            onTertiary = Color(0xFF08121C),
            background = if (s.oled) Color.Black else Color(0xFF0D1520),
            surface = if (s.oled) Color(0xFF080A0E) else Color(0xFF182330),
            surfaceVariant = Color(0xFF233246),
            surfaceContainerHighest = Color(0xFF233246),
            onSurface = Color(0xFFE9F1FA),
            onBackground = Color(0xFFE9F1FA),
        )
    else
        lightColorScheme(
            primary = lerp(accent, Color.Black, .18f),
            onPrimary = Color.White,
            primaryContainer = lerp(accent, Color.White, .86f),
            onPrimaryContainer = lerp(accent, Color.Black, .55f),
            secondary = lerp(accent, Color.Black, .25f),
            onSecondary = Color.White,
            secondaryContainer = lerp(accent, Color.White, .86f),
            onSecondaryContainer = lerp(accent, Color.Black, .55f),
            tertiary = lerp(accent, Color.Black, .3f),
            onTertiary = Color.White,
            background = Color(0xFFF2F7FC),
            surface = Color(0xFFFCFEFF),
            surfaceVariant = Color(0xFFE4EEF8),
            surfaceContainerHighest = Color(0xFFE4EEF8),
            onSurface = Color(0xFF182D43),
            onBackground = Color(0xFF182D43),
        )
}
