package com.modosa.rootinstaller.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
                startPickFile();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - exitTime) < 2000) {
            super.onBackPressed();
        } else {
            showMyToast0(R.string.tip_exit);
            exitTime = currentTime;
        }
    }

    private void showMyToast0(final int stringId) {
        runOnUiThread(() -> Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show());
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

    private void showDialogClearCache() {
        String cachePath = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath();
        String cacheSize = FileSizeUtil.getAutoFolderOrFileSize(cachePath);
        if (EMPTY_SIZE.equals(cacheSize)) {
            showMyToast0(R.string.tip_empty_cache);
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

    private void startPickFile() {
        Intent intent = new Intent(OpUtil.MODOSA_ACTION_PICK_FILE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this, InstallActivity.class);
        startActivity(intent);
    }

}
