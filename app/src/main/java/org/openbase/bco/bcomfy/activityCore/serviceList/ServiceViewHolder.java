package org.openbase.bco.bcomfy.activityCore.serviceList;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType;

public class ServiceViewHolder {

    private static final String TAG = ServiceViewHolder.class.getSimpleName();

    private RelativeLayout serviceView;

    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig unitConfig;
    private ServiceConfigType.ServiceConfig serviceConfig;

    public ServiceViewHolder (Activity activity, ViewGroup parent, UnitConfigType.UnitConfig unitConfig, ServiceConfigType.ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        unitRegistry = Registries.getUnitRegistry();
        this.unitConfig = unitConfig;
        this.serviceConfig = serviceConfig;

        switch (serviceConfig.getServiceTemplate().getType()) {
            case POWER_STATE_SERVICE:
                initPowerStateService(activity, parent);
                break;
            default:
                serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_unknown, parent, false);
                ((TextView) serviceView.findViewById(R.id.service_unknown_textview)).setText(serviceConfig.getServiceTemplate().getType().toString());
        }
    }

    public View getServiceView() {
        return serviceView;
    }

    private void initPowerStateService(Activity activity, ViewGroup parent) {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_power_state, parent, false);
        Switch powerStateSwitch = (Switch) serviceView.findViewById(R.id.service_power_state_switch);

        try {
            PowerStateServiceRemote remote = new PowerStateServiceRemote();
            remote.init(unitConfig);
            remote.activate(true);

            // Set the initial switch position
            if (remote.getPowerState().getValue().equals(PowerState.State.ON)) {
                powerStateSwitch.setChecked(true);
            }

            // Update the switch if status of the power state has changed
            remote.addDataObserver((observable, powerStateData) -> {
                if (powerStateData.getValue().equals(PowerState.State.ON)) {
                    activity.runOnUiThread(() -> powerStateSwitch.setChecked(true));
                } else {
                    activity.runOnUiThread(() -> powerStateSwitch.setChecked(false));
                }
            });

            powerStateSwitch.setOnClickListener(v -> {
                try {
                    if (((Switch) v).isChecked()) {
                        remote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.ON).build());
                    }
                    else {
                        remote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build());
                    }
                } catch (CouldNotPerformException e) {
                    Log.e(TAG, "Error while changing the power state of unit: " + unitConfig.getId());
                    e.printStackTrace();
                }
            });

        } catch (CouldNotPerformException | InterruptedException e) {
            Log.e(TAG, "Error while fetching unit: " + unitConfig.getId());
            e.printStackTrace();
        }
    }

}
