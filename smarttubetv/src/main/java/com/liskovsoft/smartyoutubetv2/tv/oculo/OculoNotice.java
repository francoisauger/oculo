package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.view.View;

/**
 * Oculo panel (0.12.0): transparent drawers shown over the playing video (SponsorBlock « ask before skipping »,
 * chapter notice). Phase 1 keeps upstream's frame for them.
 */
public final class OculoNotice {
    private OculoNotice() {
    }

    static void applyFrame(View root, View container) {
        root.setBackground(null);
        container.setBackground(null);
    }
}
