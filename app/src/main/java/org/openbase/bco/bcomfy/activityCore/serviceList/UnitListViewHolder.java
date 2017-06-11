package org.openbase.bco.bcomfy.activityCore.serviceList;


import android.app.Activity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;

import java.util.ArrayList;
import java.util.List;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;

public class UnitListViewHolder {

    private LinearLayout serviceList;
    private LinearLayout unitList;
    private TextView labelText;
    private TextView typeText;

    private List<UnitViewHolder> unitViewHolderList;

    private DeviceRegistry deviceRegistry;
    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig deviceConfig;

    public UnitListViewHolder(LinearLayout serviceList) {
        this.serviceList = serviceList;
        unitList = (LinearLayout) serviceList.findViewById(R.id.unit_list);
        labelText = (TextView) serviceList.findViewById(R.id.device_label);
        typeText = (TextView) serviceList.findViewById(R.id.device_type);

        unitViewHolderList = new ArrayList<>();
    }

    public void displayUnit(Activity activity, String id) {
        unitList.removeAllViews();
        StreamSupport.stream(unitViewHolderList).forEach(UnitViewHolder::shutdownRemotes);
        unitViewHolderList.clear();

        try {
            deviceRegistry = Registries.getDeviceRegistry();
            unitRegistry = Registries.getUnitRegistry();
            UnitConfigType.UnitConfig unitConfig = unitRegistry.getUnitConfigById(id);

            // Distinguish whether to display a device or an unit
            if (unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE) {
                // Display a device
                deviceConfig = deviceRegistry.getDeviceConfigById(id);

                labelText.setText(deviceConfig.getLabel());
                typeText.setText(deviceRegistry.getDeviceClassById(deviceConfig.getDeviceConfig().getDeviceClassId()).getLabel());

                for (String unitId : deviceConfig.getDeviceConfig().getUnitIdList()) {
                    UnitViewHolder unitViewHolder = new UnitViewHolder(activity, unitId, unitList);
                    unitViewHolderList.add(unitViewHolder);

                    unitList.addView(unitViewHolder.getCardView());
                }
            }
            else {
                // Display a unit
                labelText.setText(unitConfig.getLabel());
                typeText.setText(unitConfig.getDescription());

                UnitViewHolder unitViewHolder = new UnitViewHolder(activity, id, unitList);
                unitViewHolderList.add(unitViewHolder);

                unitList.addView(unitViewHolder.getCardView());
            }

        } catch (CouldNotPerformException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
