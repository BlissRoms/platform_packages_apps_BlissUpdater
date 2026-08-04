/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2020-2022 SHIFT GmbH
 * SPDX-License-Identifier: Apache-2.0
 */
package org.blissroms.updater;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.blissroms.updater.controller.UpdaterController;
import org.blissroms.updater.data.Update;
import org.blissroms.updater.data.UpdateStatus;
import org.blissroms.updater.misc.Utils;
import org.blissroms.updater.util.OtaMetadataParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class UpdateImporter {
    private static final int REQUEST_PICK = 9061;
    private static final String TAG = "UpdateImporter";
    private static final String MIME_ZIP = "application/zip";
    private static final String FILE_NAME = "localUpdate.zip";

    private final Activity activity;
    private final Callbacks callbacks;

    private Thread workingThread;

    public UpdateImporter(Activity activity, Callbacks callbacks) {
        this.activity = activity;
        this.callbacks = callbacks;
    }

    public void stopImport() {
        if (workingThread != null && workingThread.isAlive()) {
            workingThread.interrupt();
            workingThread = null;
        }
    }

    public void openImportPicker() {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(MIME_ZIP);
        activity.startActivityForResult(intent, REQUEST_PICK);
    }

    public boolean onResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || requestCode != REQUEST_PICK) {
            return false;
        }

        return onPicked(data.getData());
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean onPicked(Uri uri) {
        callbacks.onImportStarted();

        workingThread = new Thread(() -> {
            File importedFile = null;
            try {
                String originalName = getFileName(uri);
                if (originalName == null || originalName.isEmpty()) {
                    originalName = activity.getString(R.string.local_update_name);
                }

                importedFile = importFile(uri);
                verifyPackage(importedFile);

                final Update update = buildLocalUpdate(importedFile, originalName);
                addUpdate(update);
                activity.runOnUiThread(() -> callbacks.onImportCompleted(update));
            } catch (Exception e) {
                Log.e(TAG, "Failed to import update package", e);
                // Do not store invalid update
                if (importedFile != null) {
                    importedFile.delete();
                }

                activity.runOnUiThread(() -> callbacks.onImportCompleted(null));
            }
        });
        workingThread.start();
        return true;
    }

    @SuppressLint("SetWorldReadable")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File importFile(Uri uri) throws IOException {
        final ParcelFileDescriptor parcelDescriptor = activity.getContentResolver()
                .openFileDescriptor(uri, "r");
        if (parcelDescriptor == null) {
            throw new IOException("Failed to obtain fileDescriptor");
        }

        final FileInputStream iStream = new FileInputStream(parcelDescriptor
                .getFileDescriptor());
        final File downloadDir = Utils.getDownloadPath(activity);
        final File outFile = new File(downloadDir, FILE_NAME);
        if (outFile.exists()) {
            outFile.delete();
        }
        final FileOutputStream oStream = new FileOutputStream(outFile);

        int read;
        final byte[] buffer = new byte[4096];
        while ((read = iStream.read(buffer)) > 0) {
            oStream.write(buffer, 0, read);
        }
        oStream.flush();
        oStream.close();
        iStream.close();
        parcelDescriptor.close();

        outFile.setReadable(true, false);

        return outFile;
    }

    private Update buildLocalUpdate(File file, String originalName) throws IOException {
        final OtaMetadataParser metadata = new OtaMetadataParser(file);
        return new Update.Builder()
            .setName(originalName)
            .setFile(file)
            .setFileSize(file.length())
            .setTimestamp(metadata.getTimestamp())
            .setOsPatchLevel(metadata.getSecurityPatchLevel())
            .setOsSdkLevel(metadata.getSdkLevel())
            .setStatus(UpdateStatus.VERIFIED)
            .setVersion(originalName)
            .build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void verifyPackage(File file) throws Exception {
        try {
            android.os.RecoverySystem.verifyPackage(file, null, null);
        } catch (Exception e) {
            if (file.exists()) {
                file.delete();
                throw new Exception("Verification failed, file has been deleted");
            } else {
                throw e;
            }
        }
    }

    private void addUpdate(Update update) {
        UpdaterController controller = UpdaterController.getInstance(activity);
        controller.addLocalUpdate(update);
    }

    public interface Callbacks {
        void onImportStarted();

        void onImportCompleted(Update update);
    }
}
