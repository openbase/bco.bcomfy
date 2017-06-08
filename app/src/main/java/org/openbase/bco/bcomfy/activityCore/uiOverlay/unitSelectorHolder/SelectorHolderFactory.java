package org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder;

import org.openbase.bco.bcomfy.activityCore.uiOverlay.AbstractUnitSelectorHolder;
import org.rajawali3d.math.vector.Vector3;

import rst.domotic.unit.UnitConfigType;

public class SelectorHolderFactory {

    private static final String TAG = SelectorHolderFactory.class.getSimpleName();

    public static AbstractUnitSelectorHolder createUnitSelectorHolder(String id, Vector3 rootVector) {
        AbstractUnitSelectorHolder unitSelectorHolder = new GenericSelectorHolder(id, rootVector);

        return unitSelectorHolder;
    }
}
