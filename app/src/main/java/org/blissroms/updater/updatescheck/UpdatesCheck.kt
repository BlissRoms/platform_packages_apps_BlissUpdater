/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.blissroms.updater.updatescheck

import android.content.Context
import android.os.SystemClock
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.theme.SettingsDimension
import kotlinx.coroutines.delay
import org.blissroms.updater.R
import org.blissroms.updater.deviceinfo.DeviceInfoUtils
import org.blissroms.updater.updates.action.UpdateAction
import org.blissroms.updater.updates.state.ProgressState
import org.blissroms.updater.updates.state.UpdateItemState
import androidx.compose.ui.draw.clip
import java.util.Date

private const val MIN_CHECKING_DURATION_MILLIS = 2_000L

// Matches SettingsLib's expressive zero-state background size.
private val AnimationSize = 160.dp

sealed interface UpdatesCheckState {
    data object Idle : UpdatesCheckState
    data object Checking : UpdatesCheckState
    data object NoInternet : UpdatesCheckState
    data object Error : UpdatesCheckState
}

data class UpdatesCheckModel(
    val state: UpdatesCheckState,
    val lastCheckedTimestamp: Long,
    val canCheckForUpdates: Boolean,
)

class UpdatesCheckUiState internal constructor(
    internal val displayedState: UpdatesCheckState,
) {
    val isStatusVisible = when (displayedState) {
        UpdatesCheckState.Idle -> false
        UpdatesCheckState.Checking,
        UpdatesCheckState.NoInternet,
        UpdatesCheckState.Error -> true
    }
}

@Composable
fun UpdatesCheck(
    model: UpdatesCheckModel,
    uiState: UpdatesCheckUiState,
    updateZipName: String?,
    primaryUpdate: UpdateItemState? = null,
    onPrimaryAction: ((UpdateAction, String) -> Unit)? = null,
    isWhatsNewVisible: Boolean = false,
    isInstallFailed: Boolean = false,
    isDownloadFailed: Boolean = false,
    isNoUpdatesFound: Boolean = false,
    onCheckClick: () -> Unit,
    onWhatsNewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lastCheckedText = remember(model.lastCheckedTimestamp) {
        formatLastCheckedText(context, model.lastCheckedTimestamp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(SettingsDimension.itemPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SettingsDimension.itemPaddingVertical),
    ) {
        if (!isWhatsNewVisible) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.software_version),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = updateZipName?.removeSuffix(".zip") ?: DeviceInfoUtils.blissBuild,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastCheckedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        val isChecking = uiState.displayedState == UpdatesCheckState.Checking
        val isCheckFailed = uiState.displayedState == UpdatesCheckState.Error || uiState.displayedState == UpdatesCheckState.NoInternet

        CheckForUpdatesButton(
            showCheckButton = true,
            isChecking = isChecking,
            primaryUpdate = primaryUpdate,
            onPrimaryAction = onPrimaryAction,
            isWhatsNewVisible = isWhatsNewVisible,
            isInstallFailed = isInstallFailed,
            isDownloadFailed = isDownloadFailed,
            isNoUpdatesFound = isNoUpdatesFound,
            isCheckFailed = isCheckFailed,
            onClick = {
                if (uiState.displayedState == UpdatesCheckState.NoInternet) {
                    android.widget.Toast.makeText(context, R.string.check_your_internet_connection, android.widget.Toast.LENGTH_SHORT).show()
                }
                onCheckClick()
            },
            onWhatsNewClick = onWhatsNewClick
        )
    }
}

@Composable
private fun CheckForUpdatesButton(
    showCheckButton: Boolean,
    isChecking: Boolean,
    primaryUpdate: UpdateItemState? = null,
    onPrimaryAction: ((UpdateAction, String) -> Unit)? = null,
    isWhatsNewVisible: Boolean,
    isInstallFailed: Boolean = false,
    isDownloadFailed: Boolean = false,
    isNoUpdatesFound: Boolean = false,
    isCheckFailed: Boolean = false,
    onClick: () -> Unit,
    onWhatsNewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showCheckButton) {
            val isFailed = isInstallFailed || isDownloadFailed || isCheckFailed
            val primaryAction = primaryUpdate?.actions?.primary

            if (primaryUpdate != null && primaryAction != null) {
                val progress = primaryUpdate.progress
                val percent = if (progress is ProgressState.Determinate) progress.percent / 100f else 0f
                val buttonText = when (progress) {
                    is ProgressState.Determinate -> "Downloading ${"%.1f".format(progress.percent)}%"
                    else -> primaryAction.type.title(context)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(enabled = primaryAction.enabled && !isChecking) {
                            onPrimaryAction?.invoke(primaryAction, primaryUpdate.downloadId)
                        }
                ) {
                    if (progress is ProgressState.Determinate) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(percent)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    } else if (progress is ProgressState.Indeterminate || isChecking) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                    Text(
                        text = if (isChecking) stringResource(R.string.check_for_updates) else buttonText,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onClick,
                    enabled = !isChecking,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isFailed) androidx.compose.ui.graphics.Color.Red else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (isFailed) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        val textRes = when {
                            isInstallFailed -> R.string.failed_to_install
                            isDownloadFailed -> R.string.failed_to_download
                            isNoUpdatesFound -> R.string.no_updates_found
                            isCheckFailed -> R.string.updates_check_failed
                            else -> R.string.check_for_updates
                        }
                        Text(text = stringResource(textRes))
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        IconButton(
            onClick = onWhatsNewClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (isWhatsNewVisible) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = if (isWhatsNewVisible) stringResource(R.string.close) else stringResource(R.string.whats_new),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Keeps the checking state visible long enough for the progress animation to be readable.
 */
@Composable
private fun rememberStateWithMinimumCheckingDuration(
    state: UpdatesCheckState,
): UpdatesCheckState {
    var displayedState by remember { mutableStateOf(state) }
    var checkingStartedAtMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state) {
        if (state == UpdatesCheckState.Checking) {
            checkingStartedAtMillis = SystemClock.elapsedRealtime()
            displayedState = state
            return@LaunchedEffect
        }

        if (displayedState == UpdatesCheckState.Checking) {
            val elapsed = SystemClock.elapsedRealtime() - checkingStartedAtMillis
            val remaining = MIN_CHECKING_DURATION_MILLIS - elapsed
            if (remaining > 0L) delay(remaining)
        }

        displayedState = state
    }

    return displayedState
}

@Composable
internal fun rememberUpdatesCheckUiState(
    state: UpdatesCheckState,
): UpdatesCheckUiState {
    val displayedState = rememberStateWithMinimumCheckingDuration(state)
    return remember(displayedState) { UpdatesCheckUiState(displayedState) }
}

/**
 * Formats the last checked time.
 *
 * Today's checks show only the time. Older checks show both date and time.
 */
private fun formatLastCheckedText(
    context: Context,
    timestampMillis: Long,
): String {
    val time = DateFormat.getTimeFormat(context).format(Date(timestampMillis))

    if (DateUtils.isToday(timestampMillis)) {
        return context.getString(R.string.header_last_updates_check_time, time)
    }

    val date = DateUtils.formatDateTime(
        context,
        timestampMillis,
        DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_NO_YEAR,
    )

    return context.getString(R.string.header_last_updates_check, date, time)
}
