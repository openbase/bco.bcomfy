package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import org.openbase.type.domotic.service.ServiceConfigType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;

public abstract class AbstractServiceViewHolder {

    private static final String TAG = AbstractServiceViewHolder.class.getSimpleName();

    protected Activity activity;
    protected ViewGroup viewParent;
    protected RelativeLayout serviceView;

    protected boolean operation;
    protected boolean provider;
    protected boolean consumer;

    protected ServiceConfig serviceConfig;
    protected UnitRemote unitRemote;

    public AbstractServiceViewHolder(Activity activity, ViewGroup viewParent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        this.activity = activity;
        this.viewParent = viewParent;

        this.serviceConfig = serviceConfig;
        this.unitRemote = unitRemote;

        this.operation = operation;
        this.provider = provider;
        this.consumer = consumer;

        initServiceView();
        updateDynamicContent();
    }

    public View getServiceView() {
        return serviceView;
    }

    abstract protected void initServiceView();

    abstract public void updateDynamicContent();
}
