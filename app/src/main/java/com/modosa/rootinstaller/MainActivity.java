package com.modosa.rootinstaller;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private boolean istemp = false;
    private String apkPath = null;
    private Uri uri;
    boolean isenforce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int version = Build.VERSION.SDK_INT;
        isenforce ="1".equals(ShellUtils.execWithRoot("getenforce")+"");
        Log.e("isenforce",isenforce+"");
        Intent intent = getIntent();
        uri = intent.getData();
        if (uri != null) {
            Log.e("--getData--", uri + "");
            String CONTENT = "content://";
            String FILE = "file://";
            if (uri.toString().contains(FILE)) {
                apkPath = uri.getPath();
                if (!(version < Build.VERSION_CODES.M)) {
                    confirmPermission();
                }
            } else if (uri.toString().contains(CONTENT)) {
                apkPath = createApkFromUri(this);
            }

        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startInstall();
        finish();
    }

    private void startInstall() {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            final File apkFile = new File(apkPath);
            showToast(getString(R.string.start_install) + apkFile.getPath());
            if(isenforce){
                ShellUtils.execWithRoot("setenforce 0");
            }
            new Thread(() -> {
                if (ShellUtils.execWithRoot("pm install -r --user 0 \"" + apkPath + "\"") == 0) {
                    showToast(getApkName(apkPath) + " " + getString(R.string.success_install));
                } else {
                    showToast(getApkName(apkPath) + " " + getString(R.string.failed_install));
                }
                if (istemp) {
                    deleteSingleFile(apkFile);
                }
                finish();
            }).start();
        } else {
            showToast(getString(R.string.failed_read));
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0x233);
    }

    private void confirmPermission() {
        int permissionWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean judge = (permissionWrite == 0);
        if (!judge) {
            requestPermission();
        }
    }


    private String getApkName(String apkSourcePath) {
        if (apkSourcePath == null) {
            return null;
        }
        PackageManager pm = getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkSourcePath, PackageManager.GET_ACTIVITIES);

        if (pkgInfo != null) {
            pkgInfo.applicationInfo.sourceDir = apkSourcePath;
            pkgInfo.applicationInfo.publicSourceDir = apkSourcePath;
            return pm.getApplicationLabel(pkgInfo.applicationInfo).toString();
        }
        return "";
    }

    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show());
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

    private void deleteSingleFile(File file) {
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.e("--DELETE--", "deleteSingleFile" + file.getAbsolutePath() + " OK！");
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

}
