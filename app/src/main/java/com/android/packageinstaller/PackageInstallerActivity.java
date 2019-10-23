package com.android.packageinstaller;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.packageinstaller.utils.ShellUtils;
import com.android.packageinstaller.utils.Utils;
import com.android.packageinstaller.utils.apksource.ApkSource;
import com.android.packageinstaller.utils.installer.ApkSourceBuilder;
import com.android.packageinstaller.utils.installer.SAIPackageInstaller;
import com.android.packageinstaller.utils.installer.rooted.RootedSAIPackageInstaller;
import com.miui.packageinstaller.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author dadaewq
 */
public class PackageInstallerActivity extends AbstractInstallActivity implements SAIPackageInstaller.InstallationStatusListener {

    private long mOngoingSessionId;
    private String apkPath;

    @Override
    public void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            this.apkPath = apkPath;
            apkFile = new File(apkPath);

            ArrayList<File> files = new ArrayList<>();
            files.add(apkFile);
            new Thread(() -> {
                showToast0(String.format(getString(R.string.start_install), apkinfo[0]));
                try {
                    installPackages(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ).start();
        } else {
            showToast0(getString(R.string.failed_read));
            finish();
        }
    }


    private void installPackages(List<File> apkFiles) {
        Context mContext = getApplication();
        SAIPackageInstaller mInstaller = RootedSAIPackageInstaller.getInstance(mContext);
        mInstaller.addStatusListener(this);
        ApkSource apkSource = new ApkSourceBuilder()
                .fromApkFiles(apkFiles)
                .build();

        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
    }


    @Override
    public void onStatusChanged(long installationID, SAIPackageInstaller.InstallationStatus status, @Nullable String packageNameOrErrorDescription) {
        if (installationID != mOngoingSessionId) {
            return;
        }
        Log.d("status", status + "");

        switch (status) {
            case QUEUED:
            case INSTALLING:
                break;
            case INSTALLATION_SUCCEED:
                deleteCache();
                showToast0(String.format(getString(R.string.success_install), apkinfo[0]));
                finish();
                break;
            case INSTALLATION_FAILED:

                if (packageNameOrErrorDescription != null) {
                    if (packageNameOrErrorDescription.contains(getString(R.string.installer_error_root_no_root))) {
                        installByShellUtils();
                    } else {
                        deleteCache();
                        copyErr(packageNameOrErrorDescription);
                        String err = packageNameOrErrorDescription.substring(packageNameOrErrorDescription.indexOf("Err:") + 4);
                        showToast1(String.format(getString(R.string.failed_install), apkinfo[0], err));
                    }
                } else {
                    deleteCache();
                    copyErr(getString(R.string.unknown));
                    showToast1(String.format(getString(R.string.failed_install), apkinfo[0], ""));
                }
                finish();
                break;
            default:
                finish();
        }
    }

    private void installByShellUtils() {
        String installcommand = "pm install -r " + "\"" + apkPath + "\"";
        String[] resultSElinux = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resultSElinux = ShellUtils.execWithRoot("setenforce 0");
        }
        String[] result = ShellUtils.execWithRoot(installcommand);

        if ("0".equals(result[3])) {
            deleteCache();
            showToast0(String.format(getString(R.string.success_install), apkinfo[0]));
        } else {
            deleteCache();
            StringBuilder err = new StringBuilder(String.format("%s: %s %s | %s | Android %s \n\n", getString(R.string.installer_device), Build.BRAND, Build.MODEL, Utils.isMiui() ? "MIUI" : "Not MIUI", Build.VERSION.RELEASE))
                    .append(String.format("Command: %s\nExit code: %s\nOut:\n%s\n=============\nErr:\n%s", result[2], result[3], result[0], result[1]));

            if (resultSElinux != null && !"0".equals(resultSElinux[3])) {
                copyErr(err.append("\n") + resultSElinux[1]);
                showToast1(String.format(getString(R.string.failed_install), apkinfo[0], resultSElinux[1] + "\n" + result[1]));
            } else {
                copyErr(err.toString());
                showToast1(String.format(getString(R.string.failed_install), apkinfo[0], result[1]));
            }

        }
    }

}
