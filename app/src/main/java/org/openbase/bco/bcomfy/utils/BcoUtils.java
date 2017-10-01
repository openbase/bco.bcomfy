package org.openbase.bco.bcomfy.utils;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.BComfy;
import org.openbase.bco.bcomfy.activitySettings.SettingsActivity;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.vecmath.Point3d;

import java8.util.stream.StreamSupport;
import rsb.introspection.LacksOsInformationException;
import rsb.util.os.RuntimeOsUtilities;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.location.LocationConfigType;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.math.Vec3DDoubleType;
import rst.spatial.PlacementConfigType;
import rst.spatial.ShapeType;

public final class BcoUtils {

    private static final String TAG = BcoUtils.class.getSimpleName();

    public static boolean filterByMetaTag(UnitConfig unitConfig) {
        boolean preferenceUseFilter = PreferenceManager.getDefaultSharedPreferences(BComfy.getAppContext())
                .getBoolean(SettingsActivity.KEY_PREF_MISC_USE_META_FILTER, false);

        String preferenceFilterValue = PreferenceManager.getDefaultSharedPreferences(BComfy.getAppContext())
                .getString(SettingsActivity.KEY_PREF_MISC_META_FILTER, "CUSTOM_META_FILTER");

        if (preferenceUseFilter) {
            try {
                MetaConfigVariableProvider mcvp = new MetaConfigVariableProvider("UnitConfig", unitConfig.getMetaConfig());
                return Boolean.parseBoolean(mcvp.getValue(preferenceFilterValue));
            } catch (NotAvailableException ex) {
                return false;
            }
        }
        else {
            return true;
        }
    }

    public static class UpdateUnitPositionTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = UpdateLocationShapeTask.class.getSimpleName();
        private UnitConfig unitConfig;
        private double[] glToBcoTransform;
        private double[] newPosition;
        private OnTaskFinishedListener<Boolean> listener;
        private boolean updateSuccessful;

        public UpdateUnitPositionTask(UnitConfig unitConfig, double[] glToBcoTransform, double[] newPosition, OnTaskFinishedListener<Boolean> listener) {
            this.unitConfig = unitConfig;
            this.glToBcoTransform = glToBcoTransform;
            this.newPosition = newPosition;
            this.listener = listener;
            this.updateSuccessful = false;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Transform OpenGL position to BCO Position
                double[] bcoPosition = TangoSupport.doubleTransformPoint(glToBcoTransform, newPosition);
//                double[] bcoPosition = TangoSupport.doubleTransformPoint(glToBcoTransform, currentEditPosition.toArray());

                // Get location for that specific coordinate
                List<UnitConfig> locations =
                        Registries.getLocationRegistry().getLocationConfigsByCoordinate(
                                Vec3DDoubleType.Vec3DDouble.newBuilder().setX(bcoPosition[0]).setY(bcoPosition[1]).setZ(bcoPosition[2]).build());

                UnitConfig[] location = new UnitConfig[1];

                if (locations.size() == 0) {
                    location[0] = Registries.getLocationRegistry().getLocationConfigById(unitConfig.getPlacementConfig().getLocationId());
                    Log.w(TAG, "No location found for current unit position! Retaining old location information...");
                }
                else {
                    // Get Region if there is any
                    StreamSupport.stream(locations)
                            .filter(unitConfig -> unitConfig.getLocationConfig().getType() == LocationConfigType.LocationConfig.LocationType.REGION)
                            .findAny()
                            .ifPresent(unitConfig -> location[0] = unitConfig);
                    // Otherwise use tile if there is any
                    if (location[0] == null) {
                        StreamSupport.stream(locations)
                                .filter(unitConfig -> unitConfig.getLocationConfig().getType() == LocationConfigType.LocationConfig.LocationType.TILE)
                                .findAny()
                                .ifPresent(unitConfig -> location[0] = unitConfig);
                    }
                    // Otherwise return... Unknown LocationType...
                    if (location[0] == null) {
                        location[0] = Registries.getLocationRegistry().getLocationConfigById(unitConfig.getPlacementConfig().getLocationId());
                        Log.w(TAG, "No valid location found for selected position! Retaining old location information...");
                    }
                }

                // Transform BCO-Root position to BCO-Location-of-selected-point position
                Point3d transformedBcoPosition = new Point3d(bcoPosition[0], bcoPosition[1], bcoPosition[2]);
                Registries.getLocationRegistry().waitForData();
                Registries.getLocationRegistry().getUnitTransformation(location[0]).get(3, TimeUnit.SECONDS).getTransform().transform(transformedBcoPosition);

                // Generate new protobuf unitConfig
                TranslationType.Translation translation =
                        unitConfig.getPlacementConfig().getPosition().getTranslation().toBuilder().setX(transformedBcoPosition.x).setY(transformedBcoPosition.y).setZ(transformedBcoPosition.z).build();
                RotationType.Rotation rotation;
                if (unitConfig.getPlacementConfig().hasPosition()) {
                    rotation = unitConfig.getPlacementConfig().getPosition().getRotation();
                }
                else {
                    rotation = RotationType.Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
                }
                PoseType.Pose pose  =
                        unitConfig.getPlacementConfig().getPosition().toBuilder().setTranslation(translation).setRotation(rotation).build();
                PlacementConfigType.PlacementConfig placementConfig =
                        unitConfig.getPlacementConfig().toBuilder().setPosition(pose).setLocationId(location[0].getId()).build();
                UnitConfig newUnitConfig =
                        unitConfig.toBuilder().setPlacementConfig(placementConfig).build();

                // Update unitConfig
                Registries.getUnitRegistry().updateUnitConfig(newUnitConfig).get();

                updateSuccessful = true;
            } catch (TimeoutException | CouldNotPerformException | InterruptedException | ExecutionException e) {
                Log.e(TAG, "Error while updating locationConfig of unit: " + unitConfig, e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            listener.taskFinishedCallback(updateSuccessful);
        }
    }

