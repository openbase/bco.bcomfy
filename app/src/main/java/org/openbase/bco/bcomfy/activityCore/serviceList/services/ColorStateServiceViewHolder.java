package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.slider.LightnessSlider;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.DrawerDisablingOnTouchListener;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.schedule.RecurrenceEventFilter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.HSBColorType;
import org.openbase.type.vision.RGBColorType.RGBColor;

public class ColorStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = ColorStateServiceViewHolder.class.getSimpleName();

    private RecurrenceEventFilter<Integer> recurrenceEventFilter = new RecurrenceEventFilter<Integer>(500) {
        @Override
        public void relay() throws Exception {
            try {
                HSBColorType.HSBColor hsbColor =
                        HSBColorToRGBColorTransformer.transform(
                                RGBColor.newBuilder().setRed(Color.red(getLatestValue())).setGreen(Color.green(getLatestValue())).setBlue(Color.blue(getLatestValue())).build());

                ((Future) Services.invokeOperationServiceMethod(ServiceType.COLOR_STATE_SERVICE, unitRemote,
                        ColorState.newBuilder().setColor(ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(hsbColor).build()).build())).get();
            } catch (CouldNotPerformException | ExecutionException e) {
                Log.e(TAG, "Error while changing the color state of unit: " + serviceConfig.getUnitId() + "\n" + Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                Log.e(TAG, "Error while changing the color state of unit: " + serviceConfig.getUnitId() + "\n" + Log.getStackTraceString(e));
                Thread.currentThread().interrupt();
            }
        }
    };

    private ColorPickerView colorPickerView;
    private LightnessSlider lightnessSlider;

    public ColorStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_color_state, viewParent, false);
        colorPickerView = serviceView.findViewById(R.id.color_picker_view);
        lightnessSlider = serviceView.findViewById(R.id.v_lightness_slider);

        colorPickerView.setOnTouchListener(new DrawerDisablingOnTouchListener());
        lightnessSlider.setOnTouchListener(new DrawerDisablingOnTouchListener());

        if (operation) {
            colorPickerView.addOnColorChangedListener(value -> {
                try {
                    recurrenceEventFilter.trigger(value);
                } catch (CouldNotPerformException e) {
                    Log.w(TAG, "Could not set color state", e);
                }
            });
        } else {
            colorPickerView.setClickable(false);
            lightnessSlider.setClickable(false);
        }
    }

    @Override
    public void updateDynamicContent() {
        try {
            ColorState colorState = (ColorState) Services.invokeProviderServiceMethod(ServiceType.COLOR_STATE_SERVICE, unitRemote);
            RGBColor rgbColor;

            if (colorState.getColor().getType().equals(ColorType.Color.Type.HSB)) {
                rgbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getHsbColor());
            } else {
                rgbColor = colorState.getColor().getRgbColor();
            }

            activity.runOnUiThread(() -> colorPickerView.setColor(Color.rgb(((int) (rgbColor.getRed() * 255d)), ((int) (rgbColor.getGreen() * 255d)), ((int) (rgbColor.getBlue() * 255d))), false));
            // android 26 api only: activity.runOnUiThread(() -> colorPickerView.setColor(Color.rgb(((float) rgbColor.getRed()), ((float) rgbColor.getGreen()), ((float) rgbColor.getBlue())), false));

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
