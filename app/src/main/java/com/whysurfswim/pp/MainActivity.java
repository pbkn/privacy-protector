package com.whysurfswim.pp;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class MainActivity extends AppCompatPreferenceActivity {

    private static final int REQUEST_ENABLE = 1;
    private static final boolean TRUE = true;
    private static DevicePolicyManager devicePolicyManager;
    private static AudioManager audioManager;
    private static ComponentName componentName;
    private static boolean volButtonsState = true;

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * To check whether the app has administrator access or not
     *
     * @return boolean
     */
    private static boolean isNotActiveAdmin() {
        return !devicePolicyManager.isAdminActive(componentName);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        recreate();
    }

    /**
     * checkAdminAccess is used to check whether the app now has admins access or not
     */
    private void checkAdminAccess() {
        if (isNotActiveAdmin()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(getResources().getString(R.string.pref_camera_key), true);
            editor.apply();
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.device_admin_description);
            startActivityForResult(intent, REQUEST_ENABLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        audioManager = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        componentName = new ComponentName(this, AppAdminReceiver.class);
        checkAdminAccess();
        setupActionBar();
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAdminAccess();
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkAdminAccess();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (volButtonsState && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return TRUE;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class MainPreferenceFragment extends PreferenceFragment {

        Preference cameraPreference;
        Preference micPreference;
        Preference volButtonsPreference;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_headers);

            cameraPreference = findPreference(getResources().getString(R.string.pref_camera_key));
            micPreference = findPreference(getResources().getString(R.string.pref_mic_key));
            volButtonsPreference = findPreference(getResources().getString(R.string.pref_vol_buttons_key));

            // feedback preferences click listener
            cameraPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    doCamera(preference);
                    return TRUE;
                }
            });
            micPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    doMic(preference);
                    return TRUE;
                }
            });
            volButtonsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    doVolButtons(preference);
                    return TRUE;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        /**
         * Disables or Enables camera functionality in phone
         */
        public void doCamera(Preference preference) {
            boolean toggleValue = preference.getSharedPreferences().getBoolean(getResources().getString(R.string.pref_camera_key), TRUE);
            if (isNotActiveAdmin()) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.device_admin_description);
                startActivityForResult(intent, REQUEST_ENABLE);
            }
            devicePolicyManager.setCameraDisabled(componentName, !toggleValue);
        }

        /**
         * Disables or Enables microphone functionality in phone
         */
        public void doMic(Preference preference) {
            boolean toggleValue = preference.getSharedPreferences().getBoolean(getResources()
                    .getString(R.string.pref_mic_key), TRUE);
            if (audioManager != null) {
                audioManager.setMicrophoneMute(!toggleValue);
            }
        }

        /**
         * Disables or Enables volume buttons in phone
         */
        public void doVolButtons(Preference preference) {
            boolean toggleValue = preference.getSharedPreferences().getBoolean(getResources()
                    .getString(R.string.pref_vol_buttons_key), TRUE);
            volButtonsState = !toggleValue;
        }

    }
}
