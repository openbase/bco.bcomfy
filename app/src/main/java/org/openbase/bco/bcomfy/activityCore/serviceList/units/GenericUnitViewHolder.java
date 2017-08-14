package org.openbase.bco.bcomfy.activityCore.serviceList.units;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.serviceList.services.AbstractServiceViewHolder;
import org.openbase.bco.bcomfy.activityCore.serviceList.services.ServiceViewHolderFactory;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Remote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class GenericUnitViewHolder extends AbstractUnitViewHolder<AbstractUnitRemote> {

    private static final String TAG = AbstractUnitViewHolder.class.getSimpleName();

    private LinearLayout serviceList;
    private List<AbstractServiceViewHolder> serviceViewHolderList;

    public GenericUnitViewHolder(Activity activity, UnitConfig unitConfig, ViewGroup parent) throws InstantiationException {
        super(AbstractUnitRemote.class, activity, unitConfig, parent);

        serviceViewHolderList = new ArrayList<>();
        serviceList = cardView.findViewById(R.id.service_per_unit_list);

        // Only initialize services if this unit contains at least one
        if (unitConfig.getServiceConfigCount() == 0) {
            return;
        }

        // Sort all the ServiceConfigs by type and place them into a new list
        List<ServiceConfig> sortedServiceConfigs =
                StreamSupport.stream(unitConfig.getServiceConfigList())
                        .sorted((o1, o2) -> o1.getServiceDescription().getType().compareTo(o2.getServiceDescription().getType()))
                        .collect(Collectors.toList());

        // These booleans are used to determine what kind of patterns are associated to a single service
        boolean operation = false;
        boolean provider = false;
        boolean consumer = false;
        ServiceConfig previousServiceConfig = sortedServiceConfigs.get(0);

        // Compare every service config to the previous one. If they are the same,
        // just set the corresponding pattern flag to true. If they are not, generate
        // a new PowerStateServiceViewHolder based on the previous service and attach it to
        // the serviceList.
        for (ServiceConfig currentServiceConfig : sortedServiceConfigs) {
            // Compare type to previous service
            if (!currentServiceConfig.getServiceDescription().getType().equals(previousServiceConfig.getServiceDescription().getType())) {
                createAndAddServiceView(activity, previousServiceConfig, operation, provider, consumer);

                operation = false;
                provider = false;
                consumer = false;

                previousServiceConfig = currentServiceConfig;
            }

            // Set the pattern flag
            switch (currentServiceConfig.getServiceDescription().getPattern()) {
                case PROVIDER:
                    provider = true;
                case OPERATION:
                    operation = true;
                case CONSUMER:
                    consumer = true;
            }
        }

        // Since the last service is not added, do this now
        createAndAddServiceView(activity, previousServiceConfig, operation, provider, consumer);
    }

    @Override
    protected void onConfigChanged(UnitConfig unitConfig) {
        Log.i(TAG, this.unitConfig.getLabel() + " -> onConfigChanged");
    }

    @Override
    protected void onConnectionStateChanged(Remote.ConnectionState connectionState) {
        Log.i(TAG, this.unitConfig.getLabel() + " -> onConnectionStateChanged");
    }

    @Override
    protected void onDataChanged(Object data) {
        StreamSupport.stream(serviceViewHolderList).forEach(AbstractServiceViewHolder::updateDynamicContent);
    }

    private void createAndAddServiceView(Activity activity, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) {
        try {
            AbstractServiceViewHolder serviceViewHolder = ServiceViewHolderFactory.createServiceViewHolder(activity, serviceList, abstractUnitRemote, serviceConfig, operation, provider, consumer);

            if (serviceViewHolderList.size() == 0) {
                addDividerThick(activity);
            }
            else {
                addDivider(activity);
            }

            serviceViewHolderList.add(serviceViewHolder);
            serviceList.addView(serviceViewHolder.getServiceView());
        } catch (CouldNotPerformException | InterruptedException ex) {
            Log.i(TAG, ex.getMessage());
        }
    }

    private void addDivider(Activity activity) {
        LayoutInflater.from(activity).inflate(R.layout.divider_service, serviceList, true);
    }

    private void addDividerThick(Activity activity) {
        LayoutInflater.from(activity).inflate(R.layout.divider_service_thick, serviceList, true);
    }
}
