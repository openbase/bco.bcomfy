package org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.IIcon;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Remote;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;

import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;

public abstract class AbstractUnitSelectorHolder {
    private static final String TAG = AbstractUnitSelectorHolder.class.getSimpleName();

    private IIcon icon;
    private View view;
    private RelativeLayout circleShape;
    private boolean isMainSelector;
    private Vector3 positionFromRoot;
    private int parentWidth;
    private int parentHeight;

    private boolean selected;

    private UnitConfigType.UnitConfig unitConfig;
    private UnitRemote unitRemote;

    public AbstractUnitSelectorHolder(IIcon icon, UnitConfigType.UnitConfig unitConfig, boolean isMainSelector) throws InstantiationException, InterruptedException {
        try {
            this.icon = icon;
            this.unitConfig = unitConfig;
            this.isMainSelector = isMainSelector;

            if (isMainSelector) {
                updatePositionFromRoot();
            }

            this.parentWidth = 0;
            this.parentHeight = 0;

            unitRemote = Units.getFutureUnit(unitConfig, true).get(10, TimeUnit.SECONDS);
            unitRemote.addConfigObserver((observable, newUnitConfig) -> {
                this.unitConfig = (UnitConfigType.UnitConfig) newUnitConfig;
                this.updatePositionFromRoot();
            });
            unitRemote.addConnectionStateObserver((observable, connectionState) ->
                    updateConnectionState((Remote.ConnectionState) connectionState));
        } catch (CancellationException | ExecutionException | TimeoutException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public String getUnitHostId() {
        if (unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE) {
            return unitConfig.getId();
        }
        else {
            return unitConfig.getUnitHostId();
        }
    }

    public UnitConfigType.UnitConfig getCorrespondingUnitConfig() throws CouldNotPerformException, InterruptedException {
        if (unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE || !unitConfig.getBoundToUnitHost()) {
            return unitConfig;
        }
        else {
            return Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId());
        }
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
        circleShape = view.findViewById(R.id.circleShape);
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
                view.setVisibility(View.VISIBLE);
                view.setX((float) ((parentWidth/2)  + (parentWidth/2) *pixelVector.x - (view.getWidth()/2)));
                view.setY((float) ((parentHeight/2) - (parentHeight/2)*pixelVector.y - (view.getHeight()/2)));
            });
        }
        else {
            ((Activity) context).runOnUiThread(() -> {
                view.setVisibility(View.INVISIBLE);
            });
        }
    }

    public void alignViewToParent(float x, float y) {
        throw new UnsupportedOperationException();
    }

    public void initIcon() {
        ImageView imageView = this.getView().findViewById(R.id.iconView);
        imageView.setImageBitmap(new IconicsDrawable(imageView.getContext()).icon(icon).color(Color.BLACK).sizeDp(48).toBitmap());
    }

    public void setSelected(boolean selected) {
        this.selected = selected;

        if (selected) {
            circleShape.getBackground().setColorFilter(Color.parseColor("#FF88CC88"), PorterDuff.Mode.MULTIPLY);
        }
        else {
            circleShape.getBackground().clearColorFilter();
        }
    }

    public boolean isSelected() {
        return selected;
    }

    private void updateConnectionState(Remote.ConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTED:
                Log.i(TAG, "Connection to unit " + unitConfig.getId() + " established.");
                break;
            default:
                Log.i(TAG, "Connection to unit " + unitConfig.getId() + " lost.");
                break;
        }
    }

    private void updatePositionFromRoot() throws InterruptedException, TimeoutException, CouldNotPerformException {
        try {
            Point3d unitVector = new Point3d(0, 0, 0);

            Transform3D transform3D = Units.getUnitTransformation(unitConfig).get(10, TimeUnit.SECONDS).getTransform();
            transform3D.invert();
            transform3D.transform(unitVector);

            positionFromRoot = new Vector3(unitVector.x, unitVector.y, unitVector.z);
        } catch (CancellationException | NotAvailableException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not update position from root!", ex);
        }

    }
}
