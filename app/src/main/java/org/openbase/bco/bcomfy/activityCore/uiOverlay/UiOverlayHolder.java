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
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.Matrix4;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;

public class UiOverlayHolder implements OnDeviceSelectedListener {
    private static final String TAG = UiOverlayHolder.class.getSimpleName();

    private RelativeLayout uiOverlay;
    private Context context;
    private OnDeviceSelectedListener onDeviceSelectedListener;

    private HashMap<String, AbstractUnitSelectorHolder> holderMap;
    private AbstractUnitSelectorHolder selectedHolder;

    public UiOverlayHolder(Context context, OnDeviceSelectedListener onDeviceSelectedListener) {
        this.uiOverlay                = ((Activity) context).findViewById(R.id.ui_container);
        this.context                  = context;
        this.onDeviceSelectedListener = onDeviceSelectedListener;

        holderMap = new HashMap<>();
        selectedHolder = null;
    }

    public void updateBcoToPixelTransform(Matrix4 bcoToPixelTransform) {
        StreamSupport.stream(holderMap.values()).forEach(unit ->
                unit.alignViewToPixel(context, bcoToPixelTransform));
    }

    public void showAllDevices() {
        updateUiOverlay();

//        try {
//            Registries.getUnitRegistry().addDataObserver((observable, unitRegistryData) -> {
//                updateUiOverlay();
//            });
//        } catch (NotAvailableException | InterruptedException e) {
//            Log.e(TAG, Log.getStackTraceString(e));
//        }
    }

    public void updateUiOverlay() {
        new fetchNewUnitMapTask(returnObject -> {
            clearUiOverlay();

            holderMap = returnObject;

            StreamSupport.stream(holderMap.values()).forEach(this::initUnitSelector);
        }).execute();
    }

    public void checkAndAddNewUnit(UnitConfigType.UnitConfig unitConfig) throws InterruptedException, ExecutionException, CouldNotPerformException, TimeoutException {
        if (!holderMap.containsKey(unitConfig.getId())) {
            AbstractUnitSelectorHolder holder = SelectorHolderFactory.createUnitSelectorHolder(unitConfig);

            holderMap.put(unitConfig.getId(), holder);
            initUnitSelector(holder);
        }
    }

    public void removeUnit(UnitConfigType.UnitConfig unitConfig) {
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
                onDeviceSelectedListener.onDeviceSelected(holder.getUnitHostConfig());
            } catch (CouldNotPerformException | InterruptedException e) {
                Log.e(TAG, "Error while fetching unit config of unit " + holder.getUnitHostId() + "\n" + Log.getStackTraceString(e));
            }
        });
        uiOverlay.addView(holder.getView());
    }

    @Override
    public void onDeviceSelected(UnitConfigType.UnitConfig unitConfig) {
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
                StreamSupport.stream(Registries.getUnitRegistry().getUnitConfigs(UnitTemplateType.UnitTemplate.UnitType.DEVICE))
                        .filter(unitConfig -> unitConfig.getPlacementConfig().getPosition().getTranslation().getX() != 0 ||
                                unitConfig.getPlacementConfig().getPosition().getTranslation().getY() != 0 ||
                                unitConfig.getPlacementConfig().getPosition().getTranslation().getZ() != 0)
                        .forEach(unitConfig -> {
                            try {
                                Log.i(TAG, "fetched unit: " + unitConfig.getLabel() + " -> [" + unitConfig.getId() + "]");
                                newUnitMap.put(unitConfig.getId(), SelectorHolderFactory.createUnitSelectorHolder(unitConfig));
                            } catch (InterruptedException | CouldNotPerformException | ExecutionException | TimeoutException e) {
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

}
