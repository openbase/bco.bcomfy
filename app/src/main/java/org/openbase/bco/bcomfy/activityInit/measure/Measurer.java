package org.openbase.bco.bcomfy.activityInit.measure;

import android.util.Log;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;

public class Measurer {
    private static final String TAG = Measurer.class.getSimpleName();

    private MeasurerState measurerState;
    private Room currentRoom;
    private ArrayList<Room> roomList;

    public enum MeasurerState {
        INIT, MARK_GROUND, MARK_CEILING, MARK_WALLS, ENOUGH_WALLS
    }

    public enum Measurement {
        INVALID, GROUND, CEILING, WALL
    }

    public Measurer() {
        this.roomList = new ArrayList<>();
        this.measurerState = MeasurerState.INIT;
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

    public Measurement addPlaneMeasurement(Matrix4 measurement) {
        Vector3 translation = measurement.getTranslation();
        Vector3 normal = new Vector3(1, 0, 0);
        measurement.rotateVector(normal);

        Plane plane = new Plane(translation, normal);

        switch (measurerState) {
            case INIT:
                return Measurement.INVALID;
            case MARK_GROUND:
                currentRoom.setGround(plane);
                measurerState = MeasurerState.MARK_CEILING;
                return Measurement.GROUND;
            case MARK_CEILING:
                currentRoom.setCeiling(plane);
                measurerState = MeasurerState.MARK_WALLS;
                return Measurement.CEILING;
            case MARK_WALLS:
                currentRoom.addWall(plane);
                if (currentRoom.hasEnoughWalls())
                    measurerState = MeasurerState.ENOUGH_WALLS;
                return Measurement.WALL;
            case ENOUGH_WALLS:
                currentRoom.addWall(plane);
                return Measurement.WALL;
        }

        return Measurement.INVALID;
    }

    public Measurement addPlaneMeasurement(Plane plane) {
        Log.e(TAG, "Adding Plane: \nPosition: " + plane.getPosition().toString() + "\nNormal: " + plane.getNormal().toString());

        switch (measurerState) {
            case INIT:
                return Measurement.INVALID;
            case MARK_GROUND:
                currentRoom.setGround(plane);
                measurerState = MeasurerState.MARK_CEILING;
                return Measurement.GROUND;
            case MARK_CEILING:
                currentRoom.setCeiling(plane);
                measurerState = MeasurerState.MARK_WALLS;
                return Measurement.CEILING;
            case MARK_WALLS:
                currentRoom.addWall(plane);
                if (currentRoom.hasEnoughWalls())
                    measurerState = MeasurerState.ENOUGH_WALLS;
                return Measurement.WALL;
            case ENOUGH_WALLS:
                currentRoom.addWall(plane);
                return Measurement.WALL;
        }

        return Measurement.INVALID;
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

}
