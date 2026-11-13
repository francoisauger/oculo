package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoData;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoGlowColor;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Oculo: ambient glow behind Home, as on the Google TV home screen. While a video (or channel) card has focus, the
 * browse background's glow takes the main color of its thumbnail; when focus leaves the cards (rail, top bar) it
 * fades back to the default teal glow of {@code drawable/oculo_browse_background.xml}.
 *
 * <ul>
 *     <li>A full-screen {@link GlowView} is added behind the browse fragment's content ({@link #attach}, from
 *     {@link OculoRail}). It paints the same picture as the theme background (black + one radial glow at the top
 *     right, center 78 % / 25 %, radius 84.44 % of the short side, dithered) and replaces it only on screens whose
 *     theme uses that background: the OLED theme and the other schemes are left alone, the player never has
 *     it.</li>
 *     <li>Card focus comes from {@link OculoUi#setCardFocusFrame}, already hooked in the card presenters. After a
 *     short debounce (fast scrolling does not flicker), the thumbnail bitmap that Glide already shows is scaled
 *     down to 64 px on the main thread and analysed with androidx.palette on a background thread;
 *     {@link OculoGlowColor} picks and clamps the color. Colors are cached per decoded bitmap.</li>
 *     <li>Colors crossfade over {@link #CROSSFADE_MS}: two pre-built gradient layers whose paint alpha is
 *     animated, so nothing is allocated per frame.</li>
 *     <li>32.74-13: the same color also goes to the focused card's own glow ({@link OculoUi}), on every screen with
 *     cards (no usable color = 0: soft white there).</li>
 *     <li>Setting: Settings &gt; Main UI &gt; Home &gt; "Glow from the thumbnail" ({@link OculoData}).</li>
 * </ul>
 */
public final class OculoGlow {
    /** Focus must rest this long on a card before its color is applied (fast scrolling). */
    private static final long FOCUS_DEBOUNCE_MS = 160;
    /** Delay before fading back to teal when focus leaves the cards (focus usually lands on the next card). */
    private static final long UNFOCUS_DELAY_MS = 260;
    private static final long CROSSFADE_MS = 450;
    /** Long side of the bitmap given to Palette. */
    private static final int SAMPLE_SIZE = 64;
    // Geometry of drawable/oculo_browse_background.xml
    private static final float CENTER_X = 0.78f;
    private static final float CENTER_Y = 0.25f;
    private static final float RADIUS_OF_SHORT_SIDE = 0.8444f;

    private static final LruCache<Long, Integer> sColors = new LruCache<>(300);
    private static final Integer NO_COLOR = 0;
    private static ExecutorService sExecutor;
    private static WeakReference<GlowView> sView = new WeakReference<>(null);
    private static WeakReference<View> sFocusedCard = new WeakReference<>(null);
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static int sRequest;
    private static final Runnable sApplyFocused = OculoGlow::applyFocusedCard;
    private static final Runnable sApplyDefault = () -> {
        GlowView view = sView.get();
        if (view != null) {
            view.setTarget(OculoGlowColor.DEFAULT_GLOW);
        }
    };

    private OculoGlow() {
    }

    /**
     * Adds the glow layer at the back of the browse fragment's root, when the theme uses Oculo's glow background.
     */
    public static void attach(View root) {
        if (!(root instanceof ViewGroup) || !usesGlowBackground(root.getContext())) {
            return;
        }

        GlowView view = new GlowView(root.getContext());
        ((ViewGroup) root).addView(view, 0, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sView = new WeakReference<>(view);
    }

    /**
     * The glow view of the current browse screen, if any (Home, sections).
     */
    @Nullable
    static View backgroundView() {
        return sView.get();
    }

    /**
     * Paints the glow view's picture in its own coordinates (without View.draw(), which would touch its draw state).
     */
    static void drawBackground(View view, Canvas canvas) {
        if (view instanceof GlowView) {
            int save = canvas.save();
            canvas.clipRect(0, 0, view.getWidth(), view.getHeight());
            ((GlowView) view).paint(canvas);
            canvas.restoreToCount(save);
        }
    }

    private static boolean usesGlowBackground(Context context) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(R.attr.shelfBackground, value, true) || value.resourceId == 0) {
            return false;
        }
        try {
            return "oculo_browse_background".equals(context.getResources().getResourceEntryName(value.resourceId));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Called when a card gains or loses focus. {@code card} is the view holding the thumbnail (the video card's
     * main_image_wrapper, or a channel card).
     */
    public static void onCardFocusChanged(View card, boolean focused) {
        if (card == null) {
            return;
        }

        if (focused) {
            sFocusedCard = new WeakReference<>(card);
            sRequest++;
            sHandler.removeCallbacks(sApplyDefault);
            sHandler.removeCallbacks(sApplyFocused);
            sHandler.postDelayed(sApplyFocused, FOCUS_DEBOUNCE_MS);
        } else if (sFocusedCard.get() == card) {
            // Cards are also "unselected" when created: only the card that had focus counts
            sFocusedCard = new WeakReference<>(null);
            sRequest++;
            sHandler.removeCallbacks(sApplyFocused);
            sHandler.removeCallbacks(sApplyDefault);
            sHandler.postDelayed(sApplyDefault, UNFOCUS_DELAY_MS);
        }
    }

    /**
     * Background glow (Home only, same window as the card) and the focused card's own glow ({@link OculoUi}).
     * {@code color} 0 = no usable color: teal background, soft white card glow.
     */
    private static void deliver(View card, int color) {
        GlowView view = sView.get();
        if (view != null && view.getRootView() == card.getRootView()) {
            view.setTarget(color != 0 ? color : OculoGlowColor.DEFAULT_GLOW);
        }
        OculoUi.onCardGlowColor(card, color);
    }

    private static void applyFocusedCard() {
        View card = sFocusedCard.get();
        if (card == null) {
            return;
        }

        if (!OculoData.instance(card.getContext()).isThumbnailGlowEnabled()) {
            deliver(card, 0);
            return;
        }

        Bitmap bitmap = findBitmap(card);
        if (bitmap == null || bitmap.isRecycled()) {
            deliver(card, 0);
            return;
        }

        // Glide recycles bitmaps through its pool: the generation id changes whenever a bitmap is reused
        long key = ((long) System.identityHashCode(bitmap) << 32) | (bitmap.getGenerationId() & 0xFFFFFFFFL);
        Integer cached = sColors.get(key);
        if (cached != null) {
            deliver(card, cached);
            return;
        }

        boolean hardware = VERSION.SDK_INT >= 26 && bitmap.getConfig() == Bitmap.Config.HARDWARE;
        // Software bitmaps are scaled down here, while Glide cannot reuse them; hardware ones are copied off the
        // main thread (Glide never reuses those, it only recycles them: then the copy fails and teal stays).
        Bitmap sample = hardware ? null : scale(bitmap);
        Bitmap source = hardware ? bitmap : null;
        int request = sRequest;

        executor().execute(() -> {
            int color = computeColor(sample, source);
            sHandler.post(() -> {
                sColors.put(key, color);
                View current = sFocusedCard.get();
                if (current != null && request == sRequest) {
                    deliver(current, color);
                }
            });
        });
    }

    /**
     * Runs on the background thread.
     */
    private static int computeColor(@Nullable Bitmap sample, @Nullable Bitmap hardware) {
        Bitmap bitmap = sample;
        try {
            if (bitmap == null && hardware != null) {
                Bitmap copy = hardware.copy(Bitmap.Config.ARGB_8888, false);
                if (copy == null) {
                    return 0;
                }
                bitmap = scale(copy);
                if (bitmap != copy) {
                    copy.recycle();
                }
            }
            if (bitmap == null) {
                return 0;
            }

            Palette palette = Palette.from(bitmap)
                    .maximumColorCount(16)
                    .resizeBitmapArea(SAMPLE_SIZE * SAMPLE_SIZE)
                    .generate();

            Palette.Swatch[] swatches = {
                    palette.getVibrantSwatch(),
                    palette.getDarkVibrantSwatch(),
                    palette.getDominantSwatch(),
                    palette.getLightVibrantSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDarkMutedSwatch()
            };
            int[] rgbs = new int[swatches.length];
            int[] populations = new int[swatches.length];
            for (int i = 0; i < swatches.length; i++) {
                if (swatches[i] != null) {
                    rgbs[i] = swatches[i].getRgb();
                    populations[i] = swatches[i].getPopulation();
                }
            }
            int total = 0;
            List<Palette.Swatch> all = palette.getSwatches();
            for (Palette.Swatch swatch : all) {
                total += swatch.getPopulation();
            }

            return OculoGlowColor.pick(rgbs, populations, total);
        } catch (RuntimeException e) {
            // Recycled meanwhile, out of memory...: keep the default glow
            return 0;
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * A small copy (at most {@link #SAMPLE_SIZE} px on the long side); never the source itself.
     */
    private static Bitmap scale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        float ratio = (float) SAMPLE_SIZE / Math.max(width, height);
        if (ratio >= 1) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        return Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(width * ratio)),
                Math.max(1, Math.round(height * ratio)), true);
    }

    @Nullable
    private static Bitmap findBitmap(View card) {
        ImageView image = card.findViewById(R.id.main_image);
        if (image == null) {
            image = card.findViewById(R.id.channel_image);
        }
        return image != null ? toBitmap(image.getDrawable()) : null;
    }

    @Nullable
    private static Bitmap toBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        if (drawable instanceof GifDrawable) {
            return ((GifDrawable) drawable).getFirstFrame();
        }
        if (drawable instanceof LayerDrawable) {
            // Crossfade (TransitionDrawable): the last layer is the new picture
            LayerDrawable layers = (LayerDrawable) drawable;
            return layers.getNumberOfLayers() > 0 ? toBitmap(layers.getDrawable(layers.getNumberOfLayers() - 1)) : null;
        }
        return null;
    }

    private static ExecutorService executor() {
        if (sExecutor == null) {
            sExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(() -> {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    runnable.run();
                }, "OculoGlow");
                thread.setDaemon(true);
                return thread;
            });
        }
        return sExecutor;
    }

    /**
     * The browse background with an animatable glow color. Two gradient layers (current and previous color): a
     * color change rebuilds one shader and crossfades the layers' paint alpha.
     */
    static final class GlowView extends View {
        private final int mBaseColor;
        private final Paint[] mPaints = {new Paint(), new Paint()};
        private final int[] mColors = {OculoGlowColor.DEFAULT_GLOW, 0};
        private final float[] mWeights = {1, 0};
        private final float[] mStartWeights = {1, 0};
        private int mTargetSlot;
        private final ValueAnimator mAnimator;

        GlowView(Context context) {
            super(context);
            mBaseColor = ContextCompat.getColor(context, R.color.scheme_grey_shelf_background_dark);
            for (Paint paint : mPaints) {
                paint.setDither(true);
            }
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

            mAnimator = ValueAnimator.ofFloat(0, 1);
            mAnimator.setDuration(CROSSFADE_MS);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mAnimator.addUpdateListener(animation -> {
                float fraction = animation.getAnimatedFraction();
                for (int i = 0; i < 2; i++) {
                    float end = i == mTargetSlot ? 1 : 0;
                    mWeights[i] = mStartWeights[i] + (end - mStartWeights[i]) * fraction;
                }
                invalidate();
            });
        }

        void setTarget(int color) {
            if (mColors[mTargetSlot] == color) {
                return;
            }

            int slot;
            if (mColors[1 - mTargetSlot] == color) {
                // Back to the color still fading out: just reverse
                slot = 1 - mTargetSlot;
            } else {
                // The new color goes into the fainter layer; mid-crossfade, that layer restarts from nothing
                // (at most half strength) rather than changing hue in place
                slot = mWeights[0] < mWeights[1] ? 0 : mWeights[1] < mWeights[0] ? 1 : 1 - mTargetSlot;
                mWeights[slot] = 0;
                mColors[slot] = color;
                updateShader(slot);
            }
            mTargetSlot = slot;

            mAnimator.cancel();
            mStartWeights[0] = mWeights[0];
            mStartWeights[1] = mWeights[1];
            if (VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) {
                // "Remove animations" (accessibility): switch at once
                mWeights[slot] = 1;
                mWeights[1 - slot] = 0;
                invalidate();
            } else {
                mAnimator.start();
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updateShader(0);
            updateShader(1);
        }

        private void updateShader(int slot) {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0 || mColors[slot] == 0) {
                mPaints[slot].setShader(null);
                return;
            }
            int color = mColors[slot];
            mPaints[slot].setShader(new RadialGradient(width * CENTER_X, height * CENTER_Y,
                    RADIUS_OF_SHORT_SIDE * Math.min(width, height), color, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            paint(canvas);
        }

        /** Also used by {@link OculoRowEdge} to repaint the background over cards scrolled out on the left. */
        void paint(Canvas canvas) {
            canvas.drawColor(mBaseColor);
            for (int i = 0; i < 2; i++) {
                if (mWeights[i] > 0.002f && mPaints[i].getShader() != null) {
                    mPaints[i].setAlpha(Math.round(255 * mWeights[i]));
                    canvas.drawPaint(mPaints[i]);
                }
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            mAnimator.cancel();
            super.onDetachedFromWindow();
        }
    }
}
