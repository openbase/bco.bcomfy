package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.AlarmStateType;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.state.TemperatureStateType.TemperatureState;

public class TemperatureStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = TemperatureStateServiceViewHolder.class.getSimpleName();

    private SeekBar seekBar;
    private TextView temperatureStateTextView;

    public TemperatureStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_temperature_state, viewParent, false);
        seekBar = serviceView.findViewById(R.id.seek_bar_temperature);
        seekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        seekBar.setThumb(null);

        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        temperatureStateTextView = serviceView.findViewById(R.id.temperature_state_text);
    }

    @Override
    public void updateDynamicContent() {
        try {
            TemperatureState temperatureState = (TemperatureState) Services.invokeProviderServiceMethod(ServiceType.TEMPERATURE_STATE_SERVICE, unitRemote);

            double temperatureValue = (Math.round(temperatureState.getTemperature() * 10.0d)) / 10.0d;

            activity.runOnUiThread(() -> {
                seekBar.setProgress((int) ((temperatureValue - 14.0d) * 10.0d));

                if (temperatureValue < 17.0d) {
                    seekBar.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                } else if (temperatureValue < 19.0d) {
                    seekBar.getProgressDrawable().setColorFilter(Color.CYAN, PorterDuff.Mode.SRC_IN);
                } else if (temperatureValue < 21.0d) {
                    seekBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                } else if (temperatureValue < 23.0d) {
                    seekBar.getProgressDrawable().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
                } else {
                    seekBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                }

                temperatureStateTextView.setText(temperatureValue + " °C");
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
