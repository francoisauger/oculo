package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.oculo.settings.OculoSettingsModel;
import com.liskovsoft.smartyoutubetv2.common.oculo.settings.OculoSettingsModel.Tile;
import com.liskovsoft.smartyoutubetv2.common.oculo.settings.OculoSettingsNode;
import com.liskovsoft.smartyoutubetv2.common.oculo.settings.OculoSettingsTiles;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.utils.SimpleEditDialog;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.interfaces.SettingsSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo (0.11.0, step 5 of docs/mockups/settings/settings-redesign.html): the Settings section as a list of categories
 * at left and the selected category's settings at right, as in Google TV's settings.
 * <ul>
 *     <li>Up / down in the list shows each category's settings and values at right before entering (dimmed).</li>
 *     <li>Right or OK enters the category; OK toggles a switch or opens a value list, a group, a list of buttons;
 *     choosing a value comes back. Left or Back goes back one level, then to the list.</li>
 *     <li>Values on the right of each row, one-line Roboto summaries, white focus pill, « Advanced » heading in
 *     advanced mode, « Recent » shortcuts at the top.</li>
 *     <li>The settings password (General › Misc) is asked once per visit, before entering a category (as
 *     SettingsGridFragment asks before opening a tile).</li>
 * </ul>
 * The rows are {@link OculoSettingsModel}'s, i.e. upstream's own items: selecting runs upstream's callbacks. After a
 * change the model is rebuilt (shortly after, so key presses stay immediate) to refresh every value. Upstream's
 * « on close » callbacks (restart notice…) run when leaving the section.
 */
