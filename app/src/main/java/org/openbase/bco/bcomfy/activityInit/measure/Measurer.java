package org.openbase.bco.bcomfy.activityInit.measure;

import org.rajawali3d.math.Matrix4;

import java.util.ArrayList;

public class Measurer {

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
        roomList.add(currentRoom);
        measurerState = MeasurerState.INIT;
    }

    public Measurement addPlaneMeasurement(Matrix4 measurement) {
        switch (measurerState) {
            case INIT:
                return Measurement.INVALID;
            case MARK_GROUND:
                measurerState = MeasurerState.MARK_CEILING;
                return Measurement.GROUND;
            case MARK_CEILING:
                measurerState = MeasurerState.MARK_WALLS;
                return Measurement.CEILING;
            case MARK_WALLS:
                return Measurement.WALL;
            case ENOUGH_WALLS:
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

}
