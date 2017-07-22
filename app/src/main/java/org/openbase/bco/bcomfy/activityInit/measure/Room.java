package org.openbase.bco.bcomfy.activityInit.measure;

import android.util.Log;

import org.openbase.bco.bcomfy.utils.MathUtils;
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

    public void addWallMeasurement(Plane measurement) {
        // Add new wall if it is the first wall, or the previous one has enough measurements
        if (walls.size() == 0) {
            walls.add(new Wall(measurementsPerWall));
        }
        else if (getCurrentWall().isFinished()) {
            walls.add(new Wall(measurementsPerWall));
        }

        // Add measurement to current wall
        getCurrentWall().addMeasurement(measurement);
    }

    public Wall getCurrentWall() {
        return walls.get(walls.size() - 1);
    }

    public Plane[] getWalls() {
        Plane[] wallArray = new Plane[walls.size()];
        return walls.toArray(wallArray);
    }

    public int getWallCount() {
        return walls.size();
    }

    public void removeRecentWall() {
        walls.remove(walls.size() - 1);
    }

    public boolean isInFinishedState() {
        if (walls.size() > 3) {
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

    public void finish() {
        Log.e(TAG, "Finishing Room with " + walls.size() + " walls.");
        for (int i = 0; i < walls.size() - 1; i++) {
            groundVertices.add(calcGroundIntersection(walls.get(i).getFinishedWall(), walls.get(i+1).getFinishedWall()));
            ceilingVertices.add(calcCeilingIntersection(walls.get(i).getFinishedWall(), walls.get(i+1).getFinishedWall()));
        }

        groundVertices.add(calcGroundIntersection(walls.get(walls.size()-1).getFinishedWall(), walls.get(0).getFinishedWall()));
        ceilingVertices.add(calcCeilingIntersection(walls.get(walls.size()-1).getFinishedWall(), walls.get(0).getFinishedWall()));
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
}
