package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
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
        return true;
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
