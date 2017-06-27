package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class UnknownServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = UnknownServiceViewHolder.class.getSimpleName();

    public UnknownServiceViewHolder(Activity activity, ViewGroup parent, UnitConfig unitConfig, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitConfig, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_unknown, parent, false);
        ((TextView) serviceView.findViewById(R.id.service_unknown_textview)).setText(serviceConfig.getServiceTemplate().getType().toString());
    }
}
