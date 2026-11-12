/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.touch;

import android.util.Log;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

public class TouchUtils {

    private static final String TAG = "XiaomiTouchUtils";
    private static final boolean DEBUG = true;

    /**
     * Toggles the grip rejection feature by writing to its sysfs node.
     * @param enabled true to enable grip rejection, false to disable.
     */
    public static void setEdgeRejectionEnabled(boolean enabled) {
        String value = enabled ? "1" : "0";
        if (DEBUG) Log.d(TAG, "Setting edge rejection to: " + value);

        if (FileUtils.fileExists(Constants.NODE_GRIP_REJECTION)) {
            FileUtils.writeLine(Constants.NODE_GRIP_REJECTION, value);
        } else {
            Log.e(TAG, "Sysfs node for grip rejection not found at: " + Constants.NODE_GRIP_REJECTION);
        }
    }

    /**
     * Sets the width of the strip along the long screen edges where touches are ignored.
     * @param width strip width in pixels, 0 keeps the kernel default.
     */
    public static void setEdgeDeadzoneWidth(int width) {
        if (DEBUG) Log.d(TAG, "Setting edge dead zone width to: " + width);

        if (FileUtils.fileExists(Constants.NODE_EDGE_DEADZONE)) {
            FileUtils.writeLine(Constants.NODE_EDGE_DEADZONE, width);
        } else {
            Log.e(TAG, "Sysfs node for edge dead zone not found at: " + Constants.NODE_EDGE_DEADZONE);
        }
    }
}