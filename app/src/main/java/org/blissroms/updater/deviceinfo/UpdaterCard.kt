/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.blissroms.updater.deviceinfo

import android.graphics.RuntimeShader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsShape.CornerExtraLarge1
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.blissroms.updater.R
import kotlin.math.max
import kotlin.math.roundToInt

    // Removed unused shader code

@Composable
fun UpdaterCard(
    buildVersion: String,
    androidVersion: String,
    buildDate: String,
    deviceCodename: String,
    isUpdateAvailable: Boolean = false,
    modifier: Modifier = Modifier,
    shape: Shape = CornerExtraLarge1,
) {
    val majorVersion = remember(buildVersion) { buildVersion.substringBefore(".") }

    val density = LocalDensity.current
    val displayLarge = MaterialTheme.typography.displayLarge

    val versionStyle = remember(displayLarge) {
        /*
         * Brand guide: "Roboto Light version text, spaced in 8%".
         */
        displayLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.08).em,
            lineHeight = displayLarge.fontSize,
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bliss_updater_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = majorVersion,
                    color = Color.White,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 120.sp
                )
                Text(
                    text = if (isUpdateAvailable) stringResource(R.string.new_update_available) else stringResource(R.string.blissroms),
                    color = Color.White,
                    fontSize = if (isUpdateAvailable) 24.sp else 32.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = deviceCodename,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Removed updaterHeaderPattern and InfoColumn

@UiModePreviews
@Composable
private fun UpdaterCardPreview() {
    SettingsTheme {
        UpdaterCard(
            buildVersion = "20.0",
            androidVersion = "17",
            buildDate = "Feb 20",
            deviceCodename = "Spacewar",
            modifier = Modifier.padding(SettingsDimension.itemPadding),
        )
    }
}
