package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.sharedutils.helpers.AppInfoHelpers;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AboutSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AboutSimpleSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AutoFrameRateSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.BackupSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.DeArrowSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.GeneralSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.LanguageSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.MainUISettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.PlayerSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.RemoteControlSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.SearchSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.SponsorBlockSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.SubtitleSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.AppDataSourceManager;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoData;
import com.liskovsoft.smartyoutubetv2.common.oculo.settings.OculoSettingsMap.Entry;
import com.liskovsoft.smartyoutubetv2.common.oculo.settings.OculoSettingsMap.Sel;
import com.liskovsoft.smartyoutubetv2.common.prefs.AccountsData;
import com.liskovsoft.smartyoutubetv2.common.utils.SimpleEditDialog;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oculo (0.11.0): Oculo's Settings tree, built from what upstream's settings presenters would show
 * ({@link OculoSettingsCapture}) and placed by {@link OculoSettingsMap}. The rows hold upstream's own items, so
 * preferences, side effects and restart notices are upstream's. Build a new model to refresh the values (after a
 * change); it is cheap (no network, no disk write).
 */
public final class OculoSettingsModel {
    private static final String TAG = "OculoSettings";
    private static final Pattern RESOLUTION = Pattern.compile("(\\d{3,4})p");
    private static final Pattern FPS = Pattern.compile("(\\d{2,3})fps");

    public static final class Tile {
        public final int index;
        public final int titleRes;
        public final List<OculoSettingsNode> simple = new ArrayList<>();
        public final List<OculoSettingsNode> advanced = new ArrayList<>();

        Tile(int index, int titleRes) {
            this.index = index;
            this.titleRes = titleRes;
        }

        public String getId() {
            return TILE_IDS[index];
        }
    }

    public static final String[] TILE_IDS = {"play", "av", "home", "acct", "filt", "remote", "sys", "adv"};
    public static final int[] TILE_TITLES = {R.string.oculo_tile_play, R.string.oculo_tile_av, R.string.oculo_tile_home,
            R.string.oculo_tile_acct, R.string.oculo_tile_filt, R.string.oculo_tile_remote, R.string.oculo_tile_sys, R.string.oculo_tile_adv};

    /** One-line summaries of the simple rows: {label, summary}. */
    private static final int[][] SUMMARIES = {
            {R.string.oculo_set_when_a_video_ends, R.string.oculo_sum_when_a_video_ends},
            {R.string.oculo_set_video_quality, R.string.oculo_sum_video_quality},
            {R.string.oculo_set_speed, R.string.oculo_sum_speed},
            {R.string.oculo_set_skip_step, R.string.oculo_sum_skip_step},
            {R.string.oculo_set_sleep_timer, R.string.oculo_sum_sleep_timer},
            {R.string.oculo_set_audio_language, R.string.oculo_sum_audio_language},
            {R.string.oculo_set_subtitle_style, R.string.oculo_sum_subtitle_style},
            {R.string.oculo_set_subtitle_size, R.string.oculo_sum_subtitle_size},
            {R.string.oculo_set_start_screen, R.string.oculo_sum_start_screen},
            {R.string.oculo_set_sidebar_sections, R.string.oculo_sum_sidebar_sections},
            {R.string.oculo_set_dim_the_screen_after, R.string.oculo_sum_dim_the_screen_after},
            {R.string.oculo_set_interface_size, R.string.oculo_sum_interface_size},
            {R.string.oculo_set_hide_news_and_sports_rows, R.string.oculo_sum_hide_news_and_sports_rows},
            {R.string.oculo_set_show_continue_watching, R.string.oculo_sum_show_continue_watching},
            {R.string.oculo_set_youtube_language, R.string.oculo_sum_youtube_language},
            {R.string.oculo_set_content_country, R.string.oculo_sum_content_country},
            {R.string.oculo_set_watch_history, R.string.oculo_sum_watch_history},
            {R.string.oculo_set_search_history, R.string.oculo_sum_search_history},
            {R.string.oculo_set_hide_shorts_everywhere, R.string.oculo_sum_hide_shorts_everywhere},
            {R.string.oculo_set_sponsorblock, R.string.oculo_sum_sponsorblock},
            {R.string.oculo_set_segments, R.string.oculo_sum_segments},
            {R.string.oculo_set_phone_remote, R.string.oculo_sum_phone_remote},
            {R.string.oculo_set_link_a_phone, R.string.oculo_sum_link_a_phone},
            {R.string.oculo_set_back_up_settings, R.string.oculo_sum_back_up_settings},
            {R.string.oculo_set_restore_settings, R.string.oculo_sum_restore_settings},
            {R.string.oculo_set_check_for_updates, R.string.oculo_sum_check_for_updates},
            {R.string.oculo_set_whats_new, R.string.oculo_sum_whats_new},
    };

