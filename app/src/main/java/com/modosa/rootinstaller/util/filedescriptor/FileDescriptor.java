package com.modosa.rootinstaller.util.filedescriptor;

import java.io.InputStream;

public interface FileDescriptor {

    long length();

    InputStream open() throws Exception;

}
