package org.openbase.bco.bcomfy.activityCore.deviceList;

import android.view.View;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.interfaces.OnDeviceClickedListener;


public class DeviceViewHolder extends ChildViewHolder<Device> {

    private View deviceView;
    private TextView deviceTextView;

    public DeviceViewHolder(View itemView) {
        super(itemView);
        deviceView = itemView;
        deviceTextView = (TextView) itemView.findViewById(R.id.device_textview);
    }

    public void bind(Device device, OnDeviceClickedListener onDeviceClickedListener) {
        deviceTextView.setText(device.getLabel());
        deviceView.setOnClickListener(v -> onDeviceClickedListener.onDeviceClicked(device.getId()));
    }
}
