/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.touch;

import android.os.Bundle;
import android.view.ViewGroup;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class EdgeRejectionSettingsActivity extends CollapsingToolbarBaseActivity {

    private EdgeDeadzonePreviewView mDeadzonePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The window is edge to edge, so the content frame reaches the physical screen edges.
        // Not a child of the decor view: the edge to edge helper inserts a view there while
        // the decor view attaches its children, which leaves the last one unattached.
        mDeadzonePreview = new EdgeDeadzonePreviewView(this);
        addContentView(mDeadzonePreview,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        getFragmentManager().beginTransaction().replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                new EdgeRejectionSettingsFragment(), "edge_rejection").commit();
    }

    /**
     * @param width dead zone width in panel pixels, 0 hides the preview.
     */
    public void setDeadzonePreview(int width) {
        if (mDeadzonePreview != null) {
            mDeadzonePreview.setDeadzoneWidth(width);
        }
    }
}
