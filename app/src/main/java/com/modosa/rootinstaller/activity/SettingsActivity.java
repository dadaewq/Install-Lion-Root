package com.modosa.rootinstaller.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.RequiresApi;

import com.modosa.rootinstaller.R;
import com.modosa.rootinstaller.util.FileSizeUtil;
import com.modosa.rootinstaller.util.OpUtil;

import java.util.Objects;


/**
 * @author dadaewq
 */
public class SettingsActivity extends Activity {

    // Request code for selecting a PDF document.
    private static final int PICK_APK_FILE = 2;
    private static final String EMPTY_SIZE = "0B";
    private long exitTime = 0;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.HideIcon:
                showDialogHideIcon();
                break;
            case R.id.ClearAllowedList:
                showDialogClearAllowedList();
                break;
            case R.id.ClearCache:
                showDialogClearCache();
                break;
            case R.id.InstallFromSAF:
                openFile();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == PICK_APK_FILE
                && resultCode == Activity.RESULT_OK
                && resultData != null) {

            Uri installUri = resultData.getData();

            Intent intent = new Intent(Intent.ACTION_VIEW, installUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClass(this, InstallActivity.class);
            startActivity(intent);
            // Perform operations on the document using its URI.
        } else {
            Toast.makeText(this, R.string.tip_failed_get_content, Toast.LENGTH_SHORT).show();
        }
    }


    // 用 ACTION_GET_CONTENT 而不是ACTION_OPEN_DOCUMENT 可支持更多App
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 因为设备上Apk文件的后缀并不一定是"apk"，所以不使用"application/vnd.android.package-archive"
        intent.setType("application/*");
        startActivityForResult(intent, PICK_APK_FILE);
//        startActivity(Intent.createChooser(intent,null));
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            long intervals = 2000;
            if ((System.currentTimeMillis() - exitTime) > intervals) {
                Toast.makeText(this, getString(R.string.tip_exit), Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showDialogHideIcon() {

        View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.HideIcon);

        ComponentName mainComponentName = new ComponentName(this, "com.modosa.rootinstaller.activity.MainActivity");

        boolean isEnabled = OpUtil.getComponentState(this, mainComponentName);

        checkBox.setChecked(!isEnabled);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.HideIcon)
                .setMessage(R.string.message_HideIcon)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> OpUtil.setComponentState(this, mainComponentName,
                        !checkBox.isChecked()));

        AlertDialog alertDialog = builder.create();

        OpUtil.showAlertDialog(this, alertDialog);

    }


    private void showDialogClearCache() {
        String cachePath = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath();
        String cacheSize = FileSizeUtil.getAutoFolderOrFileSize(cachePath);
        if (EMPTY_SIZE.equals(cacheSize)) {
            Toast.makeText(this, R.string.tip_empty_cache, Toast.LENGTH_SHORT).show();
        } else {
            Log.e("cacheSize", cacheSize);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.ClearCache)
                    .setMessage(String.format(getString(R.string.message_ClearCache), cacheSize))
                    .setNeutralButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> OpUtil.deleteDirectory(cachePath));

            AlertDialog alertDialog = builder.create();
            OpUtil.showAlertDialog(this, alertDialog);
        }
    }

    private void showDialogClearAllowedList() {

        View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.checkbox_ClearAllowedList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.ClearAllowedList)
                .setMessage(R.string.message_ClearAllowedList)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    SharedPreferences.Editor editor = getSharedPreferences("allowsource", Context.MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();
                });

        AlertDialog alertDialog = builder.create();


        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));

        OpUtil.showAlertDialog(this, alertDialog);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

    }


}
