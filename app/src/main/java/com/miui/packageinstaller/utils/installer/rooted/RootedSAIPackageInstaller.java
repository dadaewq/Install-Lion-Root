package com.miui.packageinstaller.utils.installer.rooted;

import android.content.Context;

import com.miui.packageinstaller.R;
import com.miui.packageinstaller.utils.installer.ShellSAIPackageInstaller;
import com.miui.packageinstaller.utils.shell.Shell;
import com.miui.packageinstaller.utils.shell.SuShell;

public class RootedSAIPackageInstaller extends ShellSAIPackageInstaller {
    private static RootedSAIPackageInstaller sInstance;

    private RootedSAIPackageInstaller(Context c) {
        super(c);
        sInstance = this;
    }

    public static RootedSAIPackageInstaller getInstance(Context c) {
        synchronized (RootedSAIPackageInstaller.class) {
            return sInstance != null ? sInstance : new RootedSAIPackageInstaller(c);
        }
    }

    @Override
    protected Shell getShell() {
        return SuShell.getInstance();
    }

    @Override
    protected String getInstallerName() {
        return "Rooted";
    }

    @Override
    protected String getShellUnavailableMessage() {
        return getContext().getString(R.string.installer_error_root_no_root);
    }
}
