package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.service.Service$;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.state.HandleStateType;

public class HandleStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = HandleStateServiceViewHolder.class.getSimpleName();

    private TextView textView;
    private ImageView imageView;

    public HandleStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_handle_state, viewParent, false);
        textView = serviceView.findViewById(R.id.textViewHandleState);
        imageView = serviceView.findViewById(R.id.imageViewHandleState);

        imageView.setImageDrawable(new IconicsDrawable(serviceView.getContext(), GoogleMaterial.Icon.gmd_navigation).color(Color.WHITE).sizeDp(24));
    }

    @Override
    public void updateDynamicContent() {
        try {
            HandleStateType.HandleState handleState = (HandleStateType.HandleState) Service$.invokeProviderServiceMethod(ServiceType.HANDLE_STATE_SERVICE, unitRemote);

            if (handleState.hasPosition()) {
                activity.runOnUiThread(() -> {
                    textView.setText("Handle orientation: " + handleState.getPosition() + "°");
                    imageView.setRotation(handleState.getPosition());
                });
            }
        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
