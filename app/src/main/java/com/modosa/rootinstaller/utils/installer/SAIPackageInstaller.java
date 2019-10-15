package com.modosa.rootinstaller.utils.installer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LongSparseArray;

import androidx.annotation.Nullable;

import com.modosa.rootinstaller.utils.apksource.ApkSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SAIPackageInstaller {
    private static final String TAG = "SAIPI";
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final ArrayDeque<QueuedInstallation> mInstallationQueue = new ArrayDeque<>();
    private final ArrayList<InstallationStatusListener> mListeners = new ArrayList<>();
    private final LongSparseArray<QueuedInstallation> mCreatedInstallationSessions = new LongSparseArray<>();
    private boolean mInstallationInProgress;
    private long mLastInstallationID = 0;
    private QueuedInstallation mOngoingInstallation;

    SAIPackageInstaller(Context c) {
        mContext = c.getApplicationContext();
    }

    protected Context getContext() {
        return mContext;
    }

    public void addStatusListener(InstallationStatusListener listener) {
        mListeners.add(listener);
    }


    public long createInstallationSession(ApkSource apkSource) {
        long installationID = mLastInstallationID++;
        mCreatedInstallationSessions.put(installationID, new QueuedInstallation(getContext(), apkSource, installationID));
        return installationID;
    }

    public void startInstallationSession(long sessionID) {
        QueuedInstallation installation = mCreatedInstallationSessions.get(sessionID);
        mCreatedInstallationSessions.remove(sessionID);
        if (installation == null) {
            return;
        }

        mInstallationQueue.addLast(installation);
        dispatchSessionUpdate(installation.getId(), InstallationStatus.QUEUED, null);
        processQueue();
    }


    private void processQueue() {
        if (mInstallationQueue.size() == 0 || mInstallationInProgress) {
            return;
        }

        QueuedInstallation installation = mInstallationQueue.removeFirst();
        mOngoingInstallation = installation;
        mInstallationInProgress = true;

        dispatchCurrentSessionUpdate(InstallationStatus.INSTALLING, null);

        mExecutor.execute(() -> installApkFiles(installation.getApkSource()));
    }

    protected abstract void installApkFiles(ApkSource apkSource);

    void installationCompleted() {
        Log.d(TAG, String.format("%s->installationCompleted(); mOngoingInstallation.id=%d", getClass().getSimpleName(), dbgGetOngoingInstallationId()));
        mInstallationInProgress = false;
        mOngoingInstallation = null;
        processQueue();
    }

    private void dispatchSessionUpdate(long sessionID, InstallationStatus status, String packageNameOrError) {
        mHandler.post(() -> {
            Log.d(TAG, String.format("%s->dispatchSessionUpdate(%d, %s, %s)", getClass().getSimpleName(), sessionID, status.name(), packageNameOrError));
            for (InstallationStatusListener listener : mListeners) {
                listener.onStatusChanged(sessionID, status, packageNameOrError);
            }
        });
    }

    void dispatchCurrentSessionUpdate(InstallationStatus status, String packageNameOrError) {
        Log.d(TAG, String.format("%s->dispatchCurrentSessionUpdate(%s, %s); mOngoingInstallation.id=%d", getClass().getSimpleName(), status.name(), packageNameOrError, dbgGetOngoingInstallationId()));
        dispatchSessionUpdate(mOngoingInstallation.getId(), status, packageNameOrError);
    }

    private long dbgGetOngoingInstallationId() {
        return mOngoingInstallation != null ? mOngoingInstallation.getId() : -1;
    }


    public enum InstallationStatus {
        QUEUED, INSTALLING, INSTALLATION_SUCCEED, INSTALLATION_FAILED
    }

    public interface InstallationStatusListener {
        void onStatusChanged(long installationID, InstallationStatus status, @Nullable String packageNameOrErrorDescription);
    }
}
