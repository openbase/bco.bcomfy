package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.ViewGroup;

import org.openbase.bco.bcomfy.activitySettings.SettingsActivity;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;

public class ServiceViewHolderFactory {

    private static final String TAG = ServiceViewHolderFactory.class.getSimpleName();

    public static AbstractServiceViewHolder createServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        switch (serviceConfig.getServiceDescription().getType()) {
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
            case TEMPERATURE_STATE_SERVICE:
                return new TemperatureStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case TEMPERATURE_ALARM_STATE_SERVICE:
                return new TemperatureAlarmStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case POWER_CONSUMPTION_STATE_SERVICE:
                return new PowerConsumptionStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case HANDLE_STATE_SERVICE:
                return new HandleStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case TAMPER_STATE_SERVICE:
                return new TamperStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case ACTIVATION_STATE_SERVICE:
                return new ActivationStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            case BLIND_STATE_SERVICE:
                return new BlindStateServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
            default:
                if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(SettingsActivity.KEY_PREF_MISC_UNKONWN_SERVICE, false)) {
                    return new UnknownServiceViewHolder(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
                }
                else {
                    throw new CouldNotPerformException("Service " + serviceConfig.getServiceDescription().getType().name() + " ignored since it is not supported!");
                }
        }
    }

}
