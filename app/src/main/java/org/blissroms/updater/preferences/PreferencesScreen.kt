/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.blissroms.updater.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import android.content.Intent
import android.net.Uri
import com.android.settingslib.spa.widget.ui.Category
import kotlinx.coroutines.launch
import org.blissroms.updater.R
import org.blissroms.updater.UpdaterApplication
import org.blissroms.updater.data.CheckInterval
import org.blissroms.updater.data.UserPreferencesRepository
import org.blissroms.updater.deviceinfo.DeviceInfoUtils
import org.blissroms.updater.util.BatteryMonitor
import java.io.File

@Composable
fun PreferencesScreen() {
    val context = LocalContext.current
    val application = remember(context) { context.applicationContext as UpdaterApplication }
    val repository = application.userPreferencesRepository
    val batteryMonitor = application.batteryMonitor
    val isABDevice = remember { DeviceInfoUtils.isABDevice }
    val showRecoveryUpdate = remember { installRecoveryScriptExists() }
    RegularScaffold(title = "Settings") {
        PreferencesContent(repository, batteryMonitor, isABDevice, showRecoveryUpdate)
    }
}

@Composable
private fun PreferencesContent(
    repository: UserPreferencesRepository,
    batteryMonitor: BatteryMonitor,
    isABDevice: Boolean,
    showRecoveryUpdate: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val abPerfMode by repository.abPerfModeFlow.collectAsStateWithLifecycle(false)
    val batteryState by batteryMonitor.batteryState.collectAsStateWithLifecycle(
        batteryMonitor.currentBatteryState
    )
    val autoDelete by repository.autoDeleteFlow.collectAsStateWithLifecycle(true)
    val streamUpdates by repository.streamUpdatesFlow.collectAsStateWithLifecycle(true)
    val checkInterval by repository.checkIntervalFlow.collectAsStateWithLifecycle(CheckInterval.default)
    val meteredNetworkWarning by repository.meteredNetworkWarningFlow.collectAsStateWithLifecycle(
        true
    )
    val periodicCheckEnabled by repository.periodicCheckEnabledFlow.collectAsStateWithLifecycle(true)
    var recoveryUpdateEnabled by remember { mutableStateOf(repository.getRecoveryUpdateEnabled()) }

    val autoUpdatesCheckSummary = stringResource(R.string.menu_auto_updates_check_summary)
    val autoDeleteUpdatesSummary = stringResource(R.string.menu_auto_delete_updates_summary)
    val meteredNetworkWarningSummary = stringResource(R.string.menu_metered_network_warning_summary)

    val selectedCheckInterval = remember(checkInterval) {
        object : IntState {
            override val intValue = checkInterval.ordinal
        }
    }
    
    val context = LocalContext.current

    Category(title = "Advanced") {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.local_update_import)
            override val summary = { "Choose file" }
            override val onClick: () -> Unit = {
                // To be implemented via a callback to activity
                (context as? PreferencesActivity)?.onLocalUpdateClick()
            }
        })
    }

    Category(title = "Preferences") {
        ListPreference(object : ListPreferenceModel {
            override val title = stringResource(R.string.menu_auto_updates_check)
            override val enabled = { true }
            override val options = listOf(
                ListPreferenceOption(
                    id = -1,
                    text = "Never",
                ),
                ListPreferenceOption(
                    id = CheckInterval.DAILY.ordinal,
                    text = stringResource(R.string.time_unit_day),
                ),
                ListPreferenceOption(
                    id = CheckInterval.WEEKLY.ordinal,
                    text = stringResource(R.string.time_unit_week),
                ),
                ListPreferenceOption(
                    id = CheckInterval.MONTHLY.ordinal,
                    text = stringResource(R.string.time_unit_month),
                ),
            )
            override val selectedId = remember(checkInterval, periodicCheckEnabled) {
                object : IntState {
                    override val intValue = if (periodicCheckEnabled) checkInterval.ordinal else -1
                }
            }
            override val onIdSelected: (Int) -> Unit = { id ->
                if (id == -1) {
                    coroutineScope.launch { repository.setPeriodicCheckEnabled(false) }
                } else {
                    val interval = CheckInterval.entries.getOrElse(id) { CheckInterval.default }
                    coroutineScope.launch { 
                        repository.setPeriodicCheckEnabled(true)
                        repository.setCheckInterval(interval)
                    }
                }
            }
        })

        SwitchPreference(object : SwitchPreferenceModel {
            override val title = stringResource(R.string.menu_auto_delete_updates)
            override val checked = { autoDelete }
            override val onCheckedChange: (Boolean) -> Unit = { value ->
                coroutineScope.launch { repository.setAutoDelete(value) }
            }
        })

        SwitchPreference(object : SwitchPreferenceModel {
            override val title = stringResource(R.string.menu_metered_network_warning)
            override val checked = { meteredNetworkWarning }
            override val onCheckedChange: (Boolean) -> Unit = { value ->
                coroutineScope.launch { repository.setMeteredNetworkWarning(value) }
            }
        })

        SwitchPreference(object : SwitchPreferenceModel {
            override val title = "Auto-Update Overnight"
            override val checked = { false } // Dummy for now
            override val onCheckedChange: (Boolean) -> Unit = { }
        })
    }

    Category(title = "Contact") {
        Preference(object : PreferenceModel {
            override val title = "Report issues"
            override val onClick: () -> Unit = {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/BlissRoms/bug_reports/issues")
                context.startActivity(intent)
            }
        })
    }
}

private fun installRecoveryScriptExists() = File("/vendor/bin/install-recovery.sh").exists()
