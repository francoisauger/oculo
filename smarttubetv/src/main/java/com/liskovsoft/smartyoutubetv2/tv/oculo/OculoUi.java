package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.animation.ValueAnimator;
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
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
     * True when the card focus ring is on (oculo flavor, API 23+): presenters then skip their focus fill.
     */
    public static boolean hasCardFocusRing(Context context) {
        return context != null && VERSION.SDK_INT >= 23
                && ContextCompat.getColor(context, R.color.oculo_card_focus_ring) != 0
                && context.getResources().getDimensionPixelSize(R.dimen.oculo_card_focus_ring_width) > 0;
    }

    /**
     * Focus ring of a video card, in the style of the Google TV home screen and Plex: a thin ring
     * ({@code R.color.oculo_card_focus_ring}, {@code R.dimen.oculo_card_focus_ring_width}) set slightly apart from the
     * thumbnail by a dark gap ({@code R.dimen.oculo_card_focus_ring_gap}, the card's own background color), drawn over
     * the edges of the thumbnail wrapper (main_image_wrapper). Outer corners = the card's rounded corners, the ring's
     * inner edge and the picture's corners concentric with them. The card is zoomed on focus (leanback
     * ZOOM_FACTOR_SMALL, 1.1), so the picture inside stays about its unfocused size. The duration badge and watched
     * bar move inside the ring. No-op when the color is transparent (upstream flavors) or below API 23 (no View
     * foreground). The ring pulses while focused ({@code R.integer.oculo_card_focus_pulse_period}), and the focus
     * change drives Home's thumbnail glow ({@link OculoGlow}). Also used by channel cards and Settings tiles.
     */
    public static void setCardFocusFrame(View wrapper, boolean focused) {
        if (wrapper == null) {
            return;
        }

        OculoGlow.onCardFocusChanged(wrapper, focused);

        if (VERSION.SDK_INT < 23) {
            return;
        }

        Context context = wrapper.getContext();
        int color = ContextCompat.getColor(context, R.color.oculo_card_focus_ring);
        int ring = context.getResources().getDimensionPixelSize(R.dimen.oculo_card_focus_ring_width);
        int gap = context.getResources().getDimensionPixelSize(R.dimen.oculo_card_focus_ring_gap);

        if (color == 0 || ring <= 0) {
            return;
        }

        if (focused && !(wrapper.getForeground() instanceof RingDrawable)) {
            float radius = context.getResources().getDimension(R.dimen.lb_rounded_rect_corner_radius);
            int pulsePeriod = context.getResources().getInteger(R.integer.oculo_card_focus_pulse_period);
            float pulseMinAlpha = context.getResources().getFraction(R.fraction.oculo_card_focus_pulse_min_alpha, 1, 1);
            wrapper.setForeground(new RingDrawable(color, ring, gap, radius, findBackgroundColor(wrapper),
                    pulsePeriod, pulseMinAlpha));
        }

        if (wrapper.getForeground() instanceof RingDrawable) {
            RingDrawable drawable = (RingDrawable) wrapper.getForeground();
            drawable.setAlpha(focused ? 255 : 0);
            drawable.setPulsing(focused);
        }

        // Badge + watched bar (aligned to the wrapper's bottom, full width): inside the ring while focused
        View clipInfo = wrapper.findViewById(R.id.clip_info);
        if (clipInfo != null) {
            int inset = ring + gap;
            clipInfo.setTranslationX(focused ? -inset : 0);
            clipInfo.setTranslationY(focused ? -inset : 0);
        }
    }

    /**
     * Same ring around a whole channel card (avatar + title).
     */
    public static void setChannelCardFocusFrame(View card, boolean focused) {
        setCardFocusFrame(card, focused);
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
     * A rounded ring set apart from the content: outer corners painted with the card background, then the ring, then
     * the gap (card background), all with concentric corners. While pulsing, only the ring's paint alpha changes
     * (1 -> min -> 1, cosine ease in-out): the drawable redraws itself, no layout, nothing allocated per frame. The
     * pulse pauses while the drawable is not visible (activity stopped, card detached) and is off when animations
     * are turned off (accessibility).
     */
    private static final class RingDrawable extends Drawable {
        private final Paint mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mRingPath = new Path();
        private final Path mBackgroundPath = new Path();
        private final RectF mRect = new RectF();
        private final int mRing;
        private final int mGap;
        private final float mRadius;
        private final int mRingAlpha;
        private final int mBackgroundAlpha;
        private boolean mVisible = true;
        private int mAlpha = 255;
        private float mPulse = 1;
        private boolean mPulsing;
        private final float mPulseMinAlpha;
        @Nullable
        private final ValueAnimator mPulseAnimator;

        RingDrawable(int color, int ring, int gap, float radius, int backgroundColor, int pulsePeriod,
                     float pulseMinAlpha) {
            mPulseMinAlpha = pulseMinAlpha;
            if (pulsePeriod > 0 && pulseMinAlpha < 1) {
                mPulseAnimator = ValueAnimator.ofFloat(0, 1);
                mPulseAnimator.setDuration(pulsePeriod);
                mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                mPulseAnimator.setInterpolator(new LinearInterpolator());
                mPulseAnimator.addUpdateListener(animation -> {
                    if (getCallback() == null) {
                        // No longer anyone's foreground
                        animation.cancel();
                        return;
                    }
                    // 1 at the start of each period, mPulseMinAlpha halfway
                    double wave = 0.5 + 0.5 * Math.cos(2 * Math.PI * animation.getAnimatedFraction());
                    mPulse = (float) (mPulseMinAlpha + (1 - mPulseMinAlpha) * wave);
                    updateRingAlpha();
                    invalidateSelf();
                });
            } else {
                mPulseAnimator = null;
            }
            mRingPaint.setColor(color);
            mBackgroundPaint.setColor(backgroundColor);
            mRingAlpha = mRingPaint.getAlpha();
            mBackgroundAlpha = mBackgroundPaint.getAlpha();
            mRing = ring;
            mGap = gap;
            mRadius = radius;
            mRingPath.setFillType(Path.FillType.EVEN_ODD);
            mBackgroundPath.setFillType(Path.FillType.EVEN_ODD);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            mRingPath.reset();
            mBackgroundPath.reset();

            // Corners outside the outer rounded rectangle (the card is not rounded at the thumbnail's bottom)
            mRect.set(bounds);
            mBackgroundPath.addRect(mRect, Path.Direction.CW);
            mBackgroundPath.addRoundRect(mRect, mRadius, mRadius, Path.Direction.CW);

            // Ring
            mRingPath.addRoundRect(mRect, mRadius, mRadius, Path.Direction.CW);
            mRect.inset(mRing, mRing);
            float r1 = Math.max(0, mRadius - mRing);
            mRingPath.addRoundRect(mRect, r1, r1, Path.Direction.CW);

            // Gap between the ring and the picture
            if (mGap > 0) {
                Path gap = new Path();
                gap.setFillType(Path.FillType.EVEN_ODD);
                gap.addRoundRect(mRect, r1, r1, Path.Direction.CW);
                mRect.inset(mGap, mGap);
                float r2 = Math.max(0, r1 - mGap);
                gap.addRoundRect(mRect, r2, r2, Path.Direction.CW);
                mBackgroundPath.addPath(gap);
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (!mVisible) {
                return;
            }
            if (mBackgroundPaint.getAlpha() != 0) {
                canvas.drawPath(mBackgroundPath, mBackgroundPaint);
            }
            canvas.drawPath(mRingPath, mRingPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mVisible = alpha != 0;
            mAlpha = alpha;
            updateRingAlpha();
            mBackgroundPaint.setAlpha(mBackgroundAlpha * alpha / 255);
            invalidateSelf();
        }

        private void updateRingAlpha() {
            mRingPaint.setAlpha(Math.round(mRingAlpha * mAlpha / 255f * mPulse));
        }

        void setPulsing(boolean pulsing) {
            boolean wasPulsing = mPulsing;
            mPulsing = pulsing;
            updatePulse(pulsing && !wasPulsing);
        }

        private void updatePulse(boolean restart) {
            if (mPulseAnimator == null) {
                return;
            }

            boolean animatorsEnabled = VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
            if (mPulsing && isVisible() && animatorsEnabled) {
                if (restart || !mPulseAnimator.isStarted()) {
                    // Each focus starts at full white
                    mPulseAnimator.cancel();
                    mPulseAnimator.start();
                }
            } else {
                mPulseAnimator.cancel();
                if (mPulse != 1) {
                    mPulse = 1;
                    updateRingAlpha();
                    invalidateSelf();
                }
            }
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            boolean changed = super.setVisible(visible, restart);
            if (changed) {
                updatePulse(false);
            }
            return changed;
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mRingPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
