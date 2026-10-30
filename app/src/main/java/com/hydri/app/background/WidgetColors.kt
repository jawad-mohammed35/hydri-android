// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.background

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.unit.ColorProvider
import com.hydri.app.R
import com.hydri.app.data.Settings
import com.hydri.app.ui.hydriColors

data class WidgetColors(
    val surface: ColorProvider,
    val primary: ColorProvider,
    val onSurface: ColorProvider,
    val onSurfaceVariant: ColorProvider,
    val surfaceVariant: ColorProvider,
    val primaryContainer: ColorProvider,
    val onPrimaryContainer: ColorProvider,
)

// Glance 1.1 restricts its resource factory although RemoteViews supports color resources.
// Keep this bridge local: resource IDs let the launcher re-resolve wallpaper and night colors
// without keeping Hydri alive. Fixed Color providers would freeze the wallpaper palette.
@SuppressLint("RestrictedApi")
fun widgetColors(context: Context, s: Settings): WidgetColors {
    val light = hydriColors(context, s.copy(theme = "Light"), false)
    val dark = hydriColors(context, s.copy(theme = "Dark"), true)
    fun role(day: Color, night: Color, dayId: Int, nightId: Int, followId: Int): ColorProvider {
        if (s.dynamicColors && Build.VERSION.SDK_INT >= 31)
            return ColorProvider(
                when (s.theme) {
                    "Light" -> dayId
                    "Dark" -> nightId
                    else -> followId
                }
            )
        return when (s.theme) {
            "Light" -> ColorProvider(day)
            "Dark" -> ColorProvider(night)
            else -> DayNightColor(day, night)
        }
    }
    val surface =
        if (s.oled && s.theme == "Dark") ColorProvider(Color.Black)
        else if (s.oled && s.theme == "System" && s.dynamicColors && Build.VERSION.SDK_INT >= 31)
            ColorProvider(R.color.widget_surface_oled_follow)
        else
            role(
                light.surface,
                if (s.oled) Color.Black else dark.surface,
                R.color.widget_surface_light,
                R.color.widget_surface_dark,
                R.color.widget_surface_follow,
            )
    return WidgetColors(
        surface,
        role(
            light.primary,
            dark.primary,
            R.color.widget_primary_light,
            R.color.widget_primary_dark,
            R.color.widget_primary_follow,
        ),
        role(
            light.onSurface,
            dark.onSurface,
            R.color.widget_on_surface_light,
            R.color.widget_on_surface_dark,
            R.color.widget_on_surface_follow,
        ),
        role(
            light.onSurfaceVariant,
            dark.onSurfaceVariant,
            R.color.widget_on_surface_variant_light,
            R.color.widget_on_surface_variant_dark,
            R.color.widget_on_surface_variant_follow,
        ),
        role(
            light.surfaceVariant,
            dark.surfaceVariant,
            R.color.widget_surface_variant_light,
            R.color.widget_surface_variant_dark,
            R.color.widget_surface_variant_follow,
        ),
        role(
            light.primaryContainer,
            dark.primaryContainer,
            R.color.widget_primary_container_light,
            R.color.widget_primary_container_dark,
            R.color.widget_primary_container_follow,
        ),
        role(
            light.onPrimaryContainer,
            dark.onPrimaryContainer,
            R.color.widget_on_primary_container_light,
            R.color.widget_on_primary_container_dark,
            R.color.widget_on_primary_container_follow,
        ),
    )
}
