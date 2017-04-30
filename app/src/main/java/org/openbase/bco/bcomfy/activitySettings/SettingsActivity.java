package org.openbase.bco.bcomfy.activitySettings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.widget.EditText;

import org.openbase.bco.bcomfy.R;
import org.openbase.jps.core.JPService;
import org.openbase.jul.extension.rsb.com.jp.JPRSBHost;
import org.openbase.jul.extension.rsb.com.jp.JPRSBPort;
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;

public class SettingsActivity extends Activity {

    public static final String KEY_PREF_IP = "pref_ip";
    public static final String KEY_PREF_PORT = "pref_port";

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            // Update the JPService properties in any case
            updateJPServiceProperties(preference.getContext());

            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            bindPreferenceSummaryToValue(findPreference(KEY_PREF_IP));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_PORT));

            EditText portPreference = ((EditTextPreference)
                    findPreference(KEY_PREF_PORT)).getEditText();
            portPreference.setKeyListener(new NumberKeyListener() {
                @Override
                public int getInputType() {
                    // The following shows the standard keyboard but switches to the view
                    // with numbers on available on the top line of chars
                    return InputType.TYPE_CLASS_NUMBER;
                    // Return the following to show a dialpad as the one shown when entering phone
                    // numbers.
                    // return InputType.TYPE_CLASS_PHONE
                }

                @NonNull
                @Override
                protected char[] getAcceptedChars() {
                    return "1234567890".toCharArray();
                }
            });
        }
    }

    public static void updateJPServiceProperties(Context context) {
        JPService.registerProperty(JPRSBHost.class,
                PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_PREF_IP, "129.70.135.69"));
        JPService.registerProperty(JPRSBPort.class,
                Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_PREF_PORT, "4803")));
        JPService.registerProperty(JPRSBTransport.class, JPRSBTransport.TransportType.SPREAD);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}

