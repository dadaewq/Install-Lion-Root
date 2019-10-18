package com.miui.packageinstaller.utils.installer;

import android.content.Context;

import com.miui.packageinstaller.utils.apksource.ApkSource;

class QueuedInstallation {

    private final ApkSource mApkSource;
    private final long mId;

    QueuedInstallation(Context c, ApkSource apkSource, long id) {
        mApkSource = apkSource;
        mId = id;
    }

    public long getId() {
        return mId;
    }

    ApkSource getApkSource() {
        return mApkSource;
    }
}
