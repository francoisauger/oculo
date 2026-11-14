package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.leanback.widget.HorizontalGridView;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Oculo: cards that scroll out of a row on the left are cut at the content's start line, so they are never drawn
 * next to the rail's icons (32.74-15, reworked in 32.74-16). Until 32.74-14 they stayed visible up to the browse
 * content's clip line ({@code oculo_rail_content_clip_start}, just right of the rail's icons).
 *
 * <p>32.74-16: a hard cut through the row grid's own clip bounds ({@link View#setClipBounds}), set only while the
 * row is scrolled (a card starts left of the row's start padding, which is the content's start line) and removed
 * as soon as it is back at rest. The clip is a property of the grid's render node: nothing is drawn by Oculo, no
 * canvas clip or layer, no card view is touched, and nothing is added to the RecyclerView itself. The state is
 * checked right before each frame (a pre-draw listener, only while the grid is attached), so it follows scrolls,
 * layouts and recycling without any RecyclerView callback.</p>
 *
 * <p>32.74-15 used an {@code ItemDecoration} (canvas clip + background repainted through a layer for a fade),
 * added from the card's setup. {@code RecyclerView.addItemDecoration()} throws while the grid is in a layout or
 * scroll, which is exactly when cards are attached: the exception left cards half attached (thumbnails not loaded,
 * cards misplaced or doubled, focus ring without its rounded corners) and could crash the app on a key press.</p>
 *
 * <p>Leanback rows keep the focused card centered as soon as they scroll ({@code WINDOW_ALIGN_BOTH_EDGE}, 50 %),
 * so the focused card, its ring and its glow are never near this edge while the cut is active; at rest nothing is
 * cut, so the first card's ring and glow keep their room up to the content clip. Above, below and right of the
 * grid the clip is far outside (rings and glows extend beyond the grid's bounds).</p>
 *
 * Flag {@code oculo_row_edge_clip} (false upstream, true in the flavor). Installed from {@link OculoUi} when a card
 * is set up, once per grid; never throws.
 */
final class OculoRowEdge implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
    private final HorizontalGridView mGrid;
    private final Rect mClip = new Rect();
    private boolean mClipped;
    private boolean mListening;
    private ViewTreeObserver mObserver;

    private OculoRowEdge(HorizontalGridView grid) {
        mGrid = grid;
    }

    static void install(View grid) {
        if (!(grid instanceof HorizontalGridView)
                || !grid.getResources().getBoolean(R.bool.oculo_row_edge_clip)
                || grid.getTag(R.id.oculo_row_edge) != null) {
            return;
        }

        OculoRowEdge edge = new OculoRowEdge((HorizontalGridView) grid);
        grid.setTag(R.id.oculo_row_edge, edge);
        grid.addOnAttachStateChangeListener(edge);
        if (grid.isAttachedToWindow()) {
            edge.startListening();
        }
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        startListening();
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        stopListening();
    }

    private void startListening() {
        if (mListening) {
            return;
        }
        mObserver = mGrid.getViewTreeObserver();
        mObserver.addOnPreDrawListener(this);
        mListening = true;
    }

    private void stopListening() {
        if (!mListening) {
            return;
        }
        if (mObserver != null && mObserver.isAlive()) {
            mObserver.removeOnPreDrawListener(this);
        } else {
            mGrid.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        mObserver = null;
        mListening = false;
    }

    @Override
    public boolean onPreDraw() {
        update();
        return true; // never cancels the frame
    }

    private void update() {
        int start = mGrid.getPaddingLeft();
        int width = mGrid.getWidth();
        int height = mGrid.getHeight();
        boolean scrolled = width > 0 && isScrolled(start);

        if (!scrolled) {
            if (mClipped) {
                mGrid.setClipBounds(null);
                mClipped = false;
            }
            return;
        }

        // Only the left side: the cards' ring and glow may still draw above, below and right of the grid
        int left = start;
        int top = -height - height / 2;
        int right = width * 2 + start;
        int bottom = height * 3;
        if (!mClipped || mClip.left != left || mClip.top != top || mClip.right != right || mClip.bottom != bottom) {
            mClip.set(left, top, right, bottom);
            mGrid.setClipBounds(mClip);
            mClipped = true;
        }
    }

    /**
     * Scrolled = a card starts left of the start line (at rest, the first card starts exactly on it).
     */
    private boolean isScrolled(int start) {
        int line = start - 1;
        for (int i = 0; i < mGrid.getChildCount(); i++) {
            View child = mGrid.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child.getLeft() + child.getTranslationX() < line) {
                return true;
            }
        }
        return false;
    }
}
