package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.DrawerDisablingOnTouchListener;
import org.openbase.bco.dal.lib.layer.service.Service$;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;

public class BrightnessStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = BrightnessStateServiceViewHolder.class.getSimpleName();

    private SeekBar seekBar;

    public BrightnessStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
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
                        Service$.invokeOperationServiceMethod(ServiceType.BRIGHTNESS_STATE_SERVICE, unitRemote,
                                BrightnessState.newBuilder().setBrightness(progress).build());
                    } catch (CouldNotPerformException e) {
                        Log.e(TAG, "Error while changing the brightness state of unit: " + serviceConfig.getUnitId());
                        e.printStackTrace();
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
            BrightnessState brightnessState = (BrightnessState) Service$.invokeProviderServiceMethod(ServiceType.BRIGHTNESS_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> seekBar.setProgress((int) brightnessState.getBrightness()));

        } catch (CouldNotPerformException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
