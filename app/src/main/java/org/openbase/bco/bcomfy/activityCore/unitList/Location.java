package org.openbase.bco.bcomfy.activityCore.unitList;

import com.bignerdranch.expandablerecyclerview.model.Parent;

import java.util.List;

public class Location implements Parent<Device> {

    private String label;
    private List<Device> deviceList;

    public Location(String label, List<Device> deviceList) {
        this.label = label;
        this.deviceList = deviceList;
    }

    @Override
    public List<Device> getChildList() {
        return deviceList;
    }

    @Override
    public boolean isInitiallyExpanded() {
        return false;
    }

    public String getLabel() {
        return label;
    }
}