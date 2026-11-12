/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.touch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Draws the edge dead zone as red strips along the long screen edges, at the size the
 * touch panel ignores them.
 */
public class EdgeDeadzonePreviewView extends View {

    private static final int STRIP_COLOR = 0x99FF0000;

    private final Paint mPaint = new Paint();
    private int mDeadzoneWidth;

    public EdgeDeadzonePreviewView(Context context) {
        super(context);
        mPaint.setColor(STRIP_COLOR);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /**
     * @param width dead zone width in panel pixels, 0 hides the strips.
     */
    public void setDeadzoneWidth(int width) {
        if (mDeadzoneWidth != width) {
            mDeadzoneWidth = width;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final Display display = getDisplay();
        if (mDeadzoneWidth <= 0 || display == null) {
            return;
        }

        final Rect bounds = getContext().getSystemService(WindowManager.class)
                .getMaximumWindowMetrics().getBounds();
        final boolean portrait = bounds.width() <= bounds.height();
        final int shortEdge = Math.min(bounds.width(), bounds.height());
        final int panelShortEdge = getPanelShortEdge(display);
        final int width = getWidth();
        final int height = getHeight();

        // The strips only line up with the panel edges while the window spans the short
        // edge of the screen, which a freeform window does not.
        if (panelShortEdge <= 0 || (portrait ? width : height) != shortEdge) {
            return;
        }

        // The dead zone is in native panel pixels, the screen may render at a lower resolution.
        final float strip = mDeadzoneWidth * (float) shortEdge / panelShortEdge;
        if (portrait) {
            canvas.drawRect(0, 0, strip, height, mPaint);
            canvas.drawRect(width - strip, 0, width, height, mPaint);
        } else {
            canvas.drawRect(0, 0, width, strip, mPaint);
            canvas.drawRect(0, height - strip, width, height, mPaint);
        }
    }

    private static int getPanelShortEdge(Display display) {
        int shortEdge = 0;
        for (Display.Mode mode : display.getSupportedModes()) {
            shortEdge = Math.max(shortEdge,
                    Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight()));
        }
        return shortEdge;
    }
}
