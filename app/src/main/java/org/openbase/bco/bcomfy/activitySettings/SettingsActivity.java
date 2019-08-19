package org.openbase.bco.bcomfy.activitySettings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.openbase.bco.authentication.lib.jp.JPBCOVarDirectory;
import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.utils.BcoUtils;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.extension.rsb.com.jp.JPRSBHost;
import org.openbase.jul.extension.rsb.com.jp.JPRSBPort;
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;

import java.io.File;

public class SettingsActivity extends Activity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String KEY_PREF_IP = "pref_ip";
    public static final String KEY_PREF_PORT = "pref_port";
    public static final String KEY_PREF_INIT_DEFAULT = "pref_init_default";
    public static final String KEY_PREF_INIT_ALIGN = "pref_init_align";
    public static final String KEY_PREF_INIT_ANCHOR = "pref_init_anchor";
    public static final String KEY_PREF_MISC_UNKONWN_SERVICE = "pref_misc_unknown_service";
    public static final String KEY_PREF_MISC_ADF = "pref_misc_adf";
    public static final String KEY_PREF_MISC_USE_META_FILTER = "pref_misc_use_meta_filter";
    public static final String KEY_PREF_MISC_META_FILTER = "pref_misc_meta_filter";
    public static final String KEY_PREF_DELETE_DEVICE_POSES = "pref_delete_device_poses";
    public static final String KEY_PREF_DELETE_LOCATION_SHAPES = "pref_delete_location_shapes";

    private static String[] adfIdList;
    private static String[] adfNameList;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
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

        return true;
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

            // Find preferences
            EditTextPreference ipPreference = (EditTextPreference) findPreference(KEY_PREF_IP);
            EditTextPreference portPreference = (EditTextPreference) findPreference(KEY_PREF_PORT);

            EditTextPreference initDefaultPreference = (EditTextPreference) findPreference(KEY_PREF_INIT_DEFAULT);
            CheckBoxPreference initAlignPreference = (CheckBoxPreference) findPreference(KEY_PREF_INIT_ALIGN);
            EditTextPreference initAnchorPreference = (EditTextPreference) findPreference(KEY_PREF_INIT_ANCHOR);
            initAnchorPreference.setEnabled(initAlignPreference.isChecked());

            ListPreference miscAdfPreference = (ListPreference) findPreference(KEY_PREF_MISC_ADF);
            if (adfNameList == null || adfIdList == null) {
                miscAdfPreference.setEnabled(false);
            }
            else {
                miscAdfPreference.setEntries(adfNameList);
                miscAdfPreference.setEntryValues(adfIdList);
            }
            CheckBoxPreference useMetaFilterPreference = (CheckBoxPreference) findPreference(KEY_PREF_MISC_USE_META_FILTER);
            EditTextPreference metaFilterPreference = (EditTextPreference) findPreference(KEY_PREF_MISC_META_FILTER);
            metaFilterPreference.setEnabled(useMetaFilterPreference.isChecked());

            Preference deleteDevicePosesUtil = findPreference(KEY_PREF_DELETE_DEVICE_POSES);
            Preference deleteLocationShapesUtil = findPreference(KEY_PREF_DELETE_LOCATION_SHAPES);

            // Bind preference summaries
            bindPreferenceSummaryToValue(ipPreference);
            bindPreferenceSummaryToValue(portPreference);
            bindPreferenceSummaryToValue(initDefaultPreference);
            bindPreferenceSummaryToValue(initAnchorPreference);
            bindPreferenceSummaryToValue(miscAdfPreference);
            bindPreferenceSummaryToValue(metaFilterPreference);

            initAlignPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                initAnchorPreference.setEnabled((boolean) newValue);
                return true;
            });

            useMetaFilterPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                metaFilterPreference.setEnabled((boolean) newValue);
                return true;
            });

            EditText portPreferenceText = portPreference.getEditText();
            portPreferenceText.setKeyListener(new NumberKeyListener() {
                @Override
                public int getInputType() {
                    return InputType.TYPE_CLASS_NUMBER;
                }

                @NonNull
                @Override
                protected char[] getAcceptedChars() {
                    return "1234567890".toCharArray();
                }
            });

            EditText initDefaultPreferenceText = initDefaultPreference.getEditText();
            initDefaultPreferenceText.setKeyListener(new NumberKeyListener() {
                @Override
                public int getInputType() {
                    return InputType.TYPE_CLASS_NUMBER;
                }

                @NonNull
                @Override
                protected char[] getAcceptedChars() {
                    return "1234567890".toCharArray();
                }
            });

            EditText initAnchorPreferenceText = initAnchorPreference.getEditText();
            initAnchorPreferenceText.setKeyListener(new NumberKeyListener() {
                @Override
                public int getInputType() {
                    return InputType.TYPE_CLASS_NUMBER;
                }

                @NonNull
                @Override
                protected char[] getAcceptedChars() {
                    return "1234567890".toCharArray();
                }
            });

            // Configure Utility to delete all unit poses
            deleteDevicePosesUtil.setEnabled(Registries.isDataAvailable());
            deleteDevicePosesUtil.setOnPreferenceClickListener(preference -> {
                deleteDevicePosesUtil.setEnabled(false);

                new BcoUtils.DeleteAllDevicePosesTask(successful -> {
                    if (successful) {
                        Toast.makeText(getContext(), "Delete All Unit Poses: SUCCESSFUL!", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getContext(), "Delete All Unit Poses: FAILED!", Toast.LENGTH_LONG).show();
                    }
                    deleteDevicePosesUtil.setEnabled(true);
                }).execute();

                return true;
            });

            // Configure Utility to delete all location shapes
            deleteLocationShapesUtil.setEnabled(Registries.isDataAvailable());
            deleteLocationShapesUtil.setOnPreferenceClickListener(preference -> {
                deleteLocationShapesUtil.setEnabled(false);

                new BcoUtils.DeleteAllLocationShapesTask(successful -> {
                    if (successful) {
                        Toast.makeText(getContext(), "Delete all Location Shapes: SUCCESSFUL!", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getContext(), "Delete all Location Shapes: FAILED!", Toast.LENGTH_LONG).show();
                    }
                    deleteLocationShapesUtil.setEnabled(true);
                }).execute();

                return true;
            });
        }
    }
    public static void setupJPDefaultValues(Context context) {
        try {
            JPService.registerProperty(JPVerbose.class, true);
            JPService.registerProperty(JPRSBTransport.class, JPRSBTransport.TransportType.SPREAD);
            JPService.registerProperty(JPRSBHost.class, "192.168.76.100");
            JPService.registerProperty(JPRSBPort.class, 4814);
            JPService.registerProperty(JPBCOVarDirectory.class, new File(context.getApplicationInfo().dataDir));
            Log.i(TAG, "detect store at:" + context.getApplicationInfo().dataDir);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void updateJPServiceProperties(Context context) {
        try {
            JPService.getProperty(JPRSBHost.class).update(
                    PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_PREF_IP, JPService.getValue(JPRSBHost.class)));
            JPService.getProperty(JPRSBPort.class).update(
                    Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_PREF_PORT, JPService.getValue(JPRSBPort.class).toString())));

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adfIdList = getIntent().getStringArrayExtra("adfIdList");
        adfNameList = getIntent().getStringArrayExtra("adfNameList");

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}

