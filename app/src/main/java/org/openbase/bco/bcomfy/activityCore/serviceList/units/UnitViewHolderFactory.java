package org.openbase.bco.bcomfy.activityCore.serviceList.units;

import android.app.Activity;
import android.view.ViewGroup;

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;

import rst.domotic.unit.UnitConfigType.UnitConfig;

public class UnitViewHolderFactory {

    private static final String TAG = UnitViewHolderFactory.class.getSimpleName();

    public static AbstractUnitViewHolder createUnitViewHolder(Activity activity, String unitId, ViewGroup parent) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);

        switch (unitConfig.getType()) {
            case COLORABLE_LIGHT:
            default:
                return new GenericUnitViewHolder(activity, unitConfig, parent);
        }
    }

}
