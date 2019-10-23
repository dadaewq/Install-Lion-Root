package com.android.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.android.packageinstaller.utils.AppInfoUtils;
import com.miui.packageinstaller.R;

import java.util.Objects;


/**
 * @author dadaewq
 */
public abstract class AbstractUninstallActivity extends Activity {

    private final String nl = System.getProperty("line.separator");
    String packageLable;
    StringBuilder alertDialogMessage;
    private AlertDialog alertDialog;
    private String pkgName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent().getAction();
        if (Intent.ACTION_DELETE.equals(action) || Intent.ACTION_UNINSTALL_PACKAGE.equals(action)) {
            pkgName = Objects.requireNonNull(getIntent().getData()).getEncodedSchemeSpecificPart();
            if (pkgName == null) {
                showToast0(getString(R.string.failed_prase));
                finish();
            } else {
                initUninstall();
            }
        } else {
            showToast1(getString(R.string.failed_prase));
            finish();
        }

    }


    private void initUninstall() {
        String[] version = AppInfoUtils.getApplicationVersion(this, pkgName);

        packageLable = AppInfoUtils.getApplicationLabel(this, pkgName);
        if (AppInfoUtils.UNINSTALLED.equals(packageLable)) {
            packageLable = "Uninstalled";
        }
        alertDialogMessage = new StringBuilder();
        alertDialogMessage
                .append(
                        String.format(
                                getString(R.string.message_name),
                                packageLable
                        )
                )
                .append(nl)
                .append(
                        String.format(
                                getString(R.string.message_packagename),
                                pkgName
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_uninstall_title));
        builder.setMessage(alertDialogMessage + nl + nl + getString(R.string.message_uninstalConfirm));
        builder.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
            startUninstall(pkgName);
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

    protected abstract void startUninstall(String pkgName);

    void copyErr(String err) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, err);
        Objects.requireNonNull(clipboard).setPrimaryClip(clipData);
    }

    void showToast0(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    void showToast1(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }


}
