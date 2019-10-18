package com.miui.packageinstaller;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.miui.packageinstaller.utils.shell.Shell;
import com.miui.packageinstaller.utils.shell.SuShell;

import java.util.Objects;

public class UninstallerActivity extends AbstractUninstallActivity {


    private String pkgname;

    @Override
    protected void startUninstall(String pkgname) {
        this.pkgname = pkgname;
        showToast(String.format(getString(R.string.uninstall_start), pkgLable));
        new UninstallApkTask().start();

    }

    private void showErrToast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }


    private void copyErr(String CMD) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, CMD);
        Objects.requireNonNull(clipboard).setPrimaryClip(clipData);
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
                copyErr(uninstallationResult.err);
                showErrToast(getString(R.string.failed_uninstall) + "," + getString(R.string.cpoy_error) + "\t" + uninstallationResult.err);
            }

        }
    }
}
