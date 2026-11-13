package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Oculo: YouTube TV style top bar, built inside leanback's title view (no upstream layout change).
 * <ul>
 *     <li>A round mic button and a "Search" pill at the content's start. D-pad up from the first row lands on the
 *     mic, right goes to the pill, down goes back to the rows. Mic = search in voice mode, pill = search.</li>
 *     <li>Clock, date and wordmark stay at the top right, smaller (YouTube's logo is 44 px high at 1080p).</li>
 *     <li>Everything is centered on one line 50.75dp from the top, level with the rail's avatar.</li>
 *     <li>The bar stays while the rows scroll (BrowseFragment keeps the title shown, as YouTube keeps its bar);
 *     the content is clipped under it.</li>
 * </ul>
 * Part of the left rail feature ({@code R.bool.oculo_left_rail}); {@link OculoRail} creates it.
 */
final class OculoTopBar {
    private final Context mContext;
    private final View mTitleView;
    private final View mGroup;
    private final View mMic;
    private final View mPill;

    @Nullable
    static OculoTopBar attach(View titleView) {
        if (titleView == null) {
            return null;
        }

        View searchOrb = titleView.findViewById(R.id.title_orb);
        if (searchOrb == null || !(searchOrb.getParent() instanceof LinearLayout)) {
            return null;
        }

        return new OculoTopBar(titleView, (LinearLayout) searchOrb.getParent());
    }

    private OculoTopBar(View titleView, LinearLayout group) {
        mContext = titleView.getContext();
        mTitleView = titleView;
        mGroup = group;
        Resources res = mContext.getResources();
        int height = res.getDimensionPixelSize(R.dimen.oculo_top_bar_height);
        int iconSize = res.getDimensionPixelSize(R.dimen.oculo_top_bar_icon_size);
        ColorStateList contentColors = ContextCompat.getColorStateList(mContext, R.color.oculo_top_bar_content);

        // Mic button
        FrameLayout mic = new FrameLayout(mContext);
        mic.setId(View.generateViewId());
        mic.setBackgroundResource(R.drawable.oculo_top_bar_mic_background);
        mic.setFocusable(true);
        mic.setFocusableInTouchMode(true);
        mic.setContentDescription(mContext.getString(R.string.oculo_voice_search));
        ImageView micIcon = new ImageView(mContext);
        micIcon.setImageResource(R.drawable.oculo_ic_mic);
        micIcon.setImageTintList(contentColors);
        micIcon.setDuplicateParentStateEnabled(true);
        mic.addView(micIcon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        mic.setOnClickListener(v -> SearchPresenter.instance(mContext).startVoice());
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(height, height);
        micLp.gravity = Gravity.CENTER_VERTICAL;

        // "Search" pill
        TextView pill = new TextView(mContext);
        pill.setId(View.generateViewId());
        pill.setBackgroundResource(R.drawable.oculo_top_bar_pill_background);
        pill.setFocusable(true);
        pill.setFocusableInTouchMode(true);
        pill.setText(R.string.action_search);
        pill.setTextColor(contentColors);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.oculo_top_bar_text_size));
        pill.setSingleLine(true);
        pill.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        int inset = res.getDimensionPixelSize(R.dimen.oculo_top_bar_text_inset);
        pill.setPaddingRelative(inset, 0, inset, 0);
        pill.setOnClickListener(v -> SearchPresenter.instance(mContext).startSearch(null));
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                res.getDimensionPixelSize(R.dimen.oculo_top_bar_pill_width), height);
        pillLp.gravity = Gravity.CENTER_VERTICAL;
        pillLp.setMarginStart(res.getDimensionPixelSize(R.dimen.oculo_top_bar_gap));

        // Left/right inside the bar (NavigateTitleView follows these ids before anything else)
        mic.setNextFocusRightId(pill.getId());
        pill.setNextFocusLeftId(mic.getId());

        group.addView(mic, 0, micLp);
        group.addView(pill, 1, pillLp);
        mMic = mic;
        mPill = pill;

        resizeBranding(res);
        applyBarGeometry(res);
    }

    /**
     * Clock, date and wordmark scaled to YouTube's slimmer bar.
     */
    private void resizeBranding(Resources res) {
        TextView time = mTitleView.findViewById(R.id.global_time);
        TextView date = mTitleView.findViewById(R.id.global_date);
        if (time != null) {
            time.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.oculo_top_bar_time_size));
        }
        if (date != null) {
            date.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.oculo_top_bar_date_size));
        }

        View badge = mTitleView.findViewById(R.id.title_badge);
        if (badge != null && badge.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) badge.getLayoutParams();
            lp.height = res.getDimensionPixelSize(R.dimen.oculo_top_bar_logo_height);
            lp.setMarginStart(res.getDimensionPixelSize(R.dimen.oculo_top_bar_logo_margin));
            badge.setLayoutParams(lp);
        }
    }

    /**
     * One bar line: fixed height, centered 50.75dp from the top whatever is shown in it (the background
     * playback button is taller than the pill and simply overflows the line, centered).
     */
    private void applyBarGeometry(Resources res) {
        int height = res.getDimensionPixelSize(R.dimen.oculo_top_bar_height);
        int center = res.getDimensionPixelSize(R.dimen.oculo_rail_avatar_center);
        int top = center - height / 2;
        mTitleView.setPaddingRelative(mTitleView.getPaddingStart(), top, mTitleView.getPaddingEnd(), 0);
        ViewGroup.LayoutParams lp = mTitleView.getLayoutParams();
        if (lp != null) {
            lp.height = top + height;
            mTitleView.setLayoutParams(lp);
        }
    }

    /**
     * Start of the bar's left group, in title view coordinates: the content's start (it follows the rail).
     */
    void setStart(int start, boolean animate) {
        float translation = start - mTitleView.getPaddingStart();
        mGroup.animate().cancel();
        if (animate) {
            mGroup.animate().translationX(translation)
                    .setDuration(mContext.getResources().getInteger(R.integer.oculo_rail_shift_duration)).start();
        } else {
            mGroup.setTranslationX(translation);
        }
    }

    /**
     * Where D-pad up from the rows lands.
     */
    View getEntry() {
        return mMic;
    }

    boolean contains(View view) {
        View current = view;
        while (current != null) {
            if (current == mMic || current == mPill) {
                return true;
            }
            ViewParent next = current.getParent();
            current = next instanceof View ? (View) next : null;
        }
        return false;
    }

    /**
     * D-pad inside the bar, before leanback's rules. Returns null to let leanback decide (down = rows,
     * left from the mic = rail).
     */
    @Nullable
    View onFocusSearch(View focused, int direction) {
        if (focused == mPill && direction == (isRtl() ? View.FOCUS_LEFT : View.FOCUS_RIGHT)) {
            View exitPip = mTitleView.findViewById(R.id.exit_pip);
            return exitPip != null && exitPip.getVisibility() == View.VISIBLE ? exitPip : mPill;
        }
        if ((focused == mMic || focused == mPill) && direction == View.FOCUS_UP) {
            return focused;
        }
        return null;
    }

    private boolean isRtl() {
        return mTitleView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Content clip under the bar, in the content container's coordinates.
     */
    static Rect contentClip(Context context, int start) {
        int top = context.getResources().getDimensionPixelSize(R.dimen.oculo_top_bar_content_clip);
        return new Rect(start, top, Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2);
    }
}
