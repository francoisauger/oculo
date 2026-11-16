package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo: reads what an upstream settings presenter would show, without showing it.
 *
 * Upstream presenters build their screen into the {@code AppDialogPresenter} singleton, then call
 * {@code showDialog(title, onFinish)}. While {@link #run(Runnable)} is running, {@code AppDialogPresenter.showDialog()}
 * hands its categories to {@link #onShowDialog} and returns without starting a view (one hook in that file). The
 * captured {@code OptionItem}s keep upstream's own callbacks, so the Oculo screens write exactly the same
 * preferences as the classic ones. Main thread only (like the presenters).
 */
public final class OculoSettingsCapture {
    private static Dialog sCurrent;

    /** A captured screen: what {@code showDialog()} received. */
    public static final class Dialog {
        public CharSequence title;
        public final List<OptionCategory> categories = new ArrayList<>();
        public final List<Runnable> onFinish = new ArrayList<>();

        public boolean isEmpty() {
            return categories.isEmpty();
        }
    }

    private OculoSettingsCapture() {
    }

    public static boolean isCapturing() {
        return sCurrent != null;
    }

    /**
     * Called by {@code AppDialogPresenter.showDialog()} while capturing. The presenter clears its own pending state
     * right after, so nothing is shown and the dialog currently on screen (if any) is untouched.
     */
    public static void onShowDialog(@Nullable CharSequence title, @Nullable List<OptionCategory> categories, @Nullable Runnable onFinish) {
        Dialog dialog = sCurrent;

        if (dialog == null) {
            return;
        }

        if (dialog.title == null) {
            dialog.title = title;
        }

        if (categories != null) {
            dialog.categories.addAll(categories);
        }

        if (onFinish != null) {
            dialog.onFinish.add(onFinish);
        }
    }

    /**
     * Runs {@code builder} (e.g. {@code GeneralSettingsPresenter.instance(context).show()}) and returns what it would have
     * shown. Never throws: a presenter that fails returns what was captured so far.
     */
    public static Dialog run(Runnable builder) {
        Dialog previous = sCurrent;
        Dialog dialog = new Dialog();
        sCurrent = dialog;

        try {
            builder.run();
        } catch (RuntimeException e) {
            com.liskovsoft.sharedutils.mylogger.Log.e("OculoSettings", "Capture failed: " + e);
        } finally {
            sCurrent = previous;
        }

        return dialog;
    }
}
