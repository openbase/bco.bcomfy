package org.openbase.bco.bcomfy.activityCore.serviceList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType;
import rst.domotic.unit.UnitConfigType;

public class ServiceViewHolder {

    private RelativeLayout serviceView;

    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig unitConfig;
    private ServiceConfigType.ServiceConfig serviceConfig;

    public ServiceViewHolder (Context context, UnitConfigType.UnitConfig unitConfig, ServiceConfigType.ServiceConfig serviceConfig, ViewGroup parent) throws CouldNotPerformException, InterruptedException {
        unitRegistry = Registries.getUnitRegistry();
        this.unitConfig = unitConfig;
        this.serviceConfig = serviceConfig;

        switch (serviceConfig.getServiceTemplate().getType()) {
            case POWER_STATE_SERVICE:
                serviceView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.service_power_state, parent, false);
                break;
            default:
                serviceView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.service_unknown, parent, false);
                ((TextView) serviceView.findViewById(R.id.service_unknown_textview)).setText(serviceConfig.getServiceTemplate().getType().toString());
        }
    }

    public View getServiceView() {
        return serviceView;
    }

}
