package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager.AccountChangeListener;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoNewContent;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.SectionHeaderItem;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Oculo: turns the browse headers into a YouTube TV style left rail.
 * <ul>
 *     <li>Collapsed (focus in the content): a narrow column of icons stays on screen, the current section in
 *     turquoise; the content starts right of it (leanback hook {@code getCollapsedHeadersWidth()}).</li>
 *     <li>Expanded (focus in the rail, D-pad left from the content): labels appear and the content moves right,
 *     as in the YouTube app; the focused item is a turquoise pill. D-pad right goes back to the content.</li>
 *     <li>The profile avatar stays at the top; Search is the list's first slot and scrolls away with the
 *     sections, as in YouTube (the list is clipped below the avatar). Expanded, the whole list moves up a little,
 *     as YouTube's does. D-pad: up from the first section = Search, then the avatar.</li>
 *     <li>{@link OculoTopBar}: mic and "Search" pill at the top of the content, smaller clock, date and logo.</li>
 * </ul>
 * Everything else stays leanback's: same headers list, same transitions, same focus rules. Enabled by
 * {@code R.bool.oculo_left_rail} (false upstream, true in the oculo flavor).
 */
public final class OculoRail implements AccountChangeListener {
    private final BrowseSupportFragment mFragment;
    private final Context mContext;
    private final ColorStateList mIconColors;
    private final ColorStateList mIconColorsExpanded;
    private boolean mExpanded;
    private VerticalGridView mGrid;
    private View mTopBlock;
    private View mAvatarRow;
    private View mSearchClip;
    private View mSearchRow;
    private View mListFade;
    private OculoTopBar mTopBar;
    private View mContent;
    private final FragmentManager.FragmentLifecycleCallbacks mHeadersCallbacks =
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v,
                                                  @Nullable Bundle savedInstanceState) {
                    if (f instanceof HeadersSupportFragment) {
                        setupHeaders((HeadersSupportFragment) f, v);
                    }
                }
            };

    public static boolean isEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_left_rail);
    }

    /**
     * Width that stays on screen while the headers are hidden (0 when the rail is off).
     */
    public static int getCollapsedWidth(Context context) {
        return isEnabled(context) ? context.getResources().getDimensionPixelSize(R.dimen.oculo_rail_collapsed_width) : 0;
    }

    /**
     * Called from BrowseFragment.onCreateView, after leanback created its views. Returns null when disabled.
     */
    @Nullable
    public static OculoRail attach(BrowseSupportFragment fragment, View root) {
        if (!isEnabled(fragment.getContext()) || root == null) {
            return null;
        }

        OculoRail rail = new OculoRail(fragment);
        rail.attachInternal(root);
        return rail;
    }

    private OculoRail(BrowseSupportFragment fragment) {
        mFragment = fragment;
        mContext = fragment.getContext();
        mIconColors = ContextCompat.getColorStateList(mContext, R.color.oculo_rail_icon);
        // As in YouTube: grey icons on the collapsed rail (current section white), white once expanded
        mIconColorsExpanded = ContextCompat.getColorStateList(mContext, R.color.oculo_rail_icon_expanded);
    }

    private void attachInternal(View root) {
        mExpanded = mFragment.isShowingHeaders();

        // No panel behind the rail: the browse background shows through, as in YouTube.
        mFragment.setBrandColor(Color.TRANSPARENT);

        // Background glow that takes the focused thumbnail's color (Google TV style)
        OculoGlow.attach(root);

        // Search and account move from the title bar into the rail; the title becomes a YouTube-style top bar.
        mTopBar = OculoTopBar.attach(mFragment.getTitleView());

        // Content: clipped right of the icons (scrolled cards) and under the top bar.
        mContent = root.findViewById(R.id.scale_frame);
        if (mContent != null) {
            mContent.setClipBounds(OculoTopBar.contentClip(mContext,
                    mContext.getResources().getDimensionPixelSize(R.dimen.oculo_rail_content_clip_start)));
        }
        applyContentStart(false);

        BrowseFrameLayout browseFrame = root.findViewById(R.id.browse_frame);
        if (browseFrame != null) {
            BrowseFrameLayout.OnFocusSearchListener upstream = browseFrame.getOnFocusSearchListener();
            browseFrame.setOnFocusSearchListener((focused, direction) -> {
                View result = onFocusSearch(focused, direction);
                return result != null ? result : upstream != null ? upstream.onFocusSearch(focused, direction) : null;
            });
        }

        mFragment.getChildFragmentManager().registerFragmentLifecycleCallbacks(mHeadersCallbacks, false);

        HeadersSupportFragment headers = mFragment.getHeadersSupportFragment();
        if (headers != null) {
            headers.setItemsAlwaysVisible(true);
            if (headers.getView() != null) {
                setupHeaders(headers, headers.getView());
            }
        }

        MediaServiceManager.instance().addAccountListener(this);

        // Abricot dot on Subscriptions when subscribed channels have new content
        if (OculoNewContent.isEnabled(mContext)) {
            OculoNewContent.instance(mContext).setListener(this::updateCurrentSection);
        }
    }

    public void detach() {
        MediaServiceManager.instance().removeAccountListener(this);
        if (OculoNewContent.isEnabled(mContext)) {
            OculoNewContent.instance(mContext).setListener(null);
        }
        mFragment.getChildFragmentManager().unregisterFragmentLifecycleCallbacks(mHeadersCallbacks);
        mGrid = null;
        mTopBlock = mAvatarRow = mSearchClip = mSearchRow = mListFade = mContent = null;
        mTopBar = null;
    }

    /**
     * Leanback resets the headers' alignment in onStart.
     */
    public void onStart() {
        applyGridAlignment();
        updateAccount();
        // New-content dot: first Home load, then at most once per Home refresh interval (never in the background)
        if (OculoNewContent.isEnabled(mContext)) {
            OculoNewContent.instance(mContext).refreshIfDue();
        }
    }

    /**
     * Leanback hook, inside the headers transition: labels fade in or out with it.
     */
    public void onHeadersShown(boolean show) {
        boolean changed = mExpanded != show;
        mExpanded = show;
        applyExpandedState();
        applyListShift(changed);
        applyContentStart(changed);
    }

    /**
     * The top bar's mic and pill start where the content starts (they move with it, as in YouTube).
     */
    private void applyContentStart(boolean animate) {
        if (mTopBar == null) {
            return;
        }

        Resources res = mContext.getResources();
        int rowsPadding = res.getDimensionPixelSize(R.dimen.lb_browse_padding_start);
        int containerStart = mExpanded ? res.getDimensionPixelSize(R.dimen.lb_browse_rows_margin_start)
                : getCollapsedWidth(mContext);
        mTopBar.setStart(containerStart + rowsPadding, animate);
    }

    /**
     * Expanded: the list (Search and sections) moves up, as YouTube's does; the clip line stays put.
     */
    private void applyListShift(boolean animate) {
        if (mGrid == null) {
            return;
        }

        float target = mExpanded ? -mContext.getResources().getDimensionPixelSize(R.dimen.oculo_rail_expanded_shift) : 0;
        mGrid.animate().cancel();
        if (animate) {
            mGrid.animate().translationY(target)
                    .setDuration(mContext.getResources().getInteger(R.integer.oculo_rail_shift_duration))
                    .setUpdateListener(animation -> onListMoved())
                    .start();
        } else {
            mGrid.setTranslationY(target);
            onListMoved();
        }
    }

    /**
     * Keeps the grid's visible top on the clip line while it is translated, and Search on the list's first
     * slot.
     */
    private void onListMoved() {
        if (mGrid == null) {
            return;
        }

        int shift = Math.round(-mGrid.getTranslationY());
        mGrid.setClipBounds(new Rect(0, shift, mGrid.getWidth() > 0 ? mGrid.getWidth() : Integer.MAX_VALUE / 2,
                Integer.MAX_VALUE / 2));
        updateSearchPosition();
    }

    /**
     * Search sits one slot above the first section and follows the list's scroll.
     */
    private void updateSearchPosition() {
        if (mGrid == null || mSearchRow == null) {
            return;
        }

        int pitch = mContext.getResources().getDimensionPixelSize(R.dimen.oculo_rail_item_height)
                + mContext.getResources().getDimensionPixelSize(R.dimen.oculo_rail_item_spacing);
        RecyclerView.ViewHolder first = mGrid.findViewHolderForAdapterPosition(0);
        float scrolled;
        if (first == null) {
            // Scrolled far down: Search is above the clip line
            mSearchRow.setVisibility(View.INVISIBLE);
            scrolled = pitch;
        } else {
            mSearchRow.setVisibility(View.VISIBLE);
            mSearchRow.setTranslationY(first.itemView.getTop() - pitch + mGrid.getTranslationY());
            scrolled = mGrid.getPaddingTop() - first.itemView.getTop();
        }

        if (mListFade != null) {
            mListFade.setAlpha(Math.max(0f, Math.min(1f, scrolled / (pitch / 2f))));
        }
    }

    private void setupHeaders(HeadersSupportFragment headers, View headersRoot) {
        VerticalGridView grid = headers.getVerticalGridView();

        if (grid == null || grid == mGrid) {
            return;
        }

        mGrid = grid;
        headers.setItemsAlwaysVisible(true);
        Resources res = mContext.getResources();

        headersRoot.setElevation(0);
        View fadingEdge = headersRoot.findViewById(R.id.fade_out_edge);
        if (fadingEdge != null) {
            fadingEdge.setVisibility(View.GONE);
        }

        grid.setBackgroundColor(Color.TRANSPARENT);
        grid.setPaddingRelative(res.getDimensionPixelSize(R.dimen.oculo_rail_list_padding_start),
                res.getDimensionPixelSize(R.dimen.oculo_rail_list_top),
                res.getDimensionPixelSize(R.dimen.oculo_rail_list_padding_end),
                res.getDimensionPixelSize(R.dimen.oculo_rail_list_bottom));
        // The list starts below the avatar; sections scroll up to that line, into the padding where Search is.
        if (grid.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams gridLp = (ViewGroup.MarginLayoutParams) grid.getLayoutParams();
            gridLp.topMargin = res.getDimensionPixelSize(R.dimen.oculo_rail_list_clip_top);
            grid.setLayoutParams(gridLp);
        }
        grid.setClipToPadding(false);
        grid.setVerticalSpacing(res.getDimensionPixelSize(R.dimen.oculo_rail_item_spacing));
        applyGridAlignment();

        grid.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                styleItem(view);
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // NOP
            }
        });
        grid.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                updateCurrentSection();
            }
        });
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateSearchPosition();
            }
        });
        grid.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> onListMoved());

        if (headersRoot instanceof RelativeLayout) {
            addTopBlock((RelativeLayout) headersRoot, grid);
        }

        applyExpandedState();
        applyListShift(false);
    }

    /**
     * The list only scrolls when the focused section would leave the rail (leanback keeps the selected header
     * at a fixed offset, which would scroll Search away as soon as the second section is focused).
     */
    private void applyGridAlignment() {
        if (mGrid == null) {
            return;
        }

        mGrid.setItemAlignmentOffset(0);
        mGrid.setItemAlignmentOffsetPercent(50);
        mGrid.setWindowAlignmentOffset(0);
        mGrid.setWindowAlignmentOffsetPercent(50);
        mGrid.setWindowAlignment(BaseGridView.WINDOW_ALIGN_BOTH_EDGE);
    }

    private void addTopBlock(RelativeLayout headersRoot, VerticalGridView grid) {
        Resources res = mContext.getResources();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        int itemHeight = res.getDimensionPixelSize(R.dimen.oculo_rail_item_height);

        // Avatar: fixed at the top, centered on the top bar's line
        FrameLayout block = new FrameLayout(mContext);
        block.setPaddingRelative(grid.getPaddingStart(), 0, grid.getPaddingEnd(), 0);

        mAvatarRow = inflater.inflate(R.layout.icon_header_item, block, false);
        ((FrameLayout.LayoutParams) mAvatarRow.getLayoutParams()).topMargin =
                res.getDimensionPixelSize(R.dimen.oculo_rail_avatar_center) - itemHeight / 2;
        ImageView avatar = mAvatarRow.findViewById(R.id.header_icon);
        int avatarSize = res.getDimensionPixelSize(R.dimen.oculo_rail_avatar_size);
        int iconSize = res.getDimensionPixelSize(R.dimen.oculo_rail_icon_size);
        ViewGroup.LayoutParams lp = avatar.getLayoutParams();
        lp.width = lp.height = avatarSize;
        avatar.setLayoutParams(lp);
        // Keep the avatar centered on the icon column
        mAvatarRow.setPaddingRelative(mAvatarRow.getPaddingStart() - (avatarSize - iconSize) / 2, 0, mAvatarRow.getPaddingEnd(), 0);
        TextView avatarLabel = mAvatarRow.findViewById(R.id.header_label);
        ViewGroup.MarginLayoutParams labelLp = (ViewGroup.MarginLayoutParams) avatarLabel.getLayoutParams();
        labelLp.setMarginStart(labelLp.getMarginStart() - (avatarSize - iconSize) / 2);
        avatarLabel.setLayoutParams(labelLp);
        mAvatarRow.setOnClickListener(v -> AccountSelectionPresenter.instance(mContext).nextAccountOrDialog());
        mAvatarRow.setOnLongClickListener(v -> {
            AccountSettingsPresenter.instance(mContext).show();
            return true;
        });
        block.addView(mAvatarRow);

        // Search: the list's first slot. Lives in a frame with the list's bounds (clipped on the same line) and
        // follows its scroll (updateSearchPosition).
        FrameLayout searchClip = new FrameLayout(mContext);
        searchClip.setPaddingRelative(grid.getPaddingStart(), 0, grid.getPaddingEnd(), 0);
        mSearchRow = inflater.inflate(R.layout.icon_header_item, searchClip, false);
        ((FrameLayout.LayoutParams) mSearchRow.getLayoutParams()).gravity = Gravity.TOP;
        ImageView searchIcon = mSearchRow.findViewById(R.id.header_icon);
        searchIcon.setImageResource(R.drawable.oculo_ic_search);
        searchIcon.setImageTintList(railIconColors());
        ((TextView) mSearchRow.findViewById(R.id.header_label)).setText(R.string.action_search);
        mSearchRow.setOnClickListener(v -> SearchPresenter.instance(mContext).startSearch(null));
        searchClip.addView(mSearchRow);

        RelativeLayout.LayoutParams clipLp = new RelativeLayout.LayoutParams(grid.getLayoutParams().width,
                ViewGroup.LayoutParams.MATCH_PARENT);
        clipLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        clipLp.topMargin = res.getDimensionPixelSize(R.dimen.oculo_rail_list_clip_top);
        // Added after the grid: when the headers get focus (start, back from the player), the list wins.
        headersRoot.addView(searchClip, clipLp);
        mSearchClip = searchClip;

        // Fade at the clip line, shown once the list has scrolled
        View fade = new View(mContext);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Color.BLACK, Color.TRANSPARENT}));
        fade.setAlpha(0);
        RelativeLayout.LayoutParams fadeLp = new RelativeLayout.LayoutParams(grid.getLayoutParams().width,
                res.getDimensionPixelSize(R.dimen.oculo_rail_list_fade));
        fadeLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        fadeLp.topMargin = res.getDimensionPixelSize(R.dimen.oculo_rail_list_clip_top);
        headersRoot.addView(fade, fadeLp);
        mListFade = fade;

        RelativeLayout.LayoutParams blockLp = new RelativeLayout.LayoutParams(grid.getLayoutParams().width,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        blockLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        headersRoot.addView(block, blockLp);
        mTopBlock = block;

        updateAccount();
        updateSearchPosition();
    }

    private View onFocusSearch(View focused, int direction) {
        if (mTopBar != null && mTopBar.contains(focused)) {
            return mTopBar.onFocusSearch(focused, direction);
        }

        if (mTopBar != null && direction == View.FOCUS_UP && !mExpanded && mContent != null
                && isDescendant(focused, mContent)) {
            return mTopBar.getEntry(); // up from the first row: the mic, as in YouTube (leanback would pick the pill)
        }

        if (mTopBlock == null || mGrid == null) {
            return null;
        }

        if (focused == mAvatarRow || focused == mSearchRow) {
            switch (direction) {
                case View.FOCUS_UP:
                    return focused == mSearchRow ? mAvatarRow : focused;
                case View.FOCUS_DOWN:
                    return focused == mAvatarRow ? mSearchRow : mGrid;
                case View.FOCUS_LEFT:
                    return focused;
                default:
                    return null; // right: leanback moves to the content and collapses the rail
            }
        }

        if (direction == View.FOCUS_UP && mExpanded && isDescendant(focused, mGrid) && mSearchRow != null) {
            mSearchRow.setVisibility(View.VISIBLE);
            return mSearchRow; // up from the first section
        }

        return null;
    }

    private static boolean isDescendant(View view, View parent) {
        View current = view;
        while (current != null) {
            if (current == parent) {
                return true;
            }
            ViewParent next = current.getParent();
            current = next instanceof View ? (View) next : null;
        }
        return false;
    }

    private void applyExpandedState() {
        if (mGrid != null) {
            for (int i = 0; i < mGrid.getChildCount(); i++) {
                styleItem(mGrid.getChildAt(i));
            }
            if (!mExpanded) {
                // Collapsed: icons are visible but must not catch focus from the content (leanback's left
                // move targets the list itself, which then expands).
                mGrid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            }
        }

        for (View row : new View[] {mAvatarRow, mSearchRow}) {
            if (row != null) {
                row.setFocusable(mExpanded);
                setLabelVisible(row, mExpanded);
            }
        }

        if (mSearchRow != null) {
            ((ImageView) mSearchRow.findViewById(R.id.header_icon)).setImageTintList(railIconColors());
        }
    }

    private ColorStateList railIconColors() {
        return mExpanded ? mIconColorsExpanded : mIconColors;
    }

    private void styleItem(View child) {
        if (mGrid == null || child == null) {
            return;
        }

        // Grid child = leanback's wrapper around the header view (layout icon_header_item)
        ImageView icon = child.findViewById(R.id.header_icon);
        View item = icon != null && icon.getParent() instanceof View ? (View) icon.getParent() : child;

        RecyclerView.ViewHolder holder = mGrid.getChildViewHolder(child);
        if (icon != null) {
            // Section icons follow the rail colors; channel avatars (pinned channels) keep their image.
            icon.setImageTintList(isResourceIcon(holder) ? railIconColors() : null);
        }

        boolean current = holder != null && holder.getAdapterPosition() == mGrid.getSelectedPosition();
        item.setActivated(current);
        setLabelVisible(item, mExpanded);

        if (icon != null && OculoNewContent.isEnabled(mContext)) {
            int sectionId = getSectionId(holder);
            if (current && (sectionId == MediaGroup.TYPE_SUBSCRIPTIONS || sectionId == MediaGroup.TYPE_CHANNEL_UPLOADS)) {
                // Subscriptions or Channels opened: the new content is acknowledged
                OculoNewContent.instance(mContext).markSeen();
            }
            setNewContentDot(icon, sectionId == MediaGroup.TYPE_SUBSCRIPTIONS
                    && OculoNewContent.instance(mContext).hasDot());
        }
    }

    private static int getSectionId(@Nullable RecyclerView.ViewHolder holder) {
        if (holder instanceof ItemBridgeAdapter.ViewHolder) {
            Object item = ((ItemBridgeAdapter.ViewHolder) holder).getItem();
            if (item instanceof PageRow && ((PageRow) item).getHeaderItem() != null) {
                return (int) ((PageRow) item).getHeaderItem().getId();
            }
        }
        return -1;
    }

    /**
     * Small round Abricot dot at the icon's top right with a thin dark outline (docs/mockups/couleur-secondaire.html,
     * {@code .ic.dotted}), drawn in the icon's overlay: visible on the collapsed and expanded rail and on the white
     * focus pill. Recycled header views lose it when they show another section.
     */
    private void setNewContentDot(ImageView icon, boolean show) {
        Object tag = icon.getTag(R.id.oculo_rail_dot);
        if (!show) {
            if (tag instanceof Drawable) {
                icon.getOverlay().remove((Drawable) tag);
                icon.setTag(R.id.oculo_rail_dot, null);
            }
            return;
        }
        if (tag instanceof Drawable) {
            return;
        }
        Resources res = mContext.getResources();
        int size = res.getDimensionPixelSize(R.dimen.oculo_rail_dot_size);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(ContextCompat.getColor(mContext, R.color.oculo_rail_dot));
        dot.setStroke(res.getDimensionPixelSize(R.dimen.oculo_rail_dot_outline),
                ContextCompat.getColor(mContext, R.color.oculo_rail_dot_outline));
        int iconSize = res.getDimensionPixelSize(R.dimen.oculo_rail_icon_size);
        dot.setBounds(iconSize - size, 0, iconSize, size);
        icon.getOverlay().add(dot);
        icon.setTag(R.id.oculo_rail_dot, dot);
    }

    private void updateCurrentSection() {
        if (mGrid == null) {
            return;
        }

        for (int i = 0; i < mGrid.getChildCount(); i++) {
            styleItem(mGrid.getChildAt(i));
        }
    }

    private static boolean isResourceIcon(RecyclerView.ViewHolder holder) {
        if (!(holder instanceof ItemBridgeAdapter.ViewHolder)) {
            return true;
        }

        Object item = ((ItemBridgeAdapter.ViewHolder) holder).getItem();

        if (item instanceof PageRow && ((PageRow) item).getHeaderItem() instanceof SectionHeaderItem) {
            return ((SectionHeaderItem) ((PageRow) item).getHeaderItem()).getIconUrl() == null;
        }

        return true;
    }

    private static void setLabelVisible(View item, boolean visible) {
        View label = item.findViewById(R.id.header_label);
        if (label != null) {
            label.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void onAccountChanged(Account account) {
        updateAccount();
    }

    private void updateAccount() {
        if (mAvatarRow == null) {
            return;
        }

        ImageView avatar = mAvatarRow.findViewById(R.id.header_icon);
        TextView label = mAvatarRow.findViewById(R.id.header_label);
        Account current = MediaServiceManager.instance().getSelectedAccount();

        if (current != null) {
            label.setText(current.getName() != null ? current.getName() : current.getEmail());
        } else {
            label.setText(R.string.dialog_account_none);
        }

        if (current != null && current.getAvatarImageUrl() != null
                && !(mContext instanceof Activity && !Utils.checkActivity((Activity) mContext))) {
            avatar.setImageTintList(null);
            try {
                Glide.with(mContext)
                        .load(current.getAvatarImageUrl())
                        .apply(ViewUtil.glideOptions())
                        .circleCrop()
                        .into(avatar);
            } catch (ExceptionInInitializerError | IllegalArgumentException e) {
                e.printStackTrace();
            }
        } else {
            Drawable icon = ContextCompat.getDrawable(mContext, R.drawable.browse_title_account);
            avatar.setImageDrawable(icon);
            avatar.setImageTintList(mIconColors);
        }
    }
}
