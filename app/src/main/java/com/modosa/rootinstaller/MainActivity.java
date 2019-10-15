package com.modosa.rootinstaller;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.modosa.rootinstaller.utils.apksource.ApkSource;
import com.modosa.rootinstaller.utils.installer.ApkSourceBuilder;
import com.modosa.rootinstaller.utils.installer.SAIPackageInstaller;
import com.modosa.rootinstaller.utils.installer.rooted.RootedSAIPackageInstaller;
import com.modosa.rootinstaller.utils.shell.Shell;
import com.modosa.rootinstaller.utils.shell.SuShell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * @author dadaewq
 */
public class MainActivity extends AbstractInstallActivity implements SAIPackageInstaller.InstallationStatusListener {

    private long mOngoingSessionId;
    private File apkFile;
    private String pkgname;


    @Override
    public void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
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

    @Override
    protected void startUninstall(String pkgname) {
        this.pkgname = pkgname;
        showToast(String.format(getString(R.string.uninstall_start), pkgLable));
        new UninstallApkTask().start();

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
                if (istemp) {
                    deleteSingleFile(apkFile);
                }
                showToast(String.format(getString(R.string.success_install), apkinfo[0]));
                finish();
                break;
            case INSTALLATION_FAILED:
                if (istemp) {
                    deleteSingleFile(apkFile);
                }
                copyErr(packageNameOrErrorDescription);
                String err;
                if (packageNameOrErrorDescription != null) {
                    if (packageNameOrErrorDescription.contains(getString(R.string.installer_error_root_no_root))) {
                        err = packageNameOrErrorDescription;
                        showErrToast(getString(R.string.failed_install) + " ==>\n" + err);
                    } else {
                        err = packageNameOrErrorDescription.substring(packageNameOrErrorDescription.indexOf("Err:") + 4);
                        showErrToast(getString(R.string.failed_install) + "," + getString(R.string.cpoy_error) + "\t" + err);
                    }
                }

                finish();
                break;
            default:
                finish();
        }
    }

    private class UninstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            Log.d("Start uninstall", pkgname);

            Shell.Result uninstallationResult = SuShell.getInstance().exec(new Shell.Command("pm", "uninstall", pkgname));
            if (0 == uninstallationResult.exitCode) {
                showToast(getString(R.string.success_uninstall));
            } else {
                showErrToast(getString(R.string.failed_uninstall) + "==>" + uninstallationResult.err);
            }

        }
    }

}
