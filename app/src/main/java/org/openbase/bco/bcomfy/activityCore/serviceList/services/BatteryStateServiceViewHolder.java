package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
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

import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;

public class BatteryStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = BatteryStateServiceViewHolder.class.getSimpleName();

    private SeekBar seekBar;
    private TextView batteryStateTextView;

    public BatteryStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_battery_state, viewParent, false);
        seekBar = serviceView.findViewById(R.id.seek_bar_battery);
        seekBar.setThumb(null);

        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        batteryStateTextView = serviceView.findViewById(R.id.battery_state_text);
    }

    @Override
    public void updateDynamicContent() {
        try {
            BatteryState batteryState = (BatteryState) Services.invokeProviderServiceMethod(ServiceType.BATTERY_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> {
                seekBar.setProgress((int) batteryState.getLevel() * 100);
                batteryStateTextView.setText((int) batteryState.getLevel() * 100 + "%");
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
