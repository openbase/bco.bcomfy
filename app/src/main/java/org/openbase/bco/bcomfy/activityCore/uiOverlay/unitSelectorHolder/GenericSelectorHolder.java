package org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.openbase.type.domotic.unit.UnitConfigType;

public class GenericSelectorHolder extends AbstractUnitSelectorHolder {

    public GenericSelectorHolder(UnitConfigType.UnitConfig unitConfig, boolean isMainSelector) throws InterruptedException, InstantiationException {
        super(GoogleMaterial.Icon.gmd_settings, unitConfig, isMainSelector);
    }
}