public class OculoSettingsFragment extends Fragment implements BrowseSupportFragment.MainFragmentAdapterProvider,
        SettingsSection, OculoSettingsTiles.Listener {
    private static final String TAG = "OculoSettings";
    private static final long REBUILD_DELAY_MS = 350;
    private static Boolean sEnabled;
    private static String sLastTileId;
    /**
     * Where the screen was when it was destroyed. Some settings rebuild the browse sections (Sidebar sections, separate
     * settings per account…), which recreates this fragment: the new one reopens the same place.
     */
    private static Restore sRestore;
    /** Advanced mode was just turned on: the Advanced category shows a dot until it is opened. */
    private static boolean sAdvancedNew;

    private static final class Restore {
        final String tileId;
        final boolean inDetail;
        final ArrayList<String> path;
        final String focusKey;
        final long time;

        Restore(String tileId, boolean inDetail, ArrayList<String> path, String focusKey) {
            this.tileId = tileId;
            this.inDetail = inDetail;
            this.path = path;
            this.focusKey = focusKey;
            this.time = System.currentTimeMillis();
        }
    }

    private final BrowseSupportFragment.MainFragmentAdapter<Fragment> mMainFragmentAdapter =
            new BrowseSupportFragment.MainFragmentAdapter<>(this);
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRebuild = this::rebuild;
    private final List<Runnable> mOnFinish = new ArrayList<>();
    /** Keys of the rows opened inside the category (groups, value lists…), outermost first. */
    private final ArrayList<String> mPath = new ArrayList<>();
    private OculoSettingsModel mModel;
    private List<Tile> mTiles = new ArrayList<>();
    private int mTile;
    private boolean mInDetail;
    private boolean mUnlocked;
    private boolean mBackDown;
    private boolean mChanged;
    /** The pane is being re-rendered: focus may pass through the list for a moment (RecyclerView rebind). */
    private boolean mRendering;
    /** A row opened something (dialog, keyboard, other screen): refresh the values when the window gets focus back. */
    private boolean mAwaitingReturn;
    private final android.view.ViewTreeObserver.OnWindowFocusChangeListener mWindowFocus = hasFocus -> {
        if (hasFocus && mAwaitingReturn) {
            mAwaitingReturn = false;
            mHandler.post(this::rebuild);
        }
    };

    private LinearLayout mList;
    private LinearLayout mRecent;
    private View mPane;
    private TextView mCrumb;
    private TextView mPaneTitle;
    private TextView mPaneDesc;
    private VerticalGridView mRows;
    private View mFade;
    private View mFadeTop;
    private TextView mLocked;
    private RowsAdapter mAdapter;

    /** Reads the flag once the app has a context (OculoRail.attach()). */
    public static void init(Context context) {
        if (sEnabled == null && context != null) {
            sEnabled = OculoSettingsTiles.isScreenEnabled(context);
        }
    }

    /** True when the Settings section should use this screen (BrowseSectionFragmentFactory hook). */
    public static boolean isEnabled() {
        return sEnabled != null && sEnabled;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fragment

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.oculo_settings_fragment, container, false);
        mList = root.findViewById(R.id.oculo_settings_list);
        mRecent = root.findViewById(R.id.oculo_settings_recent);
        mPane = root.findViewById(R.id.oculo_settings_pane);
        mCrumb = root.findViewById(R.id.oculo_settings_crumb);
        mPaneTitle = root.findViewById(R.id.oculo_settings_pane_title);
        mPaneDesc = root.findViewById(R.id.oculo_settings_pane_desc);
        mRows = root.findViewById(R.id.oculo_settings_rows);
        mFade = root.findViewById(R.id.oculo_settings_fade);
        mFadeTop = root.findViewById(R.id.oculo_settings_fade_top);
        mLocked = root.findViewById(R.id.oculo_settings_locked);

        // Entering the section (from the rail, the top bar, the gear): focus on the category, not the Recent chips
        ((androidx.leanback.widget.BrowseFrameLayout) root).setOnChildFocusListener(new androidx.leanback.widget.BrowseFrameLayout.OnChildFocusListener() {
            @Override
            public boolean onRequestFocusInDescendants(int direction, android.graphics.Rect previouslyFocusedRect) {
                if (mList != null && mTile < mList.getChildCount()) {
                    return mList.getChildAt(mTile).requestFocus();
                }
                return false;
            }

            @Override
            public void onRequestChildFocus(View child, View focused) {
                // NOP
            }
        });

        // D-pad inside the screen first: up / down move in the list (leanback's rail rules would send any « up » from
        // the content to the top bar), the pane keeps the focus while a category is open
        ((androidx.leanback.widget.BrowseFrameLayout) root).setOnFocusSearchListener((focused, direction) -> {
            View next = android.view.FocusFinder.getInstance().findNextFocus((ViewGroup) root, focused, direction);

            if (mRows != null && isDescendant(focused, mRows)) {
                if (next != null && isDescendant(next, mRows)) {
                    return next;
                }
                return direction == View.FOCUS_UP || direction == View.FOCUS_DOWN ? focused : null;
            }

            if (next != null && !isDescendant(next, mPane)) {
                return next;
            }

            return direction == View.FOCUS_DOWN ? focused : null; // up from the top: the top bar; left: the rail
        });

        mAdapter = new RowsAdapter();
        mRows.setAdapter(mAdapter);
        mRows.setItemAnimator(null);
        mRows.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE);
        mRows.setWindowAlignmentOffsetPercent(35f);
        mRows.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateFade();
            }
        });

        build();

        // Back to the category visited last time
        if (sLastTileId != null) {
            for (int i = 0; i < mTiles.size(); i++) {
                if (sLastTileId.equals(mTiles.get(i).getId())) {
                    mTile = i;
                }
            }
        }

        // Recreated right away (sections rebuilt by a setting), not a new visit: within one second
        Restore restore = sRestore != null && System.currentTimeMillis() - sRestore.time < 1_000 ? sRestore : null;
        sRestore = null;

        if (restore != null) {
            for (int i = 0; i < mTiles.size(); i++) {
                if (mTiles.get(i).getId().equals(restore.tileId)) {
                    mTile = i;
                }
            }
        }

        if (restore != null && restore.inDetail) {
            mInDetail = true;
            mPath.addAll(restore.path);
            mUnlocked = true; // the password was given before the refresh
            while (!mPath.isEmpty() && resolve(mPath.get(mPath.size() - 1)) == null) {
                mPath.remove(mPath.size() - 1);
            }
        }

        renderList();
        renderRecent();
        renderPane(mInDetail);
        updateListState();

        if (restore != null) {
            String focusKey = restore.focusKey;
            root.post(() -> {
                if (mInDetail) {
                    int position = mAdapter.indexOfKey(focusKey);
                    focusRow(position >= 0 ? position : firstFocusable());
                } else {
                    focusCategory();
                }
            });
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyViewCreated(mMainFragmentAdapter);
        }

        OculoSettingsTiles.addListener(this);
        // Back from an upstream dialog, keyboard or screen opened by a row (account, password, segments…): fresh values
        view.getViewTreeObserver().addOnWindowFocusChangeListener(mWindowFocus);
    }

    @Override
    public void onDestroyView() {
        if (currentTile() != null && mRows != null) {
            sRestore = new Restore(currentTile().getId(), mInDetail && !isLocked(), new ArrayList<>(mPath), focusedRowKey());
        }

        if (getView() != null) {
            getView().getViewTreeObserver().removeOnWindowFocusChangeListener(mWindowFocus);
        }
        super.onDestroyView();
        mHandler.removeCallbacks(mRebuild);
        OculoSettingsTiles.removeListener(this);
        runOnFinish();
        mList = null;
        mRows = null;
    }

    @Override
    public BrowseSupportFragment.MainFragmentAdapter<Fragment> getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    // SettingsSection: the screen builds its own model; the section's items are not used.
    @Override
    public void update(SettingsGroup items) {
        // NOP
    }

    @Override
    public void clear() {
        // NOP
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /** Advanced mode turned on or off (OculoSettingsTiles): categories change. */
    @Override
    public void onSettingsChanged() {
        boolean wasAdvanced = mModel != null && mModel.isAdvancedMode();
        mHandler.post(() -> {
            rebuild();
            if (!wasAdvanced && mModel != null && mModel.isAdvancedMode()) {
                sAdvancedNew = true;
                renderList();
            }
        });
    }

    private static boolean isDescendant(View view, View parent) {
        View current = view;
        while (current != null) {
            if (current == parent) {
                return true;
            }
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Model

    private void build() {
        Context context = getContext();

        if (context == null) {
            return;
        }

        if (mModel != null && mChanged) {
            mOnFinish.addAll(mModel.getOnFinish());
        }

        mModel = OculoSettingsModel.build(context);
        mTiles = mModel.getVisibleTiles();
        mChanged = false;

        if (mTile >= mTiles.size()) {
            mTile = Math.max(0, mTiles.size() - 1);
        }
    }

    private void runOnFinish() {
        if (mModel != null) {
            mOnFinish.addAll(mModel.getOnFinish());
        }

        for (Runnable runnable : new ArrayList<>(mOnFinish)) {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "onFinish: " + e);
            }
        }

        mOnFinish.clear();
    }

    /** Rebuilds the model and the screen, keeping the category, the opened rows and the focused row. */
    private void rebuild() {
        if (mList == null || getContext() == null) {
            return;
        }

        mHandler.removeCallbacks(mRebuild);
        String tileId = currentTile() != null ? currentTile().getId() : null;
        String focusKey = focusedRowKey();
        int focusPosition = mRows.getSelectedPosition();
        boolean listFocused = mList.hasFocus();
        mChanged = true; // the old model's « on close » callbacks are kept
        build();

        mTile = 0;
        for (int i = 0; i < mTiles.size(); i++) {
            if (mTiles.get(i).getId().equals(tileId)) {
                mTile = i;
            }
        }

        // Drop opened rows that no longer exist (e.g. advanced mode turned off)
        while (!mPath.isEmpty() && resolve(mPath.get(mPath.size() - 1)) == null) {
            mPath.remove(mPath.size() - 1);
        }

        renderList();
        renderRecent();
        renderPane(false);

        if (listFocused) {
            focusCategory();
        } else if (mInDetail) {
            int position = mAdapter.indexOfKey(focusKey);
            focusRow(position >= 0 ? position : Math.min(focusPosition, mAdapter.getItemCount() - 1));
        }
    }

    private void scheduleRebuild() {
        mChanged = true;
        mHandler.removeCallbacks(mRebuild);
        mHandler.postDelayed(mRebuild, REBUILD_DELAY_MS);
    }

    @Nullable
    private Tile currentTile() {
        return mTile >= 0 && mTile < mTiles.size() ? mTiles.get(mTile) : null;
    }

    /** A row of the current category by key (any depth), or null. */
    @Nullable
    private OculoSettingsNode resolve(String key) {
        Tile tile = currentTile();

        if (tile == null || key == null) {
            return null;
        }

        List<OculoSettingsNode> all = new ArrayList<>();
        OculoSettingsNode.flatten(mModel.getRows(tile), all);

        for (OculoSettingsNode node : all) {
            if (key.equals(node.key)) {
                return node;
            }
        }

        return null;
    }

    private boolean isLocked() {
        Context context = getContext();
        return !mUnlocked && context != null && GeneralData.instance(context).getSettingsPassword() != null;
    }

    /** Settings password, as SettingsGridFragment asks it before opening a tile. */
    private void unlockThen(Runnable action) {
        Context context = getContext();

        if (!isLocked() || context == null) {
            action.run();
            return;
        }

        String password = GeneralData.instance(context).getSettingsPassword();
        SimpleEditDialog.showPassword(context, context.getString(R.string.enter_settings_password), null, newValue -> {
            if (Utils.passwordMatch(password, newValue)) {
                mUnlocked = true;
                renderPane(mInDetail);
                action.run();
                return true;
            }
            return false;
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Left list

    private void renderList() {
        if (mList == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(mList.getContext());

        while (mList.getChildCount() > mTiles.size()) {
            mList.removeViewAt(mList.getChildCount() - 1);
        }

        while (mList.getChildCount() < mTiles.size()) {
            View row = inflater.inflate(R.layout.oculo_settings_category, mList, false);
            row.setOnClickListener(v -> enterDetail(mList.indexOfChild(v)));
            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && mInDetail && !mRendering) {
                    // Focus came back to the list another way than Left / Back
                    mInDetail = false;
                    mPath.clear();
                    updateListState();
                }
                if (hasFocus && !mInDetail) {
                    int index = mList.indexOfChild(v);
                    if (index != mTile || !mPath.isEmpty()) {
                        mTile = index;
                        mPath.clear();
                        renderPane(false);
                    }
                }
            });
            row.setOnKeyListener(this::onCategoryKey);
            mList.addView(row);
        }

        for (int i = 0; i < mTiles.size(); i++) {
            Tile tile = mTiles.get(i);
            View row = mList.getChildAt(i);
            ImageView icon = row.findViewById(R.id.oculo_settings_icon);
            icon.setImageResource(OculoSettingsTiles.TILE_ICONS[tile.index]);
            ((TextView) row.findViewById(R.id.oculo_settings_title)).setText(tile.titleRes);
            CharSequence summary = mModel.getSummary(tile);
            TextView summaryView = row.findViewById(R.id.oculo_settings_summary);
            summaryView.setText(summary);
            summaryView.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
            View dot = row.findViewById(R.id.oculo_settings_dot);
            boolean isNew = sAdvancedNew && "adv".equals(tile.getId());
            dot.setVisibility(isNew ? View.VISIBLE : View.GONE);
            dot.setBackgroundResource(R.drawable.oculo_settings_new_dot);
        }

        updateListState();
    }

    private void updateListState() {
        if (mList == null) {
            return;
        }

        for (int i = 0; i < mList.getChildCount(); i++) {
            mList.getChildAt(i).setActivated(mInDetail && i == mTile);
        }

        // One level deeper than the category (a value list or a group): the list fades, as in the mock C
        mList.setAlpha(mInDetail && !mPath.isEmpty() ? 0.5f : 1f);
    }

    private boolean onCategoryKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            enterDetail(mList.indexOfChild(v));
            return true;
        }

        return false;
    }

    private void focusCategory() {
        if (mList != null && mTile < mList.getChildCount()) {
            mList.getChildAt(mTile).requestFocus();
        }
    }

    private void enterDetail(int index) {
        if (index < 0 || index >= mTiles.size()) {
            return;
        }

        unlockThen(() -> {
            mTile = index;
            sLastTileId = mTiles.get(index).getId();
            if ("adv".equals(sLastTileId) && sAdvancedNew) {
                sAdvancedNew = false;
                renderList();
            }
            mPath.clear();
            mInDetail = true;
            renderPane(true);
            updateListState();
            focusRow(firstFocusable());
        });
    }

    private void exitDetail() {
        mRendering = false;
        mInDetail = false;
        mPath.clear();
        renderPane(false);
        updateListState();
        focusCategory();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Recent

    private void renderRecent() {
        if (mRecent == null || getContext() == null) {
            return;
        }

        // Keep the label (first child)
        while (mRecent.getChildCount() > 1) {
            mRecent.removeViewAt(1);
        }

        List<OculoSettingsNode> rows = OculoSettingsTiles.getRecentRows(getContext(), mModel);
        mRecent.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        float density = getResources().getDisplayMetrics().density;

        for (OculoSettingsNode node : rows) {
            TextView chip = new TextView(getContext());
            chip.setText(node.title);
            chip.setFocusable(true);
            chip.setFocusableInTouchMode(true);
            chip.setSingleLine(true);
            chip.setBackgroundResource(R.drawable.oculo_settings_chip);
            chip.setTextColor(getResources().getColorStateList(R.color.oculo_settings_title));
            chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.oculo_settings_pill_text));
            chip.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL));
            chip.setPadding((int) (9 * density), (int) (3 * density), (int) (9 * density), (int) (3 * density));
            chip.setOnClickListener(v -> openRow(node.key, node.tile));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart((int) (6 * density));
            mRecent.addView(chip, lp);
        }
    }

    /** Opens the category holding a row (by key) with the row focused (« Recent »). */
    private void openRow(String key, int tileIndex) {
        int index = -1;

        for (int i = 0; i < mTiles.size(); i++) {
            if (mTiles.get(i).index == tileIndex) {
                index = i;
            }
        }

        if (index < 0) {
            return;
        }

        int target = index;
        unlockThen(() -> {
            mTile = target;
            sLastTileId = mTiles.get(target).getId();
            mPath.clear();
            List<String> path = new ArrayList<>();

            if (findPath(mModel.getRows(mTiles.get(target)), key, path)) {
                mPath.addAll(path);
            }

            mInDetail = true;
            renderPane(true);
            updateListState();
            int position = mAdapter.indexOfKey(key);
            focusRow(position >= 0 ? position : firstFocusable());
        });
    }

    private static boolean findPath(List<OculoSettingsNode> rows, String key, List<String> path) {
        for (OculoSettingsNode node : rows) {
            if (key.equals(node.key)) {
                return true;
            }

            if (node.kind == OculoSettingsNode.PAGE) {
                path.add(node.key);
                if (findPath(node.children, key, path)) {
                    return true;
                }
                path.remove(path.size() - 1);
            }
        }

        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Right pane

    /** One line of the pane: a heading, a row of the category, or a value / item of an opened row. */
    private static final class Item {
        static final int HEADING = 0;
        static final int NODE = 1;
        static final int VALUE = 2;
        static final int TOGGLE = 3;
        static final int BUTTON = 4;
        static final int TEXT = 5;
        final int type;
        final CharSequence title;
        @Nullable final OculoSettingsNode node;
        @Nullable final OptionItem option;
        @Nullable final OculoSettingsNode parent;

        Item(int type, CharSequence title, @Nullable OculoSettingsNode node, @Nullable OptionItem option, @Nullable OculoSettingsNode parent) {
            this.type = type;
            this.title = title;
            this.node = node;
            this.option = option;
            this.parent = parent;
        }
    }

    private void renderPane(boolean active) {
        if (mRows == null || getContext() == null) {
            return;
        }

        Tile tile = currentTile();
        List<Item> items = new ArrayList<>();
        CharSequence title = tile != null ? getString(tile.titleRes) : "";
        CharSequence crumb = null;
        CharSequence desc = null;
        OculoSettingsNode opened = !mPath.isEmpty() ? resolve(mPath.get(mPath.size() - 1)) : null;

        if (opened != null) {
            StringBuilder sb = new StringBuilder(title);
            for (int i = 0; i < mPath.size() - 1; i++) {
                OculoSettingsNode node = resolve(mPath.get(i));
                if (node != null) {
                    sb.append("  ›  ").append(node.title);
                }
            }
            crumb = sb;
            title = opened.title;
            desc = opened.summary;
            addOpened(items, opened);
        } else if (tile != null) {
            for (OculoSettingsNode node : tile.simple) {
                items.add(new Item(Item.NODE, node.title, node, null, null));
            }

            if (mModel.isAdvancedMode() && !tile.advanced.isEmpty()) {
                if (!tile.simple.isEmpty()) {
                    items.add(new Item(Item.HEADING, getString(R.string.oculo_set_heading_advanced), null, null, null));
                }
                for (OculoSettingsNode node : tile.advanced) {
                    items.add(new Item(Item.NODE, node.title, node, null, null));
                }
            }
        }

        mCrumb.setText(crumb);
        mCrumb.setVisibility(crumb != null ? View.VISIBLE : View.GONE);
        mPaneTitle.setText(title);
        mPaneDesc.setText(desc);
        mPaneDesc.setVisibility(TextUtils.isEmpty(desc) ? View.GONE : View.VISIBLE);

        boolean locked = isLocked();
        mLocked.setVisibility(locked ? View.VISIBLE : View.GONE);
        mRows.setVisibility(locked ? View.INVISIBLE : View.VISIBLE);
        mPane.setAlpha(active || mInDetail ? 1f : 0.82f);
        // Rows can be focused only once the category is entered (the list keeps the focus while browsing it)
        mRows.setDescendantFocusability(mInDetail ? ViewGroup.FOCUS_AFTER_DESCENDANTS : ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        mRows.setFocusable(false);
        mRendering = mInDetail;
        mAdapter.setItems(locked ? new ArrayList<>() : items);
        mRows.scrollToPosition(0);
        mRows.post(this::updateFade);
    }

    private void addOpened(List<Item> items, OculoSettingsNode opened) {
        switch (opened.kind) {
            case OculoSettingsNode.PAGE:
                for (OculoSettingsNode child : opened.children) {
                    items.add(new Item(Item.NODE, child.title, child, null, null));
                }
                break;
            case OculoSettingsNode.CHOICE:
            case OculoSettingsNode.MULTI:
            case OculoSettingsNode.LIST:
            case OculoSettingsNode.INFO:
                OptionCategory category = opened.category;
                if (category == null || category.options == null) {
                    break;
                }
                int type = opened.kind == OculoSettingsNode.CHOICE ? Item.VALUE : opened.kind == OculoSettingsNode.MULTI ? Item.TOGGLE
                        : opened.kind == OculoSettingsNode.LIST ? Item.BUTTON : Item.TEXT;
                for (OptionItem option : category.options) {
                    if (option != null && option.getTitle() != null) {
                        items.add(new Item(type, option.getTitle(), null, option, opened));
                    }
                }
                break;
        }
    }

    private void updateFade() {
        if (mRows == null || mFade == null) {
            return;
        }

        boolean shown = mRows.getVisibility() == View.VISIBLE;
        mFade.setVisibility(shown && mRows.canScrollVertically(1) ? View.VISIBLE : View.GONE);
        mFadeTop.setVisibility(shown && mRows.canScrollVertically(-1) ? View.VISIBLE : View.GONE);
    }

    private int firstFocusable() {
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            if (mAdapter.getItem(i).type != Item.HEADING) {
                return i;
            }
        }
        return 0;
    }

    /** The checked value of a value list (focus lands there when it opens). */
    private int checkedValue() {
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            Item item = mAdapter.getItem(i);
            if (item.type == Item.VALUE && item.option != null && item.option.isSelected()) {
                return i;
            }
        }
        return firstFocusable();
    }

    private void focusRow(int position) {
        if (mRows == null || mAdapter.getItemCount() == 0) {
            mRendering = false;
            if (mInDetail && mAdapter.getItemCount() == 0 && !isLocked()) {
                exitDetail();
            }
            return;
        }

        int target = Math.max(0, Math.min(position, mAdapter.getItemCount() - 1));
        mRows.setSelectedPosition(target);
        mRows.post(() -> {
            mRendering = false;
            if (mRows == null) {
                return;
            }
            RecyclerView.ViewHolder holder = mRows.findViewHolderForAdapterPosition(target);
            if (holder != null) {
                holder.itemView.requestFocus();
            } else {
                mRows.requestFocus();
            }
        });
    }

    @Nullable
    private String focusedRowKey() {
        if (mRows == null) {
            return null;
        }

        int position = mRows.getSelectedPosition();

        if (position < 0 || position >= mAdapter.getItemCount()) {
            return null;
        }

        Item item = mAdapter.getItem(position);
        return item.node != null ? item.node.key : item.title != null ? item.title.toString() : null;
    }

    private void open(OculoSettingsNode node) {
        mPath.add(node.key);
        renderPane(true);
        updateListState();
        focusRow(node.kind == OculoSettingsNode.CHOICE ? checkedValue() : firstFocusable());
    }

    /** Left / Back in the pane: one level up, then the list. */
    private void goBack() {
        if (mPath.isEmpty()) {
            exitDetail();
            return;
        }

        String key = mPath.remove(mPath.size() - 1);
        renderPane(true);
        updateListState();
        int position = mAdapter.indexOfKey(key);
        focusRow(position >= 0 ? position : firstFocusable());
    }

    private void onItemClicked(Item item, int position) {
        mAwaitingReturn = true;

        switch (item.type) {
            case Item.NODE:
                onNodeClicked(item.node, position);
                break;
            case Item.VALUE:
                if (item.option != null) {
                    item.option.onSelect(true);
                    mChanged = true;
                    goBack();
                    rebuild();
                }
                break;
            case Item.TOGGLE:
                if (item.option != null) {
                    toggle(item.option);
                    mAdapter.notifyItemChanged(position);
                    scheduleRebuild();
                }
                break;
            case Item.BUTTON:
                if (item.option != null) {
                    mChanged = true;
                    item.option.onSelect(true);
                }
                break;
        }
    }

    private void onNodeClicked(@Nullable OculoSettingsNode node, int position) {
        if (node == null) {
            return;
        }

        switch (node.kind) {
            case OculoSettingsNode.SWITCH:
                if (node.item != null) {
                    node.item.onSelect(!node.item.isSelected());
                    mAdapter.notifyItemChanged(position);
                    scheduleRebuild();
                }
                break;
            case OculoSettingsNode.ACTION:
                if (node.item != null) {
                    mChanged = true;
                    node.item.onSelect(true);
                }
                break;
            default:
                open(node);
                break;
        }
    }

    /** A checkbox list item, with upstream's rules (AppPreferenceManager): required items, exclusive items. */
    private void toggle(OptionItem option) {
        boolean select = !option.isSelected();
        Context context = getContext();

        if (select) {
            OptionItem[] required = option.getRequired();

            if (required != null && context != null) {
                for (OptionItem item : required) {
                    if (!item.isSelected()) {
                        MessageHelpers.showMessage(context, context.getString(R.string.require_checked, item.getTitle()));
                    }
                }
            }

            OptionItem[] radio = option.getRadio();

            if (radio != null) {
                for (OptionItem item : radio) {
                    item.onSelect(false);
                }
            }
        }

        option.onSelect(select);
    }

    private boolean onRowKey(View v, int keyCode, KeyEvent event, int position) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            // Consume both halves so the activity does not leave the section
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mBackDown = true;
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP && mBackDown) {
                mBackDown = false;
                goBack();
                return true;
            }
            return false;
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            goBack();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            Item item = position < mAdapter.getItemCount() ? mAdapter.getItem(position) : null;
            if (item != null && item.type == Item.NODE && item.node != null && item.node.kind != OculoSettingsNode.SWITCH
                    && item.node.kind != OculoSettingsNode.ACTION) {
                open(item.node);
            }
            return true; // nothing to the right of the pane
        }

        return false;
    }

    private final class RowsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Item> mItems = new ArrayList<>();

        void setItems(List<Item> items) {
            mItems = items;
            notifyDataSetChanged();
        }

        Item getItem(int position) {
            return mItems.get(position);
        }

        int indexOfKey(@Nullable String key) {
            if (key == null) {
                return -1;
            }

            for (int i = 0; i < mItems.size(); i++) {
                Item item = mItems.get(i);
                if ((item.node != null && key.equals(item.node.key)) || (item.node == null && item.title != null && key.equals(item.title.toString()))) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mItems.get(position).type == Item.HEADING ? 0 : 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    viewType == 0 ? R.layout.oculo_settings_heading_row : R.layout.oculo_settings_row, parent, false);
            RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(view) {};

            if (viewType != 0) {
                view.setOnClickListener(v -> {
                    int position = holder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < mItems.size()) {
                        onItemClicked(mItems.get(position), position);
                    }
                });
                view.setOnKeyListener((v, keyCode, event) -> {
                    int position = holder.getAdapterPosition();
                    return position != RecyclerView.NO_POSITION && onRowKey(v, keyCode, event, position);
                });
            }

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Item item = mItems.get(position);
            View view = holder.itemView;
            TextView title = view.findViewById(R.id.oculo_settings_title);

            if (item.type == Item.HEADING) {
                title.setText(item.title);
                return;
            }

            Context context = view.getContext();
            TextView summary = view.findViewById(R.id.oculo_settings_summary);
            TextView value = view.findViewById(R.id.oculo_settings_value);
            ImageView radio = view.findViewById(R.id.oculo_settings_radio);
            ImageView toggle = view.findViewById(R.id.oculo_settings_switch);
            ImageView chevron = view.findViewById(R.id.oculo_settings_chevron);

            CharSequence titleText = item.type == Item.TEXT ? item.title.toString().trim() : OculoSettingsNode.firstLine(item.title);
            CharSequence summaryText = null;
            CharSequence valueText = null;
            int switchState = -1; // -1 none, 0 off, 1 on
            int radioState = -1;
            boolean showChevron = false;

            if (item.type == Item.NODE && item.node != null) {
                OculoSettingsNode node = item.node;
                summaryText = node.summary;

                switch (node.kind) {
                    case OculoSettingsNode.SWITCH:
                        switchState = node.isOn() ? 1 : 0;
                        break;
                    case OculoSettingsNode.CHOICE:
                    case OculoSettingsNode.MULTI:
                        valueText = node.getValue(context);
                        break;
                    case OculoSettingsNode.ACTION:
                        valueText = node.getValue(context);
                        showChevron = true;
                        break;
                    default:
                        showChevron = true;
                        break;
                }
            } else if (item.option != null) {
                summaryText = item.option.getDescription();

                switch (item.type) {
                    case Item.VALUE:
                        radioState = item.option.isSelected() ? 1 : 0;
                        break;
                    case Item.TOGGLE:
                        switchState = item.option.isSelected() ? 1 : 0;
                        break;
                }
            }

            title.setText(titleText);
            title.setMaxLines(item.type == Item.TEXT ? 200 : 2);
            summary.setText(summaryText);
            summary.setVisibility(TextUtils.isEmpty(summaryText) ? View.GONE : View.VISIBLE);
            value.setText(valueText);
            value.setVisibility(TextUtils.isEmpty(valueText) ? View.GONE : View.VISIBLE);
            toggle.setVisibility(switchState >= 0 ? View.VISIBLE : View.GONE);
            if (switchState >= 0) {
                toggle.setImageResource(switchState == 1 ? R.drawable.oculo_settings_switch_on : R.drawable.oculo_settings_switch_off);
            }
            radio.setVisibility(radioState >= 0 ? View.VISIBLE : View.GONE);
            if (radioState >= 0) {
                radio.setImageResource(radioState == 1 ? R.drawable.oculo_settings_radio_on : R.drawable.oculo_settings_radio_off);
            }
            chevron.setVisibility(showChevron ? View.VISIBLE : View.GONE);
        }
    }
}
