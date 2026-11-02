// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app.ui

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

private fun container(name: String, drawing: PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = drawing,
            )
        }
        .build()

/** Appended after the seven legacy Material icons; shared by every bottle representation. */
val additionalBottleIcons =
    listOf(
        container("TravelMug") {
            moveTo(6f, 6f)
            lineTo(6f, 18f)
            quadTo(6f, 21f, 9f, 21f)
            lineTo(14f, 21f)
            quadTo(17f, 21f, 17f, 18f)
            lineTo(17f, 6f)
            moveTo(5f, 6f)
            lineTo(18f, 6f)
            lineTo(17f, 3f)
            lineTo(7f, 3f)
            close()
            moveTo(17f, 9f)
            lineTo(20f, 9f)
            lineTo(20f, 15f)
            lineTo(17f, 15f)
            moveTo(9f, 12f)
            lineTo(14f, 12f)
        },
        container("DrinkCan") {
            moveTo(7f, 4f)
            quadTo(12f, 2f, 17f, 4f)
            lineTo(18f, 7f)
            lineTo(18f, 18f)
            quadTo(18f, 21f, 15f, 21f)
            lineTo(9f, 21f)
            quadTo(6f, 21f, 6f, 18f)
            lineTo(6f, 7f)
            close()
            moveTo(7f, 7f)
            lineTo(17f, 7f)
            moveTo(10f, 4f)
            lineTo(13f, 4f)
            moveTo(9f, 17f)
            lineTo(15f, 17f)
        },
        container("MasonJar") {
            moveTo(7f, 3f)
            lineTo(16f, 3f)
            lineTo(16f, 7f)
            quadTo(18f, 8f, 18f, 10f)
            lineTo(18f, 18f)
            quadTo(18f, 21f, 15f, 21f)
            lineTo(8f, 21f)
            quadTo(5f, 21f, 5f, 18f)
            lineTo(5f, 10f)
            quadTo(5f, 8f, 7f, 7f)
            close()
            moveTo(7f, 6f)
            lineTo(16f, 6f)
            moveTo(18f, 10f)
            curveTo(23f, 9f, 23f, 18f, 18f, 17f)
            moveTo(9f, 11f)
            lineTo(14f, 11f)
            moveTo(9f, 16f)
            lineTo(14f, 16f)
        },
        container("TallGlass") {
            moveTo(6f, 3f)
            lineTo(18f, 3f)
            lineTo(16f, 21f)
            lineTo(8f, 21f)
            close()
            moveTo(7f, 10f)
            lineTo(17f, 10f)
            moveTo(10f, 14f)
            lineTo(10.5f, 18f)
        },
        container("InsulatedFlask") {
            moveTo(9f, 3f)
            lineTo(15f, 3f)
            lineTo(15f, 6f)
            quadTo(18f, 7f, 18f, 10f)
            lineTo(18f, 18f)
            quadTo(18f, 21f, 15f, 21f)
            lineTo(9f, 21f)
            quadTo(6f, 21f, 6f, 18f)
            lineTo(6f, 10f)
            quadTo(6f, 7f, 9f, 6f)
            close()
            moveTo(9f, 6f)
            lineTo(15f, 6f)
            moveTo(6f, 16f)
            lineTo(18f, 16f)
        },
        container("StrawTumbler") {
            moveTo(5f, 7f)
            lineTo(19f, 7f)
            lineTo(17f, 20f)
            quadTo(12f, 22f, 7f, 20f)
            close()
            moveTo(4f, 7f)
            lineTo(20f, 7f)
            moveTo(13f, 7f)
            lineTo(15f, 2f)
            lineTo(19f, 2f)
            moveTo(9f, 11f)
            lineTo(9.5f, 17f)
        },
        container("SqueezeBottle") {
            moveTo(10f, 2f)
            lineTo(14f, 2f)
            lineTo(14f, 6f)
            lineTo(17f, 8f)
            quadTo(19f, 10f, 17f, 13f)
            quadTo(16f, 15f, 18f, 18f)
            quadTo(19f, 21f, 15f, 21f)
            lineTo(9f, 21f)
            quadTo(5f, 21f, 6f, 18f)
            quadTo(8f, 15f, 7f, 13f)
            quadTo(5f, 10f, 7f, 8f)
            lineTo(10f, 6f)
            close()
            moveTo(10f, 5f)
            lineTo(14f, 5f)
            moveTo(10f, 11f)
            lineTo(14f, 11f)
        },
    )
