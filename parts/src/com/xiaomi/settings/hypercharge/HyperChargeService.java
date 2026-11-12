/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.hypercharge;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

public class HyperChargeService extends Service {
    private static final String TAG = "HyperChargeService";

    private static final int POLLING_INTERVAL_MS = 5000;
    private static final long CURRENT_UNKNOWN = -1;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences mPrefs;
    private boolean mMonitoring;

    // The value this service last wrote to the node, or CURRENT_UNKNOWN if it holds no limit.
    private long mAppliedLimit = CURRENT_UNKNOWN;

    private final Runnable mMonitoringRunnable = new Runnable() {
        @Override
        public void run() {
            applyLimit();
            mHandler.postDelayed(this, POLLING_INTERVAL_MS);
        }
    };

    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                Log.i(TAG, "Charger connected — starting monitoring.");
                startMonitoring();
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                Log.i(TAG, "Charger disconnected — stopping monitoring.");
                stopMonitoring();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerReceiver, filter);

        if (isDevicePluggedIn()) {
            startMonitoring();
        }
    }

    private void startMonitoring() {
        // The charger only reports fast charging once it passed authentication, which takes
        // a few seconds after plug in, so keep polling for as long as it is connected.
        stopMonitoring();
        mMonitoring = true;
        mHandler.post(mMonitoringRunnable);
    }

    private void stopMonitoring() {
        mMonitoring = false;
        mHandler.removeCallbacks(mMonitoringRunnable);
    }

    /**
     * mi_thermald lowers the same node as the device warms up, so the limit is only ever
     * applied downwards: a value below ours is the thermal limit and has to stay.
     */
    private void applyLimit() {
        if (!"1".equals(FileUtils.readOneLine(Constants.NODE_FASTCHG_MODE))) {
            return;
        }

        final long target = parseCurrent(getTargetLimit());
        final long current = parseCurrent(
                FileUtils.readOneLine(Constants.NODE_CONSTANT_CHARGE_CURRENT));
        if (target <= 0) {
            return;
        }

        final boolean aboveTarget = current <= 0 || current > target;
        // The user raised the limit and the node still holds our previous, lower one.
        final boolean holdsOldLimit = mAppliedLimit != CURRENT_UNKNOWN
                && current == mAppliedLimit && current < target;
        if (aboveTarget || holdsOldLimit) {
            Log.i(TAG, "Applying charge limit: " + current + " -> " + target);
            if (FileUtils.writeLine(Constants.NODE_CONSTANT_CHARGE_CURRENT,
                    String.valueOf(target))) {
                mAppliedLimit = target;
            }
        }
    }

    /**
     * Gives the node back once no limit is wanted anymore. Nothing else raises it again
     * while the device is cool, so our limit would otherwise stay until the next reboot.
     */
    private void releaseLimit() {
        if (mAppliedLimit == CURRENT_UNKNOWN) {
            return;
        }

        final long current = parseCurrent(
                FileUtils.readOneLine(Constants.NODE_CONSTANT_CHARGE_CURRENT));
        if (current <= 0 || current == mAppliedLimit) {
            Log.i(TAG, "Releasing charge limit: " + current + " -> "
                    + Constants.CHARGE_LIMIT_RELEASE);
            FileUtils.writeLine(Constants.NODE_CONSTANT_CHARGE_CURRENT,
                    Constants.CHARGE_LIMIT_RELEASE);
        }
        mAppliedLimit = CURRENT_UNKNOWN;
    }

    private String getTargetLimit() {
        boolean masterEnabled = mPrefs.getBoolean(Constants.KEY_HYPERCHARGE_STATUS, true);
        if (!masterEnabled) {
            return Constants.CHARGE_LIMIT_33W;
        }
        return mPrefs.getString(Constants.KEY_HYPERCHARGE_LIMIT, Constants.CHARGE_LIMIT_MAX);
    }

    private static long parseCurrent(String value) {
        if (value == null) {
            return CURRENT_UNKNOWN;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return CURRENT_UNKNOWN;
        }
    }

    private boolean isDevicePluggedIn() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus == null) return false;

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return status == BatteryManager.BATTERY_PLUGGED_AC ||
               status == BatteryManager.BATTERY_PLUGGED_USB ||
               status == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Started again whenever the settings change: apply the new limit right away.
        if (mMonitoring) {
            startMonitoring();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed — Cleaning up.");
        unregisterReceiver(powerReceiver);
        stopMonitoring();
        releaseLimit();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
