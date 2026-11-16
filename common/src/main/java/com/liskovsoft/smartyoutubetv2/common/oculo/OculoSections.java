package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.service.SidebarService;

/**
 * Oculo: rail sections that can never show anything for a regular YouTube account are removed from the rail once
 * (32.74-18). They stay available in Settings > General > sections, and a section turned back on there stays.
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

        OculoData data = OculoData.instance(context);
        if (data.isDeadSectionsHidden()) {
            return;
        }

        for (int sectionId : DEAD_SECTIONS) {
            sidebar.enableSection(sectionId, false);
        }
        data.setDeadSectionsHidden(true);
    }
}
