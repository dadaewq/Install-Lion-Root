package com.modosa.rootinstaller.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.modosa.rootinstaller.R;
import com.modosa.rootinstaller.util.AppInfoUtil;
import com.modosa.rootinstaller.util.OpUtil;
import com.modosa.rootinstaller.util.PraseContentUtil;

import java.io.File;
import java.util.Objects;


/**
 * @author dadaewq
 */
public abstract class AbstractInstallActivity extends Activity {
    private static final int PICK_APK_FILE = 2;
    private static final String ILLEGALPKGNAME = "IL^&IllegalPN*@!128`+=：:,.[";
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private final String nl = System.getProperty("line.separator");
    boolean show_notification;
    String[] apkinfo;
    String packageLable;
    File installApkFile;
    private StringBuilder alertDialogMessage;
    private boolean istemp = false;
    private String[] source;
    private Uri uri;
    private SharedPreferences sourceSp;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private AlertDialog alertDialog;
    private String cachePath;
    private String uninstallPkgName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFromAction(getIntent().getAction() + "");
    }

    private void initFromAction(String action) {
        switch (action) {
            case OpUtil.MODOSA_ACTION_PICK_FILE:
                openFile();
                break;
            case Intent.ACTION_DELETE:
            case Intent.ACTION_UNINSTALL_PACKAGE:
                uninstallPkgName = Objects.requireNonNull(getIntent().getData()).getEncodedSchemeSpecificPart();
                if (uninstallPkgName == null) {
                    showToast0(R.string.tip_failed_prase);
                    finish();
                } else {
                    initUninstall();
                }
                break;
            default:
                uri = getIntent().getData();
                initFromUri();

        }
    }

    private void initFromUri() {
        sourceSp = getSharedPreferences("allowsource", Context.MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (checkPermission()) {
            initInstall();
        } else {
            requestPermission();
        }
    }

    private void initUninstall() {
        String[] version = AppInfoUtil.getApplicationVersion(this, uninstallPkgName);

        packageLable = AppInfoUtil.getApplicationLabel(this, uninstallPkgName);
        if (AppInfoUtil.UNINSTALLED.equals(packageLable)) {
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
                                uninstallPkgName
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
            startUninstall(uninstallPkgName);
            finish();
        });
        builder.setNegativeButton(android.R.string.no, (dialogInterface, i) -> finish());
        alertDialog = builder.create();
        OpUtil.showAlertDialog(this, alertDialog);
        alertDialog.setOnCancelListener(dialog -> finish());
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
    }


    private void initInstall() {
        source = checkInstallSource();
        boolean needconfirm = sharedPreferences.getBoolean("needconfirm", true);
        show_notification = sharedPreferences.getBoolean("show_notification", false);
        boolean allowsource = sourceSp.getBoolean(source[0], false);
        String validApkPath = preInstallGetValidApkPath();
        if (validApkPath == null) {
            showToast0(R.string.tip_failed_prase);
            finish();
        } else {
            cachePath = validApkPath;
            Log.e("cachePath", cachePath + "");

            if (needconfirm) {
                if (!source[1].equals(ILLEGALPKGNAME) && allowsource) {
                    startInstall(validApkPath);
                    finish();
                } else {
                    String[] version = AppInfoUtil.getApplicationVersion(this, apkinfo[1]);

                    alertDialogMessage = new StringBuilder();
                    alertDialogMessage
                            .append(nl)
                            .append(
                                    String.format(
                                            getString(R.string.message_name),
                                            apkinfo[0]
                                    )
                            )
                            .append(nl)
                            .append(
                                    String.format(
                                            getString(R.string.message_packagename),
                                            apkinfo[1]
                                    )
                            )
                            .append(nl)
                            .append(
                                    String.format(
                                            getString(R.string.message_version),
                                            apkinfo[2],
                                            apkinfo[3]
                                    )
                            )
                            .append(nl);

                    if (version != null) {
                        alertDialogMessage.append(
                                String.format(
                                        getString(R.string.message_version_existed),
                                        version[0],
                                        version[1]
                                )
                        )
                                .append(nl);
                    }

                    alertDialogMessage
                            .append(
                                    String.format(
                                            getString(R.string.message_size),
                                            apkinfo[4]
                                    )
                            )
                            .append(nl);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.dialog_install_title));

                    builder.setMessage(alertDialogMessage);
                    View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
                    builder.setView(checkBoxView);
                    CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
                    if (source[1].equals(ILLEGALPKGNAME)) {
                        checkBox.setText(getString(R.string.checkbox_installsource_unkonwn));
                        checkBox.setEnabled(false);
                    } else {
                        checkBox.setText(String.format(getString(R.string.checkbox_always_allow), source[1]));
                    }

                    builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        cachePath = null;
                        if (!source[1].equals(ILLEGALPKGNAME)) {
                            editor = sourceSp.edit();
                            editor.putBoolean(source[0], checkBox.isChecked());
                            editor.apply();
                        }
                        startInstall(validApkPath);
                        finish();
                    });
                    builder.setNegativeButton(android.R.string.no, (dialog, which) -> finish());

                    builder.setCancelable(false);
                    alertDialog = builder.create();
                    OpUtil.showAlertDialog(this, alertDialog);
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
                }
            } else {
                startInstall(validApkPath);
                finish();
            }
        }

    }

    private String[] checkInstallSource() {
        final String fromPkgLabel;
        final String fromPkgName;

        String referrer = PraseContentUtil.reflectGetReferrer(this);
        if (referrer != null) {
            fromPkgName = referrer;
        } else {
            Uri referrerUri = getReferrer();
            if (referrerUri == null || !"android-app".equals(referrerUri.getScheme())) {
                fromPkgLabel = ILLEGALPKGNAME;
                fromPkgName = ILLEGALPKGNAME;
                return new String[]{fromPkgName, fromPkgLabel};
            } else {
                fromPkgName = referrerUri.getEncodedSchemeSpecificPart().substring(2);
            }
        }
        String refererPackageLabel =
                AppInfoUtil.getApplicationLabel(this, fromPkgName);
        if (AppInfoUtil.UNINSTALLED.equals(refererPackageLabel)) {
            fromPkgLabel = ILLEGALPKGNAME;
        } else {
            fromPkgLabel = refererPackageLabel;
        }
        return new String[]{fromPkgName, fromPkgLabel};
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        Log.e("resultData", "o " + resultData);
        if (requestCode == PICK_APK_FILE
                && resultCode == Activity.RESULT_OK
                && resultData != null) {

            uri = resultData.getData();
            initFromUri();
        } else {
            Toast.makeText(this, R.string.tip_failed_get_content, Toast.LENGTH_SHORT).show();
            finish();
        }

    }


    // 用 ACTION_GET_CONTENT 而不是ACTION_OPEN_DOCUMENT 可支持更多App
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 因为设备上Apk文件的后缀并不一定是"apk"，所以不使用"application/vnd.android.package-archive"
        intent.setType("application/*");
        startActivityForResult(intent, PICK_APK_FILE);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (istemp && (cachePath != null)) {
            OpUtil.deleteSingleFile(new File(cachePath));
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (checkPermission()) {
            initInstall();
        } else {
            requestPermission();
        }
    }

    private String preInstallGetValidApkPath() {
        String getPath = null;
        Log.e("uri", uri + "");
        if (uri != null) {
            Log.e("--getData--", uri + "");
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                getPath = uri.getPath();
            } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {

                File file = PraseContentUtil.getFile(this, uri);
                if (file != null) {
                    getPath = file.getPath();
                } else {
                    istemp = true;
                    getPath = OpUtil.createApkFromUri(this, uri).getPath();
                }
                Log.e("getPathfromContent", getPath + "");
            }
            if (getPath != null) {
                apkinfo = AppInfoUtil.getApkInfo(this, getPath);
                if (apkinfo != null) {
                    return getPath;
                }
            }

        }

        if (getPath != null && istemp) {
            OpUtil.deleteSingleFile(new File(getPath));
        }
        return null;
    }


    protected abstract void startInstall(String installApkFile);

    protected abstract void startUninstall(String uninstallPkgname);

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0x233);
    }

    private boolean checkPermission() {
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return (permissionRead == 0);
    }

    void copyErr(String err) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, err);
        Objects.requireNonNull(clipboard).setPrimaryClip(clipData);
    }

    void showToast0(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    private void showToast0(final int stringId) {
        runOnUiThread(() -> Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show());
    }

    void showToast1(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }

    void showToast1(final int stringId) {
        runOnUiThread(() -> Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show());
    }

    void deleteCache() {
        if (istemp) {
            OpUtil.deleteSingleFile(installApkFile);
        }
    }

}