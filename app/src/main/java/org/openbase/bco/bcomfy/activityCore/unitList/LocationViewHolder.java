package org.openbase.bco.bcomfy.activityCore.unitList;

import android.view.View;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ParentViewHolder;

import org.openbase.bco.bcomfy.R;


public class LocationViewHolder extends ParentViewHolder {

    private View divider;
    private TextView label;

    public LocationViewHolder(View itemView) {
        super(itemView);
        divider = itemView.findViewById(R.id.location_divider_top);
        label = (TextView) itemView.findViewById(R.id.location_textview);
    }

    public void bind(Location location) {
        label.setText(location.getLabel());
        if (getAdapterPosition() == 0) {
            divider.setVisibility(View.INVISIBLE);
        }
    }
}
