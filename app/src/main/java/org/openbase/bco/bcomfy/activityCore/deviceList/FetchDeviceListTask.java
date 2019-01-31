package org.openbase.bco.bcomfy.activityCore.deviceList;

import android.os.AsyncTask;
import android.util.Log;

import org.openbase.bco.bcomfy.activityCore.ListSettingsDialogFragment.SettingValue;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java8.util.stream.StreamSupport;
import org.openbase.type.domotic.unit.UnitTemplateType;
import org.openbase.type.domotic.unit.location.LocationConfigType;

public final class FetchDeviceListTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = FetchDeviceListTask.class.getSimpleName();
    private SettingValue unitSetting;
    private SettingValue locationSetting;
    private OnTaskFinishedListener<List<Location>> listener;
    private List<Location> locationList;

    public FetchDeviceListTask(SettingValue unitSetting, SettingValue locationSetting, OnTaskFinishedListener<List<Location>> listener) {
        this.unitSetting = unitSetting;
        this.locationSetting = locationSetting;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        locationList = new ArrayList<>();

        try {
            Registries.getUnitRegistry().waitForData(10, TimeUnit.SECONDS);

            StreamSupport.stream(Registries.getUnitRegistry().getUnitConfigs(UnitTemplateType.UnitTemplate.UnitType.LOCATION))
                    .filter(locationConfig -> locationConfig.getLocationConfig().getType().equals(LocationConfigType.LocationConfig.LocationType.TILE) &&
                            ((locationSetting == SettingValue.ALL) ||
                            (locationConfig.getPlacementConfig().getShape().getFloorCount() > 0)))
                    .sorted((o1, o2) ->
                            LabelProcessor.getBestMatch(o1.getLabel(), "?")
                                    .compareTo(LabelProcessor.getBestMatch(o2.getLabel(), "?")))
                    .forEach(locationConfig -> locationList.add(new Location(locationConfig, unitSetting)));

            for (Iterator<Location> it = locationList.iterator(); it.hasNext();) {
                Location location = it.next();
                if (location.getChildList().size() == 0) {
                    it.remove();
                }
            }

        } catch (CouldNotPerformException | InterruptedException e) {
            Log.e(TAG, "Could not fetch locations!\n" + Log.getStackTraceString(e));
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        listener.taskFinishedCallback(locationList);
    }

}
