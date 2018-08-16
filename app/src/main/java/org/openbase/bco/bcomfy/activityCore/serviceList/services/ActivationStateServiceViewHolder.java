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

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PowerStateType.PowerState;

public class ActivationStateServiceViewHolder extends AbstractServiceViewHolder {

    private Switch activationStateSwitch;

    private static final String TAG = ActivationStateServiceViewHolder.class.getSimpleName();

    public ActivationStateServiceViewHolder(Activity activity, ViewGroup viewParent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, viewParent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_activation_state, viewParent, false);
        activationStateSwitch = serviceView.findViewById(R.id.activationStateSwitch);
        activationStateSwitch.setOnTouchListener(new DrawerDisablingOnTouchListener());

        if (operation) {
            activationStateSwitch.setOnClickListener(v -> {
                try {
                    if (((Switch) v).isChecked()) {
                        Services.invokeOperationServiceMethod(ServiceType.ACTIVATION_STATE_SERVICE, unitRemote,
                                ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
                    }
                    else {
                        Services.invokeOperationServiceMethod(ServiceType.ACTIVATION_STATE_SERVICE, unitRemote,
                                ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build());
                    }
                } catch (CouldNotPerformException e) {
                    Log.e(TAG, "Error while changing the activation state of unit: " + serviceConfig.getUnitId() + "\n" + Log.getStackTraceString(e));
                }
            });
        } else {
            activationStateSwitch.setClickable(false);
        }
    }

    @Override
    public void updateDynamicContent() {
        try {
            ActivationState activationState = (ActivationState) Services.invokeProviderServiceMethod(ServiceType.ACTIVATION_STATE_SERVICE, unitRemote);

            switch (activationState.getValue()) {
                case ACTIVE:
                    activity.runOnUiThread(() -> activationStateSwitch.setChecked(true));
                    break;
                case DEACTIVE:
                    activity.runOnUiThread(() -> activationStateSwitch.setChecked(false));
                    break;
            }
        } catch (CouldNotPerformException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
