package org.openbase.bco.bcomfy.activityInit.measure;

import android.util.Log;

import com.projecttango.tangosupport.TangoSupport;

import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;

public class Measurer {
    private static final String TAG = Measurer.class.getSimpleName();

    private static final Vector3 GROUND_NORMAL  = new Vector3(0.0,  1.0, 0.0);
    private static final Vector3 CEILING_NORMAL = new Vector3(0.0, -1.0, 0.0);

    private MeasurerState measurerState;
    private ArrayList<Room> roomList;
    private Room currentRoom;

    private AnchorRoom anchorRoom;
    private Vector3[] anchorNormals;

    private int measurementsPerWallDefault;
    private int measurementsPerWallAnchor;
    private boolean alignToAnchor;

    private boolean transformIsInit;
    private double[] glToBcoTransform;
    private double[] bcoToGlTransform;

    public enum MeasurerState {
        INIT, MARK_GROUND, MARK_CEILING, MARK_WALLS, ENOUGH_WALLS
    }

    public enum MeasureType {
        INVALID, GROUND, CEILING, WALL
    }

    public Measurer(int measurementsPerWallDefault, boolean alignToAnchor, int measurementsPerWallAnchor, boolean recalcTransform) {
        measurerState = MeasurerState.INIT;
        roomList = new ArrayList<>();

        anchorNormals = new Vector3[4];

        transformIsInit = false;

        this.measurementsPerWallDefault = measurementsPerWallDefault;
        this.measurementsPerWallAnchor = measurementsPerWallAnchor;
        this.alignToAnchor = alignToAnchor;

        if (recalcTransform) {
            startNewRoom();
        }
    }

    public void startNewRoom() {
        if (alignToAnchor && anchorRoom == null) {
            anchorRoom = new AnchorRoom(measurementsPerWallDefault, measurementsPerWallAnchor);
            currentRoom = anchorRoom;
        }
        else {
            currentRoom = new Room(measurementsPerWallDefault);
        }

        measurerState = MeasurerState.MARK_GROUND;
    }

    public void finishRoom() {
        currentRoom.finish();
        roomList.add(currentRoom);
        currentRoom = null;
        measurerState = MeasurerState.INIT;
    }

    public MeasureType addPlaneMeasurement(Plane plane) {
        switch (measurerState) {
            case INIT:
                return MeasureType.INVALID;
            case MARK_GROUND:
                plane.setNormal(new Vector3(GROUND_NORMAL));
                currentRoom.setGround(plane);
                measurerState = MeasurerState.MARK_CEILING;
                return MeasureType.GROUND;
            case MARK_CEILING:
                plane.setNormal(new Vector3(CEILING_NORMAL));
                currentRoom.setCeiling(plane);
                measurerState = MeasurerState.MARK_WALLS;
                return MeasureType.CEILING;
            case MARK_WALLS:
            case ENOUGH_WALLS:
                addWallMeasurement(plane);
                measurerState = currentRoom.getMeasurerState();
                return MeasureType.WALL;
        }

        return MeasureType.INVALID;
    }

    public MeasurerState getMeasurerState() {
        return measurerState;
    }

    public boolean hasFinishedRoom() {
        return roomList.size() > 0;
    }

    public ArrayList<Vector3> getLatestGroundVertices() {
        return roomList.get(roomList.size()-1).getGroundVertices();
    }

    public ArrayList<Vector3> getLatestCeilingVertices() {
        return roomList.get(roomList.size()-1).getCeilingVertices();
    }

    private void addWallMeasurement(Plane plane) {
        // Use the constraint that walls are perpendicular to the ground
        plane.setNormal(new Vector3(plane.getNormal().x, 0.0, plane.getNormal().z));

        // Align the normal to the anchor if necessary/possible
        if (alignToAnchor && anchorRoom.isAnchorFinished()) {
            Vector3 alignedNormal = plane.getNormal();
            align(alignedNormal);
            plane.setNormal(alignedNormal);
        }

        // Add the measurement to the current room
        currentRoom.addWallMeasurement(plane);

        // Check whether the anchor normals are finished
        if (alignToAnchor) {
            if (anchorRoom.isAnchorFinished() && !transformIsInit) {
                anchorNormals = anchorRoom.getAnchorNormals();
                initTransforms();
            }
        }
    }

    private void align(Vector3 normal) {
        int smallestAngleIndex = -1;
        double smallestAngle = Double.MAX_VALUE;

        for (int i = 0; i < anchorNormals.length; i++) {
            double currentAngle = normal.angle(anchorNormals[i]);
            Log.i(TAG, "Alignment in process... angle for normal " + i + ": " + currentAngle);

            if (currentAngle < smallestAngle) {
                smallestAngle = currentAngle;
                smallestAngleIndex = i;
            }
        }

        Log.i(TAG, "Aligning normal:\n" + normal.toString() +
                "\nto anchor normal:\n" + anchorNormals[smallestAngleIndex].toString() +
                "\nby an angle of:\n" + smallestAngle);
        normal.setAll(anchorNormals[smallestAngleIndex]);
    }

    private void initTransforms() {
        Vector3 anchorPoint = currentRoom.getZeroPoint();

        double[][] glPoints = { {anchorPoint.x, anchorPoint.y, anchorPoint.z} ,
                {anchorPoint.x + anchorNormals[1].x, anchorPoint.y + anchorNormals[1].y, anchorPoint.z + anchorNormals[1].z} ,
                {anchorPoint.x + anchorNormals[0].x, anchorPoint.y + anchorNormals[0].y, anchorPoint.z + anchorNormals[0].z} ,
                {anchorPoint.x + GROUND_NORMAL.x, anchorPoint.y + GROUND_NORMAL.y, anchorPoint.z + GROUND_NORMAL.z} };

        double[][] bcoPoints = { {0.0, 0.0, 0.0} ,
                                {1.0, 0.0, 0.0} ,
                                {0.0, 1.0, 0.0} ,
                                {0.0, 0.0, 1.0} };

        glToBcoTransform = TangoSupport.findCorrespondenceSimilarityTransform(glPoints,  bcoPoints);
        bcoToGlTransform = new Matrix4(glToBcoTransform).inverse().getDoubleValues();
    }

    public void undoLastMeasurement() throws CouldNotPerformException {
        if (currentRoom == null) {
            throw new CouldNotPerformException("Not able to undo measurement. Current room is null.");
        }
        else {
            currentRoom.undoLastMeasurement();
            measurerState = currentRoom.getMeasurerState();
        }
    }

    public double[] getGlToBcoTransform() {
        return glToBcoTransform;
    }

    public double[] getBcoToGlTransform() {
        return bcoToGlTransform;
    }

    public boolean isAnchorFinished() {
        if (anchorRoom == null) {
            return false;
        }
        else {
            return anchorRoom.isAnchorFinished();
        }
    }

    public int getCurrentFinishedWallCount() {
        if (currentRoom == null) {
            return -1;
        }
        else {
            return currentRoom.getCurrentFinishedWallCount();
        }
    }

    public int getNeededFinishedWallCount() {
        if (currentRoom == null) {
            return -1;
        }
        else {
            return currentRoom.getNeededFinishedWallCount();
        }
    }

    public int getCurrentMeasurementCount() {
        if (currentRoom == null) {
            return -1;
        }
        else {
            return currentRoom.getCurrentMeasurementCount();
        }
    }

    public int getNeededMeasurementCount() {
        if (currentRoom == null) {
            return -1;
        }
        else {
            return currentRoom.getNeededMeasurementCount();
        }
    }
}
