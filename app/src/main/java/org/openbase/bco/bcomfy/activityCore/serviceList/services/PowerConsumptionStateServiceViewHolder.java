package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import java.math.BigDecimal;
import java.math.MathContext;

import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerConsumptionStateType;

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
            PowerConsumptionStateType.PowerConsumptionState powerConsumptionState = (PowerConsumptionStateType.PowerConsumptionState) Services.invokeProviderServiceMethod(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> {
                if (powerConsumptionState.hasConsumption()) {
                    try {
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
                    } catch (NumberFormatException ex) {
                        Log.w(TAG, "NumberFormatException while reading powerConsumptionState of unit " + serviceConfig.getUnitId());
                        textViewCurrentValue.setText("? W");
                    }
                }
                else {
                    textViewConsumption.setVisibility(View.GONE);
                }

                if (powerConsumptionState.hasCurrent()) {
                    try {
                        textViewCurrent.setVisibility(View.VISIBLE);

                        double d = powerConsumptionState.getCurrent();
                        BigDecimal bd = new BigDecimal(d);
                        bd = bd.round(new MathContext(4));

                        textViewCurrentValue.setText(bd.doubleValue() + " A");
                    } catch (NumberFormatException ex) {
                        Log.w(TAG, "NumberFormatException while reading powerConsumptionState of unit " + serviceConfig.getUnitId());
                        textViewCurrentValue.setText("? A");
                    }
                }
                else {
                    textViewCurrent.setVisibility(View.GONE);
                }

                if (powerConsumptionState.hasVoltage()) {
                    try {
                        textViewVoltage.setVisibility(View.VISIBLE);

                        double d = powerConsumptionState.getVoltage();
                        BigDecimal bd = new BigDecimal(d);
                        bd = bd.round(new MathContext(4));

                        textViewVoltageValue.setText(bd.doubleValue() + " V");
                    } catch (NumberFormatException ex) {
                        Log.w(TAG, "NumberFormatException while reading powerConsumptionState of unit " + serviceConfig.getUnitId());
                        textViewCurrentValue.setText("? V");
                    }
                }
                else {
                    textViewVoltage.setVisibility(View.GONE);
                }
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
