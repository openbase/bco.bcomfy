package org.openbase.bco.bcomfy.activityCore.serviceList;


import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;

import java.util.ArrayList;
import java.util.List;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitConfigType;

public class ServiceListHolder {

    private LinearLayout serviceList;
    private TextView labelText;
    private TextView typeText;

    private List<UnitCardViewHolder> unitCardViewHolderList;

    private DeviceRegistry deviceRegistry;
    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig deviceConfig;

    public ServiceListHolder(LinearLayout serviceList) {
        this.serviceList = serviceList;
        labelText = (TextView) serviceList.findViewById(R.id.device_label);
        typeText = (TextView) serviceList.findViewById(R.id.device_type);

        unitCardViewHolderList = new ArrayList<>();
    }

    public void displayDevice(String id) {
        serviceList.removeViews(1, serviceList.getChildCount()-1);
        unitCardViewHolderList.clear();

        try {
            deviceRegistry = Registries.getDeviceRegistry();
            unitRegistry = Registries.getUnitRegistry();

            deviceConfig = deviceRegistry.getDeviceConfigById(id);

            labelText.setText(deviceConfig.getLabel());
            typeText.setText(deviceRegistry.getDeviceClassById(deviceConfig.getDeviceConfig().getDeviceClassId()).getLabel());

            for (String unitId : deviceConfig.getDeviceConfig().getUnitIdList()) {
                UnitCardViewHolder unitCardViewHolder = new UnitCardViewHolder(serviceList.getContext(), unitId, serviceList);
                unitCardViewHolderList.add(unitCardViewHolder);
                serviceList.addView(unitCardViewHolder.getCardView());
            }
        } catch (CouldNotPerformException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
