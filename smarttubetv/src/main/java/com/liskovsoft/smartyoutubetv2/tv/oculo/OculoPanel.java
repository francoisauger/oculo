package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.Fade;
import android.transition.Transition;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoPanelInfo;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.CommentsPreferenceDialogFragment;

/**
 * Oculo panel (0.12.0, docs/mockups/drawers/drawers-redesign.html, approved by François on 16 Nov. 2026).
 * <p>
 * Nearly every drawer of SmartTube (menus, player settings, speed, subtitles, comments, video info, accounts,
 * confirmations, the classic settings screens…) goes through one component, {@code AppDialogFragment}: a grey strip
 * glued to the right edge. This class restyles that component once, so every caller changes: the Settings detail card
 * (values/oculo_card.xml) floating at the right on a scrim, with a header (grey context label, Roboto Condensed
 * title, one line of Roboto description), rows at the Settings sizes, the white focus pill, values at the right,
 * fading edges when the rows scroll, levels that change in place (a 150 ms fade) with the path as label.
 * <p>
 * Hooks (see OCULO.md « Oculo panel »): {@code AppDialogFragment.show()} → {@link #applyFrame},
 * {@code startPreferenceFragment()} → {@link #onStartFragment}, {@code onViewCreated()} of every level
 * ({@code AppPreferenceFragment}, {@code LeanbackPreferenceDialogFragment}) → {@link #bindHeader}, and
 * {@code AppPreferenceFragment.addPreferences()} → {@link #decorate}. The layouts are flavor copies.
 * Off (upstream look) when {@code R.bool.oculo_panel} is false. Never throws: a failure leaves upstream's look.
 */
public final class OculoPanel {
    private static final String TAG = OculoPanel.class.getSimpleName();
    private static final String ARG_PATH = "oculo_panel_path";
    private static final String PATH_SEPARATOR = " › ";
    private static Boolean sEnabled;

    private OculoPanel() {
    }

