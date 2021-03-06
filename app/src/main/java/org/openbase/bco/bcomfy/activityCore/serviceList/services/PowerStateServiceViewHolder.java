package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.DrawerDisablingOnTouchListener;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;

import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.timing.TimestampType;

public class PowerStateServiceViewHolder extends AbstractServiceViewHolder {

    private Switch powerStateSwitch;

    private static final String TAG = PowerStateServiceViewHolder.class.getSimpleName();

    public PowerStateServiceViewHolder(Activity activity, ViewGroup viewParent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, viewParent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_power_state, viewParent, false);
        powerStateSwitch = serviceView.findViewById(R.id.service_power_state_switch);
        powerStateSwitch.setOnTouchListener(new DrawerDisablingOnTouchListener());

        if (operation) {
            powerStateSwitch.setOnClickListener(v -> {
                try {
                    if (((Switch) v).isChecked()) {
                        Services.invokeOperationServiceMethod(ServiceType.POWER_STATE_SERVICE, unitRemote,
                                PowerState.newBuilder().setValue(PowerState.State.ON).build());
                    }
                    else {
                        Services.invokeOperationServiceMethod(ServiceType.POWER_STATE_SERVICE, unitRemote,
                                PowerState.newBuilder().setValue(PowerState.State.OFF).build());
                    }
                } catch (CouldNotPerformException e) {
                    Log.e(TAG, "Error while changing the power state of unit: " + serviceConfig.getUnitId() + "\n" + Log.getStackTraceString(e));
                }
            });
        } else {
            powerStateSwitch.setClickable(false);
        }
    }

    @Override
    public void updateDynamicContent() {
        try {
            PowerState powerStateData = (PowerState) Services.invokeProviderServiceMethod(ServiceType.POWER_STATE_SERVICE, unitRemote);

            if (powerStateData.getValue().equals(PowerState.State.ON)) {
                activity.runOnUiThread(() -> powerStateSwitch.setChecked(true));
            } else {
                activity.runOnUiThread(() -> powerStateSwitch.setChecked(false));
            }
        } catch (CouldNotPerformException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
