package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.flask.colorpicker.ColorPickerView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.remote.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.vision.RGBColorType;

public class ColorStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = ColorStateServiceViewHolder.class.getSimpleName();

    public ColorStateServiceViewHolder(Activity activity, ViewGroup parent, UnitConfig unitConfig, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitConfig, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_color_state, parent, false);
        ColorPickerView colorPickerView = activity.findViewById(R.id.color_picker_view);

//        try {
//            ColorStateServiceRemote remote = new ColorStateServiceRemote();
//            remote.init(unitConfig);
//            remote.activate(true);
//            serviceRemote = remote;
//
//            if (provider) {
//                // Set the initial color
//                RGBColorType.RGBColor rgbColor = remote.getRGBColor();
//                activity.runOnUiThread(() -> colorPickerView.setColor(Color.rgb(rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue()), true));
//
//                // Update the switch if status of the power state has changed
//                remote.addDataObserver((observable, colorStateData) -> {
//                    RGBColorType.RGBColor rgbColorTemp = remote.getRGBColor();
//                    activity.runOnUiThread(() -> colorPickerView.setColor(Color.rgb(rgbColorTemp.getRed(), rgbColorTemp.getGreen(), rgbColorTemp.getBlue()), true));
//                });
//            }
//
//            if (operation) {
//                colorPickerView.addOnColorChangedListener(color -> {
//                    try {
//                        remote.setColor(RGBColorType.RGBColor.newBuilder().setRed(Color.red(color)).setGreen(Color.green(color)).setBlue(Color.blue(color)).build());
//                    } catch (CouldNotPerformException e) {
//                        e.printStackTrace();
//                    }
//                });
//            }
//
//
//        } catch (CouldNotPerformException | InterruptedException e) {
//            Log.e(TAG, "Error while fetching unit: " + unitConfig.getId());
//            e.printStackTrace();
//        }
    }
}
