package org.openbase.bco.bcomfy.utils;

import android.os.AsyncTask;
import android.util.Log;

import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import rsb.Factory;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.introspection.LacksOsInformationException;
import rsb.patterns.RemoteServer;
import rsb.util.os.RuntimeOsUtilities;
import rst.domotic.registry.LocationRegistryDataType;
import rst.domotic.unit.UnitConfigType;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.math.Vec3DDoubleType;
import rst.spatial.PlacementConfigType;
import rst.spatial.ShapeType;

public final class LocationUtils {

    private static final String TAG = LocationUtils.class.getSimpleName();

    public static final void updateLocationShape(final RemoteServer locationRegistry, final String location, final ArrayList<Vector3> ground, final OnTaskFinishedListener<Void> listener) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... voids) {
                // Calculate the origin of the location (i.e. the most north western vertex)
                int originVertexIndex = -1;
                double originVertexCoordinateSum = Double.MAX_VALUE;
                for (int currentVertexIndex = 0; currentVertexIndex < ground.size(); currentVertexIndex++) {
                    double currentVertexCoordinateSum = ground.get(currentVertexIndex).x + ground.get(currentVertexIndex).z;

                    if (currentVertexCoordinateSum < originVertexCoordinateSum) {
                        originVertexIndex = currentVertexIndex;
                        originVertexCoordinateSum = currentVertexCoordinateSum;
                    }
                }
                Vector3 originVertex = ground.get(originVertexIndex).clone();

                // Align all the vertices according to the origin
                for (Vector3 vertex : ground) {
                    vertex.subtract(originVertex);
                }

                // Publish the result to the locationRegistry
                try {
                    // Fetch the UnitConfig of the target location
                    LocationRegistryDataType.LocationRegistryData lrd = (LocationRegistryDataType.LocationRegistryData) locationRegistry.call("requestStatus").getData();
                    List<UnitConfigType.UnitConfig> locationList = lrd.getLocationUnitConfigList();
                    UnitConfigType.UnitConfig locationConfig = UnitConfigType.UnitConfig.getDefaultInstance();
                    for (UnitConfigType.UnitConfig current : locationList) {
                        if (current.getLabel().equals(location)) {
                            locationConfig = current;
                            break;
                        }
                    }

                    // Build the pose
                    TranslationType.Translation.Builder translationBuilder = TranslationType.Translation.getDefaultInstance().toBuilder();
                    translationBuilder.setX(originVertex.z).setY(originVertex.x).setZ(originVertex.y);
                    RotationType.Rotation.Builder rotationBuilder = RotationType.Rotation.getDefaultInstance().toBuilder();
                    rotationBuilder.setQw(1.0).setQx(0.0).setQy(0.0).setQz(0.0);

                    PoseType.Pose pose = locationConfig.getPlacementConfig().getPosition().toBuilder()
                            .setRotation(rotationBuilder.build()).setTranslation(translationBuilder.build()).build();

                    // Build the shape
                    ShapeType.Shape.Builder shapeBuilder = ShapeType.Shape.getDefaultInstance().toBuilder();
                    for (Vector3 vector3 : ground) {
                        shapeBuilder.addFloor(Vec3DDoubleType.Vec3DDouble.getDefaultInstance().toBuilder()
                                .setX(vector3.z).setY(vector3.x).setZ(vector3.y).build());
                    }

                    // Build the locationConfig
                    PlacementConfigType.PlacementConfig placementConfig = locationConfig.getPlacementConfig().toBuilder().clearShape().setShape(shapeBuilder.build()).setPosition(pose).build();
                    UnitConfigType.UnitConfig newLocationConfig = locationConfig.toBuilder().clearPlacementConfig().setPlacementConfig(placementConfig).build();

                    // Call the RPC to update the locationConfig
                    locationRegistry.call("updateLocationConfig", newLocationConfig);
                } catch (RSBException | TimeoutException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                } catch (LacksOsInformationException | RuntimeOsUtilities.RuntimeNotAvailableException e) {
                    Log.w(TAG, "No PID information available.");
                }

                return null;
            }

            protected void onPostExecute(Void v) {
                listener.onTaskFinished(null);
            }
        }.execute((Void) null);
    }

    public static final void fetchLocationList(final RemoteServer locationRegistry, final OnTaskFinishedListener<ArrayList<CharSequence>> listener) {
        new AsyncTask<Void, Void, ArrayList<CharSequence>>() {
            protected ArrayList<CharSequence> doInBackground(Void... voids) {
                ArrayList<CharSequence> locations = new ArrayList<>();

                try {
                    LocationRegistryDataType.LocationRegistryData lrd = (LocationRegistryDataType.LocationRegistryData) (locationRegistry.call("requestStatus").getData());

                    for(UnitConfigType.UnitConfig locationUnitConfig : lrd.getLocationUnitConfigList()) {
                        locations.add(locationUnitConfig.getLabel());
                    }
                } catch (RSBException | TimeoutException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                } catch (LacksOsInformationException | RuntimeOsUtilities.RuntimeNotAvailableException e) {
                    Log.w(TAG, "No PID information available.");
                }

                return locations;
            }

            protected void onPostExecute(ArrayList<CharSequence> locationList) {
                listener.onTaskFinished(locationList);
            }
        }.execute((Void) null);
    }

    public static final void initLocationRegistry(final OnTaskFinishedListener<RemoteServer> listener) {
        new AsyncTask<Void, Void, RemoteServer>() {
            protected RemoteServer doInBackground(Void... voids) {
                RemoteServer locationRegistry = null;

                try {
                    locationRegistry = Factory.getInstance().createRemoteServer("/registry/location/ctrl", RSBDefaultConfig.getDefaultParticipantConfig());
                    DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistryDataType.LocationRegistryData.getDefaultInstance()));
                    DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfigType.UnitConfig.getDefaultInstance()));
                    locationRegistry.activate();
                } catch (RSBException e) {
                    e.printStackTrace();
                } catch (LacksOsInformationException | RuntimeOsUtilities.RuntimeNotAvailableException e) {
                    Log.w(TAG, "No PID information available.");
                }

                return locationRegistry;
            }

            protected void onPostExecute(RemoteServer locationRegistry) {
                listener.onTaskFinished(locationRegistry);
            }
        }.execute((Void) null);
    }

}
