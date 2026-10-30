package com.hydri.app

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon

/** Stable native shortcut IDs. Explicit flags reuse Hydri's existing task and activity. */
object LauncherShortcuts {
    const val LOG_WATER = "com.hydri.app.LOG_WATER"
    const val CREATE_BOTTLE = "com.hydri.app.CREATE_BOTTLE"
    const val TRACKER = "com.hydri.app.TRACKER"
    private val actions = listOf(LOG_WATER, CREATE_BOTTLE, TRACKER)

    fun action(value: String?): String? = value?.takeIf { it in actions }

    fun publish(context: Context) {
        val manager = context.getSystemService(ShortcutManager::class.java)
        val specs =
            listOf(
                Triple("log-water", R.string.shortcut_log, R.drawable.shortcut_water),
                Triple("create-bottle", R.string.shortcut_bottle, R.drawable.shortcut_bottle),
                Triple("tracker", R.string.shortcut_tracker, R.drawable.shortcut_tracker),
            )
        if (
            manager.dynamicShortcuts.map { it.id to it.intent?.action }.toSet() ==
                specs.mapIndexed { i, spec -> spec.first to actions[i] }.toSet()
        )
            return
        if (manager.isRateLimitingActive) return
        manager.dynamicShortcuts =
            specs.mapIndexed { index, spec ->
                ShortcutInfo.Builder(context, spec.first)
                    .setShortLabel(context.getString(spec.second))
                    .setIcon(Icon.createWithResource(context, spec.third))
                    .setRank(index)
                    .setIntent(
                        Intent(context, MainActivity::class.java)
                            .setAction(actions[index])
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                    )
                    .build()
            }
    }
}
