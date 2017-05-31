package org.openbase.bco.bcomfy.activityCore.uiOverlay;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

public class UnitSelectorHolder {
    private static final String TAG = UnitSelectorHolder.class.getSimpleName();

    private String id;
    private View unitSelector;
    private Vector3 rootVector;
    private int parentWidth;
    private int parentHeight;

    public UnitSelectorHolder (String id, @Nullable View unitSelector, Vector3 rootVector) {
        this.id = id;
        this.unitSelector = unitSelector;
        this.rootVector = rootVector;
        this.parentWidth = 0;
        this.parentHeight = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public View getUnitSelector() {
        return unitSelector;
    }

    public void setUnitSelector(View unitSelector) {
        this.unitSelector = unitSelector;
    }

    public Vector3 getRootVector() {
        return rootVector;
    }

    public void setRootVector(Vector3 rootVector) {
        this.rootVector = rootVector;
    }

    public int getParentWidth() {
        return parentWidth;
    }

    public void setParentWidth(int parentWidth) {
        this.parentWidth = parentWidth;
    }

    public int getParentHeight() {
        return parentHeight;
    }

    public void setParentHeight(int parentHeight) {
        this.parentHeight = parentHeight;
    }

    public void alignViewToPixel(Context context, Matrix4 bcoToPixelTransform) {
        Vector3 pixelVector = bcoToPixelTransform.projectAndCreateVector(rootVector);

        if (pixelVector.x > -1 &&
                pixelVector.x < 1 &&
                pixelVector.y > -1 &&
                pixelVector.y < 1 &&
                pixelVector.z < 1) {
            ((Activity) context).runOnUiThread(() -> {
                unitSelector.setVisibility(View.VISIBLE);
                unitSelector.setX((float) ((parentWidth/2)  + (parentWidth/2) *pixelVector.x - (unitSelector.getWidth()/2)));
                unitSelector.setY((float) ((parentHeight/2) - (parentHeight/2)*pixelVector.y - (unitSelector.getHeight()/2)));
            });
        }
        else {
            ((Activity) context).runOnUiThread(() -> {
                unitSelector.setVisibility(View.INVISIBLE);
            });
        }
    }
}
