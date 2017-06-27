package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.view.ViewGroup;

import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class ServiceViewHolderFactory {

    private static final String TAG = ServiceViewHolderFactory.class.getSimpleName();

    public static AbstractServiceViewHolder createServiceViewHolder(Activity activity, ViewGroup parent, UnitConfig unitConfig, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        switch (serviceConfig.getServiceTemplate().getType()) {
            case POWER_STATE_SERVICE:
                return new PowerStateServiceViewHolder(activity, parent, unitConfig, serviceConfig, operation, provider, consumer);
            default:
                return new UnknownServiceViewHolder(activity, parent, unitConfig, serviceConfig, operation, provider, consumer);
        }
    }

}
