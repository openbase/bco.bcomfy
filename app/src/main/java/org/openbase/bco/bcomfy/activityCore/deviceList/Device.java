package org.openbase.bco.bcomfy.activityCore.deviceList;

import org.openbase.type.domotic.unit.UnitConfigType;

public class Device {

    private UnitConfigType.UnitConfig device;

    public Device(UnitConfigType.UnitConfig device) {
        this.device = device;
    }

    public UnitConfigType.UnitConfig getUnitConfig() {
        return device;
    }

}