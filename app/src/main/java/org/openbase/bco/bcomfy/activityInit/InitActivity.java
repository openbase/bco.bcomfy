package org.openbase.bco.bcomfy.activityInit;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityInit.measure.Measurer;
import org.openbase.bco.bcomfy.activityInit.measure.Plane;
import org.openbase.bco.bcomfy.activityInit.view.InitRenderer;
import org.openbase.bco.bcomfy.activityInit.view.InstructionTextView;
import org.openbase.bco.bcomfy.activityInit.view.LocationChooser;
import org.openbase.bco.bcomfy.activityStart.StartActivity;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.bcomfy.utils.LocationUtils;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.view.SurfaceView;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class InitActivity extends Activity implements View.OnTouchListener, LocationChooser.LocationChooserListener {
    private static final String TAG = InitActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;

    private SurfaceView surfaceView;
    private InitRenderer initRenderer;
    private Tango tango;
    private TangoPointCloudManager tangoPointCloudManager;
    private boolean isConnected = true;
    private double cameraPoseTimestamp = 0;

    private int connectedTextureIdGlThread = 0;
    private AtomicBoolean isFrameAvailableTangoThread = new AtomicBoolean(false);
    private double rgbTimestampGlThread;

    private int displayRotation = 0;

    private ToggleButton localized;
    private Button buttonAddRoom;
    private Button buttonFinishRoom;
    private Button buttonFinishMeasurement;
    private InstructionTextView instructionTextView;

    private Measurer measurer;

    private ValueAnimator statusBarAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        measurer = new Measurer();
        tango = StartActivity.tango;
        tangoPointCloudManager = new TangoPointCloudManager();

        setupGui();
        setupDisplayManager();
        setupCallbackListeners();
        setupRenderer();
        setDisplayRotation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        surfaceView.onResume();
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onStop() {
        super.onStop();
        surfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
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

    private void setupDisplayManager() {
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
    }

    /**
     * Set up the callback listeners for the Tango service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera.
     */
    private void setupCallbackListeners() {
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
                            runOnUiThread(() -> localized.setChecked(true));
                            if (statusBarAnimator.isRunning()) {
                                runOnUiThread(() -> statusBarAnimator.end());
                            }
                            runOnUiThread(() -> getWindow().setStatusBarColor(0xFF008800));
                        } else {
                            runOnUiThread(() -> localized.setChecked(false));
                            if (!statusBarAnimator.isRunning()) {
                                runOnUiThread(() -> statusBarAnimator.start());
                            }
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
        initRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
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
                            TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(
                                    rgbTimestampGlThread,
                                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    displayRotation);
                            if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                                // Update the camera pose from the renderer
                                initRenderer.updateRenderCameraPose(lastFramePose);
                                cameraPoseTimestamp = lastFramePose.timestamp;
                            }
                        }
                    }
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
    @SuppressLint("WrongConstant")
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        displayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        surfaceView.queueEvent(() -> {
            if (isConnected) {
                initRenderer.updateColorCameraTextureUvGlThread(displayRotation);
            }
        });
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
                Plane planeFit;
                synchronized (this) {
                    planeFit = doFitPlane(u, v, rgbTimestampGlThread);
                }

                if (planeFit != null) {
                    Measurer.MeasureType lastMeasureType = measurer.addPlaneMeasurement(planeFit);
                    updateGuiAfterPlaneMeasurement(planeFit, lastMeasureType);
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
    private void updateGuiAfterPlaneMeasurement(Plane plane, Measurer.MeasureType lastMeasureType) {
        switch (lastMeasureType) {
            case INVALID:
                return;
            case GROUND:
                initRenderer.addGroundPlane(plane.getPosition(), plane.getNormal());
                break;
            case CEILING:
                initRenderer.addCeilingPlane(plane.getPosition(), plane.getNormal());
                break;
            case WALL:
                initRenderer.addWallPlane(plane.getPosition(), plane.getNormal());
                break;
        }

        updateGuiButtons();
    }

    private void setupGui() {
        localized = (ToggleButton) findViewById(R.id.toggleButton);
        instructionTextView = new InstructionTextView((TextView) findViewById(R.id.instructionTextView));

        statusBarAnimator = ValueAnimator.ofArgb(0xFF222222, 0xFFBB2222);
        statusBarAnimator.setDuration(600);
        statusBarAnimator.setRepeatCount(ValueAnimator.INFINITE);
        statusBarAnimator.setRepeatMode(ValueAnimator.REVERSE);
        statusBarAnimator.addUpdateListener(animation -> {
            runOnUiThread(() -> getWindow().setStatusBarColor((Integer) animation.getAnimatedValue()));
        });
        runOnUiThread(() -> statusBarAnimator.start());

        buttonAddRoom = (Button) findViewById(R.id.buttonAddRoom);
        buttonAddRoom.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_add_circle_outline)
                        .color(Color.BLACK)
                        .sizePx((int) buttonAddRoom.getTextSize())
                , null, null, null);

        buttonFinishRoom = (Button) findViewById(R.id.buttonFinishRoom);
        buttonFinishRoom.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_done)
                        .color(Color.BLACK)
                        .sizePx((int) buttonFinishRoom.getTextSize())
                , null, null, null);

        buttonFinishMeasurement = (Button) findViewById(R.id.buttonFinishMeasurement);
        buttonFinishMeasurement.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_done_all)
                        .color(Color.BLACK)
                        .sizePx((int) buttonFinishMeasurement.getTextSize())
                , null, null, null);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        surfaceView.setOnTouchListener(this);
        initRenderer = new InitRenderer(this);
    }

    /**
     * Update Buttons and InstructionTextView based on the current state of the {@link Measurer}
     */
    private void updateGuiButtons() {
        switch (measurer.getMeasurerState()) {
            case INIT:
                buttonAddRoom.setEnabled(true);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(measurer.hasFinishedRoom());
                break;
            case MARK_GROUND:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(false);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_GROUND);
                break;
            case MARK_CEILING:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(false);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_CEILING);
                break;
            case MARK_WALLS:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(false);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_WALLS);
                break;
            case ENOUGH_WALLS:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(true);
                buttonFinishMeasurement.setEnabled(false);
                break;
        }
    }

    /**
     * Use the TangoSupport library with point cloud data to calculate the plane
     * of the world feature pointed at the location the camera is looking.
     * It returns the transform of the fitted plane in a double array.
     */
    private Plane doFitPlane(float u, float v, double rgbTimestamp) {
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
        TangoSupport.TangoDoubleMatrixTransformData transform =
                TangoSupport.getDoubleMatrixTransformAtTime(pointCloud.timestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                        TangoSupport.ROTATION_IGNORED);

        if (transform.statusCode == TangoPoseData.POSE_VALID) {
            // Get the transformed position of the plane
            double[] transformedPlanePosition = TangoSupport.doubleTransformPoint(transform.matrix, intersectionPointPlaneModelPair.intersectionPoint);

            // Get the transformed normal of the plane
            // For this we first need the transposed inverse of the transformation matrix
            double[] normalTransformMatrix = new double[16];
            new Matrix4(transform.matrix).inverse().transpose().toArray(normalTransformMatrix);
            double[] planeNormal = {intersectionPointPlaneModelPair.planeModel[0], intersectionPointPlaneModelPair.planeModel[1], intersectionPointPlaneModelPair.planeModel[2]};
            double[] transformedPlaneNormal = TangoSupport.doubleTransformPoint(normalTransformMatrix, planeNormal);

            return new Plane(transformedPlanePosition, transformedPlaneNormal);
        } else {
            Log.w(TAG, "Can't get depth camera transform at time " + pointCloud.timestamp);
            return null;
        }
    }

    public void onAddRoomClicked(View v) {
        measurer.startNewRoom();
        updateGuiButtons();
    }

    public void onFinishRoomClicked(View v) {
        DialogFragment dialogFragment = new LocationChooser();
        dialogFragment.show(getFragmentManager(), "locationChooser");
    }

    public void onFinishMeasurementClicked(View v) {
        updateGuiButtons();
    }

    @Override
    public CharSequence[] getLocations() {
//        return locations.toArray(new CharSequence[locations.size()]);
        return new CharSequence[10];
    }

    @Override
    public void onLocationSelected(final String location) {
        measurer.finishRoom();
        updateGuiButtons();

//        ArrayList<Vector3> ceiling = measurer.getLatestCeilingVertices();
//        final ArrayList<Vector3> ground  = measurer.getLatestGroundVertices();
//
//        for (Vector3 vertex : ground) {
//            initRenderer.addSphere(vertex, Color.BLUE);
//        }
//
//        for (Vector3 vertex : ceiling) {
//            initRenderer.addSphere(vertex, Color.RED);
//        }
//
//        initRenderer.clearPlanes();
//
//        LocationUtils.updateLocationShape(locationRegistry, location, ground, new OnTaskFinishedListener<Void>() {
//            @Override
//            public void onTaskFinished(Void result) {
//                Log.i(TAG, "Updated shape of location: " + location);
//            }
//        });
    }

}
