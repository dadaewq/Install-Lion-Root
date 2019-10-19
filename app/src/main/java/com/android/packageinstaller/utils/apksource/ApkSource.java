package com.android.packageinstaller.utils.apksource;

import java.io.InputStream;

public interface ApkSource extends AutoCloseable {

    boolean nextApk();

    InputStream openApkInputStream() throws Exception;

    long getApkLength();

    @Override
    default void close() {

    }
}
