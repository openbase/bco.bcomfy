package org.openbase.bco.bcomfy.activityInit.measure;

import android.util.Log;

import org.openbase.bco.bcomfy.utils.MathUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.VerificationFailedException;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;

public class Room {
    private static final String TAG = Room.class.getSimpleName();

    private Plane ground;
    private Plane ceiling;
    private ArrayList<Vector3> groundVertices;
    private ArrayList<Vector3> ceilingVertices;
    protected ArrayList<Wall> walls;
    protected int measurementsPerWall;

    public Room(int measurementsPerWall) {
        this.walls = new ArrayList<>();
        this.groundVertices = new ArrayList<>();
        this.ceilingVertices = new ArrayList<>();
        this.measurementsPerWall = measurementsPerWall;
    }

    public void setGround(Plane ground) {
        this.ground = ground;
    }

    public void clearGround() {
        ground = null;
    }

    public Plane getGround() {
        return ground;
    }

    public void setCeiling(Plane ceiling) {
        this.ceiling = ceiling;
    }

    public void clearCeiling() {
        ceiling = null;
    }

    public Plane getCeiling() {
        return ceiling;
    }

    public boolean addWallMeasurement(Plane measurement) {
        // First check, whether the measurement is valid which means that its normal needs to differ
        // from the previous one
        if (!isValidMeasurement(measurement)) return false;

        // Add new wall if it is the first wall, or the previous one has enough measurements
        if (walls.size() == 0) {
            walls.add(new Wall(measurementsPerWall));
        }
        else if (getCurrentWall().isFinished()) {
            walls.add(new Wall(measurementsPerWall));
        }

        // Add measurement to current wall
        try {
            getCurrentWall().addMeasurement(measurement);
        } catch (CouldNotPerformException e) {
            Log.e(TAG, "Error while adding measurement" + "\n" + Log.getStackTraceString(e));
        }

        return true;
    }

    protected boolean isValidMeasurement(Plane measurement) {
        Wall latestFinishedWall = getLatestFinishedWall();

        // Measurement can not be invalid if there is no finished wall yet
        if (latestFinishedWall == null) {
            return true;
        }

        // Measurement is invalid if the normal is too similar to the normal of the previous wall
        return !(Math.abs(latestFinishedWall.getFinishedWall().getNormal().dot(measurement.getNormal())) > 0.8);
    }

    public Wall getCurrentWall() {
        return walls.get(walls.size() - 1);
    }

    private Wall getLatestFinishedWall() {
        if (walls.size() == 0) {
            return null;
        }
        else if (getCurrentWall().isFinished()) {
            return getCurrentWall();
        }
        else if (walls.size() > 1) {
            return walls.get(walls.size() - 2);
        }
        else {
            return null;
        }
    }

    public boolean isFinishable() {
        if (walls.size() > 3 && ground != null && ceiling != null) {
            for (Wall wall : walls) {
                if (!wall.isFinished()) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    public void finish() throws CouldNotPerformException {
        if (verifyAlignmentOfLastWall()) {
            Log.i(TAG, "Finishing Room with " + walls.size() + " walls.");
            for (int i = 0; i < walls.size() - 1; i++) {
                groundVertices.add(calcGroundIntersection(walls.get(i).getFinishedWall(), walls.get(i+1).getFinishedWall()));
                ceilingVertices.add(calcCeilingIntersection(walls.get(i).getFinishedWall(), walls.get(i+1).getFinishedWall()));
            }

            groundVertices.add(calcGroundIntersection(walls.get(walls.size()-1).getFinishedWall(), walls.get(0).getFinishedWall()));
            ceilingVertices.add(calcCeilingIntersection(walls.get(walls.size()-1).getFinishedWall(), walls.get(0).getFinishedWall()));
        }
        else {
            throw new VerificationFailedException("Can not finish the room, since the first and the last wall are not rectangular to each other and this would result in an invalid shape");
        }

    }

    public ArrayList<Vector3> getCeilingVertices() {
        return ceilingVertices;
    }

    public ArrayList<Vector3> getGroundVertices() {
        return groundVertices;
    }

    public Vector3 getZeroPoint() {
        if (groundVertices.isEmpty()) {
            return MathUtils.calc3PlaneIntersection(walls.get(0).getFinishedWall().getPosition(), walls.get(0).getFinishedWall().getNormal(),
                    walls.get(1).getFinishedWall().getPosition(), walls.get(1).getFinishedWall().getNormal(),
                    ground.getPosition(), ground.getNormal());
        }
        else {
            return groundVertices.get(0);
        }
    }

    private boolean verifyAlignmentOfLastWall() {
        try {
            Plane firstWall = walls.get(0).getFinishedWall();
            Plane lastWall = getLatestFinishedWall().getFinishedWall();

            return !(Math.abs(firstWall.getNormal().dot(lastWall.getNormal())) > 0.8);
        }
        catch (NullPointerException ex) {
            return false;
        }
    }

    private Vector3 calcGroundIntersection(Plane wall0, Plane wall1) {
        return MathUtils.calc3PlaneIntersection(wall0.getPosition(), wall0.getNormal(),
                wall1.getPosition(), wall1.getNormal(),
                ground.getPosition(), ground.getNormal());
    }

    private Vector3 calcCeilingIntersection(Plane wall0, Plane wall1) {
        return MathUtils.calc3PlaneIntersection(wall0.getPosition(), wall0.getNormal(),
                wall1.getPosition(), wall1.getNormal(),
                ceiling.getPosition(), ceiling.getNormal());
    }

    public void undoLastMeasurement() throws CouldNotPerformException {
        if (walls.size() == 0) {
            if (ceiling != null) {
                clearCeiling();
            }
            else if (ground != null) {
                clearGround();
            }
            else {
                throw new CouldNotPerformException("Not able to undo measurement. This room does not contain any measurement!");
            }
        }
        else {
            getCurrentWall().removeRecentMeasurement();

            if (getCurrentWall().getMeasurementCount() == 0) {
                walls.remove(getCurrentWall());
            }
        }
    }

    public Measurer.MeasurerState getMeasurerState() {
        if (ground == null) {
            return Measurer.MeasurerState.MARK_GROUND;
        }
        if (ceiling == null) {
            return Measurer.MeasurerState.MARK_CEILING;
        }
        if (isFinishable()) {
            return Measurer.MeasurerState.ENOUGH_WALLS;
        }
        return Measurer.MeasurerState.MARK_WALLS;
    }

    public int getCurrentFinishedWallCount() {
        if (walls.isEmpty()) return 0;

        int currentFinishedWallCount = walls.size();
        if (!getCurrentWall().isFinished()) {
            currentFinishedWallCount--;
        }
        return currentFinishedWallCount;
    }

    public int getNeededFinishedWallCount() {
        return 4;
    }

    public int getCurrentMeasurementCount() {
        if (walls.isEmpty()) {
            return 0;
        }
        else if (getCurrentWall().isFinished()) {
            return 0;
        }
        else {
            return getCurrentWall().getMeasurementCount();
        }
    }

    public int getNeededMeasurementCount() {
        return measurementsPerWall;
    }
}
