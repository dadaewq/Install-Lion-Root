package com.miui.packageinstaller;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.miui.packageinstaller.utils.ShellUtils;
import com.miui.packageinstaller.utils.Utils;
import com.miui.packageinstaller.utils.apksource.ApkSource;
import com.miui.packageinstaller.utils.installer.ApkSourceBuilder;
import com.miui.packageinstaller.utils.installer.SAIPackageInstaller;
import com.miui.packageinstaller.utils.installer.rooted.RootedSAIPackageInstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * @author dadaewq
 */
public class PackageInstallerActivity extends AbstractInstallActivity implements SAIPackageInstaller.InstallationStatusListener {

    private long mOngoingSessionId;
    private File apkFile;
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
                showToast(String.format(getString(R.string.install_start), apkinfo[0]));
                try {
                    installPackages(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }

            ).start();
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
    }

    private void showErrToast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
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

    private void copyErr(String CMD) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, CMD);
        Objects.requireNonNull(clipboard).setPrimaryClip(clipData);
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
                showToast(String.format(getString(R.string.success_install), apkinfo[0]));
                finish();
                break;
            case INSTALLATION_FAILED:

                if (packageNameOrErrorDescription != null) {
                    if (packageNameOrErrorDescription.contains(getString(R.string.installer_error_root_no_root))) {
                        String installcommand = "pm install -r " + "\"" + apkPath + "\"";
                        String[] resultSElinux = null;
                        if (Build.VERSION.SDK_INT > 23) {
                            resultSElinux = ShellUtils.execWithRoot("setenforce 0");
                        }
                        String[] result = ShellUtils.execWithRoot(installcommand);

                        if ("0".equals(result[3])) {
                            deleteCache();
                            showToast(String.format(getString(R.string.success_install), apkinfo[0]));
                        } else {
                            StringBuilder err = new StringBuilder();
                            err.append(String.format("%s: %s %s | %s | Android %s \n\n", getString(R.string.installer_device), Build.BRAND, Build.MODEL, Utils.isMiui() ? "MIUI" : "Not MIUI", Build.VERSION.RELEASE))
                                    .append(String.format("Command: %s\nExit code: %s\nOut:\n%s\n=============\nErr:\n%s", result[2], result[3], result[0], result[1]));

                            deleteCache();
                            if (resultSElinux != null && !"0".equals(resultSElinux[3])) {
                                copyErr(err.append("\n") + resultSElinux[1]);
                                showErrToast(getString(R.string.failed_install) + "," + getString(R.string.cpoy_error) + "==>\t" + resultSElinux[1] + "\t" + result[1]);
                            } else {
                                copyErr(err.toString());
                                showErrToast(getString(R.string.failed_install) + "," + getString(R.string.cpoy_error) + "==>\t" + result[1]);
                            }

                        }
                    } else {
                        deleteCache();
                        copyErr(packageNameOrErrorDescription);
                        String err = packageNameOrErrorDescription.substring(packageNameOrErrorDescription.indexOf("Err:") + 4);
                        showErrToast(getString(R.string.failed_install) + "," + getString(R.string.cpoy_error) + "\t" + err);
                    }
                } else {
                    deleteCache();
                }
                finish();
                break;
            default:
                finish();
        }
    }

    private void deleteCache() {
        if (istemp) {
            deleteSingleFile(apkFile);
        }
    }


}
