package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.res.ColorStateList;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Oculo: small view tweaks called from upstream code through one-line hooks. Colors come from resources
 * (smarttubetv/src/main/res/values/oculo.xml keeps upstream's look, the oculo flavor overrides them), so the
 * hooks are no-ops in upstream's flavors.
 */
public final class OculoUi {
    private OculoUi() {
    }

    /**
     * Text color of a card badge: {@code R.color.oculo_badge_accent_text} on the accent badge (LIVE, NEW,
     * upcoming, background {@code R.color.dark_red}), the layout's own color on the others.
     */
    public static void tintBadgeText(TextView badge, int backgroundColor) {
        if (badge == null) {
            return;
        }

        Object original = badge.getTag(R.id.oculo_badge_text_colors);
        if (original == null) {
            original = badge.getTextColors();
            badge.setTag(R.id.oculo_badge_text_colors, original);
        }

        int accentText = ContextCompat.getColor(badge.getContext(), R.color.oculo_badge_accent_text);
        boolean isAccent = backgroundColor == ContextCompat.getColor(badge.getContext(), R.color.dark_red);

        if (isAccent && accentText != 0) {
            badge.setTextColor(accentText);
        } else {
            badge.setTextColor((ColorStateList) original);
        }
    }

    /**
     * Sets the text color from a resource unless the resource is transparent (= keep upstream's color).
     */
    public static void setTextColorIfSet(TextView view, int colorRes) {
        if (view == null) {
            return;
        }

        int color = ContextCompat.getColor(view.getContext(), colorRes);

        if (color != 0) {
            view.setTextColor(color);
        }
    }
}
