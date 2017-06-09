package org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder;

import android.graphics.Color;
import android.widget.ImageView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.uiOverlay.AbstractUnitSelectorHolder;
import org.openbase.jul.exception.NotAvailableException;
import org.rajawali3d.math.vector.Vector3;

import java.util.concurrent.ExecutionException;

import rst.domotic.unit.UnitConfigType;

public class ColorableLightSelectorHolder extends AbstractUnitSelectorHolder {

    public ColorableLightSelectorHolder(UnitConfigType.UnitConfig unitConfig, boolean isMainSelector) throws InterruptedException, ExecutionException, NotAvailableException {
        super(unitConfig, isMainSelector);
    }

    @Override
    public void initIcon() {
        ImageView imageView = this.getUnitSelector().findViewById(R.id.iconView);
        imageView.setImageBitmap(new IconicsDrawable(imageView.getContext()).icon(GoogleMaterial.Icon.gmd_lightbulb_outline).color(Color.BLACK).sizeDp(24).toBitmap());
    }
}
