package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Oculo (0.13.0): the player's sleep timer, chosen in the player's settings panel (« Sleep timer »: Off, 15, 30, 45,
 * 60, 90 min, End of video; flag {@code R.bool.oculo_sleep_timer}). The current value shows at the right of the
 * row (« Off », « 23 min left », « End of video »). 30 s before the end, a small card at the bottom right (the
 * SponsorBlock notice's look, OculoNotice) asks « Still watching? Pausing in 30 s » with « Keep watching » (the same
 * duration again) and « Turn off »; closing it with any other key also keeps watching. At 0 the video is paused
 * and the timer goes back to Off. « End of video » pauses when the current video ends instead of playing the next.
 *
 * <p>Not saved: a new process starts with Off. Upstream's idle timer (« pause if the remote isn't used for N
 * hours », PlayerData.getSleepTimerHours(), Settings › Playback « Pause when idle ») is untouched and still runs.
 * Two one-shot timers (warning, pause) on the main thread; the card's countdown text is updated once a second by
 * OculoNotice while it is on screen.</p>
 */
public final class OculoSleepTimer {
    private static final String TAG = "OculoSleepTimer";
    public static final int CATEGORY_ID = 0x0C1A5100;
    private static final int DIALOG_ID = 0x0C1A5101;
    private static final long RETRY_MS = 3_000;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static int sChoice = OculoSleepTimerRules.OFF;
    private static long sDeadline;
    @Nullable
    private static Context sContext;
    private static boolean sWarningShown;
    private static boolean sWarningHandled;
    private static final Runnable sWarn = OculoSleepTimer::warn;
    private static final Runnable sExpire = OculoSleepTimer::expire;

    private OculoSleepTimer() {
    }

