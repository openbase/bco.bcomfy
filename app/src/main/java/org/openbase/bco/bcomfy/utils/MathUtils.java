package org.openbase.bco.bcomfy.utils;

import android.opengl.Matrix;

import org.rajawali3d.math.vector.Vector3;

public final class MathUtils {

    public static double dotIgnoreY(Vector3 v1, Vector3 v2) {
        return v1.x*v2.x + v1.z*v2.z;
    }

    public static double detIgnoreY(Vector3 v1, Vector3 v2) {
        return v1.x*v2.z - v1.z*v2.x;
    }

    public static double clockwiseAngle(Vector3 v1, Vector3 v2) {
        double dot = dotIgnoreY(v1, v2);
        double det = detIgnoreY(v1, v2);
        return Math.atan2(det, dot);
    }

    /**
     * Calculate the intersection of three planes.
     * See http://mathworld.wolfram.com/Plane-PlaneIntersection.html
     */
    public static Vector3 calc3PlaneIntersection(Vector3 x0, Vector3 n0,
                                           Vector3 x1, Vector3 n1,
                                           Vector3 x2, Vector3 n2) {

        double det = (n0.x * n1.y * n2.z) + (n1.x * n2.y * n0.z) + (n2.x * n0.y * n1.z) -
                (n0.z * n1.y * n2.x) - (n1.z * n2.y * n0.x) - (n2.z * n0.y * n1.x);

        Vector3 v0 = new Vector3(n1);
        v0.cross(n2).multiply(x0.dot(n0));
        Vector3 v1 = new Vector3(n2);
        v1.cross(n0).multiply(x1.dot(n1));
        Vector3 v2 = new Vector3(n0);
        v2.cross(n1).multiply(x2.dot(n2));

        Vector3 result = v0.add(v1).add(v2).multiply(1/det);

        return result;
    }

    /**
     * Calculate the pose of the plane based on the position and normal orientation of the plane
     * and align it with gravity.
     */
    @Deprecated
    public static final float[] calculatePlaneTransform(double[] point, double normal[],
                                            float[] openGlTdepth) {
        // Vector aligned to gravity.
        float[] openGlUp = new float[]{0, 1, 0, 0};
        float[] depthTOpenGl = new float[16];
        Matrix.invertM(depthTOpenGl, 0, openGlTdepth, 0);
        float[] depthUp = new float[4];
        Matrix.multiplyMV(depthUp, 0, depthTOpenGl, 0, openGlUp, 0);
        // Create the plane matrix transform in depth frame from a point, the plane normal and the
        // up vector.
        float[] depthTplane = matrixFromPointNormalUp(point, normal, depthUp);
        float[] openGlTplane = new float[16];
        Matrix.multiplyMM(openGlTplane, 0, openGlTdepth, 0, depthTplane, 0);
        return openGlTplane;
    }

    /**
     * Calculates a transformation matrix based on a point, a normal and the up gravity vector.
     * The coordinate frame of the target transformation will a right handed system with Z+ in
     * the direction of the normal and Y+ up.
     */
    @Deprecated
    public static final float[] matrixFromPointNormalUp(double[] point, double[] normal, float[] up) {
        float[] zAxis = new float[]{(float) normal[0], (float) normal[1], (float) normal[2]};
        normalize(zAxis);
        float[] xAxis = crossProduct(up, zAxis);
        normalize(xAxis);
        float[] yAxis = crossProduct(zAxis, xAxis);
        normalize(yAxis);
        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[8] = zAxis[0];
        m[9] = zAxis[1];
        m[10] = zAxis[2];
        m[12] = (float) point[0];
        m[13] = (float) point[1];
        m[14] = (float) point[2];
        return m;
    }

    /**
     * Normalize a vector.
     */
    @Deprecated
    public static final void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    /**
     * Cross product between two vectors following the right hand rule.
     */
    @Deprecated
    public static final float[] crossProduct(float[] v1, float[] v2) {
        float[] result = new float[3];
        result[0] = v1[1] * v2[2] - v2[1] * v1[2];
        result[1] = v1[2] * v2[0] - v2[2] * v1[0];
        result[2] = v1[0] * v2[1] - v2[0] * v1[1];
        return result;
    }
}
