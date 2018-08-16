package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.state.MotionStateType;
import rst.domotic.state.MotionStateType.MotionState;

public class MotionStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = MotionStateServiceViewHolder.class.getSimpleName();

    private TextView textView;
    private ImageView imageView;

    private Drawable motionTrue;
    private Drawable motionFalse;

    private Animation rotation;

    public MotionStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_motion_state, viewParent, false);
        textView = serviceView.findViewById(R.id.motionStateTextView);
        imageView = serviceView.findViewById(R.id.motionStateImageView);

        rotation = AnimationUtils.loadAnimation(serviceView.getContext(), R.anim.rotate);
        rotation.setFillAfter(true);

        motionTrue  = new IconicsDrawable(serviceView.getContext(), GoogleMaterial.Icon.gmd_blur_on).color(Color.WHITE).sizeDp(24);
        motionFalse = new IconicsDrawable(serviceView.getContext(), GoogleMaterial.Icon.gmd_blur_off).color(Color.WHITE).sizeDp(24);
    }

    @Override
    public void updateDynamicContent() {
        try {
            MotionState motionState = (MotionState) Services.invokeProviderServiceMethod(ServiceType.MOTION_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> {
                switch (motionState.getValue()) {
                    case MOTION:
                        textView.setText("Motion detected");
                        textView.setTextColor(Color.CYAN);
                        imageView.setImageDrawable(motionTrue);
                        imageView.startAnimation(rotation);
                        break;
                    case NO_MOTION:
                        textView.setText("No motion");
                        textView.setTextColor(Color.GRAY);
                        imageView.setImageDrawable(motionFalse);
                        imageView.clearAnimation();
                }
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
