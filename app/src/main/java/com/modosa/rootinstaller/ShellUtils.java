package com.modosa.rootinstaller;
// Copyright (C) 2018 Bave Lee
// This file is part of Quick-Android.
// https://github.com/Crixec/Quick-Android
//
// Quick-Android is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Quick-Android is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class ShellUtils {
    private static final List<Result> RESULTS = new ArrayList<>();
    private static boolean DEBUG = true;

    private static int exec(final String sh, final List<String> cmds, final Result result) {
        Process process;
        DataOutputStream stdin = null;
        OutputReader stdout = null;
        OutputReader stderr = null;
        int resultCode = -1;
        try {
            process = Runtime.getRuntime().exec(sh);
            stdin = new DataOutputStream(process.getOutputStream());
            if (RESULTS.size() > 0) {
                stdout = new OutputReader(new BufferedReader(new InputStreamReader(process.getInputStream())),
                        text -> {
                            for (Result res : RESULTS) {
                                LOG("[STDOUT] " + text);
                                res.onStdout(text);
                            }
                        });
                stderr = new OutputReader(new BufferedReader(new InputStreamReader(process.getErrorStream())),
                        text -> {
                            for (Result res : RESULTS) {
                                LOG("[STDERR] " + text);
                                res.onStdout(text);
                            }
                        });
                stdout.start();
                stderr.start();
            }
            for (String cmd : cmds) {
                if (result != null) {
                    result.onCommand(cmd);
                    LOG("[COMMAND] " + cmd);
                }
                stdin.write(cmd.getBytes());
                stdin.writeBytes("\n");
                stdin.flush();
            }
            stdin.writeBytes("exit $?\n");
            stdin.flush();
            resultCode = process.waitFor();
            for (Result res : RESULTS) {
                LOG("[RETURN] " + resultCode);
                res.onFinish(resultCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            safeCancel(stderr);
            safeCancel(stdout);
            safeClose(stdout);
            safeClose(stderr);
            safeClose(stdin);
        }
        return resultCode;
    }

//    public static void setDebug(boolean DEBUG) {
//        ShellUtils.DEBUG = DEBUG;
//    }
//
//    public static void clearResultCallbacks() {
//        RESULTS.clear();
//    }
//
//    public static void addResultCallback(Result result) {
//        RESULTS.add(result);
//    }

    private static void LOG(String text) {
        String TAG = "ShellUtils";
        if (DEBUG) {
            android.util.Log.i(TAG, text);
        }
    }

    private static void safeCancel(OutputReader reader) {
        try {
            if (reader != null) {
                reader.cancel();
            }
        } catch (Exception ignored) {

        }
    }

    private static void safeClose(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ignored) {

        }
    }

    private static int exec(final List<String> cmds, final Result result, final boolean isRoot) {
        String sh = isRoot ? "su" : "sh";
        return exec(sh, cmds, result);
    }

//    public static int exec(final List<String> cmds, final boolean isRoot) {
//        return exec(cmds, null, isRoot);
//    }

//    public static int exec(final String cmd, boolean isRoot) {
//        return exec(cmd, null, isRoot);
//    }

    private static int exec(final String cmd, final Result result, boolean isRoot) {
        List<String> cmds = new ArrayList<>();
        cmds.add(cmd);
        return exec(cmds, result, isRoot);
    }

//    public static int exec(final String cmd) {
//        return exec(cmd, null, false);
//    }

    public static int execWithRoot(final String cmd) {
        return exec(cmd, null, true);
    }

//    public static int execWithRoot(final String cmd, final Result result) {
//        return exec(cmd, result, true);
//    }

    interface Result {
        void onStdout(String text);

//        void onStderr(String text);

        void onCommand(String command);

        void onFinish(int resultCode);
    }

    private interface Output {
        void output(String text);
    }

    static class OutputReader extends Thread implements Closeable {
        private final Output output;
        private final BufferedReader reader;
        private boolean isRunning;

        private OutputReader(BufferedReader reader, Output output) {
            this.output = output;
            this.reader = reader;
            this.isRunning = true;
        }

        @Override
        public void close() {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public void run() {
            super.run();
            String line;
            while (isRunning) {
                try {
                    line = reader.readLine();
                    if (line != null) {
                        output.output(line);
                    }
                } catch (IOException ignored) {
                }
            }
        }

        private void cancel() {
            synchronized (this) {
                isRunning = false;
                this.notifyAll();
            }
        }
    }
}