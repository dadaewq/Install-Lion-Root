package com.modosa.rootinstaller.utils.installer.rooted;

import android.content.Context;

import com.modosa.rootinstaller.R;
import com.modosa.rootinstaller.utils.installer.ShellSAIPackageInstaller;
import com.modosa.rootinstaller.utils.shell.Shell;
import com.modosa.rootinstaller.utils.shell.SuShell;

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
