package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.DrawerDisablingOnTouchListener;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.schedule.RecurrenceEventFilter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;

public class BrightnessStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = BrightnessStateServiceViewHolder.class.getSimpleName();

    private RecurrenceEventFilter<Double> recurrenceEventFilter = new RecurrenceEventFilter<Double>(500) {
        @Override
        public void relay() throws Exception {
            try {
                ((Future)Services.invokeOperationServiceMethod(ServiceType.BRIGHTNESS_STATE_SERVICE, unitRemote,
                        BrightnessState.newBuilder().setBrightness(getLatestValue()).build())).get();
            } catch (CouldNotPerformException | ExecutionException e) {
                Log.e(TAG, "Error while changing the brightness state of unit: " + serviceConfig.getUnitId() + "\n" + Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                Log.e(TAG, "Error while changing the brightness state of unit: " + serviceConfig.getUnitId() + "\n" + Log.getStackTraceString(e));
                Thread.currentThread().interrupt();
            }
        }
    };

    private SeekBar seekBar;

    public BrightnessStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Overrides
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_brightness_state, viewParent, false);
        seekBar = serviceView.findViewById(R.id.seek_bar);
        seekBar.setOnTouchListener(new DrawerDisablingOnTouchListener());

        if (operation) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;

                    try {
                        recurrenceEventFilter.trigger(progress / 100d);
                    } catch (CouldNotPerformException e) {
                        Log.w(TAG, "Could not update brightness state", e);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
        else {
            seekBar.setClickable(false);
        }
    }

    @Override
    public void updateDynamicContent() {
        try {
            BrightnessState brightnessState = (BrightnessState) Services.invokeProviderServiceMethod(ServiceType.BRIGHTNESS_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> seekBar.setProgress((int) brightnessState.getBrightness() * 100));

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
