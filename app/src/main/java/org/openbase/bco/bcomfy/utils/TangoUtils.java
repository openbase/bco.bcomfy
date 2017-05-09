package org.openbase.bco.bcomfy.utils;

import android.opengl.Matrix;
import android.util.Log;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.activityInit.measure.Plane;
import org.rajawali3d.math.Matrix4;

public final class TangoUtils {

    private static final String TAG = TangoUtils.class.getSimpleName();

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the Rajawali scene.
     *
     * @param intrinsics camera instrinsics for computing the project matrix.
     */
    public static final float[] projectionMatrixFromCameraIntrinsics(TangoCameraIntrinsics intrinsics) {
        float cx = (float) intrinsics.cx;
        float cy = (float) intrinsics.cy;
        float width = (float) intrinsics.width;
        float height = (float) intrinsics.height;
        float fx = (float) intrinsics.fx;
        float fy = (float) intrinsics.fy;

        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        float near = 0.1f;
        float far = 100;

        float xScale = near / fx;
        float yScale = near / fy;
        float xOffset = (cx - (width / 2.0f)) * xScale;
        // Color camera's coordinates has y pointing downwards so we negate this term.
        float yOffset = -(cy - (height / 2.0f)) * yScale;

        float m[] = new float[16];
        Matrix.frustumM(m, 0,
                xScale * (float) -width / 2.0f - xOffset,
                xScale * (float) width / 2.0f - xOffset,
                yScale * (float) -height / 2.0f - yOffset,
                yScale * (float) height / 2.0f - yOffset,
                near, far);
        return m;
    }



    /**
     * Use the TangoSupport library with point cloud data to calculate the plane
     * of the world feature pointed at the location the camera is looking.
     * It returns the transform of the fitted plane in a double array.
     */
    public static final Plane doFitPlane(float u, float v, double rgbTimestamp, TangoPointCloudData pointCloud, int displayRotation) {
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
}