    /** A setting as upstream shows it: a category, or one item of a category. */
    private static final class Leaf {
        final int source;
        /** Index in {@link #mSubs} of the sub-screen, or -1 for the presenter's own screen. */
        final int sub;
        final OptionCategory category;
        @Nullable final OptionItem item;
        boolean used;

        Leaf(int source, int sub, OptionCategory category, @Nullable OptionItem item) {
            this.source = source;
            this.sub = sub;
            this.category = category;
            this.item = item;
        }

        boolean isCategoryLeaf() {
            return item == null;
        }

        boolean isSingle() {
            return category.type == OptionCategory.TYPE_SINGLE_SWITCH || category.type == OptionCategory.TYPE_SINGLE_BUTTON;
        }
    }

    private static final class Sub {
        final int source;
        final Sel sel;
        final OptionItem button;

        Sub(int source, Sel sel, OptionItem button) {
            this.source = source;
            this.sel = sel;
            this.button = button;
        }
    }

    private final Context mContext;
    private final boolean mAdvancedMode;
    private final List<Tile> mTiles = new ArrayList<>();
    private final List<Runnable> mOnFinish = new ArrayList<>();
    private final List<Leaf> mLeaves = new ArrayList<>();
    private final List<Sub> mSubs = new ArrayList<>();
    private final Map<Integer, String> mStrings = new HashMap<>();
    private final OculoSettingsMap.Strings mStr = this::str;

    private OculoSettingsModel(Context context) {
        mContext = context;
        mAdvancedMode = OculoData.instance(context).isSettingsAdvancedEnabled();

        for (int i = 0; i < OculoSettingsMap.TILE_COUNT; i++) {
            mTiles.add(new Tile(i, TILE_TITLES[i]));
        }
    }

    public static OculoSettingsModel build(Context context) {
        OculoSettingsModel model = new OculoSettingsModel(context);
        Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        OculoOptionItem.setChangeListener(key -> OculoSettingsRecent.record(appContext, key));
        long start = System.currentTimeMillis();
        model.harvest();
        model.place();
        Log.d(TAG, "Model built in " + (System.currentTimeMillis() - start) + " ms, " + model.mLeaves.size() + " upstream settings");
        return model;
    }

    public boolean isAdvancedMode() {
        return mAdvancedMode;
    }

    /** The 7 categories, plus Advanced in advanced mode. */
    public List<Tile> getVisibleTiles() {
        List<Tile> result = new ArrayList<>();

        for (Tile tile : mTiles) {
            if (tile.index != OculoSettingsMap.T_ADV || mAdvancedMode) {
                result.add(tile);
            }
        }

        return result;
    }

    @Nullable
    public Tile getTile(String id) {
        for (Tile tile : mTiles) {
            if (tile.getId().equals(id)) {
                return tile;
            }
        }

        return null;
    }

    /** Rows of a category in the current mode (simple rows first; advanced rows after them in advanced mode). */
    public List<OculoSettingsNode> getRows(Tile tile) {
        List<OculoSettingsNode> rows = new ArrayList<>(tile.simple);

        if (mAdvancedMode) {
            rows.addAll(tile.advanced);
        }

        return rows;
    }

    /** A row by its key, among the rows visible in the current mode, or null. */
    @Nullable
    public OculoSettingsNode findByKey(String key) {
        if (key == null) {
            return null;
        }

        for (Tile tile : getVisibleTiles()) {
            List<OculoSettingsNode> all = new ArrayList<>();
            OculoSettingsNode.flatten(getRows(tile), all);

            for (OculoSettingsNode node : all) {
                if (key.equals(node.key) && node.kind != OculoSettingsNode.PAGE) {
                    return node;
                }
            }
        }

        return null;
    }

