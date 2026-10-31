// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hydri.app.R
import com.hydri.app.domain.WaterReferences
import com.hydri.app.i18n.*

@Composable
fun HydrationGuide(unit: String) {
    var reference by rememberSaveable { mutableStateOf("US") }
    GlassCard(Modifier.fillMaxWidth()) {
        Text(tr(R.string.daily_total_water), style = MaterialTheme.typography.titleLarge)
        Text(
            tr(R.string.these_adequate_intake_references_include_food_moisture_dri),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Choice(tr(R.string.us_nasem), reference == "US") { reference = "US" }
            Choice(tr(R.string.nordic_2023), reference == "Nordic") { reference = "Nordic" }
        }
        Text(
            if (reference == "US")
                tr(R.string.us_recommendations_institute_of_medicine_national_academie)
            else tr(R.string.nordic_nutrition_recommendations_2023_the_water_values_ado),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth()) {
            Text(tr(R.string.age), Modifier.weight(1.1f))
            Text(tr(R.string.male), Modifier.weight(1f))
            Text(tr(R.string.female), Modifier.weight(1f))
        }
        (if (reference == "US") WaterReferences.us else WaterReferences.nordic).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    tr(row.age),
                    Modifier.weight(1.1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    WaterReferences.amount(row.male, row.maleUpper, unit),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    WaterReferences.amount(row.female, row.femaleUpper, unit),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            tr(R.string.reference_authorities_use_different_methods_and_population),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text(tr(R.string.sources), style = MaterialTheme.typography.titleMedium)
        SourceLink(
            tr(R.string.national_academies_iom_2005),
            "https://www.nationalacademies.org/read/10925/chapter/6",
        )
        SourceLink(
            tr(R.string.nordic_council_of_ministers_2023),
            "https://pub.norden.org/nord2023-003/fluid-and-water-balance.html",
        )
        Text(
            tr(R.string.cdc_s_water_guidance_updated_march_5_2026_describes_changi),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SourceLink(
            tr(R.string.cdc_water_and_healthier_drinks_2026),
            "https://www.cdc.gov/healthy-weight-growth/water-healthy-drinks/",
        )
    }
}

@Composable
private fun SourceLink(label: String, url: String) {
    val context = LocalContext.current
    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) {
        Text(label)
    }
}
