package org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import org.openbase.bco.bcomfy.activityCore.uiOverlay.AbstractUnitSelectorHolder;
import org.openbase.jul.exception.NotAvailableException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import rst.domotic.unit.UnitConfigType;

public class GenericSelectorHolder extends AbstractUnitSelectorHolder {

    public GenericSelectorHolder(UnitConfigType.UnitConfig unitConfig, boolean isMainSelector) throws InterruptedException, ExecutionException, NotAvailableException, TimeoutException {
        super(GoogleMaterial.Icon.gmd_help_outline, unitConfig, isMainSelector);
    }
}
