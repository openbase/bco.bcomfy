package org.openbase.bco.bcomfy.activityCore.uiOverlay;


import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder.AbstractUnitSelectorHolder;
import org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder.SelectorHolderFactory;
import org.openbase.bco.bcomfy.interfaces.OnDeviceSelectedListener;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.bcomfy.utils.BcoUtils;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.Matrix4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;

public class UiOverlayHolder implements OnDeviceSelectedListener {
    private static final String TAG = UiOverlayHolder.class.getSimpleName();

    private RelativeLayout uiOverlay;
    private Context context;
    private OnDeviceSelectedListener onDeviceSelectedListener;
    private OnUiOverlayInitiatedListener onUiOverlayInitiatedListener;

    private HashMap<String, AbstractUnitSelectorHolder> holderMap;
    private AbstractUnitSelectorHolder selectedHolder;

    public UiOverlayHolder(Context context, OnDeviceSelectedListener onDeviceSelectedListener, OnUiOverlayInitiatedListener onUiOverlayInitiatedListener) {
        this.uiOverlay                    = ((Activity) context).findViewById(R.id.ui_container);
        this.context                      = context;
        this.onDeviceSelectedListener     = onDeviceSelectedListener;
        this.onUiOverlayInitiatedListener = onUiOverlayInitiatedListener;

        holderMap = new HashMap<>();
        selectedHolder = null;
    }

    public void updateBcoToPixelTransform(Matrix4 bcoToPixelTransform) {
        StreamSupport.stream(holderMap.values()).forEach(unit ->
                unit.alignViewToPixel(context, bcoToPixelTransform));
    }

    public void initUiOverlay() {
        new fetchNewUnitMapTask(returnObject -> {
            clearUiOverlay();
            holderMap = returnObject;
            StreamSupport.stream(holderMap.values()).forEach(this::initUnitSelector);
            onUiOverlayInitiatedListener.onUiOverlayInitiated();
        }).execute();
    }

    public void checkAndAddNewUnit(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        if (!holderMap.containsKey(unitConfig.getId())) {
            AbstractUnitSelectorHolder holder = SelectorHolderFactory.createUnitSelectorHolder(unitConfig);

            holderMap.put(unitConfig.getId(), holder);
            initUnitSelector(holder);
        }
    }

    public void removeUnit(UnitConfig unitConfig) {
        if (!holderMap.containsKey(unitConfig.getId())) {
            uiOverlay.removeView(holderMap.get(unitConfig.getId()).getView());
            holderMap.remove(unitConfig.getId());
        }
    }

    public void setUiOverlayVisibility(int visibility) {
        uiOverlay.setVisibility(visibility);
    }

    private void clearUiOverlay() {
        StreamSupport.stream(holderMap.values()).forEach(unit ->
                ((Activity) context).runOnUiThread(() -> uiOverlay.removeView(unit.getView())));
    }

    private View createNewDeviceView() {
        View unitView = LayoutInflater.from(context).inflate(R.layout.core_selector, uiOverlay, false);
        unitView.setVisibility(View.INVISIBLE);
        return unitView;
    }

    private void initUnitSelector(AbstractUnitSelectorHolder holder) {
        holder.setView(createNewDeviceView());
        holder.initIcon();
        holder.setParentWidth(uiOverlay.getWidth());
        holder.setParentHeight(uiOverlay.getHeight());
        holder.getView().setOnClickListener(v -> {
            try {
                onDeviceSelectedListener.onDeviceSelected(holder.getCorrespondingUnitConfig());
            } catch (CouldNotPerformException | InterruptedException ex) {
                Log.e(TAG, "Error while trying to get corresponding unitConfig of unit " + holder.getCorrespondingUnitId(), ex);
            }
        });
        uiOverlay.addView(holder.getView());
    }

    @Override
    public void onDeviceSelected(UnitConfig unitConfig) {
        if (selectedHolder != null) selectedHolder.setSelected(false);

        if (holderMap.containsKey(unitConfig.getId())) {
            AbstractUnitSelectorHolder holder = holderMap.get(unitConfig.getId());

            selectedHolder = holder;
            holder.setSelected(true);
        }
    }

    private static class fetchNewUnitMapTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = UiOverlayHolder.fetchNewUnitMapTask.class.getSimpleName();
        private OnTaskFinishedListener<HashMap<String, AbstractUnitSelectorHolder>> listener;
        private HashMap<String, AbstractUnitSelectorHolder> newUnitMap;

        fetchNewUnitMapTask(OnTaskFinishedListener<HashMap<String, AbstractUnitSelectorHolder>> listener) {
            this.listener = listener;
            this.newUnitMap = new HashMap<>();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                StreamSupport.stream(Registries.getUnitRegistry().getUnitConfigs())
                        .filter(BcoUtils::filterByMetaTag)
                        .filter(unitConfig -> unitConfig.getPlacementConfig().hasPosition())
                        .filter(unitConfig -> unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE ||
                                (!unitConfig.getBoundToUnitHost() &&
                                        unitConfig.getType() != UnitTemplateType.UnitTemplate.UnitType.LOCATION &&
                                        unitConfig.getType() != UnitTemplateType.UnitTemplate.UnitType.CONNECTION))
                        .filter(unitConfig -> unitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED)
                        .forEach(unitConfig -> {
                            try {
                                Log.i(TAG, "fetched unit: " + unitConfig.getLabel() + " -> [" + unitConfig.getId() + "]");
                                newUnitMap.put(unitConfig.getId(), SelectorHolderFactory.createUnitSelectorHolder(unitConfig));
                            } catch (InterruptedException | CouldNotPerformException e) {
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                        });
            } catch (CouldNotPerformException | InterruptedException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            listener.taskFinishedCallback(newUnitMap);
        }
    }

    public void updateBlobVisibility(boolean distantBlobsSetting, UnitConfig location) {
        // Set all blob visibilites to true if the distantBlobsSetting is enabled
        if (distantBlobsSetting) {
            updateBlobVisibilityAllToTrue();
            return;
        }

        // Get a list of all present units in the current location
        List<UnitConfig> unitsInCurrentLocation;
        List<String> unitIdsInCurrentLocation;
        try {
            unitsInCurrentLocation = Registries.getLocationRegistry().getUnitConfigsByLocation(location.getId(), true);
            unitIdsInCurrentLocation = StreamSupport.stream(unitsInCurrentLocation).map(UnitConfig::getId).collect(Collectors.toList());
        } catch (CouldNotPerformException | InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(new CouldNotPerformException("Error while fetching units of location " + location.getId() + "!\nDrawing all blobs regardless of the current location...", e)));
            updateBlobVisibilityAllToTrue();
            return;
        }

        // Check for each blob whether its unitId is present in the previously fetched list and set its visibility respectively
        StreamSupport.stream(holderMap.values()).forEach(unit -> {
                    if (StreamSupport.stream(unitIdsInCurrentLocation).anyMatch(s -> s.equals(unit.getCorrespondingUnitId()))) {
                        unit.setVisible(true);
                    }
                    else {
                        unit.setVisible(false);
                    }
                });
    }

    private void updateBlobVisibilityAllToTrue() {
        StreamSupport.stream(holderMap.values()).forEach(unit -> unit.setVisible(true));
    }

}
