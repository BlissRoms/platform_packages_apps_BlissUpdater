/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.blissroms.updater.model;

public interface UpdateBaseInfo {
    String getName();

    String getDownloadId();

    long getTimestamp();

    String getVersion();

    String getDownloadUrl();

    long getFileSize();
}
