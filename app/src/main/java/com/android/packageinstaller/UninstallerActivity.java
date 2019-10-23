package com.android.packageinstaller;


import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.android.packageinstaller.utils.Utils;
import com.android.packageinstaller.utils.shell.Shell;
import com.android.packageinstaller.utils.shell.SuShell;
import com.miui.packageinstaller.R;

public class UninstallerActivity extends AbstractUninstallActivity {


    private String pkgName;

    @Override
    protected void startUninstall(String pkgName) {
        this.pkgName = pkgName;
        new UninstallApkTask().start();
    }


    private class UninstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            Log.d("Start uninstall", pkgName);
            Looper.prepare();
            if (!SuShell.getInstance().isAvailable()) {
                copyErr(String.format("%s\n\n%s\n%s", getString(R.string.dialog_uninstall_title), alertDialogMessage, getString(R.string.installer_error_root_no_root)));
                showToast1(String.format(getString(R.string.failed_uninstall), packageLable, getString(R.string.installer_error_root_no_root)));
            } else {
                Shell.Result uninstallationResult = SuShell.getInstance().exec(new Shell.Command("pm", "uninstall", pkgName));
                if (0 == uninstallationResult.exitCode) {
                    showToast0(String.format(getString(R.string.success_uninstall), packageLable));
                } else {
                    String saiVersion = "???";
                    try {
                        saiVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    } catch (PackageManager.NameNotFoundException ignore) {
                    }
                    String info = String.format("%s: %s %s | %s | Android %s | Install Lion-Root %s\n\n", getString(R.string.installer_device), Build.BRAND, Build.MODEL, Utils.isMiui() ? "MIUI" : "Not MIUI", Build.VERSION.RELEASE, saiVersion);
                    copyErr(info + uninstallationResult.toString());
                    showToast1(String.format(getString(R.string.failed_uninstall), packageLable, uninstallationResult.err));
                }
            }
            Looper.loop();
        }
    }
}
