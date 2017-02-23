package org.openbase.bco.bcomfy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import java.util.ArrayList;

public class StartActivity extends Activity {

    private static final String TAG = StartActivity.class.getSimpleName();
    private static final int SECS_TO_MILLISECS = 1000;

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    private Tango tango;
    private TangoConfig tangoConfig;

    private CheckBox relocationCheckBox;

    private double previousPoseTimeStamp;
    private double timeToNextUpdate = UPDATE_INTERVAL_MS;

    private boolean isRelocalized;

    private static final double UPDATE_INTERVAL_MS = 1000.0;

    private final Object sharedLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);
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

    public void onButtonCancelClicked(View v) {
        Intent intent = new Intent(this, InitActivity.class);
        startActivity(intent);
    }

    public void startRelocationClicked(View v) {
        System.out.println("startRelocationClicked");
    }

    private void setupTextViewsAndButtons(Tango tango) {
        //relocationCheckBox = (CheckBox) findViewById(R.id.checkBox);
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
}
