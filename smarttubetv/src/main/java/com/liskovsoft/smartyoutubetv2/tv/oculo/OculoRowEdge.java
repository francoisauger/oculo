package com.liskovsoft.smartyoutubetv2.tv.oculo;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.HorizontalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Oculo: cards that scroll out of a row on the left are cut at the content's start line, with a short soft fade,
 * as on the Google TV home screen (32.74-15). Until 32.74-14 they stayed visible up to the browse content's clip
 * line ({@code oculo_rail_content_clip_start}, just right of the rail's icons, about 30dp left of where the first
 * card sits).
 *
 * <p>An {@link RecyclerView.ItemDecoration} on each row's {@link HorizontalGridView}, active only while the row is
 * scrolled (a card starts left of the row's start padding, which is the content's start line). Leanback rows keep
 * the focused card centered as soon as they scroll ({@code WINDOW_ALIGN_BOTH_EDGE}, offset 50 %), so the focused
 * card, its ring and its glow are never near this edge while it is active; when the row is not scrolled nothing is
 * cut, so the first card's ring and glow keep the room up to the content clip.</p>
 *
 * <ul>
 *     <li>Hard cut: {@code onDraw()} (before the children) clips the canvas at
 *     {@code start - oculo_row_edge_fade_outside}; {@code onDrawOver()} restores it. No layer, no allocation.</li>
 *     <li>Fade: from that line to {@code start + oculo_row_edge_fade_inside}, the page background is painted over
 *     the cards with its alpha going from 100 % to 0 % (a band-sized layer). The background is the one really behind
 *     the row: Home's {@link OculoGlow} view, else the first ancestor background (search and channel screens: the
 *     window background). No background found: hard cut at the start line, no fade.</li>
 * </ul>
 * Flag {@code oculo_row_edge_clip} (false upstream, true in the flavor). Installed from {@link OculoUi} when a card
 * is set up (every card with the focus ring), once per grid.
 */
final class OculoRowEdge extends RecyclerView.ItemDecoration {
    private final int mFadeOutside;
    private final int mFadeInside;
    private final Paint mFadePaint = new Paint();
    private final int[] mGridLocation = new int[2];
    private final int[] mSourceLocation = new int[2];
    private int mShaderLeft = Integer.MIN_VALUE;
    private int mShaderRight = Integer.MIN_VALUE;
    private int mSave = -1;
    private boolean mActive;
    @Nullable
    private View mSourceView;
    @Nullable
    private Drawable mSourceDrawable;

    private OculoRowEdge(Resources res) {
        mFadeOutside = res.getDimensionPixelSize(R.dimen.oculo_row_edge_fade_outside);
        mFadeInside = res.getDimensionPixelSize(R.dimen.oculo_row_edge_fade_inside);
        mFadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mFadePaint.setDither(true);
    }

    static void install(View grid) {
        if (!(grid instanceof HorizontalGridView)
                || !grid.getResources().getBoolean(R.bool.oculo_row_edge_clip)
                || grid.getTag(R.id.oculo_row_edge) != null) {
            return;
        }

        OculoRowEdge edge = new OculoRowEdge(grid.getResources());
        ((HorizontalGridView) grid).addItemDecoration(edge);
        grid.setTag(R.id.oculo_row_edge, edge);
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        mSave = -1;
        mActive = isScrolled(parent);
        if (!mActive) {
            return;
        }

        findBackground(parent);
        int start = parent.getPaddingLeft();
        int clip = hasBackground() ? start - mFadeOutside : start;
        mSave = c.save();
        // Only the left side: the cards' ring and glow may still draw above, below and right of the grid
        c.clipRect(clip, -parent.getHeight(), Integer.MAX_VALUE / 4, parent.getHeight() * 2);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (mSave >= 0) {
            c.restoreToCount(mSave);
            mSave = -1;
        }

        if (!mActive || !hasBackground()) {
            clearBackground();
            return;
        }

        int start = parent.getPaddingLeft();
        int left = start - mFadeOutside;
        int right = start + mFadeInside;
        int height = parent.getHeight();
        if (right <= left || height <= 0) {
            clearBackground();
            return;
        }

        if (left != mShaderLeft || right != mShaderRight) {
            // DST_IN keeps the background painted below with this alpha: opaque at the cut, clear inside the content
            mFadePaint.setShader(new LinearGradient(left, 0, right, 0, Color.BLACK, Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
            mShaderLeft = left;
            mShaderRight = right;
        }

        int layer = c.saveLayer(left, 0, right, height, null);
        drawBackground(c, parent);
        c.drawRect(left, 0, right, height, mFadePaint);
        c.restoreToCount(layer);
        clearBackground();
    }

    /**
     * Scrolled = a card starts left of the start line (at rest, the first card starts exactly on it).
     */
    private static boolean isScrolled(RecyclerView grid) {
        int start = grid.getPaddingLeft() - 1;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child.getLeft() < start) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBackground() {
        return mSourceView != null || mSourceDrawable != null;
    }

    private void clearBackground() {
        mSourceView = null;
        mSourceDrawable = null;
    }

    private void findBackground(RecyclerView grid) {
        clearBackground();

        View glow = OculoGlow.backgroundView();
        if (glow != null && glow.isAttachedToWindow() && glow.isShown() && glow.getRootView() == grid.getRootView()) {
            mSourceView = glow;
            return;
        }

        for (ViewParent parent = grid.getParent(); parent instanceof View; parent = parent.getParent()) {
            View view = (View) parent;
            Drawable background = view.getBackground();
            if (background != null && !(background instanceof ColorDrawable && ((ColorDrawable) background).getAlpha() == 0)) {
                mSourceView = view;
                mSourceDrawable = background;
                return;
            }
        }
    }

    /**
     * Paints the background behind the grid in the grid's coordinates (translation and scale of the content, e.g.
     * while the rail expands).
     */
    private void drawBackground(Canvas c, RecyclerView grid) {
        View owner = mSourceView;
        if (owner == null) {
            return;
        }

        grid.getLocationInWindow(mGridLocation);
        owner.getLocationInWindow(mSourceLocation);
        float scale = windowScale(grid) / windowScale(owner);
        if (scale <= 0) {
            return;
        }

        int save = c.save();
        c.scale(1 / scale, 1 / scale);
        c.translate(mSourceLocation[0] - mGridLocation[0], mSourceLocation[1] - mGridLocation[1]);
        if (mSourceDrawable != null) {
            mSourceDrawable.draw(c);
        } else {
            OculoGlow.drawBackground(owner, c);
        }
        c.restoreToCount(save);
    }

    private static float windowScale(View view) {
        float scale = 1;
        for (View current = view; current != null;
             current = current.getParent() instanceof View ? (View) current.getParent() : null) {
            scale *= current.getScaleX();
        }
        return scale;
    }
}
