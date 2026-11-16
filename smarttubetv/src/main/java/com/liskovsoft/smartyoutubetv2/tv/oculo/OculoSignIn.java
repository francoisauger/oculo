package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoQr;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Oculo (32.74-18, design.md M8): the sign-in screen as two centered columns ({@code layout/oculo_signin.xml}): the
 * QR code (generated on the device, {@link OculoQr}) on a white card at left; at right three numbered steps, the
 * address in light Abricot, the code large in Abricot; « Done » / « Terminé » as the primary pill, « Open in browser »
 * as a secondary button. No « Works only with Firefox or Chrome » note. SignInFragment keeps leanback's GuidedStep
 * views (hidden, not focusable) so its lifecycle is unchanged; this layout is drawn over them. Part of the left rail
 * feature ({@code R.bool.oculo_left_rail}).
 */
public final class OculoSignIn {
    private final View mRoot;
    private final ImageView mQr;
    private final TextView mUrl;
    private final TextView mCode;
    private final View mDone;

    private OculoSignIn(View root) {
        mRoot = root;
        mQr = root.findViewById(R.id.oculo_signin_qr);
        mUrl = root.findViewById(R.id.oculo_signin_url);
        mCode = root.findViewById(R.id.oculo_signin_code);
        mDone = root.findViewById(R.id.oculo_signin_done);
    }

    /**
     * Wraps the GuidedStep view; returns null when Oculo's UI is off (upstream screen kept).
     */
    @Nullable
    public static OculoSignIn wrap(@Nullable View guidedStepView, View.OnClickListener onDone, View.OnClickListener onBrowser) {
        if (guidedStepView == null || !OculoRail.isEnabled(guidedStepView.getContext())) {
            return null;
        }
        Context context = guidedStepView.getContext();
        FrameLayout wrapper = new FrameLayout(context);
        wrapper.addView(guidedStepView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        guidedStepView.setVisibility(View.INVISIBLE);
        if (guidedStepView instanceof ViewGroup) {
            ((ViewGroup) guidedStepView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        guidedStepView.setFocusable(false);

        View layout = LayoutInflater.from(context).inflate(R.layout.oculo_signin, wrapper, false);
        wrapper.addView(layout);
        OculoSignIn signIn = new OculoSignIn(wrapper);
        signIn.mDone.setOnClickListener(onDone);
        layout.findViewById(R.id.oculo_signin_browser).setOnClickListener(onBrowser);
        return signIn;
    }

    public View getRoot() {
        return mRoot;
    }

    public void focus() {
        mDone.requestFocus();
    }

    public void showCode(String userCode, String signInUrl, String fullSignInUrl) {
        mCode.setText(userCode);
        mUrl.setText(signInUrl);
        Context context = mRoot.getContext();
        Drawable placeholder = context.getDrawable(R.drawable.activate_account_qrcode);
        Glide.with(context)
                .load(OculoQr.toBitmap(fullSignInUrl, 600))
                .apply(new RequestOptions().placeholder(placeholder).error(placeholder))
                .into(mQr);
    }
}