    public static class UpdateLocationShapeTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = UpdateLocationShapeTask.class.getSimpleName();
        private String locationId;
        ArrayList<Vector3> ground;
        double[] glToBcoTransform;
        private OnTaskFinishedListener<Void> listener;

        public UpdateLocationShapeTask(String locationId, ArrayList<Vector3> ground, double[] glToBcoTransform, OnTaskFinishedListener<Void> listener) {
            this.locationId = locationId;
            this.ground = ground;
            this.glToBcoTransform = glToBcoTransform;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Transform vertices into BCO world
            ArrayList<Vector3> transformedGround = new ArrayList<>();
            StreamSupport.stream(ground).forEach(vector3 -> transformedGround.add(
                    new Vector3(TangoSupport.doubleTransformPoint(glToBcoTransform, vector3.toArray()))
            ));


            // Calculate the origin of the location (i.e. the most north western vertex)
            int originVertexIndex = -1;
            double originVertexCoordinateSum = Double.MAX_VALUE;
            for (int currentVertexIndex = 0; currentVertexIndex < transformedGround.size(); currentVertexIndex++) {
                double currentVertexCoordinateSum = transformedGround.get(currentVertexIndex).x + transformedGround.get(currentVertexIndex).y;

                if (currentVertexCoordinateSum < originVertexCoordinateSum) {
                    originVertexIndex = currentVertexIndex;
                    originVertexCoordinateSum = currentVertexCoordinateSum;
                }
            }
            Vector3 originVertex = transformedGround.get(originVertexIndex).clone();

            // Align all the vertices according to the origin
            for (Vector3 vertex : transformedGround) vertex.subtract(originVertex);

            // Publish the result to the locationRegistry
            try {
                // Fetch the UnitConfig of the target location
                UnitConfig locationConfig = Registries.getLocationRegistry().getLocationConfigById(locationId);

                // Build the pose
                TranslationType.Translation.Builder translationBuilder = TranslationType.Translation.getDefaultInstance().toBuilder();
                translationBuilder.setX(originVertex.x).setY(originVertex.y).setZ(originVertex.z);
                RotationType.Rotation.Builder rotationBuilder = RotationType.Rotation.getDefaultInstance().toBuilder();
                rotationBuilder.setQw(1.0).setQx(0.0).setQy(0.0).setQz(0.0);

                PoseType.Pose pose = locationConfig.getPlacementConfig().getPosition().toBuilder()
                        .setRotation(rotationBuilder.build()).setTranslation(translationBuilder.build()).build();

                // Build the shape
                ShapeType.Shape.Builder shapeBuilder = ShapeType.Shape.getDefaultInstance().toBuilder();
                for (Vector3 vector3 : transformedGround) {
                    shapeBuilder.addFloor(Vec3DDoubleType.Vec3DDouble.getDefaultInstance().toBuilder()
                            .setX(vector3.x).setY(vector3.y).setZ(vector3.z).build());
                }

                // Build the locationConfig
                PlacementConfigType.PlacementConfig placementConfig = locationConfig.getPlacementConfig().toBuilder().clearShape().setShape(shapeBuilder.build()).setPosition(pose).build();
                UnitConfig newLocationConfig = locationConfig.toBuilder().clearPlacementConfig().setPlacementConfig(placementConfig).build();

                // Update the locationConfig
                Registries.getLocationRegistry().updateLocationConfig(newLocationConfig);
            } catch (InterruptedException | CouldNotPerformException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            } catch (LacksOsInformationException | RuntimeOsUtilities.RuntimeNotAvailableException e) {
                Log.w(TAG, "No PID information available.");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            listener.taskFinishedCallback(null);
        }
    }

    public static class DeleteAllDevicePosesTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = DeleteAllDevicePosesTask.class.getSimpleName();
        private OnTaskFinishedListener<Boolean> listener;
        private Boolean successful;

        public DeleteAllDevicePosesTask(OnTaskFinishedListener<Boolean> listener) {
            this.listener = listener;
            this.successful = Boolean.FALSE;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                StreamSupport.stream(Registries.getUnitRegistry().getUnitConfigs())
                        .filter(unitConfig -> unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE)
                        .filter(unitConfig -> unitConfig.getPlacementConfig().hasPosition())
                        .forEach(unitConfig -> {
                            try {
                                PlacementConfigType.PlacementConfig placementConfig =
                                    unitConfig.getPlacementConfig().toBuilder().clearPosition().build();

                                UnitConfig newUnitConfig =
                                    unitConfig.toBuilder().setPlacementConfig(placementConfig).build();

                                Registries.getUnitRegistry().updateUnitConfig(newUnitConfig);
                            } catch (CouldNotPerformException | InterruptedException e) {
                                Log.e(TAG, "Error while updating unitConfig!", e);
                            }
                        });

                successful = Boolean.TRUE;
            } catch (CouldNotPerformException | InterruptedException e) {
                Log.e(TAG, "Error while fetching unitConfigs!", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            listener.taskFinishedCallback(successful);
        }
    }

    public static class DeleteAllLocationShapesTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = DeleteAllLocationShapesTask.class.getSimpleName();
        private OnTaskFinishedListener<Boolean> listener;
        private Boolean successful;

        public DeleteAllLocationShapesTask(OnTaskFinishedListener<Boolean> listener) {
            this.listener = listener;
            this.successful = Boolean.FALSE;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                StreamSupport.stream(Registries.getUnitRegistry().getUnitConfigs())
                        .filter(unitConfig -> unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.LOCATION ||
                                                unitConfig.getType() == UnitTemplateType.UnitTemplate.UnitType.CONNECTION)
                        .filter(unitConfig -> unitConfig.getPlacementConfig().getShape().getFloorCount() > 0)
                        .forEach(unitConfig -> {
                            try {
                                PlacementConfigType.PlacementConfig placementConfig =
                                        unitConfig.getPlacementConfig().toBuilder().clearShape().clearPosition().build();

                                UnitConfig newUnitConfig =
                                        unitConfig.toBuilder().setPlacementConfig(placementConfig).build();

                                Registries.getUnitRegistry().updateUnitConfig(newUnitConfig);
                            } catch (CouldNotPerformException | InterruptedException e) {
                                Log.e(TAG, "Error while updating unitConfig!", e);
                            }
                        });

                successful = Boolean.TRUE;
            } catch (CouldNotPerformException | InterruptedException e) {
                Log.e(TAG, "Error while fetching unitConfigs!", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            listener.taskFinishedCallback(successful);
        }
    }

}
