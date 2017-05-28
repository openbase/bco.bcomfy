package org.openbase.bco.bcomfy;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.activityStart.StartActivity;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.view.SurfaceView;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class TangoActivity extends Activity {

    private static final String TAG = TangoActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;

    private SurfaceView surfaceView;
    private TangoRenderer renderer;

    private Tango tango;
    protected TangoPointCloudManager tangoPointCloudManager;
    protected TangoPoseData currentPose;
    private boolean isConnected = true;
    private double cameraPoseTimestamp = 0;

    private int connectedTextureIdGlThread = 0;
    private AtomicBoolean isFrameAvailableTangoThread = new AtomicBoolean(false);
    protected double rgbTimestampGlThread;
    protected TangoCameraIntrinsics intrinsics;

    protected int displayRotation = 0;

    private ValueAnimator statusBarAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tangoPointCloudManager = new TangoPointCloudManager();
        tango = StartActivity.tango;

        statusBarAnimator = ValueAnimator.ofArgb(0xFF222222, 0xFFBB2222);
        statusBarAnimator.setDuration(600);
        statusBarAnimator.setRepeatCount(ValueAnimator.INFINITE);
        statusBarAnimator.setRepeatMode(ValueAnimator.REVERSE);
        statusBarAnimator.addUpdateListener(animation -> runOnUiThread(() -> getWindow().setStatusBarColor((Integer) animation.getAnimatedValue())));
        runOnUiThread(() -> statusBarAnimator.start());

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

    protected abstract void setupGui();

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
                            if (statusBarAnimator.isRunning()) {
                                runOnUiThread(() -> statusBarAnimator.end());
                            }
                            runOnUiThread(() -> getWindow().setStatusBarColor(0xFF000000));
                        } else {
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
        renderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // Prevent concurrent access to {@code isFrameAvailableTangoThread} from the Tango
                // callback thread and service disconnection from an onPause event.
                try {
                    synchronized (TangoActivity.this) {
                        // Don't execute any tango API actions if we're not connected to the service
                        if (!isConnected) {
                            return;
                        }

                        // Set-up scene camera projection to match RGB camera intrinsics.
                        if (!renderer.isSceneCameraConfigured()) {
                            intrinsics =
                                    TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(
                                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                            displayRotation);
                            renderer.setProjectionMatrix(
                                    TangoUtils.projectionMatrixFromCameraIntrinsics(intrinsics));
//                            projectionMatrix = TangoUtils.projectionMatrixFromCameraIntrinsics(intrinsics);
//                            renderer.setProjectionMatrix(projectionMatrix);
                        }

                        // Connect the camera texture to the OpenGL Texture if necessary
                        if (connectedTextureIdGlThread != renderer.getTextureId()) {
                            tango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    renderer.getTextureId());
                            connectedTextureIdGlThread = renderer.getTextureId();
                            Log.d(TAG, "connected to texture id: " + renderer.getTextureId());
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
                            currentPose = TangoSupport.getPoseAtTime(
                                    rgbTimestampGlThread,
                                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    displayRotation);
                            if (currentPose.statusCode == TangoPoseData.POSE_VALID) {
                                // Update the camera pose from the renderer
                                renderer.updateRenderCameraPose(currentPose);
                                cameraPoseTimestamp = currentPose.timestamp;

                                // Call the method to do any additional stuff per frame
                                if (callPostPreFrame()) {
                                    onPostPreFrame();
                                }
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

            @Override
            public boolean callPostFrame() {
                return false;
            }
        });

        surfaceView.setSurfaceRenderer(renderer);
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
                renderer.updateColorCameraTextureUvGlThread(displayRotation);
            }
        });
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    public TangoRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(TangoRenderer renderer) {
        this.renderer = renderer;
    }

    protected void onPostPreFrame() {

    }

    protected boolean callPostPreFrame() {
        return false;
    }

}
