package org.openbase.bco.bcomfy.activityStart;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityInit.InitActivity;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.com.jp.JPRSBHost;
import org.openbase.jul.extension.rsb.com.jp.JPRSBPort;
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;

public class StartActivity extends Activity {

    private static final String TAG = StartActivity.class.getSimpleName();
    private static final int SECS_TO_MILLISECS = 1000;

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    private StartActivityState state = StartActivityState.INIT_BCO;

    private ProgressBar progressBar;
    private TextView infoMessage;
    private Button buttonInitialize;
    private Button buttonCancel;
    private Button buttonRetry;
    private Button buttonSettings;
    private Button buttonPublish;
    private Button buttonRecord;
    private Button buttonManage;

    private Tango tango;
    private TangoConfig tangoConfig;

    private double previousPoseTimeStamp;
    private double timeToNextUpdate = UPDATE_INTERVAL_MS;

    private boolean isRelocalized;

    private static final double UPDATE_INTERVAL_MS = 1000.0;

    private final Object sharedLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        initGui();

        // Set JPS default values
        JPService.registerProperty(JPRSBHost.class, "129.70.135.69");
        JPService.registerProperty(JPRSBPort.class, 4803);
        JPService.registerProperty(JPRSBTransport.class, JPRSBTransport.TransportType.SPREAD);

        // Set system property. This workaround is needed for RSB.
        System.setProperty("sun.arch.data.model", "32");

        // Ask for ADF loading and saving permissions.
        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);

        initBco();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        tango = new Tango(StartActivity.this, new Runnable() {
//            // Pass in a Runnable to be called from UI thread when Tango is ready, this Runnable
//            // will be running on a new thread.
//            // When Tango is ready, we can call Tango functions safely here only when there is no UI
//            // thread changes involved.
//            @Override
//            public void run() {
//                synchronized (StartActivity.this) {
//                    try {
//                        tangoConfig = setTangoConfig(tango);
//                        tango.connect(tangoConfig);
//                        startupTango();
//                    } catch (TangoOutOfDateException e) {
//                        Log.e(TAG, getString(R.string.tango_out_of_date_exception), e);
//                    } catch (TangoErrorException e) {
//                        Log.e(TAG, getString(R.string.tango_error), e);
//                    } catch (TangoInvalidException e) {
//                        Log.e(TAG, getString(R.string.tango_invalid), e);
//                    } catch (SecurityException e) {
//                        // Area Learning permissions are required. If they are not available,
//                        // SecurityException is thrown.
//                        Log.e(TAG, getString(R.string.no_permissions), e);
//                    }
//                }
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        synchronized (StartActivity.this) {
//                            setupTextViewsAndButtons(tango);
//                        }
//                    }
//                });
//            }
//        });
    }

    @Override
    protected void onPause() {
        super.onPause();
//
//        isRelocalized = false;
//        synchronized (this) {
//            try {
//                tango.disconnect();
//            } catch (TangoErrorException e) {
//                Log.e(TAG, getString(R.string.tango_error), e);
//            }
//        }
    }

    private void changeState(StartActivityState startActivityState) {
        state = startActivityState;

        switch (state) {
            case INIT_BCO:
                infoMessage.setText(R.string.gui_connect_bco);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case GET_ADF:
//                infoMessage.setText(R.string.gui_update_adf);
//                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
//                TODO: implement ADF fetching
                changeState(StartActivityState.GET_ADF_FAILED);
                break;
            case GET_ADF_FAILED:
                infoMessage.setText(R.string.gui_update_adf_failed);
                setVisibilities(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case INIT_TANGO:
                infoMessage.setText(R.string.gui_init_tango);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case SETTINGS:
                setVisibilities(View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
                break;
            default:
                break;
        }
    }

    public void onButtonInitializeClicked(View view) {
        Intent intent = new Intent(this, InitActivity.class);
        intent.putExtra("useADF", true);
        startActivity(intent);
    }

    public void onButtonCancelClicked(View view) {
        Registries.shutdown();
        changeState(StartActivityState.SETTINGS);
    }

    public void onButtonRetryClicked(View view) {
        initBco();
    }

    public void onButtonSettingsClicked(View view) {
        Log.e(TAG, "Operation not implemented: onButtonSettingsClicked");
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

    private void setVisibilities(int progress, int info, int init, int cancel, int retry, int settings, int publish, int record, int manage) {
        progressBar.setVisibility(progress);
        infoMessage.setVisibility(info);
        buttonInitialize.setVisibility(init);
        buttonCancel.setVisibility(cancel);
        buttonRetry.setVisibility(retry);
        buttonSettings.setVisibility(settings);
        buttonPublish.setVisibility(publish);
        buttonRecord.setVisibility(record);
        buttonManage.setVisibility(manage);
    }

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
//    private TangoConfig setTangoConfig(Tango tango) {
//        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
//        config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, false);
//
//        ArrayList<String> fullUuidList;
//        fullUuidList = tango.listAreaDescriptions();
//        if (fullUuidList.size() > 0) {
//            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
//                    fullUuidList.get(fullUuidList.size() - 1));
//        }
//
//        return config;
//    }

    private void startupTango() {
        // Set Tango Listeners for Poses Device wrt Start of Service, Device wrt
        // ADF and Start of Service wrt ADF.
//        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
//        framePairs.add(new TangoCoordinateFramePair(
//                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
//                TangoPoseData.COORDINATE_FRAME_DEVICE));
//        framePairs.add(new TangoCoordinateFramePair(
//                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
//                TangoPoseData.COORDINATE_FRAME_DEVICE));
//        framePairs.add(new TangoCoordinateFramePair(
//                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
//                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));
//
//        tango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
//            @Override
//            public void onPoseAvailable(TangoPoseData pose) {
//                // Make sure to have atomic access to Tango Data so that UI loop doesn't interfere
//                // while Pose call back is updating the data.
//                synchronized (sharedLock) {
//                    // Check for Device wrt ADF pose, Device wrt Start of Service pose, Start of
//                    // Service wrt ADF pose (This pose determines if the device is relocalized or
//                    // not).
//                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
//                            && pose.targetFrame == TangoPoseData
//                            .COORDINATE_FRAME_START_OF_SERVICE) {
//                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
//                            isRelocalized = true;
//                        } else {
//                            isRelocalized = false;
//                        }
//                    }
//                }
//
//                final double deltaTime = (pose.timestamp - previousPoseTimeStamp) *
//                        SECS_TO_MILLISECS;
//                previousPoseTimeStamp = pose.timestamp;
//                timeToNextUpdate -= deltaTime;
//
//                if (timeToNextUpdate < 0.0) {
//                    timeToNextUpdate = UPDATE_INTERVAL_MS;
//
//                    Log.e(TAG, pose.toString());
//                    Log.e(TAG, Boolean.toString(isRelocalized));
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            synchronized (sharedLock) {
////                                relocationCheckBox.setChecked(isRelocalized);
//                            }
//                        }
//                    });
//                }
//            }
//        });
    }
    private void initBco() {
        changeState(StartActivityState.INIT_BCO);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
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
                changeState(StartActivityState.GET_ADF);
            }
        }.execute((Void) null);
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
    }
}
