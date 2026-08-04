/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.blissroms.updater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.blissroms.updater.updatescheck.UpdatesCheckState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.framework.theme.SettingsDimension
import org.blissroms.updater.controller.UpdaterController
import org.blissroms.updater.data.Update
import org.blissroms.updater.data.UpdateStatus
import org.blissroms.updater.deviceinfo.DeviceInfoBanner
import org.blissroms.updater.preferences.PreferencesActivity
import org.blissroms.updater.updates.UpdateList
import org.blissroms.updater.updates.action.AlertDialogState
import org.blissroms.updater.updatescheck.UpdatesCheck
import org.blissroms.updater.updatescheck.rememberUpdatesCheckUiState
import org.blissroms.updater.updates.action.UpdateActionDialog
import org.blissroms.updater.updates.action.UpdateActionHandler
import org.blissroms.updater.updates.state.UpdateItemStateMapper
import org.blissroms.updater.updatescheck.UpdatesCheck
import org.blissroms.updater.updatescheck.UpdatesCheckModel
import org.blissroms.updater.updatescheck.rememberUpdatesCheckUiState
import org.blissroms.updater.UpdaterApplication
import org.blissroms.updater.updates.state.UpdateItemState

abstract class UpdatesScaffoldActivity : ComponentActivity() {
    private val viewModel by viewModels<UpdatesViewModel>()
    private var activeUpdaterController: UpdaterController? by mutableStateOf(null)
    private var controllerStateVersion: Int by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupCompose() {
        setContent {
            val navController = remember {
                object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {}
                    override fun navigateBack() = finish()
                }
            }

            CompositionLocalProvider(LocalNavController provides navController) {
                SettingsTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    UpdatesScaffoldContent(
                        uiState = uiState,
                        updaterController = activeUpdaterController,
                        controllerStateVersion = controllerStateVersion,
                        onRefreshClick = { onRefreshClick() },
                        onLocalUpdateClick = { onLocalUpdateClick() },
                        onPreferencesClick = {
                            startActivity(
                                Intent(
                                    this@UpdatesScaffoldActivity,
                                    PreferencesActivity::class.java,
                                )
                            )
                        },
                        onControllerStateChanged = { notifyControllerStateChanged() },
                    )
                }
            }
        }
    }

    protected fun setUpdaterController(controller: UpdaterController?) {
        activeUpdaterController = controller
        notifyControllerStateChanged()
    }

    protected fun notifyControllerStateChanged() {
        controllerStateVersion++
    }

    open fun onRefreshClick() {}
    open fun onLocalUpdateClick() {}
    open fun exportUpdate(update: Update) {}
}

