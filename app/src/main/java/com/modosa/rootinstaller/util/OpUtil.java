package com.modosa.rootinstaller.util;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Window;

import com.modosa.rootinstaller.R;


/**
 * @author dadaewq
 */
public class OpUtil {

    public static void showAlertDialog(Context context, AlertDialog alertDialog) {
        Window window = alertDialog.getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(R.color.Background, null)));
            } else {
                window.setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(R.color.Background)));
            }

        }
        alertDialog.show();
    }

    public static boolean checkRoot() {
        String[] resultCheckRoot;
        resultCheckRoot = ShellUtil.execWithRoot("exit");
        return "0".equals(resultCheckRoot[3]);
    }

    public static boolean getComponentState(Context context, ComponentName componentName) {
        PackageManager pm = context.getPackageManager();
        return (pm.getComponentEnabledSetting(componentName) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(componentName) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
    }

    public static void setComponentState(Context context, ComponentName componentName, boolean isenable) {
        PackageManager pm = context.getPackageManager();
        int flag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        if (isenable) {
            flag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        }

        pm.setComponentEnabledSetting(componentName, flag, PackageManager.DONT_KILL_APP);
    }


}
