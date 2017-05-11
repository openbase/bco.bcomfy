package org.openbase.bco.bcomfy.activityCore.unitList;

import android.view.View;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;

import org.openbase.bco.bcomfy.R;


public class DeviceViewHolder extends ChildViewHolder {

    private TextView deviceTextView;

    public DeviceViewHolder(View itemView) {
        super(itemView);
        deviceTextView = (TextView) itemView.findViewById(R.id.device_textview);
    }

    public void bind(Device device) {
        deviceTextView.setText(device.getLabel());
    }
}
