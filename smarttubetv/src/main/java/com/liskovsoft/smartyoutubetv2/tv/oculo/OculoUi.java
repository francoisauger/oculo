package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
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

    /**
     * Text color of a focused card. When the theme gives focused cards no fill of their own (selected background =
     * default background, Oculo's YouTube-style focus), the text stays the default (white) one instead of
     * upstream's dark text meant for a light fill.
     */
    public static int cardSelectedTextColor(int selectedBackground, int defaultBackground, int selectedText,
                                            int defaultText) {
        return selectedBackground == defaultBackground ? defaultText : selectedText;
    }

    /**
     * Focus contour of a video card: a rounded frame ({@code R.color.oculo_card_focus_frame},
     * {@code R.dimen.oculo_card_focus_frame_width}) drawn over the edges of the thumbnail wrapper (main_image_wrapper),
     * outer corners = the card's rounded corners, inner corners concentric. The card is zoomed on focus (leanback
     * ZOOM_FACTOR_SMALL, 1.1), so the picture inside the frame stays about its unfocused size and the frame reads as a
     * contour around it, like YouTube's outline. The duration badge and watched bar move inside the frame.
     * No-op when the color is transparent (upstream flavors) or below API 23 (no View foreground).
     */
    public static void setCardFocusFrame(View wrapper, boolean focused) {
        setCardFocusFrame(wrapper, focused, R.dimen.oculo_card_focus_frame_width);
    }

    /**
     * Same frame around a whole channel card (avatar + title, 30dp high), thinner
     * ({@code R.dimen.oculo_channel_card_focus_frame_width}).
     */
    public static void setChannelCardFocusFrame(View card, boolean focused) {
        setCardFocusFrame(card, focused, R.dimen.oculo_channel_card_focus_frame_width);
    }

    private static void setCardFocusFrame(View wrapper, boolean focused, int widthRes) {
        if (wrapper == null || VERSION.SDK_INT < 23) {
            return;
        }

        Context context = wrapper.getContext();
        int color = ContextCompat.getColor(context, R.color.oculo_card_focus_frame);
        int width = context.getResources().getDimensionPixelSize(widthRes);

        if (color == 0 || width <= 0) {
            return;
        }

        if (focused && !(wrapper.getForeground() instanceof FrameDrawable)) {
            float radius = context.getResources().getDimension(R.dimen.lb_rounded_rect_corner_radius);
            wrapper.setForeground(new FrameDrawable(color, width, radius, findBackgroundColor(wrapper)));
        }

        if (wrapper.getForeground() instanceof FrameDrawable) {
            wrapper.getForeground().setAlpha(focused ? 255 : 0);
        }

        // Badge + watched bar (aligned to the wrapper's bottom, full width): inside the frame while focused
        View clipInfo = wrapper.findViewById(R.id.clip_info);
        if (clipInfo != null) {
            clipInfo.setTranslationX(focused ? -width : 0);
            clipInfo.setTranslationY(focused ? -width : 0);
        }
    }

    /**
     * The card's own background color (upstream presenters set a plain color on the card view), used to paint the
     * frame's outer corners where the card itself is not rounded (bottom of the thumbnail).
     */
    private static int findBackgroundColor(View view) {
        for (View current = view; current != null;
             current = current.getParent() instanceof View ? (View) current.getParent() : null) {
            if (current.getBackground() instanceof ColorDrawable) {
                return ((ColorDrawable) current.getBackground()).getColor();
            }
        }
        return 0;
    }

    /**
     * A rounded frame: outer rounded rectangle minus the inner one (inset by the frame width, concentric corners).
     */
    private static final class FrameDrawable extends Drawable {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mPath = new Path();
        private final Path mCornerPath = new Path();
        private final RectF mRect = new RectF();
        private final int mWidth;
        private final float mRadius;
        private final int mColorAlpha;

        FrameDrawable(int color, int width, float radius, int cornerColor) {
            mPaint.setColor(color);
            mCornerPaint.setColor(cornerColor);
            mCornerPath.setFillType(Path.FillType.EVEN_ODD);
            mPaint.setStyle(Paint.Style.FILL);
            mColorAlpha = mPaint.getAlpha();
            mWidth = width;
            mRadius = radius;
            mPath.setFillType(Path.FillType.EVEN_ODD);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            mPath.reset();
            mCornerPath.reset();
            mRect.set(bounds);
            mCornerPath.addRect(mRect, Path.Direction.CW);
            mCornerPath.addRoundRect(mRect, mRadius, mRadius, Path.Direction.CW);
            mPath.addRoundRect(mRect, mRadius, mRadius, Path.Direction.CW);
            mRect.inset(mWidth, mWidth);
            float inner = Math.max(0, mRadius - mWidth);
            mPath.addRoundRect(mRect, inner, inner, Path.Direction.CW);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (mPaint.getAlpha() != 0) {
                if (mCornerPaint.getAlpha() != 0) {
                    canvas.drawPath(mCornerPath, mCornerPaint);
                }
                canvas.drawPath(mPath, mPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(mColorAlpha * alpha / 255);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
