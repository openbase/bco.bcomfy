package org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder;

import android.util.Log;

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import rst.domotic.unit.UnitConfigType;

public class SelectorHolderFactory {

    private static final String TAG = SelectorHolderFactory.class.getSimpleName();


    public static AbstractUnitSelectorHolder createUnitSelectorHolder(UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        AbstractUnitSelectorHolder unitSelectorHolder;

        switch (unitConfig.getUnitType()) {
            case DEVICE:
                if (unitConfig.getDeviceConfig().getUnitIdCount() > 1) {
                    unitSelectorHolder = createUnitSelectorHolderGroup(unitConfig);
                }
                else {
                    unitSelectorHolder =
                            createUnitSelectorHolder(Registries.getUnitRegistry().getUnitConfigById(
                                    unitConfig.getDeviceConfig().getUnitId(0)));
                }
                break;
            case COLORABLE_LIGHT:
                unitSelectorHolder = new ColorableLightSelectorHolder(unitConfig, true);
                break;
            default:
                unitSelectorHolder = new GenericSelectorHolder(unitConfig, true);
                break;
        }

        return unitSelectorHolder;
    }

    public static AbstractUnitSelectorHolder createUnitSelectorHolderGroup(UnitConfigType.UnitConfig unitConfig) throws InstantiationException, InterruptedException {
        Log.e(TAG, "unitSelectorHolderGroup not supported yet! Falling back to generic device visualization...");
        return new GenericSelectorHolder(unitConfig, true);
    }
}
