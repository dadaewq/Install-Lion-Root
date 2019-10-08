package com.modosa.rootinstaller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.modosa.rootinstaller.utils.ApplicationLabelUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;


/**
 * @author dadaewq
 */
public abstract class AbstractInstallActivity extends Activity {
    private static final String ILLEGALPKGNAME = "Fy^&IllegalPN*@!128`+=：:,.[";
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private final String nl = System.getProperty("line.separator");
    boolean istemp = false;
    String[] apkinfo;
    private Uri uri;
    private boolean needrequest;
    private SharedPreferences sourceSp;
    private SharedPreferences.Editor editor;
    private AlertDialog alertDialog;
    private String cachePath;
    private String pkgname;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (Intent.ACTION_UNINSTALL_PACKAGE.equals(action) || Intent.ACTION_DELETE.equals(action)) {
            pkgname = Objects.requireNonNull(getIntent().getData()).getEncodedSchemeSpecificPart();
            if (pkgname == null) {
                showToast(getString(R.string.failed_prase));
                finish();
            } else {
                initUninstall();
            }
        } else {
            uri = getIntent().getData();

            assert uri != null;
            needrequest = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (ContentResolver.SCHEME_FILE.equals(uri.getScheme()));

            sourceSp = getSharedPreferences("allowsource", Context.MODE_PRIVATE);

            init();
        }

    }

    private void initUninstall() {

        PackageManager pm = getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(pkgname, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (applicationInfo == null) {
            showToast(getString(R.string.failed_prase));
            finish();
        } else {

            String apkPath = applicationInfo.sourceDir;
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            StringBuilder alertDialogMessage = new StringBuilder();
            alertDialogMessage.append(getString(R.string.message_name))
                    .append(pm.getApplicationLabel(applicationInfo).toString())
                    .append(nl)
                    .append(getString(R.string.message_packagename))
                    .append(pkgname)
                    .append(nl);
            if (pkgInfo != null) {
                alertDialogMessage.append(getString(R.string.message_versionname))
                        .append(pkgInfo.versionName)
                        .append(nl)
                        .append(getString(R.string.message_versioncode))
                        .append(pkgInfo.versionCode)
                        .append(nl)
                        .append(nl)
                        .append(getString(R.string.message_uninstalConfirm));
            } else {

                alertDialogMessage.append(nl)
                        .append(getString(R.string.message_uninstalConfirm));
            }


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
    }

    private void init() {
        String[] source = checkInstallSource();
        boolean allowsource = sourceSp.getBoolean(source[0], false);
        String apkPath = preInstall();
        apkinfo = getApkPkgInfo(apkPath);
        cachePath = apkPath;
        if (allowsource) {
            startInstall(apkPath);
            finish();
        } else {

            StringBuilder alertDialogMessage = new StringBuilder();
            alertDialogMessage.append(getString(R.string.message_name))
                    .append(apkinfo[0])
                    .append(nl)
                    .append(getString(R.string.message_packagename))
                    .append(apkinfo[1])
                    .append(nl)
                    .append(getString(R.string.message_versionname))
                    .append(apkinfo[2])
                    .append(nl)
                    .append(getString(R.string.message_versioncode))
                    .append(apkinfo[3])
                    .append(nl);


            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.dialog_install_title));

            builder.setMessage(alertDialogMessage);
            View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
            builder.setView(checkBoxView);
            CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
            checkBox.setText(String.format(getString(R.string.always_allow), source[1]));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    editor = sourceSp.edit();
                    editor.putBoolean(source[0], true);
                    editor.apply();
                }
            });
            builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                cachePath = null;
                startInstall(apkPath);
                finish();
            });
            builder.setNegativeButton(android.R.string.no, (dialog, which) -> finish());


            builder.setCancelable(false);
            alertDialog = builder.show();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
        }

    }

    private String[] checkInstallSource() {

        final String fromPkgLabel;
        final String fromPkgName;
        if (Build.VERSION.SDK_INT >= 22) {
            Uri referrerUri = getReferrer();
            if (referrerUri == null || !"android-app".equals(referrerUri.getScheme())) {
                fromPkgLabel = ILLEGALPKGNAME;
                fromPkgName = ILLEGALPKGNAME;
            } else {
                fromPkgName = referrerUri.getEncodedSchemeSpecificPart().substring(2);
                String refererPackageLabel =
                        ApplicationLabelUtils.getApplicationLabel(this, null, null, fromPkgName);
                if (refererPackageLabel.equals("已卸载")) {
                    fromPkgLabel = ILLEGALPKGNAME;
                } else {
                    fromPkgLabel = refererPackageLabel;
                }
            }
        } else {
            fromPkgLabel = ILLEGALPKGNAME;
            fromPkgName = ILLEGALPKGNAME;
        }
        return new String[]{fromPkgName, fromPkgLabel};
    }

    @Override
    public void onResume() {
        super.onResume();
        if (needrequest) {
            confirmPermission();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (istemp && (cachePath != null)) {
            deleteSingleFile(new File(cachePath));
        }
        alertDialog.dismiss();
    }


    private String preInstall() {
        String apkPath = null;
        if (uri != null) {
            Log.e("--getData--", uri + "");
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                confirmPermission();
                apkPath = uri.getPath();
            } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                apkPath = createApkFromUri(this);
            } else {
                showToast(getString(R.string.failed_prase));
            }
            return apkPath;
        } else {
            finish();
            return "";
        }
    }


    protected abstract void startInstall(String apkPath);

    protected abstract void startUninstall(String pkgname);

    private void requestPermission() {
        Log.e("来了", "requestPermission: ");
        ActivityCompat.requestPermissions(this, permissions, 0x233);
    }

    private void confirmPermission() {
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean judge = (permissionRead == 0);
        if (!judge) {
            requestPermission();
        }
    }


    void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    private String createApkFromUri(Context context) {
        istemp = true;
        File tempFile = new File(context.getExternalCacheDir(), System.currentTimeMillis() + ".apk");
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is != null) {
                OutputStream fos = new FileOutputStream(tempFile);
                byte[] buf = new byte[4096 * 1024];
                int ret;
                while ((ret = is.read(buf)) != -1) {
                    fos.write(buf, 0, ret);
                    fos.flush();
                }
                fos.close();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile.getAbsolutePath();
    }


    void deleteSingleFile(File file) {
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.e("-DELETE-", "==>" + file.getAbsolutePath() + " OK！");
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    String[] getApkPkgInfo(String apkPath) {
        if (apkPath == null) {
            return null;
        } else {
            PackageManager pm = this.getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (pkgInfo != null) {
                pkgInfo.applicationInfo.sourceDir = apkPath;
                pkgInfo.applicationInfo.publicSourceDir = apkPath;

                return new String[]{pm.getApplicationLabel(pkgInfo.applicationInfo).toString(), pkgInfo.packageName, pkgInfo.versionName, pkgInfo.versionCode + ""};
            }
            return null;
        }
    }

}
