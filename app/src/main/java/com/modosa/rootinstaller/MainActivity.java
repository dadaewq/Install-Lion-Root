package com.modosa.rootinstaller;

import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.modosa.rootinstaller.utils.ShellUtils;

import java.io.File;

/**
 * @author dadaewq
 */
public class MainActivity extends AbstractInstallActivity {
    private String apkSourcePath;
    private String pkgname;

    @Override
    protected void startInstall(String apkpath) {
        apkSourcePath = apkpath;
        if (apkSourcePath != null) {
            apkinfo = getApkPkgInfo(apkSourcePath);
            showToast(String.format(getString(R.string.install_start), apkinfo[0]));
            new InstallApkTask().start();
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }

    }

    @Override
    protected void startUninstall(String pkgname) {
        this.pkgname = pkgname;
        showToast("卸载中");
        new UninstallApkTask().start();

    }

    private void showErrToast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }

    private void deleteCache() {
        if (istemp) {
            deleteSingleFile(new File(apkSourcePath));
        }
    }

    private class UninstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            Log.d("Start uninstall", pkgname);
            String uninstallcommand = "pm uninstall " + "--user 0 \"" + pkgname + "\"";

            String[] result = ShellUtils.execWithRoot(uninstallcommand);

            if ("0".equals(result[3])) {
                showToast(getString(R.string.success_uninstall));
            } else {
                showErrToast(getString(R.string.failed_uninstall) + "==>\t" + result[1]);
            }

        }
    }

    private class InstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            Log.d("Start install", apkSourcePath + "");
            String uninstallcommand = "pm install -r " + "--user 0 \"" + apkSourcePath + "\"";
            String[] result;
            if (Build.VERSION.SDK_INT > 23) {
                result = ShellUtils.execWithRoot("setenforce 0 && " + uninstallcommand);
            } else {
                result = ShellUtils.execWithRoot(uninstallcommand);
            }

            if ("0".equals(result[3])) {
                deleteCache();
                showToast(String.format(getString(R.string.success_install), apkinfo[0]));
            } else {
                deleteCache();
                showErrToast(getString(R.string.failed_install) + "==>\t" + result[1]);
            }
        }
    }
}