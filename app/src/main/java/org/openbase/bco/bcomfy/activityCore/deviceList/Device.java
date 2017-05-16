package org.openbase.bco.bcomfy.activityCore.deviceList;

import rst.domotic.unit.UnitConfigType;

public class Device {

    private UnitConfigType.UnitConfig device;

    public Device(UnitConfigType.UnitConfig device) {
        this.device = device;
    }

    public String getLabel() {
        return device.getLabel();
    }

    public String getId() {
        return device.getId();
    }

}