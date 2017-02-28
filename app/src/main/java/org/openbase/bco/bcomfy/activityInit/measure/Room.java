package org.openbase.bco.bcomfy.activityInit.measure;

import org.rajawali3d.math.Matrix4;

import java.util.ArrayList;

public class Room {

    Matrix4 ground;
    Matrix4 ceiling;
    ArrayList<Matrix4> walls;

    public Room() {
        this.walls = new ArrayList<>();
    }

    public void setGround(Matrix4 ground) {
        this.ground = ground;
    }

    public Matrix4 getGround() {
        return ground;
    }

    public void setCeiling(Matrix4 ceiling) {
        this.ceiling = ceiling;
    }

    public Matrix4 getCeiling() {
        return ceiling;
    }

    public void addWall(Matrix4 wall) {
        walls.add(wall);
    }

    public Matrix4[] getWalls() {
        Matrix4[] wallArray = new Matrix4[walls.size()];
        return walls.toArray(wallArray);
    }

}
