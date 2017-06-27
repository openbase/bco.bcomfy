package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType;
import rst.domotic.unit.UnitConfigType;

public abstract class AbstractServiceViewHolder {

    private static final String TAG = AbstractServiceViewHolder.class.getSimpleName();

    protected Activity activity;
    protected ViewGroup parent;
    protected RelativeLayout serviceView;

    protected boolean operation;
    protected boolean provider;
    protected boolean consumer;

    protected UnitConfigType.UnitConfig unitConfig;
    protected ServiceConfigType.ServiceConfig serviceConfig;
    protected ServiceRemote serviceRemote;

    public AbstractServiceViewHolder(Activity activity, ViewGroup parent, UnitConfigType.UnitConfig unitConfig, ServiceConfigType.ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        this.activity = activity;
        this.parent = parent;

        this.unitConfig = unitConfig;
        this.serviceConfig = serviceConfig;

        this.operation = operation;
        this.provider = provider;
        this.consumer = consumer;

        initServiceView();
    }

    public View getServiceView() {
        return serviceView;
    }

    abstract protected void initServiceView();
}
