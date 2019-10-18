package com.miui.packageinstaller.utils.installer;


import com.miui.packageinstaller.utils.apksource.ApkSource;
import com.miui.packageinstaller.utils.apksource.DefaultApkSource;
import com.miui.packageinstaller.utils.filedescriptor.FileDescriptor;
import com.miui.packageinstaller.utils.filedescriptor.NormalFileDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApkSourceBuilder {


    private boolean mSourceSet;
    private List<File> mApkFiles;


    public ApkSourceBuilder fromApkFiles(List<File> apkFiles) {
        ensureSourceSetOnce();
        mApkFiles = apkFiles;
        return this;
    }


    public ApkSource build() {
        ApkSource apkSource;

        if (mApkFiles != null) {
            List<FileDescriptor> apkFileDescriptors = new ArrayList<>(mApkFiles.size());
            for (File apkFile : mApkFiles) {
                apkFileDescriptors.add(new NormalFileDescriptor(apkFile));
            }

            apkSource = new DefaultApkSource(apkFileDescriptors);
        } else {
            throw new IllegalStateException("No source set");
        }


        return apkSource;
    }

    private void ensureSourceSetOnce() {
        if (mSourceSet) {
            throw new IllegalStateException("Source can be only be set once");
        }
        mSourceSet = true;
    }
}
