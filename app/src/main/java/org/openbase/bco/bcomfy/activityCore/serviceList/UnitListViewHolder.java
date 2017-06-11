package org.openbase.bco.bcomfy.activityCore.serviceList;


import android.app.Activity;
import android.os.AsyncTask;
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
    private Activity activity;
    private String id;

    private List<UnitViewHolder> unitViewHolderList;

    private DeviceRegistry deviceRegistry;
    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig deviceConfig;

    public UnitListViewHolder(LinearLayout serviceList) {
        this.serviceList = serviceList;
        unitList  = serviceList.findViewById(R.id.unit_list);
        labelText = serviceList.findViewById(R.id.device_label);
        typeText  = serviceList.findViewById(R.id.device_type);

        unitViewHolderList = new ArrayList<>();
    }

    public void displayUnit(Activity activity, String id) {
        this.activity = activity;
        this.id = id;
        new displayUnitTask().execute(this);
    }

    private static class displayUnitTask extends AsyncTask<UnitListViewHolder, Void, Void> {
        @Override
        protected Void doInBackground(UnitListViewHolder... unitListViewHolder) {
            unitListViewHolder[0].activity.runOnUiThread(() ->
                    unitListViewHolder[0].unitList.removeAllViews());
            StreamSupport.stream(unitListViewHolder[0].unitViewHolderList).forEach(UnitViewHolder::shutdownRemotes);
            unitListViewHolder[0].unitViewHolderList.clear();

            try {
                unitListViewHolder[0].deviceRegistry = Registries.getDeviceRegistry();
                unitListViewHolder[0].unitRegistry = Registries.getUnitRegistry();
                UnitConfigType.UnitConfig unitConfig = unitListViewHolder[0].unitRegistry.getUnitConfigById(unitListViewHolder[0].id);

                // Distinguish whether to display a device or an unit
                if (unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE) {
                    // Display a device
                    unitListViewHolder[0].deviceConfig =
                            unitListViewHolder[0].deviceRegistry.getDeviceConfigById(unitListViewHolder[0].id);

                    String labelText = unitListViewHolder[0].deviceConfig.getLabel();
                    unitListViewHolder[0].activity.runOnUiThread(() ->
                            unitListViewHolder[0].labelText.setText(labelText));

                    String typeText = unitListViewHolder[0].deviceRegistry.getDeviceClassById(
                            unitListViewHolder[0].deviceConfig.getDeviceConfig().getDeviceClassId()).getLabel();
                    unitListViewHolder[0].activity.runOnUiThread(() ->
                            unitListViewHolder[0].typeText.setText(typeText));

                    for (String unitId : unitListViewHolder[0].deviceConfig.getDeviceConfig().getUnitIdList()) {
                        UnitViewHolder unitViewHolder = new UnitViewHolder(unitListViewHolder[0].activity, unitId, unitListViewHolder[0].unitList);
                        unitListViewHolder[0].unitViewHolderList.add(unitViewHolder);

                        unitListViewHolder[0].activity.runOnUiThread(() ->
                                unitListViewHolder[0].unitList.addView(unitViewHolder.getCardView()));
                    }
                }
                else {
                    // Display a unit
                    unitListViewHolder[0].activity.runOnUiThread(() ->
                            unitListViewHolder[0].labelText.setText(unitConfig.getLabel()));
                    unitListViewHolder[0].activity.runOnUiThread(() ->
                            unitListViewHolder[0].typeText.setText(unitConfig.getDescription()));

                    UnitViewHolder unitViewHolder = new UnitViewHolder(unitListViewHolder[0].activity, unitListViewHolder[0].id, unitListViewHolder[0].unitList);
                    unitListViewHolder[0].unitViewHolderList.add(unitViewHolder);

                    unitListViewHolder[0].activity.runOnUiThread(() ->
                            unitListViewHolder[0].unitList.addView(unitViewHolder.getCardView()));
                }

            } catch (CouldNotPerformException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
