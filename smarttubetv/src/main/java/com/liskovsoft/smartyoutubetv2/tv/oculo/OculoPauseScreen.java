package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.liskovsoft.sharedutils.helpers.DateHelper;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.common.misc.ScreensaverManager;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoData;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoPauseScreenRules;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoScreensaverRules;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoScreensaverView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.PlaybackFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Oculo (0.14.0): the pause screen. Upstream keeps the controls on screen for as long as the video is paused
 * (PlayerUIController only auto-hides them while playing) or shows nothing over the frozen frame when the pause came
 * from the remote's play / pause key, and has no pause overlay (its « Show UI on pause » string is unused). Here,
 * {@link OculoPauseScreenRules#SHOW_DELAY_MS} after a pause by the user or after the last key pressed while paused, a
 * full-screen layer fades in over the frozen frame (and over the controls, which it hides): a dark gradient, strongest
 * at the bottom left, the « Paused » label, the channel (and its avatar when Glide already has it), the title
 * (Roboto Condensed, two lines), « 23 min left · ends at 9:47 PM » (or « Live ») and a thin bar with elapsed / total;
 * the clock and date at the top right as in the top bar.
 *
 * <p>Keys: the pause screen never takes a key from the player. Any key (but volume / mute) fades it out and, when the
 * controls were on screen before, puts them back first, so the key does exactly what it did before the pause screen
 * showed (OK on the play button resumes, Back hides the controls, left / right move the focus or seek…). One exception:
 * when the screensaver has come over the pause screen, the key that wakes the screensaver only wakes it, and the pause
 * screen is there again.</p>
 *
 * <p>Hidden at once on resume, seek, end of video, PIP or anything opening over the player (the Oculo panel and the
 * dialogs are another activity: the fragment pauses), and never in the player's « screen off » mode. No continuous
 * redraw: static views, one fade in and one fade out; the clock and the end time change once a minute.</p>
 */
public final class OculoPauseScreen implements Player.EventListener {
    private static WeakReference<OculoPauseScreen> sCurrent = new WeakReference<>(null);
    private final WeakReference<PlaybackFragment> mFragment;
    private final ViewGroup mRoot;
    private final Runnable mShow = this::showIfNeeded;
    private final Runnable mTick = this::tick;
    private final Runnable mCheckDim = this::checkDim;
    @Nullable
    private SimpleExoPlayer mPlayer;
    @Nullable
    private FrameLayout mView;
    private TextView mClock;
    private TextView mDate;
    private ImageView mAvatar;
    private TextView mChannel;
    private TextView mTitle;
    private TextView mInfo;
    private View mBarRow;
    private ClipDrawable mBarPlayed;
    private TextView mElapsed;
    private TextView mTotal;
    private boolean mShown;
    private boolean mControlsWereShown;
    private boolean mChangingControls;
    private boolean mPaused;
    private long mPausedAtMs;
    private long mLastKeyMs;
    private int mConsumedKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    @Nullable
    private String mChannelIconUrl;
    @Nullable
    private String mBoundIconUrl;

    private OculoPauseScreen(PlaybackFragment fragment, ViewGroup root) {
        mFragment = new WeakReference<>(fragment);
        mRoot = root;
    }

    // ---- Hooks (PlaybackFragment, PlaybackActivity) ----

    /** PlaybackFragment.onCreateView(): the player's root (lb_playback_fragment.xml). */
    public static void attach(PlaybackFragment fragment, @Nullable View root) {
        if (fragment == null || !(root instanceof ViewGroup)) {
            return;
        }
        OculoPauseScreen previous = sCurrent.get();
        if (previous != null) {
            previous.release();
        }
        OculoPauseScreen screen = new OculoPauseScreen(fragment, (ViewGroup) root);
        sCurrent = new WeakReference<>(screen);
        OculoScreensaverView.setDimListener(() -> {
            OculoPauseScreen current = sCurrent.get();
            if (current != null) {
                // After ScreensaverManager has filled or emptied its container
                current.mRoot.removeCallbacks(current.mCheckDim);
                current.mRoot.post(current.mCheckDim);
            }
        });
    }

    /** PlaybackFragment.createPlayer(): each new ExoPlayer (the player is released and created again on stop). */
    public static void onPlayerCreated(PlaybackFragment fragment, @Nullable SimpleExoPlayer player) {
        OculoPauseScreen screen = get(fragment);
        if (screen == null || player == null) {
            return;
        }
        if (screen.mPlayer != null) {
            screen.mPlayer.removeListener(screen);
        }
        screen.mPlayer = player;
        player.addListener(screen);
        screen.hide(false);
        screen.mPaused = false;
    }

    public static void onResume(PlaybackFragment fragment) {
        OculoPauseScreen screen = get(fragment);
        if (screen != null) {
            screen.mLastKeyMs = now();
            screen.arm();
        }
    }

    /** Also on PIP and when the Oculo panel or a dialog opens over the player (another activity). */
    public static void onPause(PlaybackFragment fragment) {
        OculoPauseScreen screen = get(fragment);
        if (screen != null) {
            screen.mRoot.removeCallbacks(screen.mShow);
            screen.hide(false);
        }
    }

    /** PlaybackFragment.showControlsOverlay(): the controls came back by another way (remote, phone…). */
    public static void onControlsShown(PlaybackFragment fragment) {
        OculoPauseScreen screen = get(fragment);
        if (screen != null && screen.mShown && !screen.mChangingControls) {
            screen.mControlsWereShown = false;
            screen.hide(true);
        }
    }

    /** PlaybackFragment.setChannelIcon(): the avatar the player's channel button loads. */
    public static void setChannelIcon(PlaybackFragment fragment, @Nullable String iconUrl) {
        OculoPauseScreen screen = get(fragment);
        if (screen != null) {
            screen.mChannelIconUrl = iconUrl;
        }
    }

    /**
     * PlaybackActivity.dispatchKeyEvent(), before anything else. Returns true only for the key that wakes the
     * screensaver over the pause screen (and its repeats and release): it is consumed, the screensaver is woken.
     */
    public static boolean dispatchKeyEvent(Activity activity, PlaybackFragment fragment, KeyEvent event) {
        OculoPauseScreen screen = get(fragment);
        return screen != null && event != null && screen.onKey(activity, event);
    }

    // ---- Logic ----

    @Nullable
    private static OculoPauseScreen get(PlaybackFragment fragment) {
        OculoPauseScreen screen = sCurrent.get();
        return screen != null && fragment != null && screen.mFragment.get() == fragment ? screen : null;
    }

    private static long now() {
        return SystemClock.uptimeMillis();
    }

    private void release() {
        mRoot.removeCallbacks(mShow);
        mRoot.removeCallbacks(mTick);
        mRoot.removeCallbacks(mCheckDim);
        if (mPlayer != null) {
            mPlayer.removeListener(this);
            mPlayer = null;
        }
    }

    private boolean onKey(Activity activity, KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0) {
            if (keyCode == mConsumedKeyCode) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mConsumedKeyCode = KeyEvent.KEYCODE_UNKNOWN;
                }
                return true;
            }
            return false;
        }
        mConsumedKeyCode = KeyEvent.KEYCODE_UNKNOWN;

        if (OculoPauseScreenRules.isSilentKey(keyCode)) {
            return false;
        }

        mLastKeyMs = now();

        if (mShown) {
            if (isScreensaverShown(activity)) {
                // The screensaver goes, the pause screen under it stays
                mConsumedKeyCode = keyCode;
                ScreensaverManager manager = activity instanceof MotherActivity ? ((MotherActivity) activity).getScreensaverManager() : null;
                if (manager != null) {
                    manager.enable();
                }
                return true;
            }
            hide(true);
        }

        arm();
        return false;
    }

    /** (Re)starts the delay while paused. */
    private void arm() {
        mRoot.removeCallbacks(mShow);
        if (mShown || !isPausedNow()) {
            return;
        }
        long delay = OculoPauseScreenRules.delayMs(now(), mPausedAtMs, mLastKeyMs);
        mRoot.postDelayed(mShow, Math.max(50, delay));
    }

    private boolean isPausedNow() {
        return mPlayer != null && !mPlayer.getPlayWhenReady() && mPlayer.getPlaybackState() == Player.STATE_READY;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        boolean paused = !playWhenReady && playbackState == Player.STATE_READY;
        if (paused && !mPaused) {
            mPausedAtMs = now();
        }
        mPaused = paused;

        if (!paused) {
            mRoot.removeCallbacks(mShow);
            // Resume: let a fade started by the key finish; end of video, buffering, new video: at once
            hide(playWhenReady && playbackState == Player.STATE_READY);
            return;
        }

        arm();
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        // Seek (while paused: the frame changes) or next item
        mLastKeyMs = now();
        hide(true);
        arm();
    }

    private void showIfNeeded() {
        PlaybackFragment fragment = mFragment.get();
        if (fragment == null || mShown || fragment.getActivity() == null) {
            return;
        }
        Activity activity = fragment.getActivity();
        Video video = fragment.getVideo();

        OculoPauseScreenRules.State state = new OculoPauseScreenRules.State();
        state.enabled = OculoData.instance(activity).isPauseScreenEnabled();
        state.foreground = fragment.isResumed() && !fragment.isInPIPMode()
                && !AppDialogPresenter.instance(activity).isDialogShown();
        state.hasVideo = video != null && !video.isUpcoming && !video.isUnplayable;
        state.paused = isPausedNow();
        state.ended = mPlayer != null && mPlayer.getPlaybackState() == Player.STATE_ENDED;
        state.live = video != null && video.isLive;
        state.suggestionsShown = fragment.isSuggestionsShown();
        state.screenOff = isScreenOff(activity);
        state.positionMs = fragment.getPositionMs();
        state.durationMs = fragment.getDurationMs();

        if (!OculoPauseScreenRules.shouldShow(state)) {
            return;
        }

        // Hide upstream's controls (they come back with the next key); seeking may refuse it
        mControlsWereShown = fragment.isOverlayShown();
        if (mControlsWereShown) {
            mChangingControls = true;
            fragment.showOverlay(false);
            mChangingControls = false;
            if (fragment.isOverlayShown()) {
                return;
            }
        }

        ensureView(activity);
        bind(fragment, video, state);
        mShown = true;
        setCornerWidgetsAlpha(0f);
        mView.animate().cancel();
        mView.setVisibility(View.VISIBLE);
        mView.setAlpha(0f);
        mView.animate().alpha(1f).setDuration(OculoPauseScreenRules.FADE_IN_MS).setListener(null).start();
        mRoot.removeCallbacks(mTick);
        mRoot.postDelayed(mTick, OculoScreensaverRules.untilNextMinute(System.currentTimeMillis()) + 200);
    }

    /** Hides the pause screen; the controls come back when they were on screen before it. */
    private void hide(boolean fade) {
        mRoot.removeCallbacks(mTick);
        if (mView == null) {
            mShown = false;
            return;
        }
        boolean wasShown = mShown;
        mShown = false;

        if (wasShown && mControlsWereShown) {
            PlaybackFragment fragment = mFragment.get();
            if (fragment != null && fragment.isResumed()) {
                mChangingControls = true;
                fragment.showControls(true);
                mChangingControls = false;
            }
        }
        mControlsWereShown = false;
        setCornerWidgetsAlpha(1f);

        if (!wasShown) {
            if (!fade && mView.getVisibility() == View.VISIBLE) {
                // A fade out is running and something asks for « at once »
                mView.animate().cancel();
                mView.setVisibility(View.GONE);
            }
            return;
        }

        mView.animate().cancel();
        if (fade) {
            final View view = mView;
            view.animate().alpha(0f).setDuration(OculoPauseScreenRules.FADE_OUT_MS)
                    .withEndAction(() -> {
                        if (!mShown) {
                            view.setVisibility(View.GONE);
                        }
                    }).start();
        } else {
            mView.setAlpha(0f);
            mView.setVisibility(View.GONE);
        }
    }

    /** The dim container changed: the « screen off » veil sends the pause screen away. */
    private void checkDim() {
        PlaybackFragment fragment = mFragment.get();
        if (mShown && fragment != null && isScreenOff(fragment.getActivity())) {
            hide(false);
        }
    }

    /** Once a minute while shown: the clock and the end time. */
    private void tick() {
        PlaybackFragment fragment = mFragment.get();
        if (!mShown || fragment == null || mView == null) {
            return;
        }
        bindClockAndInfo(fragment, fragment.getVideo());
        mRoot.postDelayed(mTick, OculoScreensaverRules.untilNextMinute(System.currentTimeMillis()) + 200);
    }

    @Nullable
    private static View dimContainer(@Nullable Activity activity) {
        return activity != null ? activity.getWindow().getDecorView().findViewById(R.id.dim_container) : null;
    }

    private static boolean isDimShown(@Nullable View dim) {
        return dim != null && dim.getVisibility() == View.VISIBLE;
    }

    /** The player's « screen off » mode (black veil, full or partial), not the screensaver. */
    private static boolean isScreenOff(@Nullable Activity activity) {
        View dim = dimContainer(activity);
        if (!isDimShown(dim)) {
            return false;
        }
        if (OculoScreensaverView.isEnabled(activity)) {
            return !OculoScreensaverView.isShowing(dim);
        }
        ScreensaverManager manager = activity instanceof MotherActivity ? ((MotherActivity) activity).getScreensaverManager() : null;
        return manager != null && manager.isScreenOff();
    }

    private static boolean isScreensaverShown(@Nullable Activity activity) {
        return isDimShown(dimContainer(activity)) && !isScreenOff(activity);
    }

    /** Upstream's corner clock and end time (player_overlay_wrapper) sit where the pause screen's clock is. */
    private void setCornerWidgetsAlpha(float alpha) {
        View corner = mRoot.findViewById(R.id.player_overlay_wrapper);
        if (corner != null) {
            corner.setAlpha(alpha);
        }
    }

    // ---- Views ----

    private void ensureView(Context context) {
        if (mView != null) {
            return;
        }
        Resources res = context.getResources();
        FrameLayout view = new FrameLayout(context);
        view.setFocusable(false);
        view.setClickable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view.setBackground(veil(res));
        view.setVisibility(View.GONE);

        // Top right: clock over the date, as in the top bar
        LinearLayout clockBox = new LinearLayout(context);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.CENTER_HORIZONTAL);
        mClock = text(context, res.getDimension(R.dimen.oculo_top_bar_time_size), R.color.oculo_pause_title, "sans-serif", Typeface.BOLD);
        mDate = text(context, res.getDimension(R.dimen.oculo_top_bar_date_size), R.color.oculo_pause_title, "sans-serif", Typeface.BOLD);
        clockBox.addView(mClock, wrap());
        clockBox.addView(mDate, wrap());
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        clockLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.oculo_pause_clock_margin_end));
        clockLp.topMargin = dp(res, 34);
        view.addView(clockBox, clockLp);

        // Bottom left: label, channel, title, info line, bar
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams blockLp = new FrameLayout.LayoutParams(
                res.getDimensionPixelSize(R.dimen.oculo_pause_block_width), ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        blockLp.setMarginStart(res.getDimensionPixelSize(R.dimen.oculo_pause_margin_start));
        blockLp.bottomMargin = res.getDimensionPixelSize(R.dimen.oculo_pause_margin_bottom);
        view.addView(block, blockLp);

        // « ❚❚ Paused »
        LinearLayout label = new LinearLayout(context);
        label.setOrientation(LinearLayout.HORIZONTAL);
        label.setGravity(Gravity.CENTER_VERTICAL);
        View glyph = new View(context);
        glyph.setBackground(pauseGlyph(res));
        label.addView(glyph, new LinearLayout.LayoutParams(dp(res, 9), dp(res, 11)));
        TextView labelText = text(context, res.getDimension(R.dimen.oculo_pause_label_size), R.color.oculo_pause_label, "sans-serif-medium", Typeface.NORMAL);
        labelText.setText(R.string.oculo_pause_paused);
        labelText.setAllCaps(true);
        labelText.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams labelTextLp = wrap();
        labelTextLp.setMarginStart(dp(res, 7));
        label.addView(labelText, labelTextLp);
        block.addView(label, wrap());

        LinearLayout channelRow = new LinearLayout(context);
        channelRow.setOrientation(LinearLayout.HORIZONTAL);
        channelRow.setGravity(Gravity.CENTER_VERTICAL);
        mAvatar = new ImageView(context);
        mAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mAvatar.setVisibility(View.GONE);
        int avatar = res.getDimensionPixelSize(R.dimen.oculo_pause_avatar);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(avatar, avatar);
        avatarLp.setMarginEnd(dp(res, 8));
        channelRow.addView(mAvatar, avatarLp);
        mChannel = text(context, res.getDimension(R.dimen.oculo_pause_channel_size), R.color.oculo_pause_channel, "sans-serif", Typeface.NORMAL);
        mChannel.setSingleLine(true);
        mChannel.setEllipsize(TextUtils.TruncateAt.END);
        channelRow.addView(mChannel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams channelLp = matchWidth();
        channelLp.topMargin = dp(res, 14);
        block.addView(channelRow, channelLp);

        mTitle = text(context, res.getDimension(R.dimen.oculo_pause_title_size), R.color.oculo_pause_title, "sans-serif-condensed", Typeface.NORMAL);
        mTitle.setMaxLines(2);
        mTitle.setEllipsize(TextUtils.TruncateAt.END);
        mTitle.setLineSpacing(0, 1.04f);
        LinearLayout.LayoutParams titleLp = matchWidth();
        titleLp.topMargin = dp(res, 5);
        block.addView(mTitle, titleLp);

        mInfo = text(context, res.getDimension(R.dimen.oculo_pause_info_size), R.color.oculo_pause_info, "sans-serif", Typeface.NORMAL);
        mInfo.setSingleLine(true);
        mInfo.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams infoLp = matchWidth();
        infoLp.topMargin = dp(res, 10);
        block.addView(mInfo, infoLp);

        LinearLayout barRow = new LinearLayout(context);
        barRow.setOrientation(LinearLayout.HORIZONTAL);
        barRow.setGravity(Gravity.CENTER_VERTICAL);
        mElapsed = text(context, res.getDimension(R.dimen.oculo_pause_time_size), R.color.oculo_pause_time, "sans-serif", Typeface.NORMAL);
        mTotal = text(context, res.getDimension(R.dimen.oculo_pause_time_size), R.color.oculo_pause_time, "sans-serif", Typeface.NORMAL);
        View bar = new View(context);
        bar.setBackground(bar(res));
        mBarPlayed = (ClipDrawable) ((LayerDrawable) bar.getBackground()).getDrawable(1);
        barRow.addView(mElapsed, wrap());
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(0, res.getDimensionPixelSize(R.dimen.oculo_pause_bar_height), 1f);
        barLp.setMarginStart(dp(res, 12));
        barLp.setMarginEnd(dp(res, 12));
        barRow.addView(bar, barLp);
        barRow.addView(mTotal, wrap());
        mBarRow = barRow;
        LinearLayout.LayoutParams barRowLp = matchWidth();
        barRowLp.topMargin = dp(res, 14);
        block.addView(barRow, barRowLp);

        // Above the controls and everything else of the player; the screensaver's dim container stays above it
        mRoot.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mView = view;
    }

    private void bind(PlaybackFragment fragment, @Nullable Video video, OculoPauseScreenRules.State state) {
        String channel = video != null ? video.getAuthor() : null;
        mChannel.setText(channel != null ? channel : "");
        mChannel.setVisibility(TextUtils.isEmpty(channel) ? View.INVISIBLE : View.VISIBLE);
        String title = video != null ? video.getTitleFull() : null;
        mTitle.setText(title != null ? title : "");
        bindAvatar();
        bindClockAndInfo(fragment, video);
    }

    private void bindClockAndInfo(PlaybackFragment fragment, @Nullable Video video) {
        Context context = fragment.getContext();
        if (context == null) {
            return;
        }
        Resources res = context.getResources();
        mClock.setText(DateHelper.getCurrentTimeShort());
        mDate.setText(DateHelper.getCurrentDateShort());

        if (video != null && video.isLive) {
            mInfo.setText(R.string.oculo_pause_live);
            mInfo.setTextColor(res.getColor(R.color.oculo_pause_live));
            mBarRow.setVisibility(View.GONE);
            return;
        }

        long position = fragment.getPositionMs();
        long duration = fragment.getDurationMs();
        Locale locale = res.getConfiguration().locale;
        OculoPauseScreenRules.Texts texts = new OculoPauseScreenRules.Texts(
                res.getString(R.string.oculo_pause_left_one), res.getString(R.string.oculo_pause_left_other),
                res.getString(R.string.oculo_pause_left_hours), res.getString(R.string.oculo_pause_left_less),
                res.getString(R.string.oculo_pause_ends_at), res.getString(R.string.oculo_pause_separator));
        mInfo.setText(OculoPauseScreenRules.infoLine(System.currentTimeMillis(), position, duration,
                fragment.getSpeed(), is24Hour(), texts, locale, TimeZone.getDefault()));
        mInfo.setTextColor(res.getColor(R.color.oculo_pause_info));

        boolean hours = duration >= 3_600_000;
        mElapsed.setText(OculoPauseScreenRules.clock(position, hours));
        mTotal.setText(OculoPauseScreenRules.clock(duration, hours));
        mBarPlayed.setLevel(Math.round(OculoPauseScreenRules.progress(position, duration) * 10_000));
        mBarRow.setVisibility(View.VISIBLE);
    }

    /** The app's « Time format » (12 / 24 h), as its other clocks (DateHelper). */
    private static boolean is24Hour() {
        return GlobalPreferences.sInstance != null ? GlobalPreferences.sInstance.is24HourLocaleEnabled() : DateHelper.is24HourLocale();
    }

    /** Only what Glide already has (the player's channel button loaded it): no request. */
    private void bindAvatar() {
        final String url = mChannelIconUrl;
        if (url == null) {
            mBoundIconUrl = null;
            Glide.with(mAvatar.getContext().getApplicationContext()).clear(mAvatar);
            mAvatar.setVisibility(View.GONE);
            return;
        }
        if (url.equals(mBoundIconUrl) && mAvatar.getVisibility() == View.VISIBLE) {
            return;
        }
        mBoundIconUrl = url;
        mAvatar.setVisibility(View.GONE);
        // Same request as VideoPlayerGlue.setChannelIcon() (options, circle crop, the button icon's size), so the
        // picture comes from Glide's memory cache (those options keep nothing on disk)
        Drawable button = ContextCompat.getDrawable(mAvatar.getContext(), R.drawable.action_channel);
        int width = button != null ? button.getIntrinsicWidth() : Target.SIZE_ORIGINAL;
        int height = button != null ? button.getIntrinsicHeight() : Target.SIZE_ORIGINAL;
        Glide.with(mAvatar.getContext())
                .load(url)
                .apply(ViewUtil.glideOptions())
                .circleCrop()
                .override(width, height)
                .onlyRetrieveFromCache(true) // no dontAnimate(): it is an option, so part of the cache key
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        mAvatar.setVisibility(View.GONE);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (url.equals(mBoundIconUrl)) {
                            mAvatar.setVisibility(View.VISIBLE);
                        }
                        return false;
                    }
                })
                .into(mAvatar);
    }

    private static Drawable veil(Resources res) {
        GradientDrawable bottom = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{
                res.getColor(R.color.oculo_pause_bottom_start), res.getColor(R.color.oculo_pause_bottom_center), 0});
        GradientDrawable left = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{
                res.getColor(R.color.oculo_pause_left_start), res.getColor(R.color.oculo_pause_left_center), 0});
        GradientDrawable corner = new GradientDrawable();
        corner.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        corner.setColors(new int[]{res.getColor(R.color.oculo_pause_corner), 0});
        corner.setGradientCenter(1f, 0f);
        corner.setGradientRadius(dp(res, 230));
        GradientDrawable flat = new GradientDrawable();
        flat.setColor(res.getColor(R.color.oculo_pause_veil));
        for (GradientDrawable drawable : new GradientDrawable[]{bottom, left, corner}) {
            drawable.setDither(true);
        }
        return new LayerDrawable(new Drawable[]{flat, bottom, left, corner});
    }

    private static Drawable bar(Resources res) {
        float radius = dp(res, 2);
        GradientDrawable track = new GradientDrawable();
        track.setColor(res.getColor(R.color.oculo_pause_track));
        track.setCornerRadius(radius);
        GradientDrawable played = new GradientDrawable();
        played.setColor(res.getColor(R.color.oculo_pause_played));
        played.setCornerRadius(radius);
        return new LayerDrawable(new Drawable[]{track, new ClipDrawable(played, Gravity.START, ClipDrawable.HORIZONTAL)});
    }

    /** Two rounded bars. */
    private static Drawable pauseGlyph(Resources res) {
        int color = res.getColor(R.color.oculo_pause_label);
        GradientDrawable left = new GradientDrawable();
        left.setColor(color);
        left.setCornerRadius(dp(res, 1));
        GradientDrawable right = new GradientDrawable();
        right.setColor(color);
        right.setCornerRadius(dp(res, 1));
        LayerDrawable glyph = new LayerDrawable(new Drawable[]{left, right});
        int bar = dp(res, 3);
        int width = dp(res, 9);
        glyph.setLayerInset(0, 0, 0, width - bar, 0);
        glyph.setLayerInset(1, width - bar, 0, 0, 0);
        return glyph;
    }

    @NonNull
    private static TextView text(Context context, float sizePx, int colorRes, String family, int style) {
        TextView view = new TextView(context);
        view.setTypeface(Typeface.create(family, style));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        view.setTextColor(context.getResources().getColor(colorRes));
        view.setIncludeFontPadding(false);
        view.setShadowLayer(dp(context.getResources(), 2), 0, dp(context.getResources(), 1), 0x66000000);
        return view;
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(Resources res, int dp) {
        return Math.round(dp * res.getDisplayMetrics().density);
    }
}
