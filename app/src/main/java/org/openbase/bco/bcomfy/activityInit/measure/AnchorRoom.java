package org.openbase.bco.bcomfy.activityInit.measure;

import android.util.Log;

import org.openbase.bco.bcomfy.utils.MathUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.vector.Vector3;

public class AnchorRoom extends Room {
    private static final String TAG = AnchorRoom.class.getSimpleName();

    private int measurementsPerWallAnchor;
    private boolean anchorFinished;
    private Vector3[] anchorNormals;

    public AnchorRoom(int measurementsPerWall, int measurementsPerWallAnchor) {
        super(measurementsPerWall);
        this.measurementsPerWallAnchor = measurementsPerWallAnchor;
        anchorFinished = false;
        anchorNormals = new Vector3[4];
    }

    @Override
    public boolean addWallMeasurement(Plane measurement) {
        // First check, whether the measurement is valid which means that its normal needs to differ
        // from the previous one
        if (!isValidMeasurement(measurement)) return false;

        // Add new anchorWall if it is the first wall, or the previous one has enough measurements
        if (walls.size() == 0) {
            walls.add(new Wall(measurementsPerWallAnchor));
        }

        // Otherwise check if the anchor is finished to decide which kind of wall is needed
        else if (getCurrentWall().isFinished()) {
            if (anchorFinished) {
                walls.add(new Wall(measurementsPerWall));
            }
            else {
                walls.add(new Wall(measurementsPerWallAnchor));
            }
        }

        boolean wallIsFinished = false;

        try {
            // Add measurement to current wall
            wallIsFinished = getCurrentWall().addMeasurement(measurement);

            // Calculate anchor normals if wall is the second finished one
            if (wallIsFinished && walls.size() == 2) {
                rectifyAndFinishAnchor();
            }
        } catch (CouldNotPerformException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return true;
    }

    public boolean isAnchorFinished() {
        return anchorFinished;
    }

    public Vector3[] getAnchorNormals() {
        return anchorNormals;
    }

    private void rectifyAndFinishAnchor() {
        Log.i(TAG, "rectifyAndFinishAnchor");
        Vector3 firstWallNormal = walls.get(0).getFinishedWall().getNormal();
        Vector3 secondWallNormal = walls.get(1).getFinishedWall().getNormal();

        // Calculate the difference between the angles of the anchor normals
        double angle = MathUtils.clockwiseAngle(secondWallNormal, firstWallNormal);
        double angleDifference;
        if (angle > 0.0) {
            angleDifference = (Math.toRadians(90.0) - angle) / 2.0;
        } else {
            angleDifference = (-Math.toRadians(90.0) - angle) / 2.0;
        }

        // Rotate the anchor normals to make them perpendicular
        firstWallNormal.rotateY(-angleDifference);
        secondWallNormal.rotateY(angleDifference);

        anchorNormals[0] = firstWallNormal.clone();
        anchorNormals[1] = secondWallNormal.clone();
        anchorNormals[2] = firstWallNormal.clone().inverse();
        anchorNormals[3] = secondWallNormal.clone().inverse();

        Log.d(TAG, "Anchor set to:\n" + anchorNormals[0].toString() + "\n" +
                anchorNormals[1].toString() + "\n" +
                anchorNormals[2].toString() + "\n" +
                anchorNormals[3].toString());

        walls.get(0).getFinishedWall().setNormal(firstWallNormal.clone());
        walls.get(1).getFinishedWall().setNormal(secondWallNormal.clone());

        anchorFinished = true;
    }

    @Override
    public void undoLastMeasurement() throws CouldNotPerformException {
        super.undoLastMeasurement();

        if (anchorFinished) {
            if (!walls.get(1).isFinished()) {
                anchorFinished = false;
            }
        }
    }

    @Override
    public int getNeededMeasurementCount() {
        if (anchorFinished) {
            return measurementsPerWall;
        }
        else {
            return measurementsPerWallAnchor;
        }
    }
}
