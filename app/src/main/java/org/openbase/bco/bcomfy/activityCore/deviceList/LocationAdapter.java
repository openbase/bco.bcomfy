package org.openbase.bco.bcomfy.activityCore.deviceList;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.interfaces.OnDeviceClickedListener;

import java.util.List;

public class LocationAdapter extends ExpandableRecyclerAdapter<Location, Device, LocationViewHolder, DeviceViewHolder> {

    private LayoutInflater inflater;
    private OnDeviceClickedListener onDeviceClickedListener;


    public LocationAdapter(Context context, @NonNull List<Location> locationList, OnDeviceClickedListener onDeviceClickedListener) {
        super(locationList);
        inflater = LayoutInflater.from(context);
        this.onDeviceClickedListener = onDeviceClickedListener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateParentViewHolder(@NonNull ViewGroup parentViewGroup, int viewType) {
        View locationView = inflater.inflate(R.layout.location_textview, parentViewGroup, false);
        return new LocationViewHolder(locationView);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateChildViewHolder(@NonNull ViewGroup childViewGroup, int viewType) {
        View deviceView = inflater.inflate(R.layout.device_textview, childViewGroup, false);
        return new DeviceViewHolder(deviceView);
    }

    @Override
    public void onBindParentViewHolder(@NonNull LocationViewHolder locationViewHolder, int parentPosition, @NonNull Location location) {
        locationViewHolder.bind(location);
    }

    @Override
    public void onBindChildViewHolder(@NonNull DeviceViewHolder deviceViewHolder, int i, int i1, @NonNull Device device) {
        deviceViewHolder.bind(device, onDeviceClickedListener);
    }
}
