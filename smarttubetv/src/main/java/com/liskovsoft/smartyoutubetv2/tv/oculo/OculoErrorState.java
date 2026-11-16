package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.CategoryEmptyError;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.SignInError;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.fragments.ErrorSupportFragment;

/**
 * Oculo (32.74-18, design.md M9): empty sections, « sign in » and load errors in Oculo's style instead of Android's
 * sad cloud on a black veil: the logo's TV outline in grey ({@code drawable/oculo_ic_tv_outline}), a 22sp title, a
 * 16sp grey explanation, for load errors a small detail line (exception, one or two lines instead of upstream's stack
 * trace), and a white pill button in normal case (« Se connecter », « Réessayer »). The page background shows through
 * (Home's glow). Only when the flavor's {@code lb_error_fragment.xml} (with {@code oculo_error_title}) is in use.
 */
public final class OculoErrorState {
    private static final String EMPTY_RESULT = "fromNullable result is null";
    private static final int DETAIL_MAX_CHARS = 160;

    private OculoErrorState() {
    }

    /**
     * Returns true when the state was shown the Oculo way (the caller skips upstream's content).
     */
    public static boolean apply(ErrorSupportFragment fragment, @Nullable View root, @Nullable ErrorFragmentData data) {
        if (root == null || data == null) {
            return false;
        }
        TextView titleView = root.findViewById(R.id.oculo_error_title);
        TextView detailView = root.findViewById(R.id.oculo_error_detail);
        if (titleView == null) {
            return false;
        }
        Context context = root.getContext();

        String title;
        String text;
        String detail = null;
        String button;
        View.OnClickListener action;
        Runnable retry = () -> BrowsePresenter.instance(context).refresh();

        if (data instanceof SignInError) {
            title = context.getString(R.string.oculo_state_signin_title);
            text = context.getString(R.string.oculo_state_signin_text);
            button = context.getString(R.string.oculo_state_signin_button);
            action = v -> data.onAction();
        } else if (data instanceof CategoryEmptyError) {
            Throwable error = ((CategoryEmptyError) data).getError();
            boolean failed = error != null && !Helpers.containsAny(error.getMessage(), EMPTY_RESULT);
            if (failed && Helpers.startsWith(error.getMessage(), "AuthError")) {
                title = context.getString(R.string.oculo_state_signin_title);
                text = context.getString(R.string.oculo_state_signin_text);
                button = context.getString(R.string.oculo_state_signin_button);
                action = v -> data.onAction();
            } else if (failed) {
                title = context.getString(R.string.oculo_state_error_title);
                text = context.getString(R.string.oculo_state_error_text);
                detail = describe(error);
                button = context.getString(R.string.oculo_state_retry);
                action = v -> retry.run();
            } else {
                title = context.getString(R.string.oculo_state_empty_title);
                text = context.getString(R.string.oculo_state_empty_text);
                button = context.getString(R.string.oculo_state_retry);
                action = v -> retry.run();
            }
        } else {
            title = null;
            text = data.getMessage();
            button = data.getActionText();
            action = button != null ? v -> data.onAction() : null;
        }

        fragment.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        fragment.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.oculo_ic_tv_outline));
        titleView.setText(title);
        titleView.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);
        fragment.setMessage(text);
        if (detailView != null) {
            detailView.setText(detail);
            detailView.setVisibility(TextUtils.isEmpty(detail) ? View.GONE : View.VISIBLE);
        }
        if (!TextUtils.isEmpty(button)) {
            fragment.setButtonText(button);
            fragment.setButtonClickListener(action);
        } else {
            fragment.setButtonText(null);
        }

        // ErrorSupportFragment set baseline-based margins in onCreateView: back to the layout's spacing
        setTopMargin(root.findViewById(R.id.message), titleView.getVisibility() == View.VISIBLE ? R.dimen.oculo_state_gap_small : R.dimen.oculo_state_gap_large);
        setTopMargin(root.findViewById(R.id.button), R.dimen.oculo_state_gap_button);
        CenterRightOfRail.install(root);
        return true;
    }

    /**
     * 32.74-20: the state is centered in the part of the screen right of the rail, not on the whole window (it sat
     * partly under the expanded rail). The visible area runs from the content's start line, read from the top bar's
     * mic and pill group (it follows the rail, animated, see OculoTopBar.setStart()), to the rows' right padding.
     * Checked before each frame while the state is shown, so it follows the rail as it opens and closes.
     */
    private static final class CenterRightOfRail implements ViewTreeObserver.OnPreDrawListener,
            View.OnAttachStateChangeListener {
        private final View mFrame;
        private final View mContent;
        private final int[] mLocation = new int[2];

        private CenterRightOfRail(View frame, View content) {
            mFrame = frame;
            mContent = content;
        }

        static void install(View root) {
            View content = root.findViewById(R.id.oculo_error_content);
            if (content == null || root.getTag(R.id.oculo_error_content) != null) {
                return;
            }
            CenterRightOfRail centering = new CenterRightOfRail(root, content);
            root.setTag(R.id.oculo_error_content, centering);
            root.addOnAttachStateChangeListener(centering);
            if (root.isAttachedToWindow()) {
                centering.onViewAttachedToWindow(root);
            }
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            v.getViewTreeObserver().addOnPreDrawListener(this);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            v.getViewTreeObserver().removeOnPreDrawListener(this);
        }

        @Override
        public boolean onPreDraw() {
            View root = mFrame.getRootView();
            View orb = root != null ? root.findViewById(R.id.title_orb) : null;
            View bar = orb != null && orb.getParent() instanceof View ? (View) orb.getParent() : null;
            if (bar == null || !bar.isShown() || mFrame.getWidth() == 0) {
                return true;
            }
            bar.getLocationOnScreen(mLocation);
            int start = mLocation[0];
            mFrame.getLocationOnScreen(mLocation);
            int end = root.getWidth() - mFrame.getResources().getDimensionPixelSize(R.dimen.lb_browse_padding_start);
            float naturalCenter = mLocation[0] + mFrame.getWidth() / 2f;
            float target = start < end ? (start + end) / 2f - naturalCenter : 0;
            if (Math.abs(mContent.getTranslationX() - target) >= 1) {
                mContent.setTranslationX(target);
            }
            return true;
        }
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        String result = error.getClass().getSimpleName() + (TextUtils.isEmpty(message) ? "" : ": " + message.split("\n", 2)[0]);
        return result.length() > DETAIL_MAX_CHARS ? result.substring(0, DETAIL_MAX_CHARS - 1) + "…" : result;
    }

    private static void setTopMargin(@Nullable View view, int dimenRes) {
        if (view != null && view.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.topMargin = view.getResources().getDimensionPixelSize(dimenRes);
            view.setLayoutParams(lp);
        }
    }
}
