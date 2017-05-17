package org.openbase.bco.bcomfy.activityCore.serviceList;

import android.content.Context;
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

import rst.domotic.service.ServiceConfigType;
import rst.domotic.unit.UnitConfigType;

public class UnitCardViewHolder {

    private TextView unitTitle;
    private ViewGroup cardView;

    private List<ServiceViewHolder> serviceViewHolderList;
    private LinearLayout servicePerUnitList;

    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig unitConfig;

    public UnitCardViewHolder(Context context, String id, ViewGroup parent) throws CouldNotPerformException, InterruptedException {
        serviceViewHolderList = new ArrayList<>();

        unitRegistry = Registries.getUnitRegistry();
        unitConfig = unitRegistry.getUnitConfigById(id);

        cardView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.unit_card, parent, false);
        unitTitle = (TextView) cardView.findViewById(R.id.unit_title);
        servicePerUnitList = (LinearLayout) cardView.findViewById(R.id.service_per_unit_list);
        unitTitle.setText(unitConfig.getType().toString());

        for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            LayoutInflater.from(context).inflate(R.layout.divider_service, servicePerUnitList, true);

            ServiceViewHolder serviceViewHolder = new ServiceViewHolder(context, unitConfig, serviceConfig, servicePerUnitList);
            serviceViewHolderList.add(serviceViewHolder);

            servicePerUnitList.addView(serviceViewHolder.getServiceView());
        }
    }

    public View getCardView() {
        return cardView;
    }

}
