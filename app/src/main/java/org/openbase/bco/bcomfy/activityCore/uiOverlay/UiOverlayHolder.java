package org.openbase.bco.bcomfy.activityCore.uiOverlay;


import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitTemplateType;
import rst.geometry.TranslationType;

public class UiOverlayHolder {
    private static final String TAG = UiOverlayHolder.class.getSimpleName();

    private RelativeLayout uiOverlay;
    private Context context;

    private List<UnitSelectorHolder> units;

    public UiOverlayHolder(Context context) {

        this.uiOverlay = (RelativeLayout) ((Activity) context).findViewById(R.id.ui_container);
        this.context = context;

        units = new ArrayList<>();
    }

    public void updateBcoToPixelTransform(Matrix4 bcoToPixelTransform) {
        StreamSupport.stream(units).forEach(unit ->
                unit.alignViewToPixel(context, bcoToPixelTransform));
    }

    public void showAllDevices() {
        clearUiOverlay();

        new fetchNewUnitMapTask(returnObject -> {
            units = returnObject;

            StreamSupport.stream(units).forEach(unit -> {
                unit.setUnitSelector(createNewDeviceView());
                unit.setParentWidth(uiOverlay.getWidth());
                unit.setParentHeight(uiOverlay.getHeight());
                uiOverlay.addView(unit.getUnitSelector());
            });
        }).execute();
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
        private OnTaskFinishedListener<List<UnitSelectorHolder>> listener;
        private List<UnitSelectorHolder> newUnitList;

        public fetchNewUnitMapTask(OnTaskFinishedListener<List<UnitSelectorHolder>> listener) {
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
                                Log.e(TAG, "device: " + unitConfig.getLabel());
                                TranslationType.Translation unitPosition = unitConfig.getPlacementConfig().getPosition().getTranslation();
                                Vector3d unitVector = new Vector3d(unitPosition.getX(), unitPosition.getY(), unitPosition.getZ());

                                Transform3D transform3D = Registries.getLocationRegistry().getUnitTransformation(unitConfig).get().getTransform();
                                transform3D.invert();
                                transform3D.transform(unitVector);

                                Vector3 rootUnitPosition = new Vector3(unitVector.x, unitVector.y, unitVector.z);

                                UnitSelectorHolder unitSelectorHolder = new UnitSelectorHolder(unitConfig.getId(), null, rootUnitPosition);
                                newUnitList.add(unitSelectorHolder);
                            } catch (NotAvailableException | InterruptedException | ExecutionException e) {
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
