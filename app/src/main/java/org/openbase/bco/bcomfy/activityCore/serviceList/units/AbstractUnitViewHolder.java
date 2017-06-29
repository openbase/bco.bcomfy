package org.openbase.bco.bcomfy.activityCore.serviceList.units;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Remote.ConnectionState;

import java.util.concurrent.TimeUnit;

import rst.domotic.unit.UnitConfigType.UnitConfig;

public abstract class AbstractUnitViewHolder<URC extends AbstractUnitRemote> {

    private static final String TAG = AbstractUnitViewHolder.class.getSimpleName();

    protected UnitRegistry unitRegistry;
    protected UnitConfig unitConfig;
    protected ViewGroup cardView;
    protected TextView unitLabel;
    protected URC abstractUnitRemote;

    public AbstractUnitViewHolder(final Class<? extends URC> unitRemoteClass, Activity activity, UnitConfig unitConfig, ViewGroup parent) throws InstantiationException {
        try {
            unitRegistry = Registries.getUnitRegistry();
            this.unitConfig = unitConfig;

            cardView = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.unit_card, parent, false);
            unitLabel = cardView.findViewById(R.id.unit_title);
            unitLabel.setText(unitConfig.getType().toString());

            abstractUnitRemote = Units.getUnit(unitConfig, false, unitRemoteClass);
            abstractUnitRemote.waitForData(1, TimeUnit.SECONDS);

            abstractUnitRemote.addConfigObserver((observable, config) -> onConfigChanged((UnitConfig) config));
            abstractUnitRemote.addConnectionStateObserver((observable, connectionState) -> onConnectionStateChanged((ConnectionState) connectionState));
            abstractUnitRemote.addDataObserver((observable, data) -> onDataChanged(data));
        } catch (CouldNotPerformException | InterruptedException e) {
            throw new InstantiationException(AbstractUnitViewHolder.class, e);
        }
    }

    public View getCardView() {
        return cardView;
    }

    abstract protected void onConfigChanged(UnitConfig unitConfig);

    abstract protected void onConnectionStateChanged(ConnectionState connectionState);

    abstract protected void onDataChanged(Object data);
}
