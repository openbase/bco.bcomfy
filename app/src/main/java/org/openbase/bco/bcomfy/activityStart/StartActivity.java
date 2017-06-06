package org.openbase.bco.bcomfy.activityStart;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.CoreActivity;
import org.openbase.bco.bcomfy.activityInit.InitActivity;
import org.openbase.bco.bcomfy.activitySettings.SettingsActivity;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;

public class StartActivity extends Activity {

    private static final String TAG = StartActivity.class.getSimpleName();
    private static Context applicationContext;

    private StartActivityState state = StartActivityState.INIT_BCO;

    private InitBcoTask initBcoTask;

    private ProgressBar progressBar;
    private TextView infoMessage;
    private Button buttonInitialize;
    private Button buttonCancel;
    private Button buttonRetry;
    private Button buttonSettings;
    private Button buttonPublish;
    private Button buttonRecord;
    private Button buttonManage;
    private Button buttonDebugStart;

    public static Tango tango;
    private TangoConfig tangoConfig;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        applicationContext = getApplicationContext();
        initGui();

        // Set default preferences if not already set.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Set system property. This workaround is needed for RSB.
        System.setProperty("sun.arch.data.model", "32");

        // Ask for ADF loading and saving permissions.
        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);

        initBcoTask = new InitBcoTask(returnObject -> StartActivity.this.changeState(StartActivityState.GET_ADF));
        initBcoTask.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private void bindTangoService() {
        tango = new Tango(StartActivity.this, () -> {
            // Synchronize against disconnecting while the service is being used in the
            // OpenGL thread or in the UI thread.
            synchronized (StartActivity.this) {
                try {
                    TangoSupport.initialize(tango);
                    tangoConfig = setupTangoConfig(tango);
                    tango.connect(tangoConfig);
                    runOnUiThread(() -> changeState(StartActivityState.SETTINGS));
                    if (state == StartActivityState.INIT_TANGO_TO_CORE) {
                        startCoreActivity();
                    }
                    else {
                        startInitActivity();
                    }
                } catch (TangoOutOfDateException e) {
                    Log.e(TAG, getString(R.string.tango_out_of_date_exception), e);
                    changeState(StartActivityState.INIT_TANGO_FAILED);
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.tango_error), e);
                    changeState(StartActivityState.INIT_TANGO_FAILED);
                } catch (TangoInvalidException e) {
                    Log.e(TAG, getString(R.string.tango_invalid), e);
                    changeState(StartActivityState.INIT_TANGO_FAILED);
                }
            }
        });
    }

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use default configuration for Tango Service, plus color camera, low latency
        // IMU integration and drift correction.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        // NOTE: Low latency integration is necessary to achieve a precise alignment of
        // virtual objects with the RBG image and produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        // Drift correction allows motion tracking to recover after it loses tracking.
        // The drift corrected pose is is available through the frame pair with
        // base frame AREA_DESCRIPTION and target frame DEVICE.
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);

        ArrayList<String> fullUUIDList;
        // Returns a list of ADFs with their UUIDs
        fullUUIDList = tango.listAreaDescriptions();
        // Load the latest ADF if ADFs are found.
        if (fullUUIDList.size() > 0) {
            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                    fullUUIDList.get(fullUUIDList.size() - 1));

            Log.i(TAG, "using ADF: " + fullUUIDList.get(fullUUIDList.size() - 1));
        }
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, false);

        return config;
    }

    private void changeState(StartActivityState startActivityState) {
        state = startActivityState;

        switch (state) {
            case INIT_BCO:
                infoMessage.setText(R.string.gui_connect_bco);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case GET_ADF:
//                infoMessage.setText(R.string.gui_update_adf);
//                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
//                TODO: implement ADF fetching
                changeState(StartActivityState.GET_ADF_FAILED);
                break;
            case GET_ADF_FAILED:
                infoMessage.setText(R.string.gui_update_adf_failed);
                setVisibilities(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE);
                break;
            case INIT_TANGO_TO_INIT:
            case INIT_TANGO_TO_CORE:
                infoMessage.setText(R.string.gui_init_tango);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case INIT_TANGO_FAILED:
                infoMessage.setText(R.string.gui_init_tango_failed);
                setVisibilities(View.GONE, View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case SETTINGS:
                setVisibilities(View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE);
                break;
            default:
                break;
        }
    }

    public void onButtonInitializeClicked(View view) {
        changeState(StartActivityState.INIT_TANGO_TO_INIT);
        bindTangoService();
    }

    public void onButtonCancelClicked(View view) {
        changeState(StartActivityState.SETTINGS);

        if (initBcoTask.getStatus() != AsyncTask.Status.FINISHED) {
            Log.i(TAG, "Cancel initialization task...");
            initBcoTask.cancel(true);
        } else {
            try {
                Registries.shutdown();
            } catch (CancellationException ex) {
                Log.e(TAG, "Can not shutdown registries! Maybe they were not started?");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void onButtonRetryClicked(View view) {
        if (initBcoTask.getStatus() != AsyncTask.Status.FINISHED) {
            Log.i(TAG, "Cancel initialization task...");
            initBcoTask.cancel(true);
        }

        initBcoTask = new InitBcoTask(returnObject -> changeState(StartActivityState.GET_ADF));
        initBcoTask.execute();

        changeState(StartActivityState.INIT_BCO);
    }

    public void onButtonSettingsClicked(View view) {
        startSettingsActivity();
    }

    public void onButtonPublishClicked(View view) {
        Log.e(TAG, "Operation not implemented: onButtonPublishClicked");
    }

    public void onButtonRecordClicked(View v) {
        Log.e(TAG, "Operation not implemented: onButtonRecordClicked");
    }

    public void onButtonManageClicked(View view) {
        Log.e(TAG, "Operation not implemented: onButtonManageClicked");
    }

    public void onButtonDebugStartClicked(View view) {
        changeState(StartActivityState.INIT_TANGO_TO_CORE);
        bindTangoService();
    }

    private void setVisibilities(int progress, int info, int init, int cancel, int retry, int settings, int publish, int record, int manage, int debugStart) {
        progressBar.setVisibility(progress);
        infoMessage.setVisibility(info);
        buttonInitialize.setVisibility(init);
        buttonCancel.setVisibility(cancel);
        buttonRetry.setVisibility(retry);
        buttonSettings.setVisibility(settings);
        buttonPublish.setVisibility(publish);
        buttonRecord.setVisibility(record);
        buttonManage.setVisibility(manage);
        buttonDebugStart.setVisibility(debugStart);
    }

    private void initGui() {
        progressBar      = (ProgressBar) findViewById(R.id.progressBar);
        infoMessage      = (TextView) findViewById(R.id.infoMessage);
        buttonInitialize = (Button) findViewById(R.id.button_initialize);
        buttonCancel     = (Button) findViewById(R.id.button_cancel);
        buttonRetry      = (Button) findViewById(R.id.button_retry);
        buttonSettings   = (Button) findViewById(R.id.button_settings);
        buttonPublish    = (Button) findViewById(R.id.button_publish);
        buttonRecord     = (Button) findViewById(R.id.button_record);
        buttonManage     = (Button) findViewById(R.id.button_manage);
        buttonDebugStart = (Button) findViewById(R.id.button_debug_start);
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void startInitActivity() {
        Intent intent = new Intent(this, InitActivity.class);
        startActivity(intent);
    }

    private void startCoreActivity() {
        Intent intent = new Intent(this, CoreActivity.class);
        startActivity(intent);
    }

    private static class InitBcoTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = InitBcoTask.class.getSimpleName();
        private OnTaskFinishedListener<Void> listener;

        InitBcoTask(OnTaskFinishedListener<Void> listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Set JPS values used by BCO API.
                SettingsActivity.updateJPServiceProperties(applicationContext);

                // Initiate registries
                Registries.waitForData();
            } catch (CouldNotPerformException | InterruptedException ex) {
                Log.e(TAG, "Error while initializing BCO!");
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            Log.i(TAG, "Connection to BCO initialized.");
            Log.i(TAG, BCO.BCO_LOGO_ASCI_ARTS);

            listener.taskFinishedCallback(null);
        }

        @Override
        protected void onCancelled() {
            Registries.shutdown();
        }
    }
}
