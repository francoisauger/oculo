package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import android.content.Context;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo: one row of Oculo's Settings. It holds upstream's own items (or Oculo wrappers around them), so selecting a
 * value runs upstream's callback.
 */
public final class OculoSettingsNode {
    /** On/off: {@link #item}. */
    public static final int SWITCH = 0;
    /** One value among several: {@link #category} (radio list). */
    public static final int CHOICE = 1;
    /** Several values on/off: {@link #category} (checkbox list). */
    public static final int MULTI = 2;
    /** A button: {@link #item}, selecting it runs the action (often opens an upstream screen). */
    public static final int ACTION = 3;
    /** A group of rows: {@link #children}. */
    public static final int PAGE = 4;
    /** Text only: {@link #category} (long text). */
    public static final int INFO = 5;
    /** A list of buttons: {@link #category} (string list: segments, changelog, feedback…). */
    public static final int LIST = 6;

    public interface ValueText {
        @Nullable CharSequence get(Context context);
    }

    public final int kind;
    /** Stable id (label resource name), used by « Recent ». */
    public final String key;
    public final CharSequence title;
    @Nullable public final CharSequence summary;
    @Nullable public final OptionItem item;
    @Nullable public final OptionCategory category;
    public final List<OculoSettingsNode> children;
    /** Shown only in advanced mode. */
    public final boolean advanced;
    @Nullable ValueText valueText;
    /** Index of the tile holding this row (OculoSettingsMap.T_*). */
    public int tile = -1;

    OculoSettingsNode(int kind, String key, CharSequence title, @Nullable CharSequence summary, @Nullable OptionItem item,
                      @Nullable OptionCategory category, @Nullable List<OculoSettingsNode> children, boolean advanced) {
        this.kind = kind;
        this.key = key;
        this.title = title;
        this.summary = summary;
        this.item = item;
        this.category = category;
        this.children = children != null ? children : new ArrayList<>();
        this.advanced = advanced;
    }

    public boolean isOn() {
        return item != null && item.isSelected();
    }

    /** The value shown at the right of the row, or null (buttons, pages). */
    @Nullable
    public CharSequence getValue(Context context) {
        if (valueText != null) {
            return valueText.get(context);
        }

        switch (kind) {
            case SWITCH:
                return context.getString(isOn() ? R.string.oculo_val_on : R.string.oculo_val_off);
            case CHOICE:
                OptionItem selected = getSelectedOption();
                return selected != null ? firstLine(selected.getTitle()) : null;
            case MULTI:
                if (category == null || category.options == null) {
                    return null;
                }
                int on = 0;
                for (OptionItem option : category.options) {
                    if (option != null && option.isSelected()) {
                        on++;
                    }
                }
                return context.getString(R.string.oculo_val_count, on, category.options.size());
        }

        return null;
    }

    @Nullable
    public OptionItem getSelectedOption() {
        if (category == null || category.options == null) {
            return null;
        }

        for (OptionItem option : category.options) {
            if (option != null && option.isSelected()) {
                return option;
            }
        }

        return null;
    }

    @Nullable
    public static CharSequence firstLine(@Nullable CharSequence text) {
        if (text == null) {
            return null;
        }

        String s = text.toString();
        int nl = s.indexOf('\n');
        // Upstream pads some values with spaces to align them in a monospace-less list (video presets)
        return (nl >= 0 ? s.substring(0, nl) : s).replaceAll("\\s{2,}", " ").trim();
    }

    /** Every row, pages expanded (depth first). */
    public static void flatten(List<OculoSettingsNode> nodes, List<OculoSettingsNode> out) {
        for (OculoSettingsNode node : nodes) {
            out.add(node);

            if (node.kind == PAGE) {
                flatten(node.children, out);
            }
        }
    }
}
