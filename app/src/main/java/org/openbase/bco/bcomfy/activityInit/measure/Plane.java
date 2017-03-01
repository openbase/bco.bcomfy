package org.openbase.bco.bcomfy.activityInit.measure;

import org.rajawali3d.math.vector.Vector3;

public class Plane {

    private Vector3 translation;
    private Vector3 normal;

    public Plane (Vector3 translation, Vector3 normal) {
        this.translation = translation;
        this.normal = normal;
    }

    public Vector3 getTranslation() {
        return translation;
    }

    public void setTranslation(Vector3 translation) {
        this.translation = translation;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public void setNormal(Vector3 normal) {
        this.normal = normal;
    }
}
