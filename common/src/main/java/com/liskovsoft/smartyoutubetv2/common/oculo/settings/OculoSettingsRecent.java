package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.oculo.OculoStore;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo (0.11.0): « Recent », the last three settings changed in Oculo's Settings (row keys, most recent first), shown
 * as shortcuts at the top of the Settings screen. Kept in the app prefs ({@code files/app_prefs/oculo_settings_recent},
 * written off the main thread by {@link OculoStore}).
 */
public final class OculoSettingsRecent {
    private static final String KEY = "oculo_settings_recent";
    private static final String SEPARATOR = "\n";
    public static final int MAX = 3;
    private static List<String> sKeys;

    private OculoSettingsRecent() {
    }

    public static synchronized List<String> get(Context context) {
        if (sKeys == null) {
            sKeys = new ArrayList<>();
            String data = context != null ? AppPrefs.instance(context).getData(KEY) : null;

            if (!TextUtils.isEmpty(data)) {
                for (String key : data.split(SEPARATOR)) {
                    if (!key.isEmpty() && sKeys.size() < MAX) {
                        sKeys.add(key);
                    }
                }
            }
        }

        return new ArrayList<>(sKeys);
    }

    public static synchronized void record(@Nullable Context context, @Nullable String key) {
        if (context == null || TextUtils.isEmpty(key)) {
            return;
        }

        List<String> keys = get(context);

        if (!keys.isEmpty() && key.equals(keys.get(0))) {
            return;
        }

        keys.remove(key);
        keys.add(0, key);

        while (keys.size() > MAX) {
            keys.remove(keys.size() - 1);
        }

        sKeys = keys;
        OculoStore.write(context, KEY, TextUtils.join(SEPARATOR, keys));
    }
}