    public static boolean isEnabled(@Nullable Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_sleep_timer);
    }

    /** HQDialogController (player settings panel): the « Sleep timer » row. */
    public static OptionCategory createCategory(Context context) {
        sContext = context.getApplicationContext();
        String title = context.getString(R.string.oculo_sleep_timer);
        List<OptionItem> options = new ArrayList<>();
        for (int choice : OculoSleepTimerRules.CHOICES) {
            options.add(UiOptionItem.from(choiceTitle(context, choice), option -> select(choice), choice == sChoice));
        }
        WeakReference<Context> localized = new WeakReference<>(context); // the activity: localized strings, not kept
        OculoPanelInfo.putRowValue(title, () -> {
            Context current = localized.get();
            return current != null ? rowValue(current) : null;
        });
        return OptionCategory.from(CATEGORY_ID, OptionCategory.TYPE_RADIO_LIST, title, options);
    }

    private static String choiceTitle(Context context, int choice) {
        if (choice == OculoSleepTimerRules.OFF) {
            return context.getString(R.string.oculo_sleep_timer_off);
        }
        if (choice == OculoSleepTimerRules.END_OF_VIDEO) {
            return context.getString(R.string.oculo_sleep_timer_end_of_video);
        }
        return context.getString(R.string.oculo_sleep_timer_minutes, choice);
    }

    /** Value at the right of the row. */
    static CharSequence rowValue(Context context) {
        if (OculoSleepTimerRules.isDuration(sChoice)) {
            int minutes = OculoSleepTimerRules.minutesLeft(SystemClock.elapsedRealtime(), sDeadline);
            return context.getResources().getQuantityString(R.plurals.oculo_sleep_timer_left, minutes, minutes);
        }
        return choiceTitle(context, sChoice);
    }

    private static void select(int choice) {
        sHandler.removeCallbacks(sWarn);
        sHandler.removeCallbacks(sExpire);
        sChoice = choice;
        sDeadline = 0;
        if (OculoSleepTimerRules.isDuration(choice)) {
            schedule(choice);
        }
        Log.d(TAG, "Sleep timer: %s", choice);
    }

    private static void schedule(int minutes) {
        long now = SystemClock.elapsedRealtime();
        sDeadline = OculoSleepTimerRules.deadline(now, minutes);
        sHandler.postDelayed(sWarn, OculoSleepTimerRules.warningAt(now, sDeadline) - now);
        sHandler.postDelayed(sExpire, sDeadline - now);
    }

    /** VideoLoaderController.onPlayEnd(): true = « End of video » was chosen, the caller stops instead of going on. */
    public static boolean onPlayEnd() {
        if (sChoice != OculoSleepTimerRules.END_OF_VIDEO) {
            return false;
        }
        select(OculoSleepTimerRules.OFF);
        return true;
    }

    @Nullable
    private static PlaybackView playingView() {
        if (sContext == null) {
            return null;
        }
        PlaybackView view = PlaybackPresenter.instance(sContext).getView();
        return view != null && !view.isEmbed() && view.isPlaying() ? view : null;
    }

    private static void warn() {
        PlaybackView view = playingView();
        if (view == null || sContext == null) {
            return; // paused or closed: nothing to ask; the pause still comes at the deadline
        }
        Context context = PlaybackPresenter.instance(sContext).getContext();
        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        if (dialog.isDialogShown()) {
            // Another drawer is open: don't cover it, ask again once it is closed (one check every 3 s)
            if (sDeadline - SystemClock.elapsedRealtime() > RETRY_MS + OculoSleepTimerRules.DISMISS_MARGIN_MS) {
                sHandler.postDelayed(sWarn, RETRY_MS);
            }
            return;
        }

        sWarningShown = true;
        sWarningHandled = false;
        dialog.appendSingleButton(UiOptionItem.from(context.getString(R.string.oculo_sleep_timer_keep_watching), option -> {
            sWarningHandled = true;
            extend();
            dialog.closeDialog();
        }));
        dialog.appendSingleButton(UiOptionItem.from(context.getString(R.string.oculo_sleep_timer_turn_off), option -> {
            sWarningHandled = true;
            select(OculoSleepTimerRules.OFF);
            dialog.closeDialog();
        }));
        long now = SystemClock.elapsedRealtime();
        dialog.setCloseTimeoutMs(Math.max(1, sDeadline - now));
        dialog.enableTransparent(true);
        dialog.enableOverlay(true);
        dialog.enableExpandable(false);
        dialog.setId(DIALOG_ID);
        dialog.showDialog(warningLabel(context), () -> {
            sWarningShown = false;
            if (!sWarningHandled && OculoSleepTimerRules.isDuration(sChoice)
                    && OculoSleepTimerRules.closedByUser(SystemClock.elapsedRealtime(), sDeadline)) {
                extend(); // closed with another key: someone is watching
            }
        });
    }

    private static void extend() {
        if (OculoSleepTimerRules.isDuration(sChoice)) {
            sHandler.removeCallbacks(sWarn);
            sHandler.removeCallbacks(sExpire);
            schedule(sChoice);
        }
    }

    private static void expire() {
        int choice = sChoice;
        select(OculoSleepTimerRules.OFF);
        if (!OculoSleepTimerRules.isDuration(choice)) {
            return;
        }
        if (sContext != null && sWarningShown) {
            AppDialogPresenter dialog = AppDialogPresenter.instance(PlaybackPresenter.instance(sContext).getContext());
            if (dialog.getId() == DIALOG_ID) {
                sWarningHandled = true;
                dialog.closeDialog();
            }
        }
        PlaybackView view = playingView();
        if (view != null) {
            view.setPlayWhenReady(false);
            view.showOverlay(true);
        }
    }

    /**
     * OculoNotice: the card's label while the warning is shown (« Still watching? Pausing in 23 s »), null when the
     * card is not the sleep timer's.
     */
    @Nullable
    public static String warningLabel(Context context) {
        if (context == null || !OculoSleepTimerRules.isDuration(sChoice)) {
            return null;
        }
        return context.getString(R.string.oculo_sleep_timer_warning,
                OculoSleepTimerRules.secondsLeft(SystemClock.elapsedRealtime(), sDeadline));
    }

    /** OculoNotice: true while the sleep timer's card is on screen. */
    public static boolean isWarningShown() {
        return sWarningShown;
    }
}
