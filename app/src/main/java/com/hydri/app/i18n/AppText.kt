// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.hydri.app.HydriApplication
import com.hydri.app.LauncherShortcuts
import com.hydri.app.R
import com.hydri.app.background.HydriWidgets
import com.hydri.app.data.Bottle
import com.hydri.app.data.WaterEntry
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

/** Resource context shared by Compose, background workers and Glance; never holds an Activity. */
@android.annotation.SuppressLint(
    "StaticFieldLeak"
) // Cached contexts derive only from applicationContext.
object AppText {
    @Volatile private var resourcesContext: Context? = null
    val context: Context?
        get() = resourcesContext

    val locale: Locale
        get() = resourcesContext?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()

    fun supportedLocale(locales: List<Locale>): Locale =
        locales.firstOrNull { locale ->
            locale.language in setOf("en", "es", "fr", "ko", "ja", "id", "in") ||
                (locale.language == "zh" &&
                    (locale.script == "Hans" ||
                        (locale.script.isEmpty() && locale.country !in setOf("TW", "HK", "MO"))))
        } ?: Locale.ENGLISH

    fun refresh(context: Context) {
        val source =
            if (context is androidx.appcompat.app.AppCompatActivity) context
            else ContextCompat.getContextForLanguage(context)
        val locales = source.resources.configuration.locales
        install(context, supportedLocale((0 until locales.size()).map { locales[it] }))
        AppLocales.sync(context)
    }

    fun install(context: Context, locale: Locale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(
            if (locale.language == "zh")
                Locale.Builder().setLocale(locale).setScript("Hans").build()
            else locale
        )
        resourcesContext = context.applicationContext.createConfigurationContext(config)
    }

    fun number(value: Number): String = NumberFormat.getNumberInstance(locale).format(value)

    fun argument(value: Any): Any = if (value is Number) number(value) else value
}

fun tr(@StringRes id: Int, vararg args: Any): String {
    val context = checkNotNull(AppText.context) { "Application resources are not initialized" }
    return if (args.isEmpty()) context.getString(id)
    else context.getString(id, *args.map(AppText::argument).toTypedArray())
}

fun plural(id: Int, quantity: Int, vararg args: Any): String {
    val values = if (args.isEmpty()) arrayOf<Any>(quantity) else args
    return checkNotNull(AppText.context)
        .resources
        .getQuantityString(id, quantity, *values.map(AppText::argument).toTypedArray())
}

fun unitLabel(unit: String): String =
    if (AppText.context == null) unit
    else
        tr(
            when (unit) {
                "L" -> R.string.unit_l
                "oz" -> R.string.unit_oz
                else -> R.string.unit_ml
            }
        )

fun percentText(percent: Int): String =
    NumberFormat.getPercentInstance(AppText.locale).format(percent / 100.0)

fun routeLabel(route: String): String =
    tr(
        when (route) {
            "Home" -> R.string.home
            "Bottles" -> R.string.bottles
            "Tracker" -> R.string.tracker
            "Notifications" -> R.string.notifications
            "Hydration Guide" -> R.string.hydration_guide
            "Data & Backup" -> R.string.data_backup
            "About" -> R.string.about
            else -> R.string.more
        }
    )

fun displaySetting(value: String): String =
    when (value) {
        "System" -> tr(R.string.system_option)
        "Light" -> tr(R.string.light)
        "Dark" -> tr(R.string.dark)
        "12-hour" -> tr(R.string.twelve_hour)
        "24-hour" -> tr(R.string.twenty_four_hour)
        "Day" -> tr(R.string.day)
        "Week" -> tr(R.string.week)
        "Month" -> tr(R.string.month)
        else -> value
    }

// Legacy built-in identifiers are serialized data, not locale-dependent display strings.
private fun builtInName(id: String?, name: String): Int? =
    when {
        id == "glass" && name == "Everyday glass" -> R.string.everyday_glass
        id == "bottle" && name == "Water bottle" -> R.string.water_bottle
        id == "large" && name == "Large bottle" -> R.string.large_bottle
        else -> null
    }

fun bottleDisplayName(bottle: Bottle): String =
    builtInName(bottle.id, bottle.name)?.let { tr(it) } ?: bottle.name

fun entryDisplayName(entry: WaterEntry): String =
    if (entry.bottleId == null && entry.name == "Custom water") tr(R.string.custom_water)
    else builtInName(entry.bottleId, entry.name)?.let { tr(it) } ?: entry.name

object AppLocales {
    val selection = kotlinx.coroutines.flow.MutableStateFlow("")

    fun sync(context: Context) {
        selection.value = current(context)
    }

    val languages =
        linkedMapOf(
            "en" to R.string.language_en,
            "es" to R.string.language_es,
            "fr" to R.string.language_fr,
            "zh-Hans" to R.string.language_zh,
            "ko" to R.string.language_ko,
            "ja" to R.string.language_ja,
            "id" to R.string.language_id,
        )

    fun tags(context: Context = checkNotNull(AppText.context)): String =
        if (android.os.Build.VERSION.SDK_INT >= 33)
            LocaleManagerCompat.getApplicationLocales(context).toLanguageTags()
        else AppCompatDelegate.getApplicationLocales().toLanguageTags()

    fun current(context: Context): String {
        val tag = tags(context)
        if (tag.isEmpty()) return ""
        val locale = Locale.forLanguageTag(tag.substringBefore(','))
        return if (locale.language == "zh") "zh-Hans"
        else if (locale.language == "in") "id" else locale.language
    }

    fun label(code: String): String =
        if (code.isEmpty()) tr(R.string.follow_system_254)
        else languages[code]?.let { tr(it) } ?: tr(R.string.follow_system_254)

    fun supports(tags: String): Boolean {
        if (tags.isEmpty()) return true
        if (tags.length > 128 || tags.split(',').size > 7) return false
        return try {
            tags.split(',').all { tag ->
                val locale = Locale.Builder().setLanguageTag(tag).build()
                val code =
                    if (
                        locale.language == "zh" &&
                            (locale.script == "Hans" ||
                                (locale.script.isEmpty() &&
                                    locale.country !in setOf("TW", "HK", "MO")))
                    )
                        "zh-Hans"
                    else if (locale.language == "in") "id" else locale.language
                code in languages
            }
        } catch (_: java.util.IllformedLocaleException) {
            false
        }
    }

    fun select(code: String) {
        require(supports(code))
        val app = checkNotNull(AppText.context).applicationContext as HydriApplication
        Handler(Looper.getMainLooper()).post {
            val first = Locale.forLanguageTag(code.substringBefore(','))
            selection.value =
                if (code.isEmpty()) ""
                else if (first.language == "zh") "zh-Hans"
                else if (first.language == "in") "id" else first.language
            if (current(app) == code) return@post
            val chosen =
                if (code.isEmpty()) {
                    val locales = Resources.getSystem().configuration.locales
                    AppText.supportedLocale((0 until locales.size()).map { locales[it] })
                } else Locale.forLanguageTag(code.substringBefore(','))
            AppText.install(app, chosen)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                app.getSystemService(android.app.LocaleManager::class.java).applicationLocales =
                    android.os.LocaleList.forLanguageTags(code)
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            }
            com.hydri.app.background.Reminders.refreshLabels(app)
            LauncherShortcuts.publish(app)
            app.scope.launch { HydriWidgets.update(app) }
        }
    }
}

class UserMessage(message: String) : IllegalArgumentException(message)
