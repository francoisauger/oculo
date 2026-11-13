package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.liskovsoft.smartyoutubetv2.common.R;

/**
 * Oculo: in-player notice shown when SponsorBlock skips a segment automatically, instead of the system toast
 * (a text toast cannot be styled: since API 30 its view is the system's). Style of the board's « Commandite passée »
 * (docs/mockups/couleur-secondaire.html, {@code .skip}): a dark rounded pill at the top right of the player with a
 * 4 × 18dp bar in the segment color ({@code R.color.oculo_skip_notice_accent}, Abricot in the flavor) before the
 * text. Fades in, stays {@link #SHOW_MS}, fades out; a new skip replaces the text and restarts the timer.
 *
 * The view is added on top of the activity's content view (android.R.id.content); nothing else of the player is
 * touched. Off when {@code R.bool.oculo_skip_notice} is false (upstream), and falls back to the toast when the player
 * has no window focus (background playback, PIP) so the message is never lost.
 */
public final class OculoSkipNotice {
    private static final String TAG = "oculo_skip_notice";
    private static final long SHOW_MS = 3000;
    private static final long FADE_MS = 200;

    private OculoSkipNotice() {
    }

    /**
     * @return true when the notice was shown (the caller then skips its toast)
     */
    public static boolean show(Activity activity, String text) {
        if (activity == null || TextUtils.isEmpty(text) || activity.isFinishing()
                || !activity.getResources().getBoolean(R.bool.oculo_skip_notice)) {
            return false;
        }
        if (VERSION.SDK_INT >= 24 && activity.isInPictureInPictureMode()) {
            return false;
        }
        if (!activity.hasWindowFocus()) {
            return false;
        }

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) {
            return false;
        }

        FrameLayout root = (FrameLayout) content;
        LinearLayout notice = root.findViewWithTag(TAG);
        if (notice == null) {
            notice = create(root);
        } else {
            // Keep it on top of anything added since
            notice.bringToFront();
        }

        ((TextView) notice.getChildAt(1)).setText(text);

        notice.removeCallbacks((Runnable) notice.getTag(R.id.oculo_skip_notice_hide));
        notice.animate().cancel();
        notice.setVisibility(View.VISIBLE);
        notice.animate().alpha(1).setDuration(FADE_MS).start();

        LinearLayout finalNotice = notice;
        Runnable hide = () -> finalNotice.animate().alpha(0).setDuration(FADE_MS)
                .withEndAction(() -> {
                    if (finalNotice.getParent() instanceof ViewGroup) {
                        ((ViewGroup) finalNotice.getParent()).removeView(finalNotice);
                    }
                }).start();
        notice.setTag(R.id.oculo_skip_notice_hide, hide);
        notice.postDelayed(hide, SHOW_MS);

        return true;
    }

    private static LinearLayout create(FrameLayout root) {
        Resources res = root.getResources();

        LinearLayout notice = new LinearLayout(root.getContext());
        notice.setTag(TAG);
        notice.setOrientation(LinearLayout.HORIZONTAL);
        notice.setGravity(Gravity.CENTER_VERTICAL);
        notice.setFocusable(false);
        notice.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        notice.setAlpha(0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(root.getContext(), R.color.oculo_skip_notice_background));
        background.setCornerRadius(dp(res, 8));
        notice.setBackground(background);
        notice.setPadding(dp(res, 10), dp(res, 8), dp(res, 14), dp(res, 8));

        View bar = new View(root.getContext());
        GradientDrawable barShape = new GradientDrawable();
        barShape.setColor(ContextCompat.getColor(root.getContext(), R.color.oculo_skip_notice_accent));
        barShape.setCornerRadius(dp(res, 2));
        bar.setBackground(barShape);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(res, 4), dp(res, 18));
        barLp.setMarginEnd(dp(res, 10));
        notice.addView(bar, barLp);

        TextView label = new TextView(root.getContext());
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setMaxWidth(dp(res, 420));
        notice.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lp.topMargin = dp(res, 27);
        lp.setMarginEnd(dp(res, 48));
        root.addView(notice, lp);
        if (VERSION.SDK_INT >= 21) {
            notice.setElevation(dp(res, 8));
        }
        return notice;
    }

    private static int dp(Resources res, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, res.getDisplayMetrics()));
    }
}
