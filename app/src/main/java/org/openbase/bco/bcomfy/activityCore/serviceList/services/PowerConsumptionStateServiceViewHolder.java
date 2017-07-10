package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.service.Service$;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import java.math.BigDecimal;
import java.math.MathContext;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerConsumptionStateType;

public class PowerConsumptionStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = PowerConsumptionStateServiceViewHolder.class.getSimpleName();

    private ProgressBar progressBar;
    private LinearLayout textViewConsumption;
    private TextView textViewConsumptionValue;
    private LinearLayout textViewCurrent;
    private TextView textViewCurrentValue;
    private LinearLayout textViewVoltage;
    private TextView textViewVoltageValue;

    public PowerConsumptionStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_power_consumption_state, viewParent, false);
        progressBar = serviceView.findViewById(R.id.progressBarPowerConsumption);

        textViewConsumption         = serviceView.findViewById(R.id.layoutConsumption);
        textViewConsumptionValue    = serviceView.findViewById(R.id.textViewConsumptionValue);
        textViewCurrent             = serviceView.findViewById(R.id.layoutCurrent);
        textViewCurrentValue        = serviceView.findViewById(R.id.textViewCurrentValue);
        textViewVoltage             = serviceView.findViewById(R.id.layoutVoltage);
        textViewVoltageValue        = serviceView.findViewById(R.id.textViewVoltageValue);
    }

    @Override
    public void updateDynamicContent() {
        try {
            PowerConsumptionStateType.PowerConsumptionState powerConsumptionState = (PowerConsumptionStateType.PowerConsumptionState) Service$.invokeProviderServiceMethod(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> {
                if (powerConsumptionState.hasConsumption()) {
                    if (powerConsumptionState.getConsumption() > 0.0d) {
                        progressBar.setIndeterminate(true);
                    }
                    else {
                        progressBar.setIndeterminate(false);
                    }
                    textViewConsumption.setVisibility(View.VISIBLE);

                    double d = powerConsumptionState.getConsumption();
                    BigDecimal bd = new BigDecimal(d);
                    bd = bd.round(new MathContext(4));

                    textViewConsumptionValue.setText(bd.doubleValue() + " W");
                }
                else {
                    textViewConsumption.setVisibility(View.GONE);
                }

                if (powerConsumptionState.hasCurrent()) {
                    textViewCurrent.setVisibility(View.VISIBLE);

                    double d = powerConsumptionState.getCurrent();
                    BigDecimal bd = new BigDecimal(d);
                    bd = bd.round(new MathContext(4));

                    textViewCurrentValue.setText(bd.doubleValue() + " A");
                }
                else {
                    textViewCurrent.setVisibility(View.GONE);
                }

                if (powerConsumptionState.hasVoltage()) {
                    textViewVoltage.setVisibility(View.VISIBLE);

                    double d = powerConsumptionState.getVoltage();
                    BigDecimal bd = new BigDecimal(d);
                    bd = bd.round(new MathContext(4));

                    textViewVoltageValue.setText(bd.doubleValue() + " V");
                }
                else {
                    textViewVoltage.setVisibility(View.GONE);
                }
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
