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
import rst.domotic.state.ContactStateType;
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;

public class ContactStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = ContactStateServiceViewHolder.class.getSimpleName();

    private TextView textView;
    private ImageView imageView;

    private Drawable contactClosed;
    private Drawable contactOpen;

    public ContactStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_contact_state, viewParent, false);
        textView = serviceView.findViewById(R.id.contactStateTextView);
        imageView = serviceView.findViewById(R.id.contactStateImageView);

        contactClosed = new IconicsDrawable(serviceView.getContext(), GoogleMaterial.Icon.gmd_vertical_align_center).color(Color.WHITE).sizeDp(24);
        contactOpen   = new IconicsDrawable(serviceView.getContext(), GoogleMaterial.Icon.gmd_unfold_more).color(Color.WHITE).sizeDp(24);
    }

    @Override
    public void updateDynamicContent() {
        try {
            ContactState contactState = (ContactState) Service$.invokeProviderServiceMethod(ServiceType.CONTACT_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> {
                switch (contactState.getValue()) {
                    case OPEN:
                        textView.setText("OPEN");
                        imageView.setImageDrawable(contactOpen);
                        break;
                    case CLOSED:
                        textView.setText("CLOSED");
                        imageView.setImageDrawable(contactClosed);
                        break;
                }
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
