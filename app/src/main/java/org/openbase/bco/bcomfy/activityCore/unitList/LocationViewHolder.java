package org.openbase.bco.bcomfy.activityCore.unitList;

import android.view.View;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ParentViewHolder;

import org.openbase.bco.bcomfy.R;


public class LocationViewHolder extends ParentViewHolder {

    private TextView locationTextView;

    public LocationViewHolder(View itemView) {
        super(itemView);
        locationTextView = (TextView) itemView.findViewById(R.id.location_textview);
    }

    public void bind(Location location) {
        locationTextView.setText(location.getLabel());
    }
}
