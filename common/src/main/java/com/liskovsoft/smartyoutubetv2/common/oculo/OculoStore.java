package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Oculo (32.74-18): Oculo's own data files written off the main thread. {@code AppPrefs.setData()} writes its file
 * synchronously on the calling thread (the main thread for Oculo's watch log, hidden list, history cache and seen
 * channels). Same files as AppPrefs ({@code files/app_prefs/<key>}), so {@code AppPrefs.getData()} still reads them
 * (and data written by older versions is read as before); only Oculo's own keys go through here, so AppPrefs' write
 * cache (hash per key) is never involved.
 *
 * <p>One background thread, writes applied in order; each write goes to a temporary file renamed over the target,
 * so a reader never sees a half-written file. An empty string empties the file (as {@code setData("")} did).</p>
 */
public final class OculoStore {
    private static final String TAG = "OculoStore";
    /** AppPrefs.PREFS_DIR */
    private static final String PREFS_DIR = "app_prefs";
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static ExecutorService sExecutor;

    private OculoStore() {
    }

    /** Queues the write of {@code data} (null = empty) to AppPrefs' file for {@code key}. */
    public static void write(Context context, String key, String data) {
        if (context == null || key == null) {
            return;
        }
        File dir = new File(context.getFilesDir(), PREFS_DIR);
        String content = data != null ? data : "";
        executor().execute(() -> writeNow(dir, key, content));
    }

    private static void writeNow(File dir, String key, String content) {
        File target = new File(dir, key);
        File temp = new File(dir, key + ".oculo-tmp");
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new IOException("cannot create " + dir);
            }
            try (OutputStream out = new FileOutputStream(temp)) {
                out.write(content.getBytes(UTF_8));
            }
            if (!temp.renameTo(target)) {
                throw new IOException("cannot rename to " + target);
            }
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "write failed: " + key, e);
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private static synchronized ExecutorService executor() {
        if (sExecutor == null) {
            sExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(() -> {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    runnable.run();
                }, TAG);
                thread.setDaemon(true);
                return thread;
            });
        }
        return sExecutor;
    }
}
