/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.blissroms.updater.preferences

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme

import org.blissroms.updater.UpdateImporter
import org.blissroms.updater.data.Update
import android.widget.Toast
import android.app.AlertDialog
import org.blissroms.updater.R
import org.blissroms.updater.util.StringUtil

class PreferencesActivity : ComponentActivity(), UpdateImporter.Callbacks {
    private lateinit var updateImporter: UpdateImporter
    private var importDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateImporter = UpdateImporter(this, this)
        setContent {
            val navController = remember {
                object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {}
                    override fun navigateBack() = onBackPressedDispatcher.onBackPressed()
                }
            }
            CompositionLocalProvider(LocalNavController provides navController) {
                SettingsTheme {
                    PreferencesScreen()
                }
            }
        }
    }

    @Suppress("deprecation", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        if (!updateImporter.onResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onLocalUpdateClick() {
        updateImporter.openImportPicker()
    }

    override fun onImportStarted() {
        if (importDialog?.isShowing == true) {
            importDialog?.dismiss()
        }

        importDialog = AlertDialog.Builder(this)
            .setTitle(R.string.local_update_import)
            .setView(R.layout.progress_dialog)
            .setCancelable(false)
            .create()

        importDialog?.show()
    }

    override fun onImportCompleted(update: Update?) {
        importDialog?.dismiss()
        importDialog = null

        if (update == null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.local_update_import)
                .setMessage(R.string.local_update_import_failure)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.local_update_import_success_title)
            .setMessage(
                getString(
                    R.string.local_update_import_success_message,
                    StringUtil.formatBuildDate(this, update.timestamp)
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
