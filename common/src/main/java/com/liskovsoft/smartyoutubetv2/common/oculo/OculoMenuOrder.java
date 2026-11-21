package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;

import java.util.List;

/**
 * Oculo (0.14.0): « Add to Watch later » on and first in the video long-press menu (flag
 * {@code R.bool.oculo_watch_later_first}, false upstream). Called from {@code MainUIData.restoreState()} once the
 * saved order is read: applied once per install (flag in OculoData), and again for a profile without a saved order
 * (fresh install, new profile). The rest of the saved menu (order, enabled items) is untouched. Oculo's own
 * contextual items (« Ne plus proposer ici » on Continue watching cards, « Masquer les vidéos vues » on Subscriptions
 * cards) are appended before the ordered items by VideoMenuPresenter, so they stay above it on their rows.
 */
public final class OculoMenuOrder {
    private OculoMenuOrder() {
    }

    /**
     * Moves the item first in {@code order} (in place) when it has to be applied.
     *
     * @return true when applied: the caller then enables the item and saves its data
     */
    public static boolean applyOnce(Context context, List<Long> order, boolean hadSavedOrder) {
        if (context == null || order == null || !context.getResources().getBoolean(R.bool.oculo_watch_later_first)) {
            return false;
        }

        OculoData data = OculoData.instance(context);

        if (!OculoMenuOrderRules.shouldApply(data.isWatchLaterFirstApplied(), hadSavedOrder)) {
            return false;
        }

        OculoMenuOrderRules.apply(order, 0, MainUIData.MENU_ITEM_ADD_TO_WATCH_LATER);
        data.setWatchLaterFirstApplied(true);

        return true;
    }
}
