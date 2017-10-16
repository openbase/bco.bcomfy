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
import org.openbase.bco.dal.lib.layer.service.Service$;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.schedule.RecurrenceEventFilter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType.RGBColor;

public class ColorStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = ColorStateServiceViewHolder.class.getSimpleName();

    private RecurrenceEventFilter<Integer> recurrenceEventFilter = new RecurrenceEventFilter<Integer>(500) {
        @Override
        public void relay() throws Exception {
            try {
                HSBColorType.HSBColor hsbColor =
                        HSBColorToRGBColorTransformer.transform(
                                RGBColor.newBuilder().setRed(Color.red(getLastValue())).setGreen(Color.green(getLastValue())).setBlue(Color.blue(getLastValue())).build());

                ((Future)Service$.invokeOperationServiceMethod(ServiceType.COLOR_STATE_SERVICE, unitRemote,
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
            colorPickerView.addOnColorChangedListener(value -> recurrenceEventFilter.trigger(value));
        }
        else {
            colorPickerView.setClickable(false);
            lightnessSlider.setClickable(false);
        }
    }

    @Override
    public void updateDynamicContent() {
        try {
            ColorState colorState = (ColorState) Service$.invokeProviderServiceMethod(ServiceType.COLOR_STATE_SERVICE, unitRemote);
            RGBColor rgbColor;

            if (colorState.getColor().getType().equals(ColorType.Color.Type.HSB)) {
                rgbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getHsbColor());
            }
            else {
                rgbColor = colorState.getColor().getRgbColor();
            }

            activity.runOnUiThread(() -> colorPickerView.setColor(Color.rgb(rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue()), false));

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
