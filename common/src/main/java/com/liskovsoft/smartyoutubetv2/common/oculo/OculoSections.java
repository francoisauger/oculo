package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.service.SidebarService;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;

/**
 * Oculo: rail sections that can never show anything for a regular YouTube account are removed from the rail once
 * per profile (32.74-18, per profile since 32.74-20). They stay available in Settings > General > sections, and a section turned back on there stays.
 * <ul>
 *     <li>Kids: {@code FEkids_home} through the YouTube Kids TV client ({@code getBrowseResultKids}) answers with no
 *     section for an adult account (no Kids profile): « emitGroupsPartial: groups are null or empty », then the
 *     empty-section screen every time.</li>
 *     <li>My videos: {@code FEmy_videos} (TV client) lists the account's own uploads; with none it returns an empty
 *     grid (« MediaGroup doesn't contain media items », « fromNullable result is null »).</li>
 * </ul>
 * Flag {@code R.bool.oculo_hide_dead_sections} (false upstream, true in the oculo flavor).
 */
public final class OculoSections {
    private static final String TAG = "OculoSections";
    /** AppPrefs' current profile name (its key is private upstream) */
    private static final String LAST_PROFILE_NAME = "last_profile_name";
    private static final String DONE_KEY_PREFIX = "oculo_dead_sections_hidden_";
    private static final int[] DEAD_SECTIONS = {MediaGroup.TYPE_KIDS_HOME, MediaGroup.TYPE_MY_VIDEOS};

    private OculoSections() {
    }

    /**
     * Called by SidebarService after its state is restored.
     */
    public static void hideDeadSectionsOnce(Context context, SidebarService sidebar) {
        if (context == null || sidebar == null || !context.getResources().getBoolean(R.bool.oculo_hide_dead_sections)) {
            return;
        }

        // The rail's sections are saved per profile (AppPrefs « <profile>_sidebar_data », the profile = the signed-in
        // account, « anonymous » before the account is known), so the done-flag is per profile too. 32.74-18 used
        // one global flag: it was consumed by the first profile restored at startup (before the account was loaded)
        // and François's own list, restored right after on the account change, kept Kids.
        AppPrefs prefs = AppPrefs.instance(context);
        String key = DONE_KEY_PREFIX + prefs.getString(LAST_PROFILE_NAME, "");
        if (prefs.getBoolean(key, false)) {
            return;
        }

        for (int sectionId : DEAD_SECTIONS) {
            sidebar.enableSection(sectionId, false);
        }
        prefs.putBoolean(key, true);
        Log.d(TAG, "Kids and My videos removed from the rail (%s)", key);
    }
}
