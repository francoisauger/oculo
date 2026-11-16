package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;
import android.os.Build.VERSION;

import com.liskovsoft.leanbackassistant.utils.SafeWorkManager;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;

/**
 * Oculo: no Android TV home-screen channels (32.74-18). Upstream's "Update channels" periodic work
 * (leanbackassistant's UpdateChannelsWorker, WorkManager, every 15 min, even with the app closed: subscriptions,
 * recommendations and history, about 6 YouTube requests per run) publishes channels that the TCL's Google TV home
 * never shows ({@code browsable=0}). With {@code R.bool.oculo_tv_channels} false (oculo flavor), the work is never
 * scheduled and an instance scheduled by an older version is cancelled at startup; the flavor manifest also removes
 * its boot / screen-on receiver. Upstream default: true (unchanged behaviour).
 */
public final class OculoBackground {
    private static final String TAG = "OculoBackground";
    /** UpdateChannelsWorker.WORK_NAME (private upstream) */
    private static final String CHANNELS_WORK_NAME = "Update channels";

    private OculoBackground() {
    }

    public static boolean isTvChannelsEnabled(Context context) {
        return context == null || context.getResources().getBoolean(R.bool.oculo_tv_channels);
    }

    /**
     * Replaces Utils.updateChannels() when the channels are off: cancels the periodic work. Returns true when handled.
     */
    public static boolean cancelTvChannelsIfDisabled(Context context) {
        if (isTvChannelsEnabled(context)) {
            return false;
        }

        if (VERSION.SDK_INT >= 23) {
            try {
                SafeWorkManager.getInstance(context).cancelUniqueWork(CHANNELS_WORK_NAME);
                Log.d(TAG, "TV home channels off: \"%s\" work cancelled", CHANNELS_WORK_NAME);
            } catch (RuntimeException e) {
                Log.e(TAG, "Can't cancel the channels work: %s", e.getMessage());
            }
        }

        return true;
    }
}
