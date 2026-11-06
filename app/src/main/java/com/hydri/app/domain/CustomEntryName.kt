// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app.domain

/** Keep the legacy default sentinel so default labels follow the app language. */
object CustomEntryName {
    const val DEFAULT = "Custom water"

    fun normalize(name: String) = name.trim().ifBlank { DEFAULT }

    fun stored(name: String, localizedDefault: String): String =
        normalize(name).let { if (it == localizedDefault) DEFAULT else it }
}
