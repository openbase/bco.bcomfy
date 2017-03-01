package org.openbase.bco.bcomfy.activityInit.measure;

import android.os.Build;
import android.renderscript.Matrix3f;
import android.util.Log;

import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
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

    private Vector3 calcGroundIntersection(Plane wall0, Plane wall1) {
        return calc3PlaneIntersection(wall0.getTranslation(), wall0.getNormal(),
                wall1.getTranslation(), wall1.getNormal(),
                ground.getTranslation(), ground.getNormal());
    }

    private Vector3 calcCeilingIntersection(Plane wall0, Plane wall1) {
        return calc3PlaneIntersection(wall0.getTranslation(), wall0.getNormal(),
                wall1.getTranslation(), wall1.getNormal(),
                ceiling.getTranslation(), ceiling.getNormal());
    }


    /**
     * Calculate the intersection of three planes.
     * See http://mathworld.wolfram.com/Plane-PlaneIntersection.html
     */
    private Vector3 calc3PlaneIntersection(Vector3 x0, Vector3 n0,
                                           Vector3 x1, Vector3 n1,
                                           Vector3 x2, Vector3 n2) {

        double det = (n0.x * n1.y * n2.z) + (n1.x * n2.y * n0.z) + (n2.x * n0.y * n1.z) -
                     (n0.z * n1.y * n2.x) - (n1.z * n2.y * n0.x) - (n2.z * n0.y * n1.x);

        Vector3 v0 = n1.cross(n2).multiply(x0.dot(n0));
        Vector3 v1 = n2.cross(n0).multiply(x1.dot(n1));
        Vector3 v2 = n0.cross(n1).multiply(x2.dot(n2));

        Vector3 result = v0.add(v1).add(v2).multiply(1/det);

        return result;
    }

}
