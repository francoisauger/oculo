package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.CommentsReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;

/**
 * Oculo: an upstream {@link OptionItem} under an Oculo label. Selection and callbacks go to the upstream item, so the
 * same preference is written; {@code inverted} shows a « Disable X » switch as « X » (on = upstream off).
 * Without a delegate it is a synthetic item ({@link Choice}).
 */
public class OculoOptionItem implements OptionItem {
    private final OptionItem mDelegate;
    private final CharSequence mTitle;
    private final CharSequence mDescription;
    private final boolean mInverted;

    public OculoOptionItem(OptionItem delegate, @Nullable CharSequence title, @Nullable CharSequence description, boolean inverted) {
        mDelegate = delegate;
        mTitle = title;
        mDescription = description;
        mInverted = inverted;
    }

    public OptionItem getDelegate() {
        return mDelegate;
    }

    @Override
    public int getId() {
        return mDelegate != null ? mDelegate.getId() : 0;
    }

    @Override
    public CharSequence getTitle() {
        return mTitle != null ? mTitle : (mDelegate != null ? mDelegate.getTitle() : null);
    }

    @Override
    public CharSequence getDescription() {
        return mDescription != null ? mDescription : (mDelegate != null ? mDelegate.getDescription() : null);
    }

    @Override
    public boolean isSelected() {
        return mDelegate != null && (mInverted != mDelegate.isSelected());
    }

    @Override
    public void onSelect(boolean isSelected) {
        if (mDelegate != null) {
            mDelegate.onSelect(mInverted != isSelected);
        }
    }

    @Override
    public Object getData() {
        return mDelegate != null ? mDelegate.getData() : null;
    }

    @Override
    public void setRequired(OptionItem... items) {
        if (mDelegate != null) {
            mDelegate.setRequired(items);
        }
    }

    @Override
    public OptionItem[] getRequired() {
        return mDelegate != null ? mDelegate.getRequired() : null;
    }

    @Override
    public void setRadio(OptionItem... items) {
        if (mDelegate != null) {
            mDelegate.setRadio(items);
        }
    }

    @Override
    public OptionItem[] getRadio() {
        return mDelegate != null ? mDelegate.getRadio() : null;
    }

    @Override
    public ChatReceiver getChatReceiver() {
        return mDelegate != null ? mDelegate.getChatReceiver() : null;
    }

    @Override
    public CommentsReceiver getCommentsReceiver() {
        return mDelegate != null ? mDelegate.getCommentsReceiver() : null;
    }

    /** A value of an Oculo-made choice (« Video quality » resolutions, « More speeds »). */
    public static class Choice extends OculoOptionItem {
        public interface Selected {
            boolean get();
        }

        private final Selected mSelected;
        private final Runnable mOnSelect;

        public Choice(CharSequence title, Selected selected, Runnable onSelect) {
            super(null, title, null, false);
            mSelected = selected;
            mOnSelect = onSelect;
        }

        @Override
        public boolean isSelected() {
            return mSelected != null && mSelected.get();
        }

        @Override
        public void onSelect(boolean isSelected) {
            if (isSelected && mOnSelect != null) {
                mOnSelect.run();
            }
        }
    }

    /** An Oculo-made switch or button with its own state and action. */
    public static class Simple extends OculoOptionItem {
        public interface OnSelect {
            void onSelect(boolean isSelected);
        }

        private boolean mIsSelected;
        private final OnSelect mOnSelect;

        public Simple(CharSequence title, @Nullable CharSequence description, boolean isSelected, @Nullable OnSelect onSelect) {
            super(null, title, description, false);
            mIsSelected = isSelected;
            mOnSelect = onSelect;
        }

        @Override
        public boolean isSelected() {
            return mIsSelected;
        }

        @Override
        public void onSelect(boolean isSelected) {
            mIsSelected = isSelected;

            if (mOnSelect != null) {
                mOnSelect.onSelect(isSelected);
            }
        }
    }
}
