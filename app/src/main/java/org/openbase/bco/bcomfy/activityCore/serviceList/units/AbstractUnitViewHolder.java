package org.openbase.bco.bcomfy.activityCore.serviceList.units;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.unit.UnitConfigType.UnitConfig;

public abstract class AbstractUnitViewHolder {

    private static final String TAG = AbstractUnitViewHolder.class.getSimpleName();

    protected UnitRegistry unitRegistry;
    protected UnitConfig unitConfig;
    protected ViewGroup cardView;
    protected TextView unitLabel;

    public AbstractUnitViewHolder(Activity activity, UnitConfig unitConfig, ViewGroup parent) throws CouldNotPerformException, InterruptedException {
        unitRegistry = Registries.getUnitRegistry();
        this.unitConfig = unitConfig;

        cardView = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.unit_card, parent, false);
        unitLabel = cardView.findViewById(R.id.unit_title);
        unitLabel.setText(unitConfig.getType().toString());
    }

    public View getCardView() {
        return cardView;
    }
}
