package org.openbase.bco.bcomfy.activityCore.serviceList.units;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Remote.ConnectionState;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rst.domotic.unit.UnitConfigType.UnitConfig;

public abstract class AbstractUnitViewHolder {

    private static final String TAG = AbstractUnitViewHolder.class.getSimpleName();

    protected UnitRegistry unitRegistry;
    protected UnitConfig unitConfig;
    protected ViewGroup cardView;
    protected TextView unitLabel;
    protected UnitRemote unitRemote;

    public AbstractUnitViewHolder(Activity activity, UnitConfig unitConfig, ViewGroup parent) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        unitRegistry = Registries.getUnitRegistry();
        this.unitConfig = unitConfig;

        cardView = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.unit_card, parent, false);
        unitLabel = cardView.findViewById(R.id.unit_title);
        unitLabel.setText(unitConfig.getType().toString());

        unitRemote = Units.getFutureUnit(unitConfig, true).get(1, TimeUnit.SECONDS);

        unitRemote.addConfigObserver((observable, config) -> onConfigChanged((UnitConfig) config));
        unitRemote.addConnectionStateObserver((observable, connectionState) -> onConnectionStateChanged((ConnectionState) connectionState));
        unitRemote.addDataObserver((observable, data) -> onDataChanged(data));
    }

    public View getCardView() {
        return cardView;
    }

    abstract protected void onConfigChanged(UnitConfig unitConfig);

    abstract protected void onConnectionStateChanged(ConnectionState connectionState);

    abstract protected void onDataChanged(Object data);
}
