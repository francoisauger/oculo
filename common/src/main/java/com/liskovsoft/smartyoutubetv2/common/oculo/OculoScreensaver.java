package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;

import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;

/**
 * Oculo (32.74-21): idle screen dimming (SmartTube's screensaver: black overlay after a delay without a key press)
 * after 5 minutes by default instead of upstream's 60 s ({@code R.integer.oculo_screensaver_timeout_default_sec}:
 * 60 upstream in common's {@code oculo.xml}, 300 in the oculo flavor).
 *
 * <p>GeneralData stores all its fields in one string, rewritten whole on any change, so a stored 60 s cannot be told
 * apart from a user's choice of « 1 min ». Migration, once per install: a stored 60 s becomes the new default; any
 * other stored value (Never, seconds, 2 to 15 min) is kept. After that, any value is the user's.
 */
public final class OculoScreensaver {
    private static final String TAG = "OculoScreensaver";
    private static final int UPSTREAM_DEFAULT_MS = 60 * 1_000;
    private static final String DONE_KEY = "oculo_screensaver_default_migrated";

    private OculoScreensaver() {
    }

    public static int defaultTimeoutMs(Context context) {
        return context.getResources().getInteger(R.integer.oculo_screensaver_timeout_default_sec) * 1_000;
    }

    /**
     * Called by GeneralData.restoreState() with the stored (or default) value. Returns the value to use; when it
     * differs from {@code storedMs}, the caller persists it.
     */
    public static int migrate(Context context, int storedMs) {
        if (context == null) {
            return storedMs;
        }
        int defaultMs = defaultTimeoutMs(context);
        if (defaultMs == UPSTREAM_DEFAULT_MS) {
            return storedMs;
        }
        AppPrefs prefs = AppPrefs.instance(context);
        if (prefs.getBoolean(DONE_KEY, false)) {
            return storedMs;
        }
        prefs.putBoolean(DONE_KEY, true);
        if (storedMs == UPSTREAM_DEFAULT_MS) {
            Log.d(TAG, "Screen dimming: 60 s (old default) -> %s s", defaultMs / 1_000);
            return defaultMs;
        }
        return storedMs;
    }
}
