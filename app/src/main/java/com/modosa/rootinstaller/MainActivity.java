package com.modosa.rootinstaller;

import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * @author dadaewq
 */
public class MainActivity extends AbstractInstallActivity {
    private String apkpath;

    @Override
    protected void startInstall(String apkpath) {
        this.apkpath = apkpath;
        if (this.apkpath != null) {
            apkinfo = getApkPkgInfo(this.apkpath);
            showToast(getString(R.string.install_start) + apkinfo[1]);
            new InstallApkTask().start();
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }

    }

    private void showErrToast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }

    private void deleteCache() {
        if (istemp) {
            deleteSingleFile(new File(apkpath));
        }
    }

    private class InstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            String installcommand = "pm install -r " + "--user 0 \"" + apkpath + "\"";
            String[] result = ShellUtils.execWithRoot("setenforce 0 && " + installcommand);
            if ("0".equals(result[3])) {
                deleteCache();
                showToast(apkinfo[1] + " " + getString(R.string.success_install));
            } else if (result[1].contains("SELinux is disabled")) {
                Log.e("ERROR=>", "SELinux is disabled,start another method");

                String[] result1 = ShellUtils.execWithRoot(installcommand);
                if (result1[3] != null) {
                    deleteCache();
                }
                if ("0".equals(result1[3])) {
                    showToast(apkinfo[1] + " " + getString(R.string.success_install));
                } else {
                    showErrToast(getString(R.string.failed_install) + "==>\t" + result1[1]);
                }
            } else {
                deleteCache();
                showErrToast(getString(R.string.failed_install) + "==>\t" + result[1]);
            }
        }
    }
}