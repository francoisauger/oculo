package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.prefs.common.DataSaverBase;

/**
 * Oculo: Home behaviour settings. Defaults come from resources (common/src/main/res/values/oculo.xml keeps
 * upstream's behaviour; the oculo flavor overrides them in smarttubetv/src/oculo/res/values/oculo_config.xml).
 */
public class OculoData extends DataSaverBase {
    private static final int INDEX_HOME_ROWS_FILTER = 0;
    private static final int INDEX_HOME_HIDE_WATCHED = 1;
    private static final int INDEX_HOME_CONTINUE_WATCHING = 2;
    private static OculoData sInstance;
    private final Context mContext;

    private OculoData(Context context) {
        super(context);
        mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
    }

    public static OculoData instance(Context context) {
        if (sInstance == null) {
            sInstance = new OculoData(context);
        }

        return sInstance;
    }

    /**
     * Home is reloaded on return to the app when older than this.
     */
    public long getHomeRefreshIntervalMs() {
        return mContext.getResources().getInteger(R.integer.oculo_home_refresh_minutes) * 60L * 1_000;
    }

    /**
     * Hide Home rows whose title matches the hardcoded news/sports list (upstream behaviour).
     */
    public boolean isHomeRowsFilterEnabled() {
        return getBoolean(INDEX_HOME_ROWS_FILTER, mContext.getResources().getBoolean(R.bool.oculo_home_rows_filter_default));
    }

    public void setHomeRowsFilterEnabled(boolean enabled) {
        setBoolean(INDEX_HOME_ROWS_FILTER, enabled);
    }

    /**
     * Hide videos watched more than 95% (or opened then closed) in Home's first row (upstream behaviour).
     */
    public boolean isHomeHideWatchedEnabled() {
        return getBoolean(INDEX_HOME_HIDE_WATCHED, mContext.getResources().getBoolean(R.bool.oculo_home_hide_watched_default));
    }

    public void setHomeHideWatchedEnabled(boolean enabled) {
        setBoolean(INDEX_HOME_HIDE_WATCHED, enabled);
    }

    /**
     * Show the « Continue watching » row (OculoContinueWatching) as Home's second row.
     */
    public boolean isHomeContinueWatchingEnabled() {
        return getBoolean(INDEX_HOME_CONTINUE_WATCHING, mContext.getResources().getBoolean(R.bool.oculo_home_continue_watching_default));
    }

    public void setHomeContinueWatchingEnabled(boolean enabled) {
        setBoolean(INDEX_HOME_CONTINUE_WATCHING, enabled);
    }
}
