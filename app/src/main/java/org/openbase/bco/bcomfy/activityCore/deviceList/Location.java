package org.openbase.bco.bcomfy.activityCore.deviceList;

import android.util.Log;

import com.bignerdranch.expandablerecyclerview.model.Parent;

import org.openbase.bco.bcomfy.activityCore.ListSettingsDialogFragment.SettingValue;
import org.openbase.bco.bcomfy.utils.BcoUtils;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java8.util.Comparators;
import java8.util.stream.StreamSupport;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;

public class Location implements Parent<Device> {

    private static final String TAG = Location.class.getSimpleName();

    private UnitConfigType.UnitConfig locationConfig;
    private List<Device> deviceList;

    public Location(UnitConfigType.UnitConfig locationConfig, UnitRegistryRemote remote, SettingValue unitSetting) {
        this.locationConfig = locationConfig;
        this.deviceList = new ArrayList<>();

        try {
            StreamSupport.stream(remote.getUnitConfigsByLocation(locationConfig.getId(), true))
                    .filter(BcoUtils::filterByMetaTag)
                    .filter(unitConfig -> unitConfig.getUnitType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE || !unitConfig.getBoundToUnitHost())
                    .filter(unitConfig -> (unitSetting == SettingValue.ALL ||
                                          (unitSetting == SettingValue.LOCATED && unitConfig.getPlacementConfig().hasPosition()) ||
                                          (unitSetting == SettingValue.UNLOCATED && !unitConfig.getPlacementConfig().hasPosition())))
                    .filter(unitConfig -> unitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED)
                    .sorted(Comparators.thenComparing(Comparators.comparing(UnitConfigType.UnitConfig::getUnitType), (unitConfig1) -> {
                        try {
                            return LabelProcessor.getBestMatch(Locale.getDefault(), unitConfig1.getLabel());
                        } catch (NotAvailableException e) {
                            return "?";
                        }
                    }))
                    .forEachOrdered(unitConfig -> deviceList.add(new Device(unitConfig)));
        } catch (CouldNotPerformException e) {
            Log.e(TAG, "Could not fetch units of Location: " + locationConfig.getId() + "\n" + Log.getStackTraceString(e));
        }
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
        try {
            return LabelProcessor.getBestMatch(Locale.getDefault(), locationConfig.getLabel());
        } catch (NotAvailableException e) {
            return "?";
        }
    }

    public String getId() {
        return locationConfig.getId();
    }

    public UnitConfigType.UnitConfig getUnitConfig() {
        return locationConfig;
    }
}