    public static boolean isEnabled(Context context) {
        if (sEnabled == null && context != null) {
            sEnabled = context.getResources().getBoolean(R.bool.oculo_panel);
        }
        return sEnabled != null && sEnabled;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Frame: card, margins, scrim

    /**
     * {@code AppDialogFragment.show()}: card and scrim for this dialog. Transparent dialogs (SponsorBlock « ask »,
     * chapter notice) keep upstream's frame here: they become a small card at the bottom right (OculoNotice).
     */
    public static void applyFrame(Fragment dialogFragment, boolean isTransparent, boolean isOverlay) {
        if (dialogFragment == null || !isEnabled(dialogFragment.getActivity())) {
            return;
        }

        try {
            View root = dialogFragment.getView();
            View container = root != null ? root.findViewById(R.id.settings_preference_fragment_container) : null;
            if (container == null) {
                return;
            }
            Context context = container.getContext();

            if (isTransparent) {
                OculoNotice.applyFrame(root, container);
                return;
            }

            container.setTag(R.id.oculo_panel_notice, null);
            boolean inPlayer = isInPlayer(context);
            container.setBackgroundResource(inPlayer ? R.drawable.oculo_panel_card_player : R.drawable.oculo_panel_card);
            if (VERSION.SDK_INT >= 21) {
                container.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                container.setClipToOutline(true);
            }
            applyMargins(container);
            applyScrim(root, container, inPlayer, isOverlay);
        } catch (RuntimeException e) {
            Log.e(TAG, "applyFrame: " + e.getMessage());
        }
    }

    /**
     * 328dp card in SmartTube's 360dp band: 24dp from the screen edge, top and bottom, 8dp towards the video. The band
     * is at the left when the comments are placed left (upstream's {@code ViewUtil.enableLeftDialog()}).
     */
    private static void applyMargins(View container) {
        ViewGroup.LayoutParams params = container.getLayoutParams();
        if (!(params instanceof FrameLayout.LayoutParams)) {
            return;
        }
        Resources res = container.getResources();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
        int outer = res.getDimensionPixelSize(R.dimen.oculo_panel_margin);
        int inner = res.getDimensionPixelSize(R.dimen.oculo_panel_margin_inner);
        boolean atStart = isAtStart(lp.gravity);
        lp.gravity = atStart ? Gravity.START : Gravity.END;
        lp.width = res.getDimensionPixelSize(R.dimen.oculo_panel_width);
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.topMargin = outer;
        lp.bottomMargin = outer;
        lp.setMarginStart(atStart ? outer : inner);
        lp.setMarginEnd(atStart ? inner : outer);
        lp.leftMargin = atStart ? outer : inner;
        lp.rightMargin = atStart ? inner : outer;
        container.setLayoutParams(lp);
    }

    private static boolean isAtStart(int gravity) {
        int horizontal = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        return horizontal == Gravity.START || horizontal == Gravity.LEFT;
    }

    /**
     * Home, search, channels: black at 58 %. Player: nothing, SmartTube shrinks the video to the left of the band;
     * when it does not (« Don't resize the video to fit the dialog », overlays), a gradient on the card's side only.
     */
    private static void applyScrim(View root, View container, boolean inPlayer, boolean isOverlay) {
        Context context = root.getContext();
        if (!inPlayer) {
            root.setBackgroundColor(context.getResources().getColor(R.color.oculo_panel_scrim));
            return;
        }
        boolean videoKeepsItsSize = isOverlay || PlayerTweaksData.instance(context).isDontResizeVideoToFitDialogEnabled();
        if (videoKeepsItsSize) {
            boolean atStart = isAtStart(((FrameLayout.LayoutParams) container.getLayoutParams()).gravity);
            root.setBackground(new SideScrim(context.getResources().getColor(R.color.oculo_panel_scrim_side), atStart));
        } else {
            root.setBackground(null);
        }
    }

    public static boolean isInPlayer(Context context) {
        try {
            return ViewManager.instance(context).getTopView() == PlaybackView.class;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Levels: path and transitions

    /**
     * {@code AppDialogFragment.startPreferenceFragment()}, before the transaction: the new level's label is the path
     * to it (previous label › previous title), and levels change in place with a short fade instead of upstream's slide.
     */
    public static void onStartFragment(@Nullable Fragment previous, @NonNull Fragment next) {
        if (!isEnabled(next.getActivity() != null ? next.getActivity() : previous != null ? previous.getActivity() : null)) {
            return;
        }

        try {
            Bundle args = next.getArguments();
            if (args == null) {
                args = new Bundle();
                next.setArguments(args);
            }
            String path = previous != null ? pathOf(previous.getView()) : null;
            if (path != null) {
                args.putString(ARG_PATH, path);
            } else {
                args.remove(ARG_PATH);
            }

            if (VERSION.SDK_INT >= 21) {
                int duration = next.getActivity() != null ? next.getActivity().getResources().getInteger(R.integer.oculo_panel_fade_ms) : 150;
                next.setEnterTransition(fade(duration));
                next.setReturnTransition(fade(duration));
                next.setExitTransition(fade(duration));
                next.setReenterTransition(fade(duration));
                if (previous != null) {
                    previous.setExitTransition(fade(duration));
                    previous.setReenterTransition(fade(duration));
                }
            }
        } catch (RuntimeException e) { // setArguments() on a fragment already added
            Log.e(TAG, "onStartFragment: " + e.getMessage());
        }
    }

    private static Transition fade(int duration) {
        Fade fade = new Fade();
        fade.setDuration(duration);
        return fade;
    }

    private static String pathOf(View levelView) {
        if (levelView == null) {
            return null;
        }
        TextView label = levelView.findViewById(R.id.oculo_panel_label);
        TextView title = levelView.findViewById(R.id.decor_title);
        String labelText = label != null && label.getVisibility() == View.VISIBLE ? textOf(label) : null;
        String titleText = textOf(title);
        if (labelText == null) {
            return titleText;
        }
        if (titleText == null) {
            return labelText;
        }
        return labelText + PATH_SEPARATOR + titleText;
    }

    private static String textOf(TextView view) {
        if (view == null || TextUtils.isEmpty(view.getText())) {
            return null;
        }
        String text = view.getText().toString().replace('\n', ' ').trim();
        return text.isEmpty() ? null : text;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Header and rows

    /**
     * {@code onViewCreated()} of every level: label, title, description, fading edges on the rows.
     */
    public static void bindHeader(@NonNull Fragment fragment, @Nullable View view) {
        if (view == null || !isEnabled(view.getContext())) {
            return;
        }

        try {
            Context context = view.getContext();
            View parent = (View) view.getParent();
            if (OculoNotice.isNoticeFrame(parent)) {
                OculoNotice.bind(view); // SponsorBlock « ask », chapter notice: small card at the bottom right
                return;
            }
            if (parent != null && parent.getId() == R.id.settings_preference_fragment_container && parent.getBackground() != null) {
                applyMargins(parent); // the comments may have moved the band to the left
            }

            TextView titleView = view.findViewById(R.id.decor_title);
            TextView labelView = view.findViewById(R.id.oculo_panel_label);
            TextView descView = view.findViewById(R.id.oculo_panel_desc);
            if (titleView == null || labelView == null || descView == null) {
                return;
            }

            Bundle args = fragment.getArguments();
            String path = args != null ? args.getString(ARG_PATH) : null;
            boolean inPlayer = isInPlayer(context);
            CharSequence original = titleView.getText();
            CharSequence title = original;
            CharSequence label = null;
            CharSequence desc = null;

            if (fragment instanceof CommentsPreferenceDialogFragment) {
                // Upstream titles the comments with the video's title: « Comments » / « Replies », video below
                title = context.getString(path == null ? R.string.oculo_panel_comments : R.string.oculo_panel_replies);
                desc = original;
            } else {
                OculoPanelInfo.Info info = OculoPanelInfo.get(original);
                if (info != null) {
                    if (info.title != null) {
                        title = info.title;
                    }
                    label = info.label;
                    desc = info.desc;
                }
            }

            if (path != null) {
                label = path;
            } else if (label == null && inPlayer) {
                label = context.getString(R.string.oculo_panel_playback);
            }
            if (desc == null && inPlayer) {
                Video video = PlaybackPresenter.instance(context).getVideo();
                String videoTitle = video != null ? video.getTitle() : null;
                if (videoTitle != null && title != null && !videoTitle.trim().equalsIgnoreCase(title.toString().trim())) {
                    desc = videoTitle;
                }
            }

            if (title != original) {
                titleView.setText(title);
            }
            setOptional(labelView, label);
            setOptional(descView, desc);

            enableFadingEdges(view.findViewById(R.id.list), inPlayer);
            enableFadingEdges(view.findViewById(android.R.id.list), inPlayer);
            enableFadingEdges(view.findViewById(R.id.messagesList), inPlayer);
        } catch (RuntimeException e) {
            Log.e(TAG, "bindHeader: " + e.getMessage());
        }
    }

    private static void setOptional(TextView view, CharSequence text) {
        boolean show = text != null && text.toString().trim().length() > 0;
        view.setText(show ? text : null);
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private static void enableFadingEdges(View list, boolean inPlayer) {
        if (!(list instanceof RecyclerView)) {
            return;
        }
        Resources res = list.getResources();
        RecyclerView recyclerView = (RecyclerView) list;
        recyclerView.setClipToPadding(false);
        list.setPadding(list.getPaddingLeft(), list.getPaddingTop(), list.getPaddingRight(),
                Math.max(list.getPaddingBottom(), res.getDimensionPixelSize(R.dimen.oculo_panel_rows_bottom)));
        Fades.attach(recyclerView, res.getColor(inPlayer ? R.color.oculo_panel_background_player : R.color.oculo_panel_background));
    }

    /**
     * Fades at the top and bottom of the rows when there are more rows that way, as in the Settings card. Leanback's
     * grids do not compute their scroll range (no View fading edges, no canScrollVertically()), so this reads the laid
     * out children: two gradient drawables in the list's overlay, shown or hidden after each scroll and layout.
     */
    private static final class Fades extends RecyclerView.OnScrollListener implements View.OnLayoutChangeListener {
        private final RecyclerView mList;
        private final GradientDrawable mTop;
        private final GradientDrawable mBottom;
        private final int mTopHeight;
        private final int mBottomHeight;

        private Fades(RecyclerView list, int color) {
            mList = list;
            int clear = color & 0x00FFFFFF;
            mTop = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {color, clear});
            mBottom = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {clear, (color & 0x00FFFFFF) | 0xF2000000});
            mTopHeight = list.getResources().getDimensionPixelSize(R.dimen.oculo_card_fade);
            mBottomHeight = list.getResources().getDimensionPixelSize(R.dimen.oculo_card_fade_bottom);
        }

        static void attach(RecyclerView list, int color) {
            if (VERSION.SDK_INT < 18 || list.getTag(R.id.oculo_panel_fades) != null) {
                return;
            }
            Fades fades = new Fades(list, color);
            list.setTag(R.id.oculo_panel_fades, fades);
            list.getOverlay().add(fades.mTop);
            list.getOverlay().add(fades.mBottom);
            list.addOnScrollListener(fades);
            list.addOnLayoutChangeListener(fades);
            fades.update();
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            update();
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            update();
        }

        private void update() {
            int width = mList.getWidth();
            int height = mList.getHeight();
            mTop.setBounds(0, 0, width, mTopHeight);
            mBottom.setBounds(0, Math.max(0, height - mBottomHeight), width, height);
            mTop.setAlpha(hasMore(-1) ? 255 : 0);
            mBottom.setAlpha(hasMore(1) ? 255 : 0);
        }

        private boolean hasMore(int direction) {
            RecyclerView.Adapter<?> adapter = mList.getAdapter();
            int count = mList.getChildCount();
            if (adapter == null || count == 0) {
                return false;
            }
            if (mList.getLayoutManager() instanceof LinearLayoutManager) {
                return mList.canScrollVertically(direction); // comments: real scroll range
            }
            View edge = mList.getChildAt(direction < 0 ? 0 : count - 1);
            int position = mList.getChildAdapterPosition(edge);
            if (direction < 0) {
                return position > 0 || edge.getTop() < mList.getPaddingTop() - 1;
            }
            return (position != RecyclerView.NO_POSITION && position < adapter.getItemCount() - 1)
                    || edge.getBottom() > mList.getHeight() - mList.getPaddingBottom() + 1;
        }
    }

    /**
     * {@code AppPreferenceFragment.addPreferences()}: a row that opens a value list shows the current value at the
     * right (the summary, laid out at the right by the flavor's leanback_preference.xml); a row that opens another
     * list gets a chevron. Switch rows keep their switch (preference_widget_checkbox.xml).
     */
    public static Preference decorate(Preference preference) {
        if (preference == null || !isEnabled(preference.getContext())) {
            return preference;
        }

        if (preference instanceof ListPreference) {
            ListPreference list = (ListPreference) preference;
            setValueSummary(list, list.getValue());
            Preference.OnPreferenceChangeListener upstream = list.getOnPreferenceChangeListener();
            list.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean accepted = upstream == null || upstream.onPreferenceChange(pref, newValue);
                if (accepted) {
                    setValueSummary(list, newValue != null ? newValue.toString() : null);
                }
                return accepted;
            });
        } else if (preference instanceof MultiSelectListPreference) {
            preference.setWidgetLayoutResource(R.layout.oculo_panel_chevron);
        } else if (!(preference instanceof CheckBoxPreference) && preference.getClass() != Preference.class) {
            preference.setWidgetLayoutResource(R.layout.oculo_panel_chevron); // comments, chat
        }

        return preference;
    }

    /**
     * The chosen value's title, first line only (upstream appends an item's description to its title after a line
     * break). ListPreference formats its summary with String.format(), hence the escaped percent signs.
     */
    private static void setValueSummary(ListPreference list, String value) {
        CharSequence own = OculoPanelInfo.rowValue(list.getTitle()); // 0.13.0: sleep timer « 23 min left »
        if (own != null) {
            list.setSummary(own.toString().replace("%", "%%"));
            return;
        }
        int index = value != null ? list.findIndexOfValue(value) : -1;
        CharSequence[] entries = list.getEntries();
        String text = index >= 0 && entries != null && index < entries.length && entries[index] != null ? entries[index].toString() : "";
        int lineBreak = text.indexOf('\n');
        if (lineBreak >= 0) {
            text = text.substring(0, lineBreak);
        }
        list.setSummary(text.trim().replace("%", "%%"));
    }

    // -------------------------------------------------------------------------------------------------------------

    /**
     * Gradient on the card's third of the screen only (transparent up to 45 %, 62 % black from 66 %), for a video that
     * keeps its full size behind the card. Mirrored when the band is at the left.
     */
    private static final class SideScrim extends Drawable {
        private final Paint mPaint = new Paint();
        private final int mColor;
        private final boolean mAtStart;

        SideScrim(int color, boolean atStart) {
            mColor = color;
            mAtStart = atStart;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            int clear = mColor & 0x00FFFFFF;
            float[] positions = {0f, 0.45f, 0.66f, 1f};
            int[] colors = {clear, clear, mColor, mColor};
            float x0 = mAtStart ? bounds.right : bounds.left;
            float x1 = mAtStart ? bounds.left : bounds.right;
            mPaint.setShader(new LinearGradient(x0, 0, x1, 0, colors, positions, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawRect(getBounds(), mPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
