package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoSleepTimer;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Oculo panel (0.12.0, mock « 5. SponsorBlock »): the transparent drawers shown over the playing video (SponsorBlock
 * « ask before skipping », chapter notice) become a small card at the bottom right, of the family of the « segment
 * skipped » notice (OculoSkipNotice): dark rounded card, an Abricot bar before the grey label, the button as a row
 * with the white focus pill. No scrim: nothing else covers the video. Keys are upstream's (overlay: OK presses the
 * button, the other keys close the card and go to the player).
 */
public final class OculoNotice {
    private OculoNotice() {
    }

    /** From {@link OculoPanel#applyFrame}: small card at the bottom right. */
    static void applyFrame(View root, View container) {
        root.setBackground(null);
        Resources res = container.getResources();
        container.setBackgroundResource(R.drawable.oculo_panel_notice);
        container.setTag(R.id.oculo_panel_notice, Boolean.TRUE); // read by bindHeader()
        if (VERSION.SDK_INT >= 21) {
            container.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            container.setClipToOutline(true);
        }
        ViewGroup.LayoutParams params = container.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
            int margin = res.getDimensionPixelSize(R.dimen.oculo_panel_margin);
            lp.width = res.getDimensionPixelSize(R.dimen.oculo_notice_width);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM | Gravity.END;
            lp.setMargins(margin, margin, margin, margin * 2);
            lp.setMarginStart(margin);
            lp.setMarginEnd(margin);
            container.setLayoutParams(lp);
        }
    }

    static boolean isNoticeFrame(View container) {
        return container != null && Boolean.TRUE.equals(container.getTag(R.id.oculo_panel_notice));
    }

    /**
     * From {@link OculoPanel#bindHeader}, after upstream's transparent styling: label with the Abricot bar (the
     * drawer's title, « SponsorBlock »), rows with the white pill, the card as tall as its rows.
     */
    static void bind(View view) {
        Resources res = view.getResources();
        View header = view.findViewById(R.id.decor_title_container);
        TextView title = view.findViewById(R.id.decor_title);
        TextView label = view.findViewById(R.id.oculo_panel_label);
        View desc = view.findViewById(R.id.oculo_panel_desc);
        boolean hasTitle = title != null && !TextUtils.isEmpty(title.getText());
        if (header != null && label != null && title != null) {
            header.setVisibility(hasTitle ? View.VISIBLE : View.GONE);
            header.setPadding(header.getPaddingLeft(), dp(res, 14), header.getPaddingRight(), dp(res, 4));
            label.setText(title.getText());
            label.setVisibility(View.VISIBLE);
            label.setEllipsize(TextUtils.TruncateAt.END);
            GradientDrawable bar = new GradientDrawable();
            bar.setColor(res.getColor(R.color.oculo_skip_notice_accent));
            bar.setCornerRadius(dp(res, 2));
            bar.setBounds(0, 0, dp(res, 4), dp(res, 14));
            label.setCompoundDrawablesRelative(bar, null, null, null);
            label.setCompoundDrawablePadding(dp(res, 8));
            title.setVisibility(View.GONE);
            if (desc != null) {
                desc.setVisibility(View.GONE);
            }
        }
        if (label != null && hasTitle && OculoSleepTimer.isWarningShown()) {
            startCountdown(label); // 0.13.0: « Still watching? Pausing in 23 s »
        }

        View list = view.findViewById(R.id.list);
        if (list == null) {
            list = view.findViewById(android.R.id.list);
        }
        int rows = 1;
        if (list instanceof VerticalGridView) {
            VerticalGridView grid = (VerticalGridView) list;
            // Upstream paints each row grey 40 % (transparent_dialog_item_bg): the white pill instead
            grid.setOnChildLaidOutListener((parent, child, position, id) -> child.setBackgroundResource(R.drawable.oculo_list_focus));
            RecyclerView.Adapter<?> adapter = grid.getAdapter();
            rows = adapter != null ? Math.max(1, Math.min(adapter.getItemCount(), 3)) : 1;
        }

        // The card wraps its content: give the level the height of its header and rows (leanback's grid fills its parent)
        int height = rows * res.getDimensionPixelSize(R.dimen.oculo_panel_row_height) + dp(res, 12)
                + (hasTitle ? dp(res, 32) : 0);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            lp.height = height;
            view.setLayoutParams(lp);
        }
        view.setPadding(view.getPaddingLeft(), hasTitle ? 0 : dp(res, 6), view.getPaddingRight(), dp(res, 6));
    }

    /**
     * The sleep timer's card: its label counts down once a second while it is on screen (stops when the card is
     * detached or the warning is over).
     */
    private static void startCountdown(TextView label) {
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                String text = OculoSleepTimer.isWarningShown() ? OculoSleepTimer.warningLabel(label.getContext()) : null;
                if (text == null || !ViewCompat.isAttachedToWindow(label)) {
                    return;
                }
                label.setText(text);
                label.postDelayed(this, 1_000);
            }
        };
        label.postDelayed(tick, 1_000);
    }

    private static int dp(Resources res, int dp) {
        return Math.round(dp * res.getDisplayMetrics().density);
    }
}