@Composable
private fun UpdatesScaffoldContent(
    uiState: UpdatesViewModel.UiState,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onControllerStateChanged: () -> Unit,
) {
    val title = getTitleForUpdateStatus(uiState.updates)

    val actionDialogState = remember { mutableStateOf<AlertDialogState?>(null) }
    actionDialogState.value?.let { dialog ->
        UpdateActionDialog(
            dialog = dialog,
            onDismiss = { actionDialogState.value = null },
        )
    }

    val showWhatsNew = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as UpdatesScaffoldActivity
    val updaterApplication = context.applicationContext as UpdaterApplication
    val networkMonitor = remember { updaterApplication.networkMonitor }
    val networkState by networkMonitor.networkState.collectAsState(
        initial = networkMonitor.currentNetworkState,
    )
    val userPreferencesRepository = remember { updaterApplication.userPreferencesRepository }
    val streamUpdatesEnabled by userPreferencesRepository.streamUpdatesFlow.collectAsState(
        initial = true,
    )

    val updateItems = remember(
        uiState.updates,
        updaterController,
        networkState,
        streamUpdatesEnabled,
        controllerStateVersion,
    ) {
        val controller = updaterController ?: return@remember emptyList<UpdateItemState>()
        val mapper = UpdateItemStateMapper(context, controller, streamUpdatesEnabled)
        uiState.updates.filter { it.status != UpdateStatus.INSTALLATION_FAILED && it.status != UpdateStatus.PAUSED_ERROR }.mapNotNull { update ->
            controller.getUpdate(update.downloadId)?.let {
                mapper.map(it, networkState)
            }
        }
    }

    val actionHandler = remember(updaterController) {
        updaterController?.let { controller ->
            UpdateActionHandler(
                activity = activity,
                updaterController = controller,
                exportUpdate = { update -> activity.exportUpdate(update) },
                showDialog = { actionDialogState.value = it },
            )
        }
    }

    val primaryUpdate = updateItems.firstOrNull()

    val isInstallFailed = uiState.updates.any { it.status == UpdateStatus.INSTALLATION_FAILED }
    val isDownloadFailed = uiState.updates.any { it.status == UpdateStatus.PAUSED_ERROR }
    val isNoUpdatesFound = uiState.updates.isEmpty() && 
        uiState.updatesCheckModel.state != UpdatesCheckState.Error && 
        uiState.updatesCheckModel.state != UpdatesCheckState.NoInternet

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { 
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 8.dp)
                    ) 
                },
                navigationIcon = {
                    val activity = LocalContext.current as? ComponentActivity
                    IconButton(
                        onClick = { activity?.finish() },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onPreferencesClick,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
            )
        },
        bottomBar = {
            UpdatesCheck(
                model = uiState.updatesCheckModel,
                uiState = rememberUpdatesCheckUiState(uiState.updatesCheckModel.state),
                updateZipName = uiState.updates.firstOrNull()?.name,
                primaryUpdate = primaryUpdate,
                onPrimaryAction = { action, downloadId ->
                    val controller = updaterController ?: return@UpdatesCheck
                    val update = controller.getUpdate(downloadId) ?: return@UpdatesCheck
                    actionHandler?.perform(action, update)
                    onControllerStateChanged()
                },
                isWhatsNewVisible = showWhatsNew.value,
                isInstallFailed = isInstallFailed,
                isDownloadFailed = isDownloadFailed,
                isNoUpdatesFound = isNoUpdatesFound,
                onCheckClick = onRefreshClick,
                onWhatsNewClick = { showWhatsNew.value = !showWhatsNew.value }
            )
        }
    ) { paddingValues ->
        val isCheckFailed = uiState.updatesCheckModel.state == UpdatesCheckState.Error || 
                           uiState.updatesCheckModel.state == UpdatesCheckState.NoInternet

        val isInstallFailedContent = uiState.updates.any { it.status == UpdateStatus.INSTALLATION_FAILED }
        val isDownloadFailedContent = uiState.updates.any { it.status == UpdateStatus.PAUSED_ERROR }

        if (showWhatsNew.value) {
            WhatsNewScreen(
                paddingValues = paddingValues,
                uiState = uiState,
            )
        } else if (isWideScreen()) {
            WideUpdatesScaffold(
                paddingValues = paddingValues,
                updates = uiState.updates,
            )
        } else {
            UpdatesScaffold(
                paddingValues = paddingValues,
                updates = uiState.updates,
            )
        }
    }
}

@Composable
private fun WhatsNewScreen(
    paddingValues: PaddingValues,
    uiState: UpdatesViewModel.UiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(SettingsDimension.itemPadding)
        ) {
            Text(
                text = stringResource(R.string.whats_new),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.whats_new_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WideUpdatesScaffold(
    paddingValues: PaddingValues,
    updates: List<Update>,
) {
    val layoutDirection = LocalLayoutDirection.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
            )
    ) {
        UpdatesInformationPane(
            updates = updates,
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding()),
        )
    }
}

@Composable
private fun UpdatesScaffold(
    paddingValues: PaddingValues,
    updates: List<Update>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        UpdatesInformationPane(updates = updates)
    }
}

@Composable
private fun UpdatesInformationPane(
    updates: List<Update>,
    modifier: Modifier = Modifier,
) {
    val updateZipName = updates.firstOrNull()?.name
    DeviceInfoBanner(updateZipName = updateZipName, modifier = modifier)
}



@Composable
private fun isWideScreen(): Boolean {
    val minWideScreenWidth = 600.dp
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize

    return with(density) { windowSize.width.toDp() >= minWideScreenWidth }
}



@Composable
private fun getTitleForUpdateStatus(updates: List<Update>): String = when {
    updates.any { it.status == UpdateStatus.UPDATED_NEED_REBOOT } ->
        stringResource(R.string.installing_update_finished)

    updates.any { it.status == UpdateStatus.INSTALLATION_FAILED } ->
        stringResource(R.string.installing_update_error)

    updates.any {
        it.status == UpdateStatus.INSTALLING ||
                it.status == UpdateStatus.INSTALLATION_SUSPENDED
    } -> stringResource(R.string.installing_update)

    else -> "Updater"
}
