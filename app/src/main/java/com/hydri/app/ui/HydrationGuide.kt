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
import com.hydri.app.domain.WaterReferences

@Composable
fun HydrationGuide(unit: String) {
    var reference by rememberSaveable { mutableStateOf("US") }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Daily total water", style = MaterialTheme.typography.titleLarge)
        Text(
            "These adequate intake references include food moisture, drinking water and all other beverages. They are not drinking-water-only goals or personalized medical advice.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Choice("US · NASEM", reference == "US") { reference = "US" }
            Choice("Nordic · 2023", reference == "Nordic") { reference = "Nordic" }
        }
        Text(
            if (reference == "US")
                "US recommendations: Institute of Medicine / National Academies, 2005."
            else
                "Nordic Nutrition Recommendations, 2023. The water values adopt EFSA's 2010 scientific recommendations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth()) {
            Text("Age", Modifier.weight(1.1f))
            Text("Male", Modifier.weight(1f))
            Text("Female", Modifier.weight(1f))
        }
        (if (reference == "US") WaterReferences.us else WaterReferences.nordic).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.age, Modifier.weight(1.1f), style = MaterialTheme.typography.bodyMedium)
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
            "Reference authorities use different methods and populations. Values describe generally healthy people; needs vary with climate, activity, pregnancy, breastfeeding and health. Follow your clinician's advice for fluid restrictions. Hydri never chooses goals from age or sex.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Sources", style = MaterialTheme.typography.titleMedium)
        SourceLink(
            "National Academies / IOM · 2005",
            "https://www.nationalacademies.org/read/10925/chapter/6",
        )
        SourceLink(
            "Nordic Council of Ministers · 2023",
            "https://pub.norden.org/nord2023-003/fluid-and-water-balance.html",
        )
        Text(
            "CDC's water guidance, updated March 5, 2026, describes changing needs and contributions from food and beverages. That webpage date is not a new set of age/sex reference values.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SourceLink(
            "CDC · Water and Healthier Drinks · 2026",
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
