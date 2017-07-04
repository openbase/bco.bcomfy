package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.view.ViewGroup;

import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;

public class ServiceViewHolderFactory {

    private static final String TAG = ServiceViewHolderFactory.class.getSimpleName();

    public static AbstractServiceViewHolder createServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        switch (serviceConfig.getServiceTemplate().getType()) {
            case POWER_STATE_SERVICE:
                return new PowerStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case COLOR_STATE_SERVICE:
                return new ColorStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case BRIGHTNESS_STATE_SERVICE:
                return new BrightnessStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case BATTERY_STATE_SERVICE:
                return new BatteryStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case ILLUMINANCE_STATE_SERVICE:
                return new IlluminanceStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case CONTACT_STATE_SERVICE:
                return new ContactStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case MOTION_STATE_SERVICE:
                return new MotionStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            default:
                return new UnknownServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
        }
    }

}
