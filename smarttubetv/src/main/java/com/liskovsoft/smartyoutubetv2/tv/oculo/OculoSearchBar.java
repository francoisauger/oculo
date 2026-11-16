package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.SearchBar;
import androidx.leanback.widget.SearchOrbView;
import androidx.leanback.widget.SpeechOrbView;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.time.DateTimeView;

/**
 * Oculo: the search screen's bar laid out like Home's top bar ({@link OculoTopBar}, design.md M3): a 36dp round mic
 * then the search field as a pill at the same place (content start, centered 50.75dp from the top), the search
 * settings gear as a 36dp round button right after the pill, clock and wordmark at the top right. No magnifier orb
 * (the keyboard's « Search » key submits). Colors are the top bar's: white 15 % at rest, white {@code #F1F1F1} with
 * dark ink when focused; the field shows a white ring while its text editor has focus. No zoom or shadow on the orbs.
 * Done in code on leanback's views (ids and listeners unchanged), so upstream's {@code lb_search_bar.xml} stays as is.
 * Part of the left rail feature ({@code R.bool.oculo_left_rail}).
 */
public final class OculoSearchBar {
    private OculoSearchBar() {
    }

    /**
     * Search screen: geometry, orbs, pill, clock and logo. {@code frame} = {@code lb_search_frame}.
     */
    public static void applySearchScreen(@Nullable ViewGroup frame, @Nullable SearchBar searchBar) {
        if (frame == null || searchBar == null || !OculoRail.isEnabled(frame.getContext())) {
            return;
        }
        Context context = frame.getContext();
        Resources res = context.getResources();
        int height = res.getDimensionPixelSize(R.dimen.oculo_top_bar_height);
        int center = res.getDimensionPixelSize(R.dimen.oculo_rail_avatar_center);
        int start = res.getDimensionPixelSize(R.dimen.oculo_search_content_start);
        int gap = res.getDimensionPixelSize(R.dimen.oculo_top_bar_gap);

        // The bar: one 36dp line centered at the same height as Home's top bar
        ViewGroup.LayoutParams barLp = searchBar.getLayoutParams();
        if (barLp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) barLp).topMargin = center - height / 2;
            barLp.height = height;
            searchBar.setLayoutParams(barLp);
        }
        searchBar.setPaddingRelative(0, 0, 0, 0);

        styleOrbs(searchBar);

        View speechOrb = searchBar.findViewById(androidx.leanback.R.id.lb_search_bar_speech_orb);
        if (speechOrb != null && speechOrb.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) speechOrb.getLayoutParams();
            lp.setMarginStart(start);
            speechOrb.setLayoutParams(lp);
        }

        View items = searchBar.findViewById(androidx.leanback.R.id.lb_search_bar_items);
        if (items != null && items.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) items.getLayoutParams();
            lp.width = res.getDimensionPixelSize(R.dimen.oculo_search_field_width);
            lp.height = height;
            lp.topMargin = 0;
            lp.bottomMargin = 0;
            lp.setMarginStart(gap);
            items.setLayoutParams(lp);
        }

        // Magnifier orb: gone; the gear follows the pill
        View searchOrb = searchBar.findViewById(R.id.lb_search_bar_search_orb);
        if (searchOrb != null) {
            searchOrb.setVisibility(View.GONE);
            searchOrb.setFocusable(false);
        }
        View settingsOrb = searchBar.findViewById(R.id.search_settings_orb);
        if (settingsOrb != null && items != null && settingsOrb.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) settingsOrb.getLayoutParams();
            lp.removeRule(RelativeLayout.START_OF);
            lp.removeRule(RelativeLayout.LEFT_OF);
            lp.addRule(RelativeLayout.END_OF, items.getId());
            lp.setMarginStart(gap);
            lp.setMarginEnd(0);
            settingsOrb.setLayoutParams(lp);
            View editor = searchBar.findViewById(androidx.leanback.R.id.lb_search_text_editor);
            if (editor != null) {
                editor.setNextFocusRightId(settingsOrb.getId());
            }
            settingsOrb.setNextFocusLeftId(androidx.leanback.R.id.lb_search_text_editor);
        }

        // Search results start at the same line as the bar (Home's content start)
        View results = frame.findViewById(androidx.leanback.R.id.lb_results_frame);
        if (results != null) {
            int shift = start - res.getDimensionPixelSize(R.dimen.oculo_search_rows_padding_start);
            if (shift > 0) {
                results.setPaddingRelative(shift, results.getPaddingTop(), results.getPaddingEnd(), results.getPaddingBottom());
                if (results instanceof ViewGroup) {
                    ((ViewGroup) results).setClipToPadding(false);
                    ((ViewGroup) results).setClipChildren(false);
                }
            }
        }

        addBranding(frame, center);
    }

    /**
     * Orbs (mic, gear, magnifier) and field in Oculo's top bar style: 36dp, white 15 % at rest, white with dark ink when
     * focused, no shadow. Also used for the channel page's search bar (same leanback widget).
     */
    public static void styleOrbs(@Nullable SearchBar searchBar) {
        if (searchBar == null || !OculoRail.isEnabled(searchBar.getContext())) {
            return;
        }
        Context context = searchBar.getContext();
        int rest = ContextCompat.getColor(context, R.color.oculo_top_bar_button);
        int focused = ContextCompat.getColor(context, R.color.oculo_top_bar_button_focused);
        int ink = ContextCompat.getColor(context, R.color.oculo_top_bar_text_focused);
        int icon = ContextCompat.getColor(context, R.color.oculo_white_focus);
        SearchOrbView.Colors restColors = new SearchOrbView.Colors(rest, rest, icon);
        SearchOrbView.Colors focusedColors = new SearchOrbView.Colors(focused, focused, ink);

        View speech = searchBar.findViewById(androidx.leanback.R.id.lb_search_bar_speech_orb);
        if (speech instanceof SpeechOrbView) {
            SpeechOrbView orb = (SpeechOrbView) speech;
            int listening = ContextCompat.getColor(context, R.color.oculo_search_listening);
            orb.setListeningOrbColors(new SearchOrbView.Colors(listening, listening, ink));
            orb.setNotListeningOrbColors(orb.hasFocus() ? focusedColors : restColors);
            orb.setOrbColors(orb.hasFocus() ? focusedColors : restColors);
            OnFocusChangeListener previous = orb.getOnFocusChangeListener();
            orb.setOnFocusChangeListener((v, hasFocus) -> {
                // Before SearchBar's listener: it may call showNotListening(), which applies these colors
                orb.setNotListeningOrbColors(hasFocus ? focusedColors : restColors);
                orb.setOrbColors(hasFocus ? focusedColors : restColors);
                if (previous != null) {
                    previous.onFocusChange(v, hasFocus);
                }
            });
            resizeOrb(orb);
        }

        for (int id : new int[] {R.id.search_settings_orb, R.id.lb_search_bar_search_orb}) {
            View view = searchBar.findViewById(id);
            if (!(view instanceof SearchOrbView)) {
                continue;
            }
            SearchOrbView orb = (SearchOrbView) view;
            orb.setOrbColors(orb.hasFocus() ? focusedColors : restColors);
            OnFocusChangeListener previous = orb.getOnFocusChangeListener();
            orb.setOnFocusChangeListener((v, hasFocus) -> {
                orb.setOrbColors(hasFocus ? focusedColors : restColors);
                if (previous != null) {
                    previous.onFocusChange(v, hasFocus);
                }
            });
            resizeOrb(orb);
        }

        View items = searchBar.findViewById(androidx.leanback.R.id.lb_search_bar_items);
        if (items instanceof ViewGroup) {
            // Focused state follows the text editor: white ring around the pill while typing
            ((ViewGroup) items).setAddStatesFromChildren(true);
            items.setBackgroundResource(R.drawable.oculo_search_field_background);
        }
        TextView editor = searchBar.findViewById(androidx.leanback.R.id.lb_search_text_editor);
        if (editor != null) {
            editor.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    context.getResources().getDimension(R.dimen.oculo_top_bar_text_size));
            if (editor.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) editor.getLayoutParams();
                lp.setMarginStart(context.getResources().getDimensionPixelSize(R.dimen.oculo_top_bar_text_inset));
                editor.setLayoutParams(lp);
            }
        }
    }

    /**
     * 36dp disc, 20dp icon, no margin around the disc, no shadow (the disc's elevation changes with focus).
     */
    private static void resizeOrb(SearchOrbView orb) {
        Resources res = orb.getResources();
        int size = res.getDimensionPixelSize(R.dimen.oculo_top_bar_height);
        int iconSize = res.getDimensionPixelSize(R.dimen.oculo_top_bar_icon_size);
        View disc = orb.findViewById(androidx.leanback.R.id.search_orb);
        if (disc != null) {
            ViewGroup.LayoutParams lp = disc.getLayoutParams();
            lp.width = size;
            lp.height = size;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMargins(0, 0, 0, 0);
            }
            disc.setLayoutParams(lp);
            disc.setOutlineProvider(null);
        }
        View icon = orb.findViewById(androidx.leanback.R.id.icon);
        if (icon instanceof ImageView) {
            ViewGroup.LayoutParams lp = icon.getLayoutParams();
            lp.width = iconSize;
            lp.height = iconSize;
            icon.setLayoutParams(lp);
            ((ImageView) icon).setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        orb.setPadding(0, 0, 0, 0);
        ViewGroup.LayoutParams lp = orb.getLayoutParams();
        if (lp instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) lp;
            rlp.width = size;
            rlp.height = size;
            rlp.addRule(RelativeLayout.CENTER_VERTICAL);
            orb.setLayoutParams(rlp);
        }
    }

    /**
     * Clock, date and wordmark at the top right, as on Home ({@code lb_title_view_logo}, sized by OculoTopBar's values).
     */
    private static void addBranding(ViewGroup frame, int center) {
        Context context = frame.getContext();
        Resources res = context.getResources();
        FrameLayout holder = new FrameLayout(context);
        LayoutInflater.from(context).inflate(R.layout.lb_title_view_logo, holder, true);
        FrameLayout.LayoutParams holderLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, center * 2, Gravity.TOP | Gravity.END);
        holderLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.oculo_search_branding_end));
        frame.addView(holder, holderLp);

        boolean clock = GeneralData.instance(context).isGlobalClockEnabled();
        DateTimeView time = holder.findViewById(R.id.global_time);
        DateTimeView date = holder.findViewById(R.id.global_date);
        if (time != null) {
            time.showDate(false);
            time.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.oculo_top_bar_time_size));
            time.setVisibility(clock ? View.VISIBLE : View.GONE);
        }
        if (date != null) {
            date.showTime(false);
            date.showDate(true);
            date.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.oculo_top_bar_date_size));
            date.setVisibility(clock ? View.VISIBLE : View.GONE);
        }

        View title = holder.findViewById(R.id.title_text);
        if (title != null) {
            title.setVisibility(View.GONE);
        }
        ImageView badge = holder.findViewById(R.id.title_badge);
        if (badge != null) {
            int logo = Helpers.getThemeAttr(context, R.attr.appLogo);
            if (logo <= 0) {
                logo = R.mipmap.app_logo_semi_red;
            }
            badge.setImageResource(logo);
            badge.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = badge.getLayoutParams();
            lp.height = res.getDimensionPixelSize(R.dimen.oculo_top_bar_logo_height);
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMarginStart(res.getDimensionPixelSize(R.dimen.oculo_top_bar_logo_margin));
            }
            badge.setLayoutParams(lp);
        }
    }
}
