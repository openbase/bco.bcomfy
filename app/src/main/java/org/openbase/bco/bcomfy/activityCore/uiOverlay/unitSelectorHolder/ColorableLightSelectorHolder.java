package org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import rst.domotic.unit.UnitConfigType;

public class ColorableLightSelectorHolder extends AbstractUnitSelectorHolder {

    public ColorableLightSelectorHolder(UnitConfigType.UnitConfig unitConfig, boolean isMainSelector) throws InterruptedException, InstantiationException {
        super(GoogleMaterial.Icon.gmd_lightbulb_outline, unitConfig, isMainSelector);
    }
}
