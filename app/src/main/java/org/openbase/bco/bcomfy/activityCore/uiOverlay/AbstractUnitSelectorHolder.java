package org.openbase.bco.bcomfy.activityCore.uiOverlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.GenericFont;
import com.mikepenz.iconics.typeface.IIcon;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.NotAvailableException;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.util.concurrent.ExecutionException;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import rst.domotic.unit.UnitConfigType;
import rst.geometry.TranslationType;

public abstract class AbstractUnitSelectorHolder {
    private static final String TAG = AbstractUnitSelectorHolder.class.getSimpleName();

    private IIcon icon;
    private String id;
    private View unitSelector;
    private boolean isMainSelector;
    private Vector3 positionFromRoot;
    private int parentWidth;
    private int parentHeight;

    public AbstractUnitSelectorHolder(IIcon icon, UnitConfigType.UnitConfig unitConfig, boolean isMainSelector) throws NotAvailableException, InterruptedException, ExecutionException {
        this.icon = icon;
        this.id = unitConfig.getId();
        this.unitSelector = null;
        this.isMainSelector = isMainSelector;

        if (isMainSelector) {
            TranslationType.Translation unitPosition = unitConfig.getPlacementConfig().getPosition().getTranslation();
            Vector3d unitVector = new Vector3d(unitPosition.getX(), unitPosition.getY(), unitPosition.getZ());

            Transform3D transform3D = Registries.getLocationRegistry().getUnitTransformation(unitConfig).get().getTransform();
            transform3D.invert();
            transform3D.transform(unitVector);

            positionFromRoot = new Vector3(unitVector.x, unitVector.y, unitVector.z);
        }

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

    public Vector3 getPositionFromRoot() {
        return positionFromRoot;
    }

    public void setPositionFromRoot(Vector3 positionFromRoot) {
        this.positionFromRoot = positionFromRoot;
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

    public boolean isMainSelector() {
        return isMainSelector;
    }

    public void setIsMainSelector(boolean isMainSelector) {
        this.isMainSelector = isMainSelector;
    }

    public void alignViewToPixel(Context context, Matrix4 bcoToPixelTransform) {
        Vector3 pixelVector = bcoToPixelTransform.projectAndCreateVector(positionFromRoot);

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

    public void alignViewToParent(float x, float y) {
        throw new UnsupportedOperationException();
    }

    public void initIcon() {
        ImageView imageView = this.getUnitSelector().findViewById(R.id.iconView);
        imageView.setImageBitmap(new IconicsDrawable(imageView.getContext()).icon(icon).color(Color.BLACK).sizeDp(48).toBitmap());
    }
}
