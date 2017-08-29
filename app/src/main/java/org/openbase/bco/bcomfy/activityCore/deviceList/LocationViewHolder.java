package org.openbase.bco.bcomfy.activityCore.deviceList;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.interfaces.OnDeviceSelectedListener;


public class LocationViewHolder extends ParentViewHolder<Location, Device> {

    private static final String TAG = LocationViewHolder.class.getSimpleName();

    private View divider;
    private TextView label;
    private ImageView indicator;
    private OnDeviceSelectedListener onDeviceSelectedListener;

    public LocationViewHolder(View itemView, OnDeviceSelectedListener onDeviceSelectedListener) {
        super(itemView);
        divider = itemView.findViewById(R.id.location_divider_top);
        label = itemView.findViewById(R.id.location_textview);
        indicator = itemView.findViewById(R.id.location_indicator);
        this.onDeviceSelectedListener = onDeviceSelectedListener;

        indicator.setImageDrawable(new IconicsDrawable(itemView.getContext(), GoogleMaterial.Icon.gmd_chevron_left).color(Color.WHITE).sizeDp(12));
    }

    public void bind(Location location) {
        label.setText(location.getLabel());
        if (getAdapterPosition() == 0) {
            divider.setVisibility(View.INVISIBLE);
        }

        itemView.setOnLongClickListener(view -> {
            Log.i("BCOMFY_STUDY", "OPEN_LOCATION: " + label.getText());
            onDeviceSelectedListener.onDeviceSelected(location.getUnitConfig());
            return true;
        });
    }

    @Override
    public void onExpansionToggled(boolean expanded) {
        super.onExpansionToggled(expanded);

        if (expanded) {
            indicator.animate()
                    .setDuration(200)
                    .rotation(0.0f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        else {
            indicator.animate()
                    .setDuration(200)
                    .rotation(-90.0f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }
}
