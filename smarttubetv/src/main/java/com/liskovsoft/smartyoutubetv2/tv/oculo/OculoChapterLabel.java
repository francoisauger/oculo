package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoSeekBarChapters;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoSeekBarLayers;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.List;

/**
 * Oculo (0.14.0): the current chapter's name left of the player's time (« Chapter name · 1:23 / 10:00 »), in Roboto
 * grey, whenever the controls are on screen (it lives in the controls row). Fed by the transport row's own updates:
 * the chapters come with the seek bar's chapter layer ({@link OculoSeekBarChapters}), the position with each
 * current-time update (playing or seeking). The chapter is looked up again only when the position leaves the
 * current chapter's range, and the text is set only when the chapter changes: no loop of its own.
 *
 * Flag {@code R.bool.oculo_seek_bar_chapters} (false upstream): no label.
 */
public final class OculoChapterLabel {
    private final TextView mLabel;
    private long[] mStartsMs = new long[0];
    private OculoSeekBarChapters mChapters;
    private int mIndex = -1;
    private long mRangeStartMs = Long.MAX_VALUE;
    private long mRangeEndMs = Long.MIN_VALUE;

    private OculoChapterLabel(TextView label) {
        mLabel = label;
    }

    /**
     * Upstream also writes the chapter under the video's meta line when the controls show (set once, not updated
     * while playing); with this label on, that line is left out (MaxControlsVideoPlayerGlue.setSeekPreviewTitle).
     */
    public static boolean isEnabled(android.content.Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_seek_bar_chapters);
    }

    /**
     * From the transport row's ViewHolder: adds the label to the time block, left of « current / total ».
     *
     * @return null when the flag is off or the layout is not the expected one
     */
    public static OculoChapterLabel attach(ViewGroup timeInfo, TextView currentTime) {
        if (!(timeInfo instanceof RelativeLayout) || currentTime == null
                || !timeInfo.getResources().getBoolean(R.bool.oculo_seek_bar_chapters)) {
            return null;
        }

        // Inside « current / total » (a RelativeLayout where current_time sits left of the separator), left of
        // current_time: the same layout rule as upstream's own time views
        View wrapper = timeInfo.findViewById(R.id.current_time_wrapper);
        if (!(wrapper instanceof RelativeLayout) || currentTime.getId() == View.NO_ID) {
            return null;
        }

        Resources res = timeInfo.getResources();
        TextView label = new TextView(timeInfo.getContext());
        label.setId(View.generateViewId());
        label.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTime.getTextSize());
        label.setTextColor(ContextCompat.getColor(timeInfo.getContext(), R.color.oculo_chapter_label));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setMaxWidth(res.getDimensionPixelSize(R.dimen.oculo_chapter_label_max_width));
        label.setGravity(Gravity.END);
        label.setIncludeFontPadding(currentTime.getIncludeFontPadding());
        label.setFocusable(false);
        label.setVisibility(View.GONE);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.START_OF, currentTime.getId());
        lp.addRule(RelativeLayout.ALIGN_BASELINE, currentTime.getId());
        ((RelativeLayout) wrapper).addView(label, lp);

        return new OculoChapterLabel(label);
    }

    /** With every {@code setSeekBarSegments()}: null = new video (no chapters), an OculoSeekBarChapters = chapters. */
    public void onSegments(List<SeekBarSegment> segments) {
        if (segments != null && !(segments instanceof OculoSeekBarChapters)) {
            return; // SponsorBlock layer: chapters unchanged
        }

        mChapters = (OculoSeekBarChapters) segments;
        mStartsMs = mChapters != null ? mChapters.getStartsMs() : new long[0];
        mIndex = -1;
        mRangeStartMs = Long.MAX_VALUE;
        mRangeEndMs = Long.MIN_VALUE;

        if (mStartsMs.length == 0) {
            mLabel.setText(null);
            mLabel.setVisibility(View.GONE);
        }
    }

    /** With every current-time update of the transport row (playback or seeking). */
    public void onPosition(long positionMs) {
        if (mStartsMs.length == 0) {
            return;
        }

        if (positionMs >= mRangeStartMs && positionMs < mRangeEndMs) {
            return; // same chapter
        }

        int index = OculoSeekBarLayers.chapterIndexAt(mStartsMs, positionMs);
        mRangeStartMs = index >= 0 ? mStartsMs[index] : Long.MIN_VALUE;
        mRangeEndMs = index >= 0 ? OculoSeekBarLayers.chapterEndMs(mStartsMs, index) : mStartsMs[0];

        if (index == mIndex) {
            return;
        }
        mIndex = index;

        String title = mChapters != null ? mChapters.getTitle(index) : null;
        if (TextUtils.isEmpty(title)) {
            mLabel.setText(null);
            mLabel.setVisibility(View.GONE);
        } else {
            mLabel.setText(mLabel.getResources().getString(R.string.oculo_chapter_label, title.trim()));
            mLabel.setVisibility(View.VISIBLE);
        }
    }
}
