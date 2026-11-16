package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Oculo: the small round Abricot « new content » dot (docs/mockups/couleur-secondaire.html, {@code .ic.dotted}):
 * {@code #FFA25C} with a thin dark outline. Drawn on the rail's Subscriptions icon (32.74-13) and, since 32.74-18,
 * after the name of a channel with new content in the Channels list (design.md M5), instead of a dark Abricot
 * background on the whole row.
 */
public final class OculoDot {
    private OculoDot() {
    }

    /**
     * A new dot drawable, bounds set to {@code size} x {@code size}.
     */
    public static GradientDrawable create(Context context, int size) {
        Resources res = context.getResources();
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(ContextCompat.getColor(context, R.color.oculo_rail_dot));
        dot.setStroke(res.getDimensionPixelSize(R.dimen.oculo_rail_dot_outline),
                ContextCompat.getColor(context, R.color.oculo_rail_dot_outline));
        dot.setSize(size, size);
        dot.setBounds(0, 0, size, size);
        return dot;
    }

    public static boolean isChannelDotEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_channel_new_content_dot);
    }

    /**
     * Channel card title: the dot right after the name when the channel has new content. The title is sized to its
     * text (ellipsized when too long) so that the dot follows the name.
     */
    public static void setChannelDot(TextView title, boolean show) {
        if (title == null || !isChannelDotEnabled(title.getContext())) {
            return;
        }

        ViewGroup.LayoutParams lp = title.getLayoutParams();
        if (lp != null && lp.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            if (lp instanceof LinearLayout.LayoutParams) {
                // Keeps the title from pushing past the card: it takes the remaining width at most
                ((LinearLayout.LayoutParams) lp).weight = 0;
            }
            title.setLayoutParams(lp);
        }

        Context context = title.getContext();
        if (show) {
            Object tag = title.getTag(R.id.oculo_rail_dot);
            GradientDrawable dot = tag instanceof GradientDrawable ? (GradientDrawable) tag
                    : create(context, context.getResources().getDimensionPixelSize(R.dimen.oculo_channel_dot_size));
            title.setTag(R.id.oculo_rail_dot, dot);
            title.setCompoundDrawablesRelative(null, null, dot, null);
            title.setCompoundDrawablePadding(context.getResources().getDimensionPixelSize(R.dimen.oculo_channel_dot_gap));
        } else {
            title.setCompoundDrawablesRelative(null, null, null, null);
        }
    }
}
