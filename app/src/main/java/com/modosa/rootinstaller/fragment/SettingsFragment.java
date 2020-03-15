package com.modosa.rootinstaller.fragment;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.modosa.rootinstaller.R;
import com.modosa.rootinstaller.util.OpUtil;
import com.modosa.rootinstaller.util.shell.SuShell;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;

/**
 * @author dadaewq
 */
public class SettingsFragment extends PreferenceFragment {
    private SwitchPreference needconfirm;
    private SwitchPreference show_notification;
    private Preference show_root_state;

    private SharedPreferences sharedPreferences;
    private MyHandler mHandler;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
        sharedPreferences = getPreferenceManager().getSharedPreferences();
        addPreferencesFromResource(R.xml.pref_settings);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove all Runnable and Message.
        mHandler.removeCallbacksAndMessages(null);
    }

    private void init() {

        needconfirm = (SwitchPreference) findPreference("needconfirm");
        show_notification = (SwitchPreference) findPreference("show_notification");
        show_root_state = findPreference("show_root_state");
        show_root_state.setOnPreferenceClickListener(preference -> {

            Executors.newSingleThreadExecutor().execute(() -> {
                Message msg = mHandler.obtainMessage();
                if (SuShell.getInstance().isAvailable()) {
                    msg.arg1 = 2;
                } else {
                    msg.arg1 = -1;
                }
                mHandler.sendMessage(msg);
            });
            return true;
        });


        Executors.newSingleThreadExecutor().execute(() -> {
            Message msg = mHandler.obtainMessage();
            if (SuShell.getInstance().isAvailable()) {
                msg.arg1 = 1;
            } else {
                msg.arg1 = -1;
            }
            mHandler.sendMessage(msg);
        });

    }

    private void refreshStatus() {
        show_notification.setChecked(sharedPreferences.getBoolean("show_notification", false));
        needconfirm.setChecked(sharedPreferences.getBoolean("needconfirm", true));
    }

    private static class MyHandler extends Handler {

        private final WeakReference<SettingsFragment> wrFragment;

        MyHandler(SettingsFragment fragment) {
            this.wrFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (wrFragment.get() == null) {
                return;
            }
            SettingsFragment settingsFragment = wrFragment.get();
            switch (msg.arg1) {
                case 1:
                    settingsFragment.show_root_state.setTitle(R.string.title_show_root_yes);
                    break;
                case 2:
                    settingsFragment.show_root_state.setTitle(R.string.title_show_root_yes);
                    OpUtil.showToast0(settingsFragment.getActivity(), R.string.title_show_root_yes);
                    break;
                case -1:
                    settingsFragment.show_root_state.setTitle(R.string.title_show_root_no_click2request);
                    OpUtil.showToast0(settingsFragment.getActivity(), R.string.installer_error_root_no_root);
                    break;
                default:

            }
        }
    }


}
