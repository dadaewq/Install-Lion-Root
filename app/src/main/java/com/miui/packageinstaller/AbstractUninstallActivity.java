package com.miui.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.miui.packageinstaller.utils.ApplicationLabelUtils;

import java.util.Objects;


/**
 * @author dadaewq
 */
public abstract class AbstractUninstallActivity extends Activity {

    private final String nl = System.getProperty("line.separator");
    String pkgLable;
    private AlertDialog alertDialog;
    private String pkgname;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent().getAction();
        if (Intent.ACTION_DELETE.equals(action) || Intent.ACTION_UNINSTALL_PACKAGE.equals(action)) {
            pkgname = Objects.requireNonNull(getIntent().getData()).getEncodedSchemeSpecificPart();
            if (pkgname == null) {
                showToast(getString(R.string.failed_prase));
                finish();
            } else {
                initUninstall();
            }
        } else {
            showToast(getString(R.string.failed_prase)+" "+action);
            finish();
        }

    }

    private String[] getExistedVersion(String pkgname) {
        PackageManager pm = getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(pkgname, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (applicationInfo == null) {
            return null;
        } else {
            String apkPath = applicationInfo.sourceDir;
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (pkgInfo != null) {
                pkgInfo.applicationInfo.sourceDir = apkPath;
                pkgInfo.applicationInfo.publicSourceDir = apkPath;
                return new String[]{pkgInfo.versionName,
                        Build.VERSION.SDK_INT < 28 ? Integer.toString(pkgInfo.versionCode) : Long.toString(pkgInfo.getLongVersionCode())};
            } else {
                return null;
            }
        }

    }

    private void initUninstall() {
        String[] version = getExistedVersion(pkgname);

        pkgLable = ApplicationLabelUtils.getApplicationLabel(this, null, null, pkgname);

        StringBuilder alertDialogMessage = new StringBuilder();
        alertDialogMessage
                .append(
                        String.format(
                                getString(R.string.message_name),
                                pkgLable
                        )
                )
                .append(nl)
                .append(
                        String.format(
                                getString(R.string.message_packagename),
                                pkgname
                        )
                )
                .append(nl);

        if (version != null) {
            alertDialogMessage.append(String.format(
                    getString(R.string.message_version),
                    version[0],
                    version[1])
            )
                    .append(nl);
        }

        alertDialogMessage
                .append(nl)
                .append(nl)
                .append(getString(R.string.message_uninstalConfirm));


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_uninstall_title));
        builder.setMessage(alertDialogMessage);
        builder.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
            startUninstall(pkgname);
            finish();
        });
        builder.setNegativeButton(android.R.string.no, (dialogInterface, i) -> finish());
        alertDialog = builder.show();
        alertDialog.setOnCancelListener(dialog -> finish());
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);

    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (alertDialog != null) {
            alertDialog.dismiss();
        }

    }


    protected abstract void startUninstall(String pkgname);


    void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }


}