    /**
     * One line under the category name: the values of its first simple rows (« Auto · 1.0x · Off »).
     */
    public CharSequence getSummary(Tile tile) {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        String advancedModeKey = name(R.string.oculo_set_advanced_mode);

        for (OculoSettingsNode node : tile.simple) {
            CharSequence value;

            if (node.kind == OculoSettingsNode.SWITCH) {
                // A switch reads as its name when on (« SponsorBlock »), and is left out when off
                value = node.isOn() && !advancedModeKey.equals(node.key) ? node.title : null;
            } else if (node.kind == OculoSettingsNode.MULTI) {
                value = null;
            } else {
                value = node.getValue(mContext);
            }

            if (TextUtils.isEmpty(value)) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" · ");
            }

            sb.append(value);

            if (++count == 3) {
                break;
            }
        }

        if (sb.length() == 0) {
            int rows = 0;
            List<OculoSettingsNode> all = new ArrayList<>();
            OculoSettingsNode.flatten(getRows(tile), all);
            for (OculoSettingsNode node : all) {
                if (node.kind != OculoSettingsNode.PAGE) {
                    rows++;
                }
            }
            return mContext.getString(R.string.oculo_val_settings_count, rows);
        }

        return sb;
    }

    /**
     * Upstream's « on close » callbacks of the screens read (restart notice, search history clearing…). Call once when
     * leaving Oculo's Settings.
     */
    public List<Runnable> getOnFinish() {
        return mOnFinish;
    }

    public void runOnFinish() {
        for (Runnable runnable : mOnFinish) {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "onFinish: " + e);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Reading upstream's screens

    private Runnable presenter(int source) {
        Context c = mContext;

        switch (source) {
            case OculoSettingsMap.GENERAL:
                return () -> GeneralSettingsPresenter.instance(c).show();
            case OculoSettingsMap.MAINUI:
                return () -> MainUISettingsPresenter.instance(c).show();
            case OculoSettingsMap.PLAYER:
                return () -> PlayerSettingsPresenter.instance(c).show();
            case OculoSettingsMap.AFR:
                return () -> AutoFrameRateSettingsPresenter.instance(c).show();
            case OculoSettingsMap.SUBS:
                return () -> SubtitleSettingsPresenter.instance(c).show();
            case OculoSettingsMap.SEARCH:
                return () -> SearchSettingsPresenter.instance(c).show();
            case OculoSettingsMap.SB:
                return () -> SponsorBlockSettingsPresenter.instance(c).show();
            case OculoSettingsMap.DEARROW:
                return () -> DeArrowSettingsPresenter.instance(c).show();
            case OculoSettingsMap.BACKUP:
                return () -> BackupSettingsPresenter.instance(c).show();
            case OculoSettingsMap.ABOUT:
                return Helpers.equalsAny(c.getPackageName(), Utils.KNOWN_PACKAGES)
                        ? () -> AboutSettingsPresenter.instance(c).show() : () -> AboutSimpleSettingsPresenter.instance(c).show();
            case OculoSettingsMap.REMOTE:
                return () -> RemoteControlSettingsPresenter.instance(c).show();
            case OculoSettingsMap.LANG:
                return () -> LanguageSettingsPresenter.instance(c).show();
        }

        return () -> {};
    }

    private void harvest() {
        List<Entry> entries = OculoSettingsMap.entries();

        for (int source = 0; source < OculoSettingsMap.SOURCE_COUNT; source++) {
            OculoSettingsCapture.Dialog dialog = OculoSettingsCapture.run(presenter(source));
            mOnFinish.addAll(dialog.onFinish);

            // Sub-screens opened by a button (Screen dimming, Network, Video speed, Google Drive)
            List<Sel> subSels = new ArrayList<>();
            for (Entry entry : entries) {
                if (entry.source == source && entry.sub != null && !containsSel(subSels, entry.sub)) {
                    subSels.add(entry.sub);
                }
            }

            List<OptionCategory> main = new ArrayList<>(dialog.categories);

            for (Sel sel : subSels) {
                OptionCategory buttonCategory = null;

                for (OptionCategory category : main) {
                    if (category != null && category.type == OptionCategory.TYPE_SINGLE_BUTTON && category.options != null
                            && category.options.size() == 1 && sel.matches(mStr, category.options.get(0).getTitle())) {
                        buttonCategory = category;
                        break;
                    }
                }

                if (buttonCategory == null) {
                    continue;
                }

                main.remove(buttonCategory);
                OptionItem button = buttonCategory.options.get(0);
                OculoSettingsCapture.Dialog subDialog = OculoSettingsCapture.run(() -> button.onSelect(true));
                mOnFinish.addAll(subDialog.onFinish);
                int subIndex = mSubs.size();
                mSubs.add(new Sub(source, sel, button));
                addLeaves(source, subIndex, subDialog.categories);
            }

            addLeaves(source, -1, main);
        }
    }

    private static boolean containsSel(List<Sel> sels, Sel sel) {
        for (Sel s : sels) {
            if (s.same(sel)) {
                return true;
            }
        }
        return false;
    }

    private void addLeaves(int source, int sub, List<OptionCategory> categories) {
        for (OptionCategory category : categories) {
            if (category == null || category.options == null) {
                continue;
            }

            if (category.type == OptionCategory.TYPE_SINGLE_SWITCH || category.type == OptionCategory.TYPE_SINGLE_BUTTON) {
                if (!category.options.isEmpty() && category.options.get(0) != null) {
                    mLeaves.add(new Leaf(source, sub, category, category.options.get(0)));
                }
                continue;
            }

            mLeaves.add(new Leaf(source, sub, category, null));

            if (category.type == OptionCategory.TYPE_CHECKBOX_LIST || category.type == OptionCategory.TYPE_STRING_LIST) {
                for (OptionItem item : category.options) {
                    if (item != null) {
                        mLeaves.add(new Leaf(source, sub, category, item));
                    }
                }
            }
        }
    }

    @Nullable
    private Leaf find(Entry entry) {
        for (Leaf leaf : mLeaves) {
            if (leaf.source != entry.source) {
                continue;
            }

            if (entry.sub == null ? leaf.sub != -1 : (leaf.sub == -1 || !mSubs.get(leaf.sub).sel.same(entry.sub))) {
                continue;
            }

            if (entry.cat != null && entry.item == null) {
                if (leaf.isCategoryLeaf() && entry.cat.matches(mStr, leaf.category.title)) {
                    return leaf;
                }
            } else if (entry.cat != null) {
                if (!leaf.isCategoryLeaf() && !leaf.isSingle() && entry.cat.matches(mStr, leaf.category.title)
                        && entry.item.matches(mStr, leaf.item.getTitle())) {
                    return leaf;
                }
            } else if (entry.item != null) {
                if (leaf.isSingle() && entry.item.matches(mStr, leaf.item.getTitle())) {
                    return leaf;
                }
            }
        }

        return null;
    }

    private void markUsed(Leaf leaf) {
        leaf.used = true;

        if (leaf.isCategoryLeaf()) {
            for (Leaf other : mLeaves) {
                if (other.category == leaf.category) {
                    other.used = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Placing

    private void place() {
        Map<String, OculoSettingsNode> pages = new LinkedHashMap<>();

        for (Entry entry : OculoSettingsMap.entries()) {
            Leaf leaf = find(entry);

            if (leaf == null) {
                continue; // not on this device or not in this context (e.g. web proxy unsupported)
            }

            markUsed(leaf);

            if (!entry.isShown()) {
                continue;
            }

            OculoSettingsNode node = createNode(entry, leaf);

            if (node == null) {
                continue;
            }

            node.tile = entry.tile;
            Tile tile = mTiles.get(entry.tile);

            if (entry.page != 0) {
                String pageKey = entry.tile + "/" + name(entry.page);
                OculoSettingsNode page = pages.get(pageKey);

                if (page == null) {
                    page = new OculoSettingsNode(OculoSettingsNode.PAGE, name(entry.page), str(entry.page), null, null, null,
                            null, true);
                    page.tile = entry.tile;
                    pages.put(pageKey, page);
                    tile.advanced.add(page);
                }

                page.children.add(node);
            } else if (entry.mode == OculoSettingsMap.S) {
                tile.simple.add(node);
            } else {
                tile.advanced.add(node);
            }
        }

        addOculoRows();
        addOtherSettings();
        addClassicSettings();

        for (Tile tile : mTiles) {
            for (OculoSettingsNode node : tile.simple) {
                node.tile = tile.index;
            }
            for (OculoSettingsNode node : tile.advanced) {
                node.tile = tile.index;
            }
        }
    }

    @Nullable
    private OculoSettingsNode createNode(Entry entry, Leaf leaf) {
        CharSequence label = str(entry.label);
        String key = (entry.page != 0 ? name(entry.page) + "/" : "") + name(entry.label);
        boolean advanced = entry.mode != OculoSettingsMap.S;
        CharSequence summary = summary(entry.label);

        if (leaf.item != null) {
            OptionItem source = leaf.item;
            CharSequence description = source.getDescription();

            if (summary == null && description != null) {
                summary = OculoSettingsNode.firstLine(description);
            }

            if (entry.has(OculoSettingsMap.SPEEDS)) {
                return speedsNode(key, label, leaf, advanced);
            }

            boolean isSwitch = leaf.category.type == OptionCategory.TYPE_SINGLE_SWITCH || leaf.category.type == OptionCategory.TYPE_CHECKBOX_LIST;
            OculoOptionItem item = new OculoOptionItem(source, label, null, entry.has(OculoSettingsMap.INVERT));
            if (isSwitch) {
                item.setKey(key);
            }
            return new OculoSettingsNode(isSwitch ? OculoSettingsNode.SWITCH : OculoSettingsNode.ACTION, key, label, summary, item,
                    null, null, advanced);
        }

        OptionCategory category = leaf.category;
        List<OptionItem> options = category.options;

        switch (category.type) {
            case OptionCategory.TYPE_RADIO_LIST:
                if (entry.has(OculoSettingsMap.QUALITY)) {
                    options = qualityChoices(options);
                } else if (entry.values != null) {
                    options = renameValues(options, entry.values);
                }
                return new OculoSettingsNode(OculoSettingsNode.CHOICE, key, label, summary, null,
                        OptionCategory.from(category.id, OptionCategory.TYPE_RADIO_LIST, label, keyed(options, key)), null, advanced);
            case OptionCategory.TYPE_CHECKBOX_LIST:
                return new OculoSettingsNode(OculoSettingsNode.MULTI, key, label, summary, null,
                        OptionCategory.from(category.id, OptionCategory.TYPE_CHECKBOX_LIST, label, keyed(options, key)), null, advanced);
            case OptionCategory.TYPE_STRING_LIST:
                return new OculoSettingsNode(OculoSettingsNode.LIST, key, label, summary, null,
                        OptionCategory.from(category.id, OptionCategory.TYPE_STRING_LIST, label, options), null, advanced);
            case OptionCategory.TYPE_LONG_TEXT:
                return new OculoSettingsNode(OculoSettingsNode.INFO, key, label, summary, null,
                        OptionCategory.from(category.id, OptionCategory.TYPE_LONG_TEXT, label, options), null, advanced);
        }

        return null;
    }

    /** Each value tells « Recent » about a change of this row. */
    private static List<OptionItem> keyed(List<OptionItem> options, String key) {
        List<OptionItem> result = new ArrayList<>(options.size());

        for (OptionItem option : options) {
            if (option instanceof OculoOptionItem) {
                result.add(((OculoOptionItem) option).setKey(key));
            } else {
                result.add(option != null ? new OculoOptionItem(option, null, null, false).setKey(key) : null);
            }
        }

        return result;
    }

    private List<OptionItem> renameValues(List<OptionItem> options, int[][] values) {
        List<OptionItem> result = new ArrayList<>(options.size());

        for (OptionItem option : options) {
            CharSequence title = option != null ? option.getTitle() : null;
            CharSequence renamed = null;

            for (int[] pair : values) {
                if (title != null && TextUtils.equals(title.toString(), str(pair[0]))) {
                    renamed = str(pair[1]);
                    break;
                }
            }

            result.add(renamed != null ? new OculoOptionItem(option, renamed, null, false) : option);
        }

        return result;
    }

    /**
     * « Video quality »: Auto, then one value per resolution, highest first. Each value selects one of upstream's
     * presets for that resolution: SDR before HDR, VP9 before AVC before AV1, then the higher frame rate. The exact
     * preset list stays under Advanced › Video formats.
     */
    private List<OptionItem> qualityChoices(List<OptionItem> presets) {
        List<OptionItem> result = new ArrayList<>();
        OptionItem auto = null;
        Map<Integer, OptionItem> best = new LinkedHashMap<>();
        Map<Integer, Integer> bestScore = new HashMap<>();
        Map<Integer, List<OptionItem>> byHeight = new HashMap<>();

        for (OptionItem preset : presets) {
            if (preset == null || preset.getTitle() == null) {
                continue;
            }

            String title = preset.getTitle().toString();
            Matcher matcher = RESOLUTION.matcher(title);

            if (!matcher.find()) {
                if (auto == null) {
                    auto = preset; // « Disabled » = the player picks the format
                }
                continue;
            }

            int height = Integer.parseInt(matcher.group(1));
            int score = qualityScore(title);
            List<OptionItem> group = byHeight.get(height);

            if (group == null) {
                group = new ArrayList<>();
                byHeight.put(height, group);
            }

            group.add(preset);

            if (!bestScore.containsKey(height) || score > bestScore.get(height)) {
                best.put(height, preset);
                bestScore.put(height, score);
            }
        }

        if (auto != null) {
            OptionItem autoItem = auto;
            result.add(new OculoOptionItem.Choice(str(R.string.oculo_val_auto), autoItem::isSelected, () -> autoItem.onSelect(true)));
        }

        List<Integer> heights = new ArrayList<>(best.keySet());
        java.util.Collections.sort(heights, (a, b) -> b - a);

        for (int height : heights) {
            OptionItem preset = best.get(height);
            List<OptionItem> group = byHeight.get(height);
            result.add(new OculoOptionItem.Choice(height + "p", () -> {
                for (OptionItem item : group) {
                    if (item.isSelected()) {
                        return true;
                    }
                }
                return false;
            }, () -> preset.onSelect(true)));
        }

        return result;
    }

    static int qualityScore(String title) {
        String t = title.toLowerCase();
        int score = 0;

        if (!t.contains("hdr")) {
            score += 1000;
        }

        if (t.contains("vp9")) {
            score += 300;
        } else if (t.contains("avc")) {
            score += 200;
        } else if (t.contains("av01")) {
            score += 100;
        }

        Matcher fps = FPS.matcher(t);
        if (fps.find()) {
            score += Math.min(Integer.parseInt(fps.group(1)), 99);
        }

        return score;
    }

    /** « More speeds »: Short / Long / Extra long instead of the two upstream switches. */
    private OculoSettingsNode speedsNode(String key, CharSequence label, Leaf leaf, boolean advanced) {
        OptionItem longList = leaf.item;
        OptionItem extraLong = null;

        for (OptionItem item : leaf.category.options) {
            if (item != null && item != longList && TextUtils.equals(String.valueOf(item.getTitle()), str(R.string.player_extra_long_speed_list))) {
                extraLong = item;
            }
        }

        OptionItem extra = extraLong;
        List<OptionItem> options = new ArrayList<>();
        options.add(new OculoOptionItem.Choice(str(R.string.oculo_val_speeds_short),
                () -> !longList.isSelected() && (extra == null || !extra.isSelected()),
                () -> {
                    if (longList.isSelected()) longList.onSelect(false);
                    if (extra != null && extra.isSelected()) extra.onSelect(false);
                }));
        options.add(new OculoOptionItem.Choice(str(R.string.oculo_val_speeds_long),
                () -> longList.isSelected() && (extra == null || !extra.isSelected()),
                () -> {
                    if (!longList.isSelected()) longList.onSelect(true);
                    if (extra != null && extra.isSelected()) extra.onSelect(false);
                }));

        if (extra != null) {
            options.add(new OculoOptionItem.Choice(str(R.string.oculo_val_speeds_extra_long),
                    extra::isSelected,
                    () -> {
                        if (longList.isSelected()) longList.onSelect(false);
                        if (!extra.isSelected()) extra.onSelect(true);
                    }));
        }

        return new OculoSettingsNode(OculoSettingsNode.CHOICE, key, label, null, null,
                OptionCategory.from(-1, OptionCategory.TYPE_RADIO_LIST, label, keyed(options, key)), null, advanced);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Rows made by Oculo

    private void addOculoRows() {
        Context c = mContext;

        // Account & History: « Account » first (switch, add, sign out: upstream's account screen)
        OculoOptionItem.Simple accountItem = new OculoOptionItem.Simple(str(R.string.oculo_set_account), null, false,
                selected -> AccountSettingsPresenter.instance(c).show());
        OculoSettingsNode account = new OculoSettingsNode(OculoSettingsNode.ACTION, name(R.string.oculo_set_account),
                str(R.string.oculo_set_account), str(R.string.oculo_sum_account), accountItem, null, null, false);
        account.valueText = context -> {
            String name = selectedAccountName();
            return name != null ? name : context.getString(R.string.oculo_val_not_signed_in);
        };
        mTiles.get(OculoSettingsMap.T_ACCT).simple.add(0, account);

        // Account & History, advanced: after « Separate settings per account »
        Tile acct = mTiles.get(OculoSettingsMap.T_ACCT);
        int at = indexOfKey(acct.advanced, name(R.string.oculo_set_separate_settings_per_account));
        at = at >= 0 ? at + 1 : acct.advanced.size();
        AccountsData accountsData = AccountsData.instance(c);
        acct.advanced.add(at, new OculoSettingsNode(OculoSettingsNode.SWITCH, name(R.string.oculo_set_ask_which_account_at_start),
                str(R.string.oculo_set_ask_which_account_at_start), null,
                new OculoOptionItem.Simple(str(R.string.oculo_set_ask_which_account_at_start), null,
                        accountsData.isSelectAccountOnBootEnabled(), accountsData::selectAccountOnBoot)
                        .setKey(name(R.string.oculo_set_ask_which_account_at_start)), null, null, true));
        acct.advanced.add(at + 1, new OculoSettingsNode(OculoSettingsNode.SWITCH, name(R.string.oculo_set_account_password),
                str(R.string.oculo_set_account_password), null,
                new OculoOptionItem.Simple(str(R.string.oculo_set_account_password), null,
                        accountsData.getAccountPassword() != null, this::onAccountPassword), null, null, true));

        // System: « Advanced mode » and « About Oculo » at the end of the simple rows
        Tile sys = mTiles.get(OculoSettingsMap.T_SYS);
        OculoData data = OculoData.instance(c);
        sys.simple.add(new OculoSettingsNode(OculoSettingsNode.SWITCH, name(R.string.oculo_set_advanced_mode),
                str(R.string.oculo_set_advanced_mode), str(R.string.oculo_set_advanced_mode_desc),
                new OculoOptionItem.Simple(str(R.string.oculo_set_advanced_mode), null, data.isSettingsAdvancedEnabled(), enabled -> {
                    data.setSettingsAdvancedEnabled(enabled);
                    com.liskovsoft.sharedutils.helpers.MessageHelpers.showMessage(c,
                            enabled ? R.string.oculo_set_advanced_shown : R.string.oculo_set_advanced_hidden);
                    OculoSettingsTiles.onAdvancedModeChanged(c);
                }).setKey(name(R.string.oculo_set_advanced_mode)), null, null, false));
        OculoOptionItem.Simple aboutItem = new OculoOptionItem.Simple(str(R.string.oculo_set_about_oculo), null, false,
                selected -> presenter(OculoSettingsMap.ABOUT).run());
        OculoSettingsNode about = new OculoSettingsNode(OculoSettingsNode.ACTION, name(R.string.oculo_set_about_oculo),
                str(R.string.oculo_set_about_oculo), str(R.string.oculo_sum_about_oculo), aboutItem, null, null, false);
        about.valueText = context -> AppInfoHelpers.getAppVersionName(context);
        sys.simple.add(about);
    }

    private void onAccountPassword(boolean enable) {
        Context c = mContext;
        AccountsData accountsData = AccountsData.instance(c);

        if (enable) {
            SimpleEditDialog.showPassword(c, c.getString(R.string.enter_account_password), null, newValue -> {
                accountsData.setAccountPassword(newValue);
                BrowsePresenter.instance(c).updateSections();
                return true;
            });
        } else {
            String password = accountsData.getAccountPassword();

            if (password == null) {
                return;
            }

            SimpleEditDialog.showPassword(c, c.getString(R.string.enter_account_password), null, newValue -> {
                if (Utils.passwordMatch(password, newValue)) {
                    accountsData.setAccountPassword(null);
                    BrowsePresenter.instance(c).updateSections();
                    return true;
                }
                return false;
            });
        }
    }

    @Nullable
    private static String selectedAccountName() {
        try {
            Account account = YouTubeServiceManager.instance().getSignInService().getSelectedAccount();

            if (account == null) {
                return null;
            }

            return account.getName() != null ? account.getName() : account.getEmail();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Upstream settings no entry placed (an upstream addition after a merge): shown as upstream names them. */
    private void addOtherSettings() {
        OculoSettingsNode page = new OculoSettingsNode(OculoSettingsNode.PAGE, name(R.string.oculo_set_other_settings),
                str(R.string.oculo_set_other_settings), str(R.string.oculo_set_other_settings_desc), null, null, null, true);

        for (Leaf leaf : mLeaves) {
            if (leaf.used) {
                continue;
            }

            if (!leaf.isCategoryLeaf() || leaf.isSingle()) {
                OptionItem item = leaf.item;

                if (item == null || TextUtils.isEmpty(item.getTitle())) {
                    continue;
                }

                // Items of a list no entry placed whole: only checkbox items are settings (string list items are buttons)
                boolean isSwitch = leaf.category.type == OptionCategory.TYPE_SINGLE_SWITCH || leaf.category.type == OptionCategory.TYPE_CHECKBOX_LIST;
                page.children.add(new OculoSettingsNode(isSwitch ? OculoSettingsNode.SWITCH : OculoSettingsNode.ACTION,
                        "other/" + item.getTitle(), OculoSettingsNode.firstLine(item.getTitle()), null, item, null, null, true));
                Log.d(TAG, "Not placed: " + item.getTitle());
            } else if (leaf.category.type != OptionCategory.TYPE_CHECKBOX_LIST && leaf.category.type != OptionCategory.TYPE_STRING_LIST
                    && !TextUtils.isEmpty(leaf.category.title)) {
                int kind = leaf.category.type == OptionCategory.TYPE_RADIO_LIST ? OculoSettingsNode.CHOICE : OculoSettingsNode.INFO;
                page.children.add(new OculoSettingsNode(kind, "other/" + leaf.category.title, leaf.category.title, null, null,
                        leaf.category, null, true));
                Log.d(TAG, "Not placed: " + leaf.category.title);
            }
        }

        if (!page.children.isEmpty()) {
            mTiles.get(OculoSettingsMap.T_ADV).advanced.add(page);
        }
    }

    /** Advanced › « Classic settings »: upstream's 13 screens, as a safety net. */
    private void addClassicSettings() {
        OculoSettingsNode page = new OculoSettingsNode(OculoSettingsNode.PAGE, name(R.string.oculo_set_classic_settings),
                str(R.string.oculo_set_classic_settings), str(R.string.oculo_set_classic_settings_desc), null, null, null, true);

        for (SettingsItem settingsItem : AppDataSourceManager.instance().getClassicSettingItems(mContext)) {
            OculoOptionItem.Simple item = new OculoOptionItem.Simple(settingsItem.title, null, false, selected -> settingsItem.onClick.run());
            page.children.add(new OculoSettingsNode(OculoSettingsNode.ACTION, "classic/" + settingsItem.title, settingsItem.title,
                    null, item, null, null, true));
        }

        mTiles.get(OculoSettingsMap.T_ADV).advanced.add(page);
    }

    private static int indexOfKey(List<OculoSettingsNode> nodes, String key) {
        for (int i = 0; i < nodes.size(); i++) {
            if (key.equals(nodes.get(i).key)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------------------------------------------

    private String str(int resId) {
        if (resId == 0) {
            return "";
        }

        String value = mStrings.get(resId);

        if (value == null) {
            value = mContext.getString(resId);
            mStrings.put(resId, value);
        }

        return value;
    }

    private String name(int resId) {
        if (resId == 0) {
            return "";
        }

        try {
            return mContext.getResources().getResourceEntryName(resId);
        } catch (RuntimeException e) {
            return String.valueOf(resId);
        }
    }

    @Nullable
    private CharSequence summary(int labelRes) {
        for (int[] pair : SUMMARIES) {
            if (pair[0] == labelRes) {
                return str(pair[1]);
            }
        }
        return null;
    }
}
