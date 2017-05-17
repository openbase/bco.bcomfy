package org.openbase.bco.bcomfy.activityCore.serviceList;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;

import rst.domotic.unit.UnitConfigType;

public class UnitCardViewHolder {

    private TextView unitTitle;

    private ViewGroup cardView;
    private UnitRegistry unitRegistry;
    private UnitConfigType.UnitConfig unitConfig;

    public UnitCardViewHolder(Context context, String id, ViewGroup parent) throws CouldNotPerformException, InterruptedException {
        unitRegistry = Registries.getUnitRegistry();
        unitConfig = unitRegistry.getUnitConfigById(id);

        cardView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.test_card_view, parent, false);
        unitTitle = (TextView) cardView.findViewById(R.id.unit_title);
        unitTitle.setText(unitConfig.getType().toString());
    }

    public View getCardView() {
        return cardView;
    }

}
