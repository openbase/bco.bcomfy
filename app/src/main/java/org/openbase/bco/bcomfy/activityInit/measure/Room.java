package org.openbase.bco.bcomfy.activityInit.measure;

import android.util.Log;

import org.openbase.bco.bcomfy.utils.MathUtils;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;

public class Room {
    private static final String TAG = Room.class.getSimpleName();

    private Plane ground;
    private Plane ceiling;
    private ArrayList<Plane> walls;
    private ArrayList<Vector3> groundVertices;
    private ArrayList<Vector3> ceilingVertices;

    public Room() {
        this.walls = new ArrayList<>();
        this.groundVertices = new ArrayList<>();
        this.ceilingVertices = new ArrayList<>();
    }

    public void setGround(Plane ground) {
        this.ground = ground;
    }

    public Plane getGround() {
        return ground;
    }

    public void setCeiling(Plane ceiling) {
        this.ceiling = ceiling;
    }

    public Plane getCeiling() {
        return ceiling;
    }

    public void addWall(Plane wall) {
        walls.add(wall);
    }

    public Plane[] getWalls() {
        Plane[] wallArray = new Plane[walls.size()];
        return walls.toArray(wallArray);
    }

    public boolean hasEnoughWalls() {
        return walls.size() > 3; //TODO: makeshift heuristic
    }

    public void finish() {
        Log.e(TAG, "Finishing Room with " + walls.size() + " walls.");
        for (int i = 0; i < walls.size() - 1; i++) {
            groundVertices.add(calcGroundIntersection(walls.get(i), walls.get(i+1)));
            ceilingVertices.add(calcCeilingIntersection(walls.get(i), walls.get(i+1)));
        }

        groundVertices.add(calcGroundIntersection(walls.get(walls.size()-1), walls.get(0)));
        ceilingVertices.add(calcCeilingIntersection(walls.get(walls.size()-1), walls.get(0)));
    }

    public ArrayList<Vector3> getCeilingVertices() {
        return ceilingVertices;
    }

    public ArrayList<Vector3> getGroundVertices() {
        return groundVertices;
    }

    public Vector3 getZeroPoint() {
        if (groundVertices.isEmpty()) {
            return MathUtils.calc3PlaneIntersection(walls.get(0).getPosition(), walls.get(0).getNormal(),
                    walls.get(1).getPosition(), walls.get(1).getNormal(),
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
