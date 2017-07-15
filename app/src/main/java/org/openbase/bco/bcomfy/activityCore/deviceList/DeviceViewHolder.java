package org.openbase.bco.bcomfy.activityCore.deviceList;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.interfaces.OnDeviceClickedListener;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.jul.exception.NotAvailableException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rst.domotic.unit.UnitConfigType;


public class DeviceViewHolder extends ChildViewHolder<Device> {

    private static final String TAG = DeviceViewHolder.class.getSimpleName();

    private View deviceView;
    private TextView deviceTextView;
    private ImageView noPositionIcon;

    public DeviceViewHolder(View itemView) {
        super(itemView);
        deviceView = itemView;
        deviceTextView = itemView.findViewById(R.id.device_textview);
        noPositionIcon = itemView.findViewById(R.id.no_position_icon);
        noPositionIcon.setImageDrawable(new IconicsDrawable(itemView.getContext(), GoogleMaterial.Icon.gmd_location_off).color(Color.WHITE).sizeDp(16));
    }

    public void bind(Device device, OnDeviceClickedListener onDeviceClickedListener) {
        deviceTextView.setText(device.getUnitConfig().getLabel());
        deviceView.setOnClickListener(v -> onDeviceClickedListener.onDeviceClicked(device.getUnitConfig()));
        updateNoPositionIcon(device.getUnitConfig());

        new ConnectRemoteTask(this, device).execute((Void) null);
    }

    private void updateNoPositionIcon(UnitConfigType.UnitConfig unitConfig) {
        if (unitConfig.getPlacementConfig().hasPosition()) {
            ((Activity) itemView.getContext()).runOnUiThread(() -> noPositionIcon.setVisibility(View.INVISIBLE));
        }
        else {
            ((Activity) itemView.getContext()).runOnUiThread(() -> noPositionIcon.setVisibility(View.VISIBLE));
        }
    }

    private static class ConnectRemoteTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = DeviceViewHolder.ConnectRemoteTask.class.getSimpleName();

        private DeviceViewHolder deviceViewHolder;
        private Device device;

        ConnectRemoteTask(DeviceViewHolder deviceViewHolder, Device device) {
            this.deviceViewHolder = deviceViewHolder;
            this.device = device;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                UnitRemote remote = Units.getFutureUnit(device.getUnitConfig().getId(), true).get(1, TimeUnit.SECONDS);
                remote.addConfigObserver((observable, unitConfig) -> deviceViewHolder.updateNoPositionIcon((UnitConfigType.UnitConfig) unitConfig));
            } catch (InterruptedException | ExecutionException | TimeoutException | NotAvailableException e) {
                Log.e(TAG, "Unable to get unitRemote of unit " + device.getUnitConfig().getId());
                e.printStackTrace();
            }

            return null;
        }
    }
}
