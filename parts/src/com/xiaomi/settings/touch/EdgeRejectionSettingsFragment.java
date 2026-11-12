/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.touch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import com.android.settingslib.widget.MainSwitchPreference;

import org.lineageos.settings.Constants;
import org.lineageos.settings.CustomSeekBarPreference;
import org.lineageos.settings.R;

public class EdgeRejectionSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private MainSwitchPreference mEdgeRejectionSwitch;
    private CustomSeekBarPreference mDeadzonePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.edge_rejection_settings);

        mEdgeRejectionSwitch = findPreference(Constants.KEY_EDGE_REJECTION);
        mEdgeRejectionSwitch.setOnPreferenceChangeListener(this);

        mDeadzonePreference = findPreference(Constants.KEY_EDGE_DEADZONE);
        mDeadzonePreference.setOnPreferenceChangeListener(this);
        mDeadzonePreference.setEnabled(mEdgeRejectionSwitch.isChecked());
        // Follow the finger while the seek bar is dragged, the node is only written on release.
        mDeadzonePreference.setOnTrackingValueChangeListener(
                value -> updatePreview(mEdgeRejectionSwitch.isChecked(), value));
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreview(mEdgeRejectionSwitch.isChecked(), mDeadzonePreference.getValue());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Constants.KEY_EDGE_REJECTION.equals(preference.getKey())) {
            boolean isEnabled = (Boolean) newValue;
            TouchUtils.setEdgeRejectionEnabled(isEnabled);
            mDeadzonePreference.setEnabled(isEnabled);
            updatePreview(isEnabled, mDeadzonePreference.getValue());

            Intent serviceIntent = new Intent(getContext(), TouchOrientationService.class);
            if (isEnabled) {
                getContext().startService(serviceIntent);
            } else {
                getContext().stopService(serviceIntent);
            }

            return true;
        } else if (Constants.KEY_EDGE_DEADZONE.equals(preference.getKey())) {
            TouchUtils.setEdgeDeadzoneWidth((Integer) newValue);
            updatePreview(mEdgeRejectionSwitch.isChecked(), (Integer) newValue);
            return true;
        }
        return false;
    }

    /** The dead zone is only active while edge rejection is on. */
    private void updatePreview(boolean edgeRejectionEnabled, int deadzoneWidth) {
        final Activity activity = getActivity();
        if (activity instanceof EdgeRejectionSettingsActivity) {
            ((EdgeRejectionSettingsActivity) activity)
                    .setDeadzonePreview(edgeRejectionEnabled ? deadzoneWidth : 0);
        }
    }
}
