package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.service.Service$;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;

public class IlluminanceStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = IlluminanceStateServiceViewHolder.class.getSimpleName();

    private SeekBar seekBar;
    private TextView illuminanceStateTextView;

    public IlluminanceStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_illuminance_state, viewParent, false);
        seekBar = serviceView.findViewById(R.id.seek_bar_illuminance);
        seekBar.getProgressDrawable().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
        seekBar.setThumb(null);

        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        illuminanceStateTextView = serviceView.findViewById(R.id.illuminance_state_text);
    }

    @Override
    public void updateDynamicContent() {
        try {
            IlluminanceState illuminanceState = (IlluminanceState) Service$.invokeProviderServiceMethod(ServiceType.ILLUMINANCE_STATE_SERVICE, unitRemote);

            double illuminanceValue = illuminanceState.getIlluminance();

            activity.runOnUiThread(() -> {
                seekBar.setProgress((int) ((Math.log10(illuminanceValue) + 3.0d) * 10.0d));
                illuminanceStateTextView.setText(((int) illuminanceValue) + " lx");
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
