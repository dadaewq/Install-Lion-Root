package com.android.packageinstaller.utils.filedescriptor;

import java.io.InputStream;

public interface FileDescriptor {

    long length();

    InputStream open() throws Exception;

}
