package org.openbase.bco.bcomfy.activityInit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.activityInit.measure.Measurer;
import org.openbase.bco.bcomfy.activityInit.view.InitRenderer;
import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityInit.view.InstructionTextView;
import org.openbase.bco.bcomfy.utils.MathUtils;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.view.SurfaceView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class InitActivity extends Activity implements View.OnTouchListener{
    private static final String TAG = InitActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    private SurfaceView surfaceView;
    private InitRenderer initRenderer;
    private Tango tango;
    private TangoConfig tangoConfig;
    private TangoPointCloudManager tangoPointCloudManager;
    private boolean isLoadingLocation;
    private boolean isConnected = false;
    private double cameraPoseTimestamp = 0;

    private ArrayList<float[]> planeList;
    private boolean planesDrawn = false;

    // Texture rendering related fields.
    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int connectedTextureIdGlThread = 0;
    private AtomicBoolean isFrameAvailableTangoThread = new AtomicBoolean(false);
    private double rgbTimestampGlThread;

    private int displayRotation = 0;

    private ToggleButton localized;
    private InstructionTextView instructionTextView;

    private Measurer measurer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        measurer = new Measurer();

        isLoadingLocation = getIntent().getBooleanExtra("load", false);
        localized = (ToggleButton) findViewById(R.id.toggleButton);
        instructionTextView = new InstructionTextView((TextView) findViewById(R.id.instructionTextView));

        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        surfaceView.setOnTouchListener(this);
        initRenderer = new InitRenderer(this);
        tangoPointCloudManager = new TangoPointCloudManager();

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {

                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayChanged(int displayId) {

                }
            }, null);
        }

        setupRenderer();

        if (isLoadingLocation) {
            try {
                FileInputStream fis = openFileInput("planeList.tmp");
                ObjectInputStream ois = new ObjectInputStream(fis);
                planeList = (ArrayList<float[]>) ois.readObject();
                ois.close();
                Log.e(TAG, "planeList loaded. Contains " + planeList.size() + " planes!");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            planeList = new ArrayList<>();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        surfaceView.onResume();

        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until
        // the Tango service is properly set-up and we start getting onFrameAvailable callbacks.
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        // Check and request camera permission at run time.
        if (checkAndRequestPermissions()) {
            bindTangoService();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        surfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread. Tango.disconnect
        // will block here until all Tango callback calls are finished. If you lock against this
        // object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            if (isConnected) {
                try {
                    isConnected = false;
                    tango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    // We need to invalidate the connected texture ID so that we cause a
                    // re-connection in the OpenGL thread after resume.
                    connectedTextureIdGlThread = INVALID_TEXTURE_ID;
                    tango.disconnect();
                    tango = null;
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.tango_error), e);
                }
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // Ignore onTouch Event if we don't want to measure something
        if (measurer.getMeasurerState() == Measurer.MeasurerState.INIT) {
            return true;
        }

        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            // Calculate click location in u,v (0;1) coordinates.
            float u = motionEvent.getX() / view.getWidth();
            float v = motionEvent.getY() / view.getHeight();

            try {
                // Fit a plane on the clicked point using the latest point cloud data
                // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
                // and a possible service disconnection due to an onPause event.
                float[] planeFitTransform;
                synchronized (this) {
                    planeFitTransform = doFitPlane(u, v, rgbTimestampGlThread);
                }

                if (planeFitTransform != null) {
                    Matrix4 planeMatrix = new Matrix4(planeFitTransform);
                    Measurer.Measurement latestMeasurement = measurer.addPlaneMeasurement(planeMatrix);
                    updateGuiAfterPlaneMeasurement(planeMatrix, latestMeasurement);
                }

            } catch (TangoException t) {
                Toast.makeText(getApplicationContext(),
                        R.string.tango_error,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.tango_error), t);
            } catch (SecurityException t) {
                Toast.makeText(getApplicationContext(),
                        R.string.no_permissions,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.no_permissions), t);
            }
        }
        return true;
    }

    /**
     * Handle a successful plane measurement
     */
    private void updateGuiAfterPlaneMeasurement(Matrix4 plane, Measurer.Measurement type) {
        switch (type) {
            case INVALID:
                return;
            case GROUND:
                initRenderer.addGroundPlane(plane);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_CEILING);
                break;
            case CEILING:
                initRenderer.addCeilingPlane(plane);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_WALLS);
                break;
            case WALL:
                initRenderer.addWallPlane(plane);
                break;
        }
    }

    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private void bindTangoService() {
        // Since we call mTango.disconnect() in onStop, this will unbind Tango Service, so every
        // time when onStart gets called, we should create a new Tango object.
        tango = new Tango(InitActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready, this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there
            // is no UI thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the
                // OpenGL thread or in the UI thread.
                synchronized (InitActivity.this) {
                    try {
                        TangoSupport.initialize();
                        tangoConfig = setupTangoConfig(tango);
                        tango.connect(tangoConfig);
                        startupTango();
                        isConnected = true;
                        setDisplayRotation();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.tango_out_of_date_exception), e);
                        showsToastAndFinishOnUiThread(R.string.tango_out_of_date_exception);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.tango_invalid);
                    }
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

        if (isLoadingLocation) {
            ArrayList<String> fullUUIDList;
            // Returns a list of ADFs with their UUIDs
            fullUUIDList = tango.listAreaDescriptions();

            // Load the latest ADF if ADFs are found.
            if (fullUUIDList.size() > 0) {
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                        fullUUIDList.get(fullUUIDList.size() - 1));
            }
        }
        else {
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        }

        return config;
    }

    /**
     * Set up the callback listeners for the Tango service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera.
     */
    private void startupTango() {
        // No need to add any coordinate frame pairs since we aren't using pose data from callbacks.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));

        tango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onFrameAvailable(int cameraId) {
                // Check if the frame available is for the camera we want and update its frame
                // on the view.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Now that we are receiving onFrameAvailable callbacks, we can switch
                    // to RENDERMODE_WHEN_DIRTY to drive the render loop from this callback.
                    // This will result on a frame rate of approximately 30FPS, in synchrony with
                    // the RGB camera driver.
                    // If you need to render at a higher rate (i.e.: if you want to render complex
                    // animations smoothly) you  can use RENDERMODE_CONTINUOUSLY throughout the
                    // application lifecycle.
                    if (surfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    }

//                    if (!planesDrawn && localized.isChecked()) {
//                        Log.e(TAG, "Inserting planes...");
//                        for (float[] plane : planeList) {
//                            initRenderer.insertPlane(plane);
//                        }
//                        planesDrawn = true;
//                        Log.e(TAG, "Planes inserted.");
//                    }

                    // Mark a camera frame is available for rendering in the OpenGL thread.
                    isFrameAvailableTangoThread.set(true);
                    // Trigger an Rajawali render to update the scene with the new RGB data.
                    surfaceView.requestRender();
                }
            }


            public void onPoseAvailable(TangoPoseData pose) {
                synchronized (this) {
                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData
                            .COORDINATE_FRAME_START_OF_SERVICE) {
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            Log.e(TAG, "YES");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    localized.setChecked(true);
                                }
                            });
                        } else {
                            Log.e(TAG, "no");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    localized.setChecked(false);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                // Save the cloud and point data for later use.
                tangoPointCloudManager.updatePointCloud(pointCloud);
            }
        });
    }

    /**
     * Connects the view and renderer to the color camera and callbacks.
     */
    private void setupRenderer() {
        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        initRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks had a chance to run and before scene objects are rendered
                // into the scene.

                // Prevent concurrent access to {@code isFrameAvailableTangoThread} from the Tango
                // callback thread and service disconnection from an onPause event.
                try {
                    synchronized (InitActivity.this) {
                        // Don't execute any tango API actions if we're not connected to the service
                        if (!isConnected) {
                            return;
                        }

                        // Set-up scene camera projection to match RGB camera intrinsics.
                        if (!initRenderer.isSceneCameraConfigured()) {
                            TangoCameraIntrinsics intrinsics =
                                    TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(
                                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                            displayRotation);
                            initRenderer.setProjectionMatrix(
                                    TangoUtils.projectionMatrixFromCameraIntrinsics(intrinsics));
                        }
                        // Connect the camera texture to the OpenGL Texture if necessary
                        // NOTE: When the OpenGL context is recycled, Rajawali may re-generate the
                        // texture with a different ID.
                        if (connectedTextureIdGlThread != initRenderer.getTextureId()) {
                            tango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    initRenderer.getTextureId());
                            connectedTextureIdGlThread = initRenderer.getTextureId();
                            Log.d(TAG, "connected to texture id: " + initRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture with it
                        if (isFrameAvailableTangoThread.compareAndSet(true, false)) {
                            rgbTimestampGlThread =
                                    tango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                        }

                        // If a new RGB frame has been rendered, update the camera pose to match.
                        if (rgbTimestampGlThread > cameraPoseTimestamp) {
                            // Calculate the camera color pose at the camera frame update time in
                            // OpenGL engine.
                            //
                            // When drift correction mode is enabled in config file, we must query
                            // the device with respect to Area Description pose in order to use the
                            // drift corrected pose.
                            //
                            // Note that if you don't want to use the drift corrected pose, the
                            // normal device with respect to start of service pose is available.
                            TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(
                                    rgbTimestampGlThread,
                                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    displayRotation);
                            if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                                // Update the camera pose from the renderer
                                initRenderer.updateRenderCameraPose(lastFramePose);
                                cameraPoseTimestamp = lastFramePose.timestamp;
                            } else {
                                // When the pose status is not valid, it indicates the tracking has
                                // been lost. In this case, we simply stop rendering.
                                //
                                // This is also the place to display UI to suggest the user walk
                                // to recover tracking.
                                Log.w(TAG, "Can't get device pose at time: " +
                                        rgbTimestampGlThread);
                            }
                        }
                    }

                    // Avoid crashing the application due to unhandled exceptions
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Tango API call error within the OpenGL render thread", e);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }

            @Override
            public boolean callPreFrame() {
                return true;
            }
        });

        surfaceView.setSurfaceRenderer(initRenderer);
    }

    /**
     * Set the color camera background texture rotation and save the camera to display rotation.
     */
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        displayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        surfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    initRenderer.updateColorCameraTextureUvGlThread(displayRotation);
                }
            }
        });
    }

    /**
     * Use the TangoSupport library with point cloud data to calculate the plane
     * of the world feature pointed at the location the camera is looking.
     * It returns the transform of the fitted plane in a double array.
     */
    private float[] doFitPlane(float u, float v, double rgbTimestamp) {
        TangoPointCloudData pointCloud = tangoPointCloudManager.getLatestPointCloud();

        if (pointCloud == null) {
            Log.e(TAG, "PointCloud == null");
            return null;
        }

        // We need to calculate the transform between the color camera at the
        // time the user clicked and the depth camera at the time the depth
        // cloud was acquired.
        TangoPoseData depthTcolorPose = TangoSupport.calculateRelativePose(
                pointCloud.timestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                rgbTimestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR);

        // Perform plane fitting with the latest available point cloud data.
        double[] identityTranslation = {0.0, 0.0, 0.0};
        double[] identityRotation = {0.0, 0.0, 0.0, 1.0};
        TangoSupport.IntersectionPointPlaneModelPair intersectionPointPlaneModelPair =
                TangoSupport.fitPlaneModelNearPoint(pointCloud,
                        identityTranslation, identityRotation, u, v, displayRotation,
                        depthTcolorPose.translation, depthTcolorPose.rotation);

        // Get the transform from depth camera to OpenGL world at the timestamp of the cloud.
        TangoSupport.TangoMatrixTransformData transform =
                TangoSupport.getMatrixTransformAtTime(pointCloud.timestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                        TangoSupport.ROTATION_IGNORED);
        if (transform.statusCode == TangoPoseData.POSE_VALID) {
            float[] openGlTPlane = MathUtils.calculatePlaneTransform(
                    intersectionPointPlaneModelPair.intersectionPoint,
                    intersectionPointPlaneModelPair.planeModel, transform.matrix);

            return openGlTPlane;
        } else {
            Log.w(TAG, "Can't get depth camera transform at time " + pointCloud.timestamp);
            return null;
        }
    }

    /**
     * Check we have the necessary permissions for this app, and ask for them if we haven't.
     *
     * @return True if we have the necessary permissions, false if we haven't.
     */
    private boolean checkAndRequestPermissions() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return false;
        }
        return true;
    }

    /**
     * Check we have the necessary permissions for this app.
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the necessary permissions for this app.
     */
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
            showRequestPermissionRationale();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION},
                    CAMERA_PERMISSION_CODE);
        }
    }

    /**
     * If the user has declined the permission before, we have to explain him the app needs this
     * permission.
     */
    private void showRequestPermissionRationale() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Java Augmented Reality Example requires camera permission")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(InitActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
    }

    /**
     * Result for requesting camera permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (hasCameraPermission()) {
            bindTangoService();
        } else {
            Toast.makeText(this, "Java Augmented Reality Example requires camera permission",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Display toast on UI thread and finishes activity.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(InitActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    public void onAddRoomClicked(View v) {
        findViewById(R.id.button2).setEnabled(false);
        measurer.startNewRoom();
        instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_GROUND);
    }

    public void onSaveLocationClicked(View v) {
//        Log.e(TAG, "savingLocation...1");
//        new Runnable() {
//
//            @Override
//            public void run() {
//                Log.e(TAG, "savingLocation...3");
//                tango.saveAreaDescription();
//
//                try {
//                    FileOutputStream fos = openFileOutput("planeList.tmp", Context.MODE_PRIVATE);
//                    ObjectOutputStream oos = new ObjectOutputStream(fos);
//                    oos.writeObject(planeList);
//                    oos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                showsToastAndFinishOnUiThread(R.string.toast_location_saved);
//            }
//        }.run();
    }
}
