package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoCardMeta;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.List;

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
     * A card's meta line with the Abricot accent on the live viewer count ({@code R.color.oculo_card_live_viewers})
     * and on a relative publish time under 24 h ({@code R.color.oculo_card_recent_time}), see {@link OculoCardMeta}.
     * Only those segments are colored; the rest keeps the card's text color (focused or not). Transparent colors
     * (upstream flavors) = text returned unchanged.
     */
    public static CharSequence accentCardMeta(Context context, CharSequence meta) {
        if (context == null || meta == null || meta.length() == 0) {
            return meta;
        }

        int liveColor = ContextCompat.getColor(context, R.color.oculo_card_live_viewers);
        int recentColor = ContextCompat.getColor(context, R.color.oculo_card_recent_time);
        if (liveColor == 0 && recentColor == 0) {
            return meta;
        }

        List<OculoCardMeta.Range> ranges = OculoCardMeta.findAccents(meta);
        if (ranges.isEmpty()) {
            return meta;
        }

        SpannableString result = new SpannableString(meta);
        for (OculoCardMeta.Range range : ranges) {
            int color = range.kind == OculoCardMeta.KIND_LIVE_VIEWERS ? liveColor : recentColor;
            if (color != 0) {
                result.setSpan(new ForegroundColorSpan(color), range.start, range.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return result;
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
     * Background of a card's info area (title + meta under the thumbnail). With the focus ring on and no focus fill
     * in the theme (selected = default background, Oculo's Dark Grey), {@code R.bool.oculo_card_hide_info_box}
     * removes the box: title and meta sit on the page background, as on Google TV and YouTube (32.74-13). Otherwise
     * (upstream flavors, themes with a fill such as OLED) the color is returned unchanged.
     */
    public static int cardInfoBackground(Context context, int color, int selectedBackground, int defaultBackground) {
        if (context == null || selectedBackground != defaultBackground || !hasCardFocusRing(context)
                || !context.getResources().getBoolean(R.bool.oculo_card_hide_info_box)) {
            return color;
        }
        return Color.TRANSPARENT;
    }

    /**
     * Meta line color (channel, views, date) under a card: {@code R.color.oculo_card_meta_text} (grey, YouTube's
     * #AAAAAA) when set, so the white title stands out once the info box is gone; transparent = keep the given color.
     * The Abricot accents ({@link #accentCardMeta}) are spans and stay on top.
     */
    public static int cardMetaTextColor(Context context, int textColor) {
        if (context == null) {
            return textColor;
        }
        int color = ContextCompat.getColor(context, R.color.oculo_card_meta_text);
        return color != 0 ? color : textColor;
    }

    /**
     * Focus ring of a card, in the style of the Google TV home screen: a thin ring
     * ({@code R.color.oculo_card_focus_ring}, {@code R.dimen.oculo_card_focus_ring_width}) set apart from the tile by a
     * gap ({@code R.dimen.oculo_card_focus_ring_gap}) where the page shows through, entirely OUTSIDE the tile (32.74-13;
     * until 32.74-12 it was drawn over the thumbnail's edges and hid part of it). Corners concentric with the tile's.
     * Around the ring, a soft glow in the thumbnail's color ({@link OculoGlow}, {@code R.dimen.oculo_card_glow_radius},
     * {@code R.fraction.oculo_card_glow_alpha}) fades in with focus. The ring pulses while focused
     * ({@code R.integer.oculo_card_focus_pulse_period}). The tile is the video card's thumbnail (main_image_wrapper),
     * or the single child of a channel card or Settings tile. Ring and glow are one drawable in the overlay of the
     * grid's item view (see {@link CardFrame}). No-op when the color is transparent (upstream flavors) or below API 23.
     */
    public static void setCardFocusFrame(View wrapper, boolean focused) {
        if (wrapper == null) {
            return;
        }

        OculoGlow.onCardFocusChanged(wrapper, focused);

        if (!hasCardFocusRing(wrapper.getContext())) {
            return;
        }

        CardFrame frame = (CardFrame) wrapper.getTag(R.id.oculo_card_frame);
        if (frame == null) {
            // Also when created unfocused: the card is prepared (rounded thumbnail, no clipping) once attached
            frame = new CardFrame(wrapper);
            wrapper.setTag(R.id.oculo_card_frame, frame);
        }
        frame.setFocused(focused);
    }

    /**
     * Same ring around a whole channel card (avatar + title).
     */
    public static void setChannelCardFocusFrame(View card, boolean focused) {
        setCardFocusFrame(card, focused);
    }

    /**
     * {@link OculoGlow} found the focused card's thumbnail color (0 = none: soft white glow).
     */
    static void onCardGlowColor(View wrapper, int color) {
        Object frame = wrapper != null ? wrapper.getTag(R.id.oculo_card_frame) : null;
        if (frame instanceof CardFrame) {
            ((CardFrame) frame).setGlowColor(color);
        }
    }

    /**
     * Ring + glow around a card's tile, drawn in the overlay of the grid's item view so that it can extend beyond the
     * tile. Setup, once the card is attached (after leanback's ShadowOverlayHelper has clipped the item view to its
     * rounded outline): the item view and the views between it and the tile stop clipping to their outline, the tile
     * itself is clipped to the rounded rectangle instead (so the thumbnail keeps rounded corners on all four sides
     * once the info box is gone), the item view's background moves to the tile for channel cards and Settings tiles,
     * and the grid stops clipping its children to their bounds (leanback's row views already do not).
     *
     * <p>Performance (32-bit MediaTek TVs): the glow is a pre-rendered, blurred alpha mask (half resolution, cached per
     * tile size) tinted by the paint color; focus, pulse and fade only change paint alphas and redraw the drawable.
     * No blur, layout or allocation per frame.
     */
    private static final class CardFrame extends Drawable implements View.OnLayoutChangeListener,
            View.OnAttachStateChangeListener {
        private static final LruCache<String, Bitmap> sMasks = new LruCache<>(8);
        private static final long GLOW_FADE_IN_MS = 220;
        private final View mWrapper;
        @Nullable
        private View mTile;
        @Nullable
        private View mHost;
        private final Paint mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mGlowPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final RectF mRingRect = new RectF();
        private final Rect mGlowRect = new Rect();
        private final int mRing;
        private final int mGap;
        private final int mGlowRadius;
        private final int mRingAlpha;
        private final float mGlowMaxAlpha;
        private final float mGlowFallbackAlpha;
        private final float mPulseMinAlpha;
        private float mRadius;
        private float mRingRadius;
        @Nullable
        private Bitmap mMask;
        private boolean mFocused;
        private float mPulse = 1;
        private float mGlow;
        private int mGlowColor = Color.WHITE;
        private float mGlowColorAlpha;
        @Nullable
        private final ValueAnimator mPulseAnimator;
        private final ValueAnimator mGlowAnimator;

        CardFrame(View wrapper) {
            mWrapper = wrapper;
            Context context = wrapper.getContext();
            Resources res = context.getResources();
            mRing = res.getDimensionPixelSize(R.dimen.oculo_card_focus_ring_width);
            mGap = res.getDimensionPixelSize(R.dimen.oculo_card_focus_ring_gap);
            mGlowRadius = res.getDimensionPixelSize(R.dimen.oculo_card_glow_radius);
            mGlowMaxAlpha = res.getFraction(R.fraction.oculo_card_glow_alpha, 1, 1);
            mGlowFallbackAlpha = res.getFraction(R.fraction.oculo_card_glow_fallback_alpha, 1, 1);
            mPulseMinAlpha = res.getFraction(R.fraction.oculo_card_focus_pulse_min_alpha, 1, 1);
            int pulsePeriod = res.getInteger(R.integer.oculo_card_focus_pulse_period);

            mRingPaint.setStyle(Paint.Style.STROKE);
            mRingPaint.setStrokeWidth(mRing);
            mRingPaint.setColor(ContextCompat.getColor(context, R.color.oculo_card_focus_ring));
            mRingAlpha = mRingPaint.getAlpha();

            if (pulsePeriod > 0 && mPulseMinAlpha < 1) {
                mPulseAnimator = ValueAnimator.ofFloat(0, 1);
                mPulseAnimator.setDuration(pulsePeriod);
                mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                mPulseAnimator.setInterpolator(new LinearInterpolator());
                mPulseAnimator.addUpdateListener(animation -> {
                    if (!mFocused || mHost == null || !mHost.isShown()) {
                        // Restarted by draw() when the card shows again (activity back, card re-attached)
                        animation.cancel();
                        return;
                    }
                    // 1 at the start of each period, mPulseMinAlpha halfway
                    double wave = 0.5 + 0.5 * Math.cos(2 * Math.PI * animation.getAnimatedFraction());
                    mPulse = (float) (mPulseMinAlpha + (1 - mPulseMinAlpha) * wave);
                    invalidateSelf();
                });
            } else {
                mPulseAnimator = null;
            }

            mGlowAnimator = ValueAnimator.ofFloat(0, 1);
            mGlowAnimator.setDuration(GLOW_FADE_IN_MS);
            mGlowAnimator.setInterpolator(new DecelerateInterpolator());
            mGlowAnimator.addUpdateListener(animation -> {
                mGlow = (float) animation.getAnimatedValue();
                invalidateSelf();
            });

            if (ViewCompat.isAttachedToWindow(wrapper)) {
                setup();
            }
            wrapper.addOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            setup();
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            if (mPulseAnimator != null) {
                mPulseAnimator.cancel();
            }
            mGlowAnimator.cancel();
        }

        private void setup() {
            if (mHost != null) {
                return;
            }

            // Item view = the ancestor whose parent is the grid (a RecyclerView)
            View host = null;
            for (View view = mWrapper; view != null;
                 view = view.getParent() instanceof View ? (View) view.getParent() : null) {
                if (view.getParent() instanceof RecyclerView) {
                    host = view;
                    break;
                }
            }
            if (host == null) {
                return;
            }

            View tile = mWrapper;
            if (mWrapper.getId() != R.id.main_image_wrapper && mWrapper instanceof ViewGroup
                    && ((ViewGroup) mWrapper).getChildCount() == 1) {
                // Channel card, Settings tile: the card's only child is the tile
                tile = ((ViewGroup) mWrapper).getChildAt(0);
            }

            // Leanback clipped the item view to its rounded outline: move the rounding to the tile
            boolean rounded = false;
            for (View view = tile.getParent() instanceof View ? (View) tile.getParent() : null; view != null;
                 view = view.getParent() instanceof View && view != host ? (View) view.getParent() : null) {
                if (view.getClipToOutline()) {
                    rounded = true;
                    view.setClipToOutline(false);
                }
                if (view == host) {
                    break;
                }
            }
            Resources res = mWrapper.getResources();
            mRadius = rounded ? res.getDimension(R.dimen.lb_rounded_rect_corner_radius) : 0;
            if (rounded) {
                final float radius = mRadius;
                tile.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                    }
                });
                tile.setClipToOutline(true);
            }

            if (tile != mWrapper && tile.getBackground() == null && mWrapper.getBackground() != null) {
                // Channel card, Settings tile: the card's color belongs to the tile (rounded with it)
                tile.setBackground(mWrapper.getBackground());
                mWrapper.setBackground(null);
            } else if (tile == mWrapper && host.getBackground() instanceof ColorDrawable) {
                // Video card: its color was only "temporarily visible during animations" (square corners now)
                host.setBackground(null);
            }

            if (res.getBoolean(R.bool.oculo_card_no_shadow)) {
                // Leanback's dynamic shadow (ShadowOverlayHelper: the item view's Z goes to 10dp on focus) is cast from
                // the whole card's outline, so with a transparent info box it shows as a black area under the texts.
                // No outline = no shadow, whatever the Z (which still draws the focused card above its neighbors).
                host.setOutlineProvider(null);
            }

            if (host.getParent() instanceof ViewGroup) {
                ((ViewGroup) host.getParent()).setClipChildren(false);
            }

            mHost = host;
            mTile = tile;
            mRingRadius = mRadius + mGap + mRing / 2f;
            tile.addOnLayoutChangeListener(this);
            host.getOverlay().add(this);
            updateGeometry();
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                                   int oldRight, int oldBottom) {
            updateGeometry();
        }

        private void updateGeometry() {
            if (mHost == null || mTile == null) {
                return;
            }
            int left = 0;
            int top = 0;
            for (View view = mTile; view != null && view != mHost;
                 view = view.getParent() instanceof View ? (View) view.getParent() : null) {
                left += view.getLeft();
                top += view.getTop();
            }
            int width = mTile.getWidth();
            int height = mTile.getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            float ringInset = mGap + mRing / 2f;
            mRingRect.set(left - ringInset, top - ringInset, left + width + ringInset, top + height + ringInset);
            int outset = mGap + mRing + mGlowRadius;
            mGlowRect.set(left - outset, top - outset, left + width + outset, top + height + outset);
            setBounds(mGlowRect);
            mMask = mGlowMaxAlpha > 0 ? glowMask(width, height) : null;
            invalidateSelf();
        }

        /**
         * Blurred ring-shaped alpha mask around a tile of this size, at half resolution, with the tile itself cut
         * out (the glow never tints the thumbnail).
         */
        @Nullable
        private Bitmap glowMask(int width, int height) {
            String key = width + "x" + height + ":" + mRadius + ":" + mGap + ":" + mRing + ":" + mGlowRadius;
            Bitmap mask = sMasks.get(key);
            if (mask != null && !mask.isRecycled()) {
                return mask;
            }
            float scale = 0.5f;
            int outset = mGap + mRing + mGlowRadius;
            int maskWidth = Math.max(1, Math.round((width + 2 * outset) * scale));
            int maskHeight = Math.max(1, Math.round((height + 2 * outset) * scale));
            try {
                mask = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8);
            } catch (OutOfMemoryError e) {
                return null;
            }
            Canvas canvas = new Canvas(mask);
            canvas.scale(scale, scale);
            canvas.translate(outset, outset);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.BLACK);
            // Source = the ring's outer edge; the blur spreads about mGlowRadius beyond it
            paint.setMaskFilter(new BlurMaskFilter(Math.max(1, mGlowRadius * 0.5f), BlurMaskFilter.Blur.NORMAL));
            float edge = mGap + mRing;
            float r = mRadius + edge;
            canvas.drawRoundRect(new RectF(-edge, -edge, width + edge, height + edge), r, r, paint);
            Paint clear = new Paint(Paint.ANTI_ALIAS_FLAG);
            clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            float cut = mGap / 2f;
            canvas.drawRoundRect(new RectF(-cut, -cut, width + cut, height + cut), mRadius + cut, mRadius + cut, clear);
            sMasks.put(key, mask);
            return mask;
        }

        void setFocused(boolean focused) {
            if (focused == mFocused) {
                return;
            }
            mFocused = focused;
            mGlowAnimator.cancel();
            mGlow = 0;
            if (mPulseAnimator != null) {
                mPulseAnimator.cancel();
            }
            mPulse = 1;
            if (focused) {
                if (mHost == null) {
                    setup();
                }
                startPulse();
            }
            invalidateSelf();
        }

        void setGlowColor(int color) {
            if (!mFocused) {
                return;
            }
            mGlowColor = color != 0 ? (color | 0xFF000000) : Color.WHITE;
            mGlowColorAlpha = color != 0 ? mGlowMaxAlpha : mGlowMaxAlpha * mGlowFallbackAlpha;
            mGlowAnimator.cancel();
            if (animatorsEnabled()) {
                mGlowAnimator.setFloatValues(mGlow, 1);
                mGlowAnimator.start();
            } else {
                mGlow = 1;
                invalidateSelf();
            }
        }

        private static boolean animatorsEnabled() {
            return VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
        }

        private void startPulse() {
            if (mPulseAnimator != null && mFocused && mHost != null && mHost.isShown() && animatorsEnabled()
                    && !mPulseAnimator.isStarted()) {
                mPulseAnimator.start();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (!mFocused) {
                return;
            }
            if (mMask != null && mGlow > 0.002f && mGlowColorAlpha > 0) {
                mGlowPaint.setColor(mGlowColor);
                mGlowPaint.setAlpha(Math.round(255 * mGlowColorAlpha * mGlow));
                canvas.drawBitmap(mMask, null, mGlowRect, mGlowPaint);
            }
            mRingPaint.setAlpha(Math.round(mRingAlpha * mPulse));
            canvas.drawRoundRect(mRingRect, mRingRadius, mRingRadius, mRingPaint);
            if (mPulseAnimator != null && !mPulseAnimator.isStarted()) {
                // Paused while hidden: resume once drawn again
                mHost.post(this::startPulse);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            // Alphas are driven by focus, pulse and glow fade
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
