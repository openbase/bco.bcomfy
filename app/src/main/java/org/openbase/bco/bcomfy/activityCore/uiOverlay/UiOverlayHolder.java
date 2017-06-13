package org.openbase.bco.bcomfy.activityCore.uiOverlay;


import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.uiOverlay.unitSelectorHolder.SelectorHolderFactory;
import org.openbase.bco.bcomfy.interfaces.OnDeviceClickedListener;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.Matrix4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitTemplateType;

public class UiOverlayHolder {
    private static final String TAG = UiOverlayHolder.class.getSimpleName();

    private RelativeLayout uiOverlay;
    private Context context;
    private OnDeviceClickedListener onDeviceClickedListener;

    private List<AbstractUnitSelectorHolder> units;

    public UiOverlayHolder(Context context, OnDeviceClickedListener onDeviceClickedListener) {
        this.uiOverlay               = ((Activity) context).findViewById(R.id.ui_container);
        this.context                 = context;
        this.onDeviceClickedListener = onDeviceClickedListener;

        units = new ArrayList<>();
    }

    public void updateBcoToPixelTransform(Matrix4 bcoToPixelTransform) {
        StreamSupport.stream(units).forEach(unit ->
                unit.alignViewToPixel(context, bcoToPixelTransform));
    }

    public void showAllDevices() {
        updateUiOverlay();

//        try {
//            Registries.getUnitRegistry().addDataObserver((observable, unitRegistryData) -> {
//                updateUiOverlay();
//            });
//        } catch (NotAvailableException | InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public void updateUiOverlay() {
        new fetchNewUnitMapTask(returnObject -> {
            clearUiOverlay();

            units = returnObject;

            StreamSupport.stream(units).forEach(unit -> {
                unit.setUnitSelector(createNewDeviceView());
                unit.initIcon();
                unit.setParentWidth(uiOverlay.getWidth());
                unit.setParentHeight(uiOverlay.getHeight());
                unit.getUnitSelector().setOnClickListener(v -> onDeviceClickedListener.onDeviceClicked(unit.getDeviceId()));
                uiOverlay.addView(unit.getUnitSelector());
            });
        }).execute();
    }

    public void setUiOverlayVisibility(int visibility) {
        uiOverlay.setVisibility(visibility);
    }

    private void clearUiOverlay() {
        StreamSupport.stream(units).forEach(unit ->
                ((Activity) context).runOnUiThread(() -> uiOverlay.removeView(unit.getUnitSelector())));
    }

    private View createNewDeviceView() {
        View unitView = LayoutInflater.from(context).inflate(R.layout.core_selector, uiOverlay, false);
        unitView.setVisibility(View.INVISIBLE);
        return unitView;
    }

    private static class fetchNewUnitMapTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = UiOverlayHolder.fetchNewUnitMapTask.class.getSimpleName();
        private OnTaskFinishedListener<List<AbstractUnitSelectorHolder>> listener;
        private List<AbstractUnitSelectorHolder> newUnitList;

        fetchNewUnitMapTask(OnTaskFinishedListener<List<AbstractUnitSelectorHolder>> listener) {
            this.listener = listener;
            this.newUnitList = new ArrayList<>();
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
                                newUnitList.add(SelectorHolderFactory.createUnitSelectorHolder(unitConfig));
                            } catch (InterruptedException | CouldNotPerformException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (CouldNotPerformException | InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            listener.taskFinishedCallback(newUnitList);
        }
    }

}
