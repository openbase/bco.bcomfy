package org.openbase.bco.bcomfy.activityInit.measure;

import org.rajawali3d.math.vector.Vector3;

public class Plane {

    private Vector3 position;
    private Vector3 normal;

    public Plane (Vector3 position, Vector3 normal) {
        this.position = position;
        this.normal = normal;
    }

    public Plane (double[] position, double[] normal) {
        this.position = new Vector3(position[0], position[1], position[2]);
        this.normal = new Vector3(normal[0], normal[1], normal[2]);
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public void setNormal(Vector3 normal) {
        this.normal = normal;
    }
}
