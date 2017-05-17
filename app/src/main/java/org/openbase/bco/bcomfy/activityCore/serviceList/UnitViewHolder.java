package org.openbase.bco.bcomfy.activityCore.serviceList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;

import java.util.ArrayList;
import java.util.List;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.unit.UnitConfigType;

public class UnitViewHolder {

    private TextView unitTitle;
    private ViewGroup cardView;

    private List<ServiceViewHolder> serviceViewHolderList;
    private LinearLayout servicePerUnitList;

    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig unitConfig;

    public UnitViewHolder(Activity activity, String id, ViewGroup parent) throws CouldNotPerformException, InterruptedException {
        serviceViewHolderList = new ArrayList<>();

        unitRegistry = Registries.getUnitRegistry();
        unitConfig = unitRegistry.getUnitConfigById(id);

        cardView = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.unit_card, parent, false);
        unitTitle = (TextView) cardView.findViewById(R.id.unit_title);
        servicePerUnitList = (LinearLayout) cardView.findViewById(R.id.service_per_unit_list);
        unitTitle.setText(unitConfig.getType().toString());

        // Only initialize Service if this units contains at least one
        if (unitConfig.getServiceConfigCount() == 0) {
            return;
        }

        // Sort all the ServiceConfigs by type and place them into a new list
        List<ServiceConfig> sortedServiceConfigs =
                StreamSupport.stream(unitConfig.getServiceConfigList())
                        .sorted((o1, o2) -> o1.getServiceTemplate().getType().compareTo(o2.getServiceTemplate().getType()))
                        .collect(Collectors.toList());

        // These booleans are used to determine what kind of patterns are associated to a single service
        boolean operation = false;
        boolean provider = false;
        boolean consumer = false;
        ServiceConfig previousServiceConfig = unitConfig.getServiceConfig(0);

        // Compare every service config to the previous one. If they are the same,
        // just set the corresponding pattern flag to true. If they are not, generate
        // a new ServiceViewHolder based on the previous service and attach it to
        // the servicePerUnitList.
        for (ServiceConfig currentServiceConfig : sortedServiceConfigs) {
            // Compare type to previous service
            if (!currentServiceConfig.getServiceTemplate().getType().equals(previousServiceConfig.getServiceTemplate().getType())) {
                LayoutInflater.from(activity).inflate(R.layout.divider_service, servicePerUnitList, true);

                ServiceViewHolder serviceViewHolder = new ServiceViewHolder(activity, servicePerUnitList, unitConfig, previousServiceConfig, operation, provider, consumer);
                serviceViewHolderList.add(serviceViewHolder);

                servicePerUnitList.addView(serviceViewHolder.getServiceView());

                operation = false;
                provider = false;
                consumer = false;

                previousServiceConfig = currentServiceConfig;
            }

            // Set the pattern flag
            switch (currentServiceConfig.getServiceTemplate().getPattern()) {
                case PROVIDER:
                    provider = true;
                case OPERATION:
                    operation = true;
                case CONSUMER:
                    consumer = true;
            }
        }

        // Since the last service is not added, do this now
        LayoutInflater.from(activity).inflate(R.layout.divider_service, servicePerUnitList, true);

        ServiceViewHolder serviceViewHolder = new ServiceViewHolder(activity, servicePerUnitList, unitConfig, previousServiceConfig, operation, provider, consumer);
        serviceViewHolderList.add(serviceViewHolder);

        servicePerUnitList.addView(serviceViewHolder.getServiceView());
    }

    public View getCardView() {
        return cardView;
    }

}
