package org.openbase.bco.bcomfy.activityInit.measure;

import android.util.Log;

import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.utils.MathUtils;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;

import java8.util.stream.StreamSupport;

public class Measurer {
    private static final String TAG = Measurer.class.getSimpleName();

    private static final Vector3 GROUND_NORMAL  = new Vector3(0.0,  1.0, 0.0);
    private static final Vector3 CEILING_NORMAL = new Vector3(0.0, -1.0, 0.0);

    private MeasurerState measurerState;
    private Room currentRoom;
    private ArrayList<Room> roomList;
    private AnchorState anchorState;
    private Vector3[] anchorNormals;
    private ArrayList<Plane> currentWall;
    private int measurementsPerWallDefault;
    private int measurementsPerWallAnchor;
    private boolean alignToAnchor;
    private int currentWallMeasurements;
    private double[] glToBcoTransform;
    private double[] bcoToGlTransform;

    public enum MeasurerState {
        INIT, MARK_GROUND, MARK_CEILING, MARK_WALLS, ENOUGH_WALLS
    }

    public enum AnchorState {
        UNSET, FIRST_ANCHOR_SET, FINISHED
    }

    public enum MeasureType {
        INVALID, GROUND, CEILING, WALL
    }

    public Measurer(int measurementsPerWallDefault, boolean alignToAnchor, int measurementsPerWallAnchor, boolean recalcTransform) {
        measurerState = MeasurerState.INIT;
        roomList = new ArrayList<>();
        anchorState = AnchorState.UNSET;
        anchorNormals = new Vector3[4];
        currentWall = new ArrayList<>();
        this.measurementsPerWallDefault = measurementsPerWallDefault;
        this.measurementsPerWallAnchor = measurementsPerWallAnchor;
        this.alignToAnchor = alignToAnchor;
        currentWallMeasurements = 0;

        if (recalcTransform) {
            startNewRoom();
        }
    }

    public void startNewRoom() {
        currentRoom = new Room();
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
                addPlaneToWall(plane);
                if (currentRoom.hasEnoughWalls())
                    measurerState = MeasurerState.ENOUGH_WALLS;
                return MeasureType.WALL;
            case ENOUGH_WALLS:
                addPlaneToWall(plane);
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

    private void addPlaneToWall(Plane plane) {
        plane.setNormal(new Vector3(plane.getNormal().x, 0.0, plane.getNormal().z));

        currentWall.add(plane);
        currentWallMeasurements++;

        if (anchorState == AnchorState.FINISHED) {
            if (currentWall.size() >= measurementsPerWallDefault) {
                finishWall();
            }
        } else {
            if (currentWall.size() >= measurementsPerWallAnchor) {
                finishWall();
            }
        }
    }

    private void finishWall() {
        Vector3 meanPosition = new Vector3();
        Vector3 meanNormal = new Vector3();

        StreamSupport.stream(currentWall).forEach(plane -> {
            meanPosition.add(plane.getPosition());
            meanNormal.add(plane.getNormal());
        });

        meanPosition.divide(currentWallMeasurements);
        meanNormal.normalize();

        if (alignToAnchor && anchorState == AnchorState.FINISHED) {
            align(meanNormal);
        }

        currentRoom.addWall(new Plane(meanPosition, meanNormal));
        currentWall.clear();
        currentWallMeasurements = 0;

        if (anchorState == AnchorState.UNSET) {
            anchorState = AnchorState.FIRST_ANCHOR_SET;
        } else if (anchorState == AnchorState.FIRST_ANCHOR_SET) {
            rectifyAndFinishAnchor();
            initTransforms();
            anchorState = AnchorState.FINISHED;
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

    private void rectifyAndFinishAnchor() {
        Vector3 firstWallNormal = currentRoom.getWalls()[0].getNormal();
        Vector3 secondWallNormal = currentRoom.getWalls()[1].getNormal();

        double angle = MathUtils.clockwiseAngle(secondWallNormal, firstWallNormal);
        double angleDifference;
        if (angle > 0.0) {
            angleDifference = (Math.toRadians(90.0) - angle) / 2.0;
        } else {
            angleDifference = (-Math.toRadians(90.0) - angle) / 2.0;
        }

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

    public double[] getGlToBcoTransform() {
        return glToBcoTransform;
    }

    public double[] getBcoToGlTransform() {
        return bcoToGlTransform;
    }

    public AnchorState getAnchorState() {
        return anchorState;
    }
}
