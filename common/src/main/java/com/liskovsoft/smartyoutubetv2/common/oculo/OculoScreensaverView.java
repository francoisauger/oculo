package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.helpers.DateHelper;
import com.liskovsoft.smartyoutubetv2.common.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Oculo (0.13.0): the look of the idle screensaver (upstream: {@code ScreensaverManager} shows its dim container,
 * a black veil at 80 %, after the « Dim the screen after » delay without a key press). In the oculo flavor
 * ({@code R.bool.oculo_screensaver_look}) the container shows, on black: a large clock (Roboto Condensed Light),
 * the date below (Roboto), the small Oculo wordmark, and the thumbnails of cards already shown in the app
 * ({@link OculoScreensaverArt}), loaded from Glide's caches only, slowly crossfading and zooming (Ken Burns). The
 * whole content drifts to a new place every minute (burn-in).
 *
 * <p>Timings, settings and the rules for when it shows (never while a video plays, never on the sign-in screens,
 * any key dismisses it) stay upstream's: this class only fills the container while it is shown and empties it
 * when it is hidden. Low cost: no per-frame code; the slow zoom and drift are stepped 10 times a second
 * ({@link OculoScreensaverRules#TICK_MS}, render properties only: scale and translation, no redraw of the views'
 * content), only the 2.5 s crossfade runs at the display rate, the clock text is set once a minute, one picture
 * change every {@link OculoScreensaverRules#PICTURE_MS}.</p>
 */
@SuppressLint("ViewConstructor")
public final class OculoScreensaverView extends FrameLayout {
    private static final String TAG_VIEW = "OculoScreensaverView";
    private final LinearLayout mGroup;
    private final TextView mClock;
    private final TextView mDate;
    private final FrameLayout mPictureFrame;
    private final FrameLayout[] mSlots = new FrameLayout[2];
    private final ImageView[] mImages = new ImageView[2];
    private final Runnable mTickClock = this::tickClock;
    private final Runnable mNextPicture = this::nextPicture;
    private final Runnable mDrift = this::drift;
    private final Runnable mTickMotion = this::tickMotion;
    private boolean mTicking;
    // Ken Burns of the front picture and drift of the content, applied by tickMotion()
    private long mZoomStartMs;
    private float mZoomFrom = 1f;
    private float mZoomTo = 1f;
    private long mDriftStartMs;
    private float mDriftFromX;
    private float mDriftFromY;
    private float mDriftToX;
    private float mDriftToY;
    private List<OculoScreensaverRules.Art> mArt;
    private int mArtIndex = -1;
    private int mFront = -1;
    private int mPictureStep;
    private int mDriftStep;
    private int mFailures;
    private boolean mRunning;
    private final List<TextView> mPausedMarquees = new ArrayList<>();
    private final List<Boolean> mPausedByParent = new ArrayList<>();

    @Nullable
    private static Runnable sDimListener;

    /** 0.14.0 (smarttubetv's OculoPauseScreen): told each time the dim container is shown or hidden. */
    public static void setDimListener(@Nullable Runnable listener) {
        sDimListener = listener;
    }

    /** 0.14.0: the dim container shows the screensaver (not the « screen off » veil). */
    public static boolean isShowing(@Nullable View dimContainer) {
        return dimContainer instanceof ViewGroup && dimContainer.getVisibility() == View.VISIBLE
                && dimContainer.findViewWithTag(TAG_VIEW) != null;
    }

    public static boolean isEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_screensaver_look);
    }

    /**
     * ScreensaverManager: called each time its dim container is shown or hidden. {@code screensaver} = shown in
     * screensaver mode (not the player's « screen off » mode, which stays a black veil).
     */
    public static void update(@Nullable View dimContainer, boolean screensaver) {
        if (sDimListener != null && dimContainer != null) {
            sDimListener.run(); // 0.14.0: the pause screen gives way to the player's « screen off » veil
        }
        if (!(dimContainer instanceof ViewGroup) || !isEnabled(dimContainer.getContext())) {
            return;
        }
        ViewGroup container = (ViewGroup) dimContainer;
        View existing = container.findViewWithTag(TAG_VIEW);
        if (screensaver) {
            container.setBackgroundColor(Color.BLACK);
            if (existing == null) {
                OculoScreensaverView view = new OculoScreensaverView(container.getContext());
                view.setTag(TAG_VIEW);
                container.addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                view.pauseMarquees(container);
            }
        } else if (existing != null) {
            ((OculoScreensaverView) existing).stop();
            ((OculoScreensaverView) existing).resumeMarquees();
            container.removeView(existing);
        }
    }

    /**
     * The screen under the screensaver is hidden but still drawn: a scrolling title (upstream's marquees, the
     * framework's and SmartTube's MarqueeTextViewCompat, which redraws on every display frame while its text is
     * selected) would keep the app drawing 60 times a second (measured on Home: 60 frames/s with the screensaver
     * on, 0 on the Settings screen). The selected text views are unselected while the screensaver shows (a marquee
     * only runs while selected or focused), and selected again when it hides.
     */
    private void pauseMarquees(ViewGroup container) {
        View root = container.getRootView();
        collectMarquees(root, container);
        for (TextView text : mPausedMarquees) {
            mPausedByParent.add(isParentSelected(text));
            text.setSelected(false);
        }
    }

    private static boolean isParentSelected(View view) {
        return view.getParent() instanceof View && ((View) view.getParent()).isSelected();
    }

    private void collectMarquees(View view, View skip) {
        if (view == skip || view.getVisibility() != View.VISIBLE) {
            return;
        }
        if (view instanceof TextView) {
            if (view.isSelected()) {
                mPausedMarquees.add((TextView) view);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectMarquees(group.getChildAt(i), skip);
            }
        }
    }

    private void resumeMarquees() {
        // The key that dismissed the screensaver may already have moved the selection: a text selected through its
        // card is selected again only if that card still is
        for (int i = 0; i < mPausedMarquees.size(); i++) {
            TextView text = mPausedMarquees.get(i);
            boolean byParent = i < mPausedByParent.size() && mPausedByParent.get(i);
            if (!text.isSelected() && (!byParent || isParentSelected(text))) {
                text.setSelected(true);
            }
        }
        mPausedMarquees.clear();
        mPausedByParent.clear();
    }

    private OculoScreensaverView(Context context) {
        super(context);
        Resources res = context.getResources();
        setBackgroundColor(Color.BLACK);
        setClipChildren(false);

        mGroup = new LinearLayout(context);
        mGroup.setOrientation(LinearLayout.HORIZONTAL);
        mGroup.setGravity(Gravity.CENTER_VERTICAL);
        mGroup.setClipChildren(false);
        addView(mGroup, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.START);
        mGroup.addView(column, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mClock = new TextView(context);
        mClock.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
        mClock.setTextColor(res.getColor(R.color.oculo_screensaver_clock));
        mClock.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.oculo_screensaver_clock_size));
        mClock.setIncludeFontPadding(false);
        mClock.setSingleLine(true);
        column.addView(mClock, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mDate = new TextView(context);
        mDate.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        mDate.setTextColor(res.getColor(R.color.oculo_screensaver_date));
        mDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.oculo_screensaver_date_size));
        mDate.setSingleLine(true);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = dp(res, 6);
        dateLp.setMarginStart(dp(res, 4));
        column.addView(mDate, dateLp);

        Drawable logoDrawable = loadLogo(res);
        if (logoDrawable != null) {
            ImageView logo = new ImageView(context);
            logo.setImageDrawable(logoDrawable);
            logo.setAdjustViewBounds(true);
            logo.setScaleType(ImageView.ScaleType.FIT_START);
            logo.setAlpha(res.getFraction(R.fraction.oculo_screensaver_logo_alpha, 1, 1));
            LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    res.getDimensionPixelSize(R.dimen.oculo_screensaver_logo_height));
            logoLp.topMargin = dp(res, 28);
            logoLp.setMarginStart(dp(res, 2));
            column.addView(logo, logoLp);
        }

        mPictureFrame = new FrameLayout(context);
        GradientDrawable frameBackground = new GradientDrawable();
        frameBackground.setColor(res.getColor(R.color.oculo_screensaver_picture_background));
        frameBackground.setCornerRadius(res.getDimension(R.dimen.oculo_screensaver_picture_corner));
        mPictureFrame.setBackground(frameBackground);
        if (VERSION.SDK_INT >= 21) {
            final float radius = res.getDimension(R.dimen.oculo_screensaver_picture_corner);
            mPictureFrame.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            mPictureFrame.setClipToOutline(true);
        }
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(
                res.getDimensionPixelSize(R.dimen.oculo_screensaver_picture_width),
                res.getDimensionPixelSize(R.dimen.oculo_screensaver_picture_height));
        frameLp.setMarginStart(res.getDimensionPixelSize(R.dimen.oculo_screensaver_gap));
        mGroup.addView(mPictureFrame, frameLp);

        for (int i = 0; i < 2; i++) {
            // Slot = crossfade (alpha), image = Ken Burns (scale): two animators with their own durations
            FrameLayout slot = new FrameLayout(context);
            slot.setAlpha(0f);
            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            slot.addView(image, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mPictureFrame.addView(slot, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mSlots[i] = slot;
            mImages[i] = image;
        }
        mPictureFrame.setAlpha(res.getFraction(R.fraction.oculo_screensaver_picture_alpha, 1, 1));

        mArt = OculoScreensaverRules.pick(OculoScreensaverArt.recent(), OculoScreensaverRules.MAX_PICTURES);
        mPictureFrame.setVisibility(mArt.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Nullable
    private static Drawable loadLogo(Resources res) {
        try {
            Drawable drawable = res.getDrawable(R.drawable.oculo_screensaver_logo);
            return drawable != null && drawable.getIntrinsicWidth() > 0 ? drawable : null;
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void start() {
        if (mRunning) {
            return;
        }
        mRunning = true;
        tickClock();
        if (!mArt.isEmpty()) {
            nextPicture();
        }
        postDelayed(mDrift, OculoScreensaverRules.DRIFT_PERIOD_MS);
    }

    void stop() {
        if (!mRunning) {
            return;
        }
        mRunning = false;
        removeCallbacks(mTickClock);
        removeCallbacks(mNextPicture);
        removeCallbacks(mDrift);
        removeCallbacks(mTickMotion);
        mTicking = false;
        for (int i = 0; i < 2; i++) {
            mSlots[i].animate().cancel();
            clear(mImages[i]);
        }
    }

    private void tickClock() {
        if (!mRunning) {
            return;
        }
        long now = System.currentTimeMillis();
        mClock.setText(clockText(DateHelper.getCurrentTimeShort()));
        Locale locale = getResources().getConfiguration().locale;
        String pattern = VERSION.SDK_INT >= 18 ? DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM") : "EEEE d MMMM";
        String date = new java.text.SimpleDateFormat(pattern, locale).format(new Date(now));
        mDate.setText(date.isEmpty() ? date : date.substring(0, 1).toUpperCase(locale) + date.substring(1));
        postDelayed(mTickClock, OculoScreensaverRules.untilNextMinute(now) + 200);
    }

    /** « 9:41 AM »: the AM / PM part small, so the 12-hour clock is as wide as the 24-hour one. */
    static CharSequence clockText(String time) {
        if (time == null) {
            return "";
        }
        int lastDigit = -1;
        for (int i = time.length() - 1; i >= 0; i--) {
            if (Character.isDigit(time.charAt(i))) {
                lastDigit = i;
                break;
            }
        }
        if (lastDigit < 0 || lastDigit == time.length() - 1) {
            return time;
        }
        SpannableString result = new SpannableString(time);
        result.setSpan(new RelativeSizeSpan(0.34f), lastDigit + 1, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }

    private void drift() {
        if (!mRunning) {
            return;
        }
        int margin = dp(getResources(), 24);
        int rangeX = Math.max(0, (getWidth() - mGroup.getWidth()) / 2 - margin);
        int rangeY = Math.max(0, (getHeight() - mGroup.getHeight()) / 2 - margin);
        int[] offset = OculoScreensaverRules.drift(++mDriftStep, rangeX, rangeY);
        mDriftFromX = mGroup.getTranslationX();
        mDriftFromY = mGroup.getTranslationY();
        mDriftToX = offset[0];
        mDriftToY = offset[1];
        mDriftStartMs = SystemClock.uptimeMillis();
        startTicking();
        postDelayed(mDrift, OculoScreensaverRules.DRIFT_PERIOD_MS);
    }

    private void startTicking() {
        if (!mTicking && mRunning) {
            mTicking = true;
            post(mTickMotion);
        }
    }

    /**
     * The slow movements (Ken Burns, drift) at {@link OculoScreensaverRules#TICK_MS} intervals (10 a second) instead
     * of every display frame: each step moves the picture by less than a pixel, and the screen is redrawn 10 times
     * a second instead of 60. Stops by itself when nothing moves.
     */
    private void tickMotion() {
        if (!mRunning) {
            mTicking = false;
            return;
        }
        long now = SystemClock.uptimeMillis();
        boolean moving = false;

        if (mFront >= 0) {
            float zoomProgress = OculoScreensaverRules.progress(now - mZoomStartMs, OculoScreensaverRules.ZOOM_MS);
            float scale = OculoScreensaverRules.lerp(mZoomFrom, mZoomTo, zoomProgress);
            mImages[mFront].setScaleX(scale);
            mImages[mFront].setScaleY(scale);
            moving = zoomProgress < 1f;
        }

        if (mDriftStartMs > 0) {
            float driftProgress = OculoScreensaverRules.progress(now - mDriftStartMs, OculoScreensaverRules.DRIFT_MS);
            float eased = OculoScreensaverRules.ease(driftProgress);
            mGroup.setTranslationX(OculoScreensaverRules.lerp(mDriftFromX, mDriftToX, eased));
            mGroup.setTranslationY(OculoScreensaverRules.lerp(mDriftFromY, mDriftToY, eased));
            moving |= driftProgress < 1f;
        }

        if (moving) {
            postDelayed(mTickMotion, OculoScreensaverRules.TICK_MS);
        } else {
            mTicking = false;
        }
    }

    private void nextPicture() {
        if (!mRunning || mArt.isEmpty()) {
            return;
        }
        if (mFailures >= mArt.size()) {
            // Nothing in the caches: clock only
            mPictureFrame.setVisibility(View.GONE);
            return;
        }
        mArtIndex = OculoScreensaverRules.next(mArtIndex, mArt.size());
        OculoScreensaverRules.Art art = mArt.get(mArtIndex);
        int target = mFront == 0 ? 1 : 0;
        ImageView image = mImages[target];
        RequestBuilder<Drawable> request = cached(art.url != null ? art.url : art.fallbackUrl);
        if (art.url != null && art.fallbackUrl != null && !art.fallbackUrl.equals(art.url)) {
            request = request.error(cached(art.fallbackUrl));
        }
        request.listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> t, boolean isFirstResource) {
                if (mRunning) {
                    mFailures++;
                    post(mNextPicture);
                }
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> t, DataSource dataSource, boolean isFirstResource) {
                if (mRunning) {
                    mFailures = 0;
                    post(() -> showPicture(target));
                }
                return false;
            }
        }).into(image);
    }

    private RequestBuilder<Drawable> cached(String url) {
        Resources res = getResources();
        return Glide.with(getContext().getApplicationContext())
                .load(url)
                .onlyRetrieveFromCache(true) // no request: what the cards already loaded
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .override(res.getDimensionPixelSize(R.dimen.oculo_screensaver_picture_width) / 2,
                        res.getDimensionPixelSize(R.dimen.oculo_screensaver_picture_height) / 2);
    }

    private void showPicture(int target) {
        if (!mRunning) {
            return;
        }
        final int previous = mFront;
        mFront = target;
        FrameLayout slot = mSlots[target];
        ImageView image = mImages[target];
        float[] zoom = OculoScreensaverRules.zoom(mPictureStep++);
        image.setPivotX(zoom[2] * Math.max(1, mPictureFrame.getWidth()));
        image.setPivotY(zoom[3] * Math.max(1, mPictureFrame.getHeight()));
        image.setScaleX(zoom[0]);
        image.setScaleY(zoom[0]);
        mZoomFrom = zoom[0];
        mZoomTo = zoom[1];
        mZoomStartMs = SystemClock.uptimeMillis();
        startTicking();
        slot.bringToFront();
        slot.animate().cancel();
        slot.setAlpha(0f);
        slot.animate()
                .alpha(1f)
                .setDuration(OculoScreensaverRules.CROSSFADE_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (previous >= 0 && previous != mFront) {
                        mSlots[previous].setAlpha(0f);
                        clear(mImages[previous]);
                    }
                })
                .start();
        if (mArt.size() > 1) {
            postDelayed(mNextPicture, OculoScreensaverRules.PICTURE_MS);
        }
    }

    private void clear(@NonNull ImageView image) {
        try {
            Glide.with(getContext().getApplicationContext()).clear(image);
        } catch (IllegalArgumentException e) {
            // Destroyed activity: nothing to free
        }
        image.setImageDrawable(null);
    }

    private static int dp(Resources res, int dp) {
        return Math.round(dp * res.getDisplayMetrics().density);
    }
}
