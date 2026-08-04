/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.blissroms.updater.deviceinfo

import android.content.res.Configuration
import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsRadius
import com.android.settingslib.spa.framework.theme.SettingsShape.CornerExtraLarge1
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.blissroms.updater.deviceinfo.actions.DeviceInfoActionButtons
import org.blissroms.updater.deviceinfo.actions.DeviceInfoTvAction
import org.blissroms.updater.util.StringUtil
import java.util.Date

@Composable
fun DeviceInfoBanner(
    updateZipName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(context, configuration.locales) { StringUtil.getCurrentLocale(context) }
    val buildVersion = remember { DeviceInfoUtils.buildVersion }
    val androidVersion = remember { DeviceInfoUtils.androidVersion }
    val buildDate = remember(locale) {
        DateFormat.getInstanceForSkeleton("MMMd", locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(DeviceInfoUtils.buildDateTimestamp * 1000L))
    }

    val deviceCodename = remember { DeviceInfoUtils.device }

    DeviceInfoBanner(
        buildVersion = buildVersion,
        androidVersion = androidVersion,
        buildDate = buildDate,

        deviceCodename = deviceCodename,
        updateZipName = updateZipName,
        modifier = modifier,
    )
}

@Composable
fun DeviceInfoBanner(
    buildVersion: String,
    androidVersion: String,
    buildDate: String,
    deviceCodename: String,
    updateZipName: String?,
    modifier: Modifier = Modifier,
) {
    val uiMode = LocalConfiguration.current.uiMode
    val isTv = (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SettingsDimension.itemPadding)
    ) {
        UpdaterCard(
            buildVersion = buildVersion,
            androidVersion = androidVersion,
            buildDate = buildDate,
            deviceCodename = deviceCodename,
            isUpdateAvailable = updateZipName != null,
            modifier = Modifier.fillMaxWidth(),
            shape = CornerExtraLarge1,
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_YES
)
@UiModePreviews
@Composable
private fun DeviceInfoBannerPreview() {
    SettingsTheme {
        DeviceInfoBanner(
            buildVersion = "20",
            androidVersion = "17",
            buildDate = "Feb 20",
            deviceCodename = "obiwan",
            updateZipName = "Bliss-v20.0-obiwan-OFFICIAL-gapps-20260804"
        )
    }
}
