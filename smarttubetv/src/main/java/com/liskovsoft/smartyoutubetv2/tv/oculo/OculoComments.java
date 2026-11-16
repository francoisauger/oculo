package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.stfalcon.chatkit.messages.MessageHolders;

/**
 * Oculo: lighter, easier to read comments panel (flag {@code oculo_comments_airy}, false upstream).
 * <p>
 * Layout: {@code layout/oculo_comment_item.xml} instead of chatkit's item (more room inside and between the
 * bubbles, side margins, regular font with 1.28 line spacing, focus ring). Text: the meta line
 * (@author · likes · date · replies) is smaller (82 %), regular and grey (white 70 %), the author medium and white;
 * the body is white 90 %, 94 % of the list's 16sp (about 15sp). Upstream: the whole header bold, body condensed.
 */
public final class OculoComments {
    private static final String META_SEPARATOR = " · ";
    private static final float META_SIZE = 0.82f;
    private static final float BODY_SIZE = 0.94f;
    private static final int AUTHOR_COLOR = 0xFFF1F1F1;
    private static final int META_COLOR = 0xB3FFFFFF; // white 70 %
    private static final int BODY_COLOR = 0xE6FFFFFF; // white 90 %

    private OculoComments() {
    }

    public static boolean isEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_comments_airy);
    }

    /** Holders for the comments list: Oculo's item layout when enabled, chatkit's default otherwise. */
    public static MessageHolders holders(Context context) {
        MessageHolders holders = new MessageHolders();
        if (isEnabled(context)) {
            holders.setIncomingTextLayout(R.layout.oculo_comment_item);
        }
        return holders;
    }

    /**
     * Comment text: meta line, a small gap, the body. {@code metaItems} follow the author (likes, date, replies…),
     * null or empty items are skipped.
     */
    public static CharSequence formatComment(String author, CharSequence message, CharSequence... metaItems) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        int metaStart = builder.length();
        if (!TextUtils.isEmpty(author)) {
            builder.append(author);
            int end = builder.length();
            builder.setSpan(new TypefaceSpan("sans-serif-medium"), metaStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.NORMAL), metaStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(AUTHOR_COLOR), metaStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int restStart = builder.length();
        for (CharSequence item : metaItems) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(META_SEPARATOR);
            }
            builder.append(item);
        }
        if (builder.length() > restStart) {
            builder.setSpan(new ForegroundColorSpan(META_COLOR), restStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (builder.length() > metaStart) {
            builder.setSpan(new RelativeSizeSpan(META_SIZE), metaStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append("\n");
            int gapStart = builder.length();
            builder.append("\n");
            builder.setSpan(new AbsoluteSizeSpan(5, true), gapStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int bodyStart = builder.length();
        if (message != null) {
            builder.append(message);
        }
        if (builder.length() > bodyStart) {
            builder.setSpan(new RelativeSizeSpan(BODY_SIZE), bodyStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(BODY_COLOR), bodyStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }
}
