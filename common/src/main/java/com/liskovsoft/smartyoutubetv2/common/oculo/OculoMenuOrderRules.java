package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.util.List;

/**
 * Oculo (0.14.0): pure rules of « Add to Watch later first » in the long-press menu (OculoMenuOrder). Upstream has the
 * item but leaves it off by default and 8th in its order; Oculo turns it on and puts it first, once, then the user's
 * later choices (Settings › context menu: order and check boxes) are kept.
 */
public final class OculoMenuOrderRules {
    private OculoMenuOrderRules() {
    }

    /**
     * @param migrated       the one-time Oculo flag is already set (an earlier launch applied the default)
     * @param hadSavedOrder  the profile had a saved menu order (false = fresh install or a new profile, whose order was
     *                       just built from upstream's defaults)
     */
    public static boolean shouldApply(boolean migrated, boolean hadSavedOrder) {
        return !migrated || !hadSavedOrder;
    }

    /**
     * Moves {@code item} to the front of {@code order} (added when missing) and enables it in {@code enabledMask}.
     * The other items keep their relative order and their enabled state.
     *
     * @return the new enabled mask
     */
    public static long apply(List<Long> order, long enabledMask, long item) {
        if (order != null) {
            Long boxed = item;
            order.remove(boxed);
            order.add(0, boxed);
        }

        return enabledMask | item;
    }
}
