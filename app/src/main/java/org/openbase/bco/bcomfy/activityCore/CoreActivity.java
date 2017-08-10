package org.openbase.bco.bcomfy.activityCore;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoPoseData;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.TangoActivity;
import org.openbase.bco.bcomfy.TangoRenderer;
import org.openbase.bco.bcomfy.activityCore.ListSettingsDialogFragment.SettingValue;
import org.openbase.bco.bcomfy.activityCore.deviceList.FetchDeviceListTask;
import org.openbase.bco.bcomfy.activityCore.deviceList.Location;
import org.openbase.bco.bcomfy.activityCore.deviceList.LocationAdapter;
import org.openbase.bco.bcomfy.activityCore.serviceList.UnitListViewHolder;
import org.openbase.bco.bcomfy.activityCore.uiOverlay.UiOverlayHolder;
import org.openbase.bco.bcomfy.interfaces.OnDeviceClickedListener;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.vecmath.Vector3d;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationConfigType;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.math.Vec3DDoubleType;
import rst.spatial.PlacementConfigType;

public class CoreActivity extends TangoActivity implements View.OnTouchListener, OnDeviceClickedListener, ListSettingsDialogFragment.OnSettingsChosenListener {
    private static final String TAG = CoreActivity.class.getSimpleName();

    private DrawerLayout drawerLayout;
    private RelativeLayout leftDrawer;
    private RecyclerView recyclerView;
    private ProgressBar progressBarLeftDrawer;
    private LinearLayout rightDrawer;

    private FloatingActionButton fabExpandDrawer;
    private FloatingActionButton fabSettings;
    private FloatingActionButton fabEditLocation;

    private SettingValue currentUnitSetting;
    private SettingValue currentLocationSetting;

    private ListSettingsDialogFragment listSettings;

    private LinearLayout buttonsEdit;
    private Button buttonEditApply;
    private Button buttonEditCancel;
    private Button buttonEditClear;

    private Matrix4 bcoToPixelTransform;
    private UiOverlayHolder uiOverlayHolder;

    boolean inEditMode = false;
    Vector3 currentEditPosition;
    private UnitConfig currentDevice;

    private UnitListViewHolder unitListViewHolder;

    private double[] glToBcoTransform;
    private double[] bcoToGlTransform;

    private ScheduledThreadPoolExecutor sch;
    private Runnable fetchLocationLabelTask;

    private Button locationLabelButton;
    private UnitConfig currentLocation;

    private String adfUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_core);
        currentUnitSetting = SettingValue.ALL;
        currentLocationSetting = SettingValue.LOCATED;

        super.onCreate(savedInstanceState);

        adfUuid = getIntent().getStringExtra("adfUuid");

        loadLocalTransform();
        initFetchLocationLabelTask();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(IconicsContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
//        sch.remove(fetchLocationLabelTask);
    }

    @Override
    public void onResume() {
        super.onResume();
//        sch.scheduleWithFixedDelay(fetchLocationLabelTask, 1, 1, TimeUnit.SECONDS);
        uiOverlayHolder.showAllDevices();
    }



    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP && inEditMode) {
            Log.i(TAG, "Calculating...");
            // Calculate click location in u,v (0;1) coordinates.
            float u = motionEvent.getX() / view.getWidth();
            float v = motionEvent.getY() / view.getHeight();

            try {
                // Fit a plane on the clicked point using the latest point cloud data
                // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
                // and a possible service disconnection due to an onPause event.
                synchronized (this) {
                    currentEditPosition = TangoUtils.doFitPoint(u, v, rgbTimestampGlThread, tangoPointCloudManager.getLatestPointCloud(), displayRotation);
                }

                if (currentEditPosition != null) {
                    getRenderer().clearSpheres();
                    getRenderer().addSphere(currentEditPosition, Color.GRAY);
                    buttonEditApply.setEnabled(true);
                }

            } catch (TangoException t) {
                Toast.makeText(getApplicationContext(),
                        R.string.tango_error,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.tango_error), t);
            } catch (SecurityException t) {
                Toast.makeText(getApplicationContext(),
                        R.string.no_permissions,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.no_permissions), t);
            }
        }
        return true;
    }

    @Override
    protected void setupGui() {
        drawerLayout          = findViewById(R.id.drawer_layout);
        leftDrawer            = findViewById(R.id.left_drawer);
        recyclerView          = findViewById(R.id.recycler_view);
        progressBarLeftDrawer = findViewById(R.id.progress_bar_left_drawer);
        rightDrawer           = findViewById(R.id.right_drawer);

        fabExpandDrawer = findViewById(R.id.fab_expand_drawer);
        fabExpandDrawer.setImageDrawable(new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_menu).color(Color.WHITE).sizeDp(24));
        fabExpandDrawer.setOnClickListener(v -> drawerLayout.openDrawer(leftDrawer));

        fabSettings = findViewById(R.id.fab_settings);
        fabSettings.setImageDrawable(new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_settings).color(Color.WHITE).sizeDp(24));
        fabSettings.setOnClickListener(v -> openSettingsDialog());
        fabSettings.setClickable(false);

        fabEditLocation = findViewById(R.id.fab_location_edit);
        fabEditLocation.setImageDrawable(new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_edit_location).color(Color.WHITE).sizeDp(24));
        fabEditLocation.setOnClickListener(v -> enterEditMode());
        fabEditLocation.setClickable(false);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (drawerView == leftDrawer) {
                    fabExpandDrawer.setAlpha(1 - slideOffset);
                    fabSettings.setAlpha(slideOffset);
                    fabSettings.setTranslationX(leftDrawer.getMeasuredWidth() * slideOffset);
                }
                else {
                    fabEditLocation.setTranslationX(-rightDrawer.getMeasuredWidth() * slideOffset);
                    fabEditLocation.setAlpha(slideOffset);
                }
            }
            @Override
            public void onDrawerOpened(View drawerView) {
                if (drawerView == leftDrawer) {
                    fabExpandDrawer.setClickable(false);
                    fabSettings.setClickable(true);
                }
                else {
                    fabEditLocation.setClickable(true);
                }
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                if (drawerView == leftDrawer) {
                    fabExpandDrawer.setClickable(true);
                    fabSettings.setClickable(false);
                }
                else {
                    fabEditLocation.setClickable(false);
                }
            }
            @Override
            public void onDrawerStateChanged(int newState) {}
        });

        buttonsEdit         = findViewById(R.id.buttons_edit);
        buttonEditApply     = findViewById(R.id.button_apply);
        buttonEditCancel    = findViewById(R.id.button_cancel);
        buttonEditClear     = findViewById(R.id.button_clear);

        buttonEditApply.setOnClickListener(v -> editApply());
        buttonEditCancel.setOnClickListener(v -> leaveEditMode());
        buttonEditClear.setOnClickListener(v -> clearUnitLocation());

        updateLeftDrawer();
        setupRightDrawer();

        locationLabelButton = findViewById(R.id.locationLabelButton);
        drawerLayout.setScrimColor(Color.TRANSPARENT);

        setSurfaceView(findViewById(R.id.surfaceview_core));
        getSurfaceView().setOnTouchListener(this);
        setRenderer(new TangoRenderer(this));

        uiOverlayHolder = new UiOverlayHolder(this, this);
        listSettings = new ListSettingsDialogFragment();
    }

    private void updateLeftDrawer() {
        progressBarLeftDrawer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);

        new FetchDeviceListTask(currentUnitSetting, currentLocationSetting, this::onFetchDeviceListTaskFinished).execute();
    }

    public void onFetchDeviceListTaskFinished(List<Location> locationList) {
        progressBarLeftDrawer.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);

        LocationAdapter locationAdapter = new LocationAdapter(this, locationList, this);
        recyclerView.setAdapter(locationAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRightDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, rightDrawer);
        unitListViewHolder = new UnitListViewHolder(rightDrawer);
    }

    private void initFetchLocationLabelTask() {
        sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);

        fetchLocationLabelTask = () -> {
            double[] bcoPosition = TangoSupport.doubleTransformPoint(glToBcoTransform, currentPose.translation);

            Vec3DDoubleType.Vec3DDouble vec3DDouble = Vec3DDoubleType.Vec3DDouble.newBuilder()
                    .setX(bcoPosition[0]).setY(bcoPosition[1]).setZ(bcoPosition[2]).build();

            try {
                Registries.waitForData();
                List<UnitConfig> unitConfigs = Registries.getLocationRegistry()
                        .getLocationConfigsByCoordinate(vec3DDouble, LocationConfigType.LocationConfig.LocationType.TILE);

                if (unitConfigs.size() > 0) {
                    currentLocation = unitConfigs.get(0);
                    runOnUiThread(() -> locationLabelButton.setText(unitConfigs.get(0).getLabel()));
                    runOnUiThread(() -> locationLabelButton.setVisibility(View.VISIBLE));
                }
                else {
                    runOnUiThread(() -> locationLabelButton.setVisibility(View.GONE));
                }

            } catch (CouldNotPerformException | InterruptedException | ExecutionException | ConcurrentModificationException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        };

        sch.scheduleWithFixedDelay(fetchLocationLabelTask, 500, 500, TimeUnit.MILLISECONDS);
    }

    @Deprecated
    private void loadLocalTransform() {
        String filename = "transforms.dat";
        FileInputStream inputStream;
        ObjectInputStream objectInputStream;

        try {
            inputStream = openFileInput(filename);
            objectInputStream = new ObjectInputStream(inputStream);

            HashMap<String, double[]> transformsMap = (HashMap<String, double[]>) objectInputStream.readObject();
            objectInputStream.close();

            glToBcoTransform = transformsMap.get(adfUuid);
            bcoToGlTransform = new Matrix4(glToBcoTransform).inverse().getDoubleValues();

            objectInputStream.close();

            Log.i(TAG, "Transform for uuid " + adfUuid + " loaded:\n" +
                    Arrays.toString(glToBcoTransform) + "\n" +
                    Arrays.toString(bcoToGlTransform) + "\n" +
                    "Total transforms in database: " + transformsMap.size());
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public void onDeviceClicked(UnitConfig unitConfig) {
        drawerLayout.closeDrawer(leftDrawer);

        currentDevice = unitConfig;
        unitListViewHolder.displayUnit(this, unitConfig);

        if (isUnitLocationEditable(unitConfig)) {
            fabEditLocation.setVisibility(View.VISIBLE);
        }
        else {
            fabEditLocation.setVisibility(View.GONE);
        }

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, rightDrawer);
        drawerLayout.openDrawer(rightDrawer);
    }

    public void onLocationLabelButtonClicked(View view) {
        if (currentLocation != null) {
            onDeviceClicked(currentLocation);
        }
    }

    @Override
    public void onSettingsChosen(SettingValue unitSetting, SettingValue locationSetting) {
        currentUnitSetting = unitSetting;
        currentLocationSetting = locationSetting;

        updateLeftDrawer();
    }

    private void openSettingsDialog() {
        listSettings.show(getFragmentManager(), "ListSettingsDialogFragment");
    }

    private void enterEditMode() {
        inEditMode = true;
        drawerLayout.closeDrawers();
        buttonsEdit.setVisibility(View.VISIBLE);
        uiOverlayHolder.setUiOverlayVisibility(View.INVISIBLE);

        addDebugSphere();
    }

    private void leaveEditMode() {
        inEditMode = false;
        drawerLayout.openDrawer(rightDrawer);
        buttonsEdit.setVisibility(View.INVISIBLE);
        uiOverlayHolder.setUiOverlayVisibility(View.VISIBLE);

        getRenderer().clearSpheres();
        buttonEditApply.setEnabled(false);
    }

    private void editApply() {
        // TODO: move to AsyncTask
        try {
            // Transform OpenGL position to BCO Position
            double[] bcoPosition = TangoSupport.doubleTransformPoint(glToBcoTransform, currentEditPosition.toArray());

            // Get location for that specific coordinate
            List<UnitConfig> locations =
                Registries.getLocationRegistry().getLocationConfigsByCoordinate(
                    Vec3DDoubleType.Vec3DDouble.newBuilder().setX(bcoPosition[0]).setY(bcoPosition[1]).setZ(bcoPosition[2]).build());

            if (locations.size() == 0) {
                Log.w(TAG, "No location found for current unit position!");
                return;
            }

            UnitConfig[] location = new UnitConfig[1];
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
            // Otherwise use zone if there is any
            if (location[0] == null) {
                StreamSupport.stream(locations)
                        .filter(unitConfig -> unitConfig.getLocationConfig().getType() == LocationConfigType.LocationConfig.LocationType.ZONE)
                        .findAny()
                        .ifPresent(unitConfig -> location[0] = unitConfig);
            }
            // Otherwise return... Unknown LocationType...
            if (location[0] == null) {
                Log.w(TAG, "No valid location found for selected position!");
                return;
            }

            // Transform BCO-Root position to BCO-Location-of-selected-point position
            Vector3d transformedBcoPosition = new Vector3d(bcoPosition[0], bcoPosition[1], bcoPosition[2]);
            Registries.getLocationRegistry().waitForData();
            Registries.getLocationRegistry().getUnitTransformation(location[0]).get(3, TimeUnit.SECONDS).getTransform().transform(transformedBcoPosition);

            // Generate new protobuf unitConfig
            TranslationType.Translation translation =
                    currentDevice.getPlacementConfig().getPosition().getTranslation().toBuilder().setX(transformedBcoPosition.x).setY(transformedBcoPosition.y).setZ(transformedBcoPosition.z).build();
            RotationType.Rotation rotation;
            if (currentDevice.getPlacementConfig().hasPosition()) {
                rotation = currentDevice.getPlacementConfig().getPosition().getRotation();
            }
            else {
                rotation = RotationType.Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
            }
            PoseType.Pose pose  =
                    currentDevice.getPlacementConfig().getPosition().toBuilder().setTranslation(translation).setRotation(rotation).build();
            PlacementConfigType.PlacementConfig placementConfig =
                    currentDevice.getPlacementConfig().toBuilder().setPosition(pose).setLocationId(location[0].getId()).build();
            UnitConfig unitConfig =
                    currentDevice.toBuilder().setPlacementConfig(placementConfig).build();

            // Update unitConfig
            Registries.getUnitRegistry().updateUnitConfig(unitConfig);

            uiOverlayHolder.checkAndAddNewUnit(unitConfig);
            leaveEditMode();
        } catch (TimeoutException | CouldNotPerformException | InterruptedException | ExecutionException e) {
            Log.e(TAG, "Error while updating locationConfig of unit: " + currentDevice + "\n" + Log.getStackTraceString(e));
        }
    }

    private void clearUnitLocation() {
        try {
            // Generate new protobuf unitConfig
            PlacementConfigType.PlacementConfig placementConfig =
                    currentDevice.getPlacementConfig().toBuilder().clearPosition().build();
            UnitConfig unitConfig =
                    currentDevice.toBuilder().setPlacementConfig(placementConfig).build();

            // Update unitConfig
            Registries.getUnitRegistry().updateUnitConfig(unitConfig);

            uiOverlayHolder.removeUnit(unitConfig);
            leaveEditMode();
        } catch (CouldNotPerformException | InterruptedException  e) {
            Log.e(TAG, "Error while updating locationConfig of unit: " + currentDevice + "\n" + Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onPostPreFrame() {
        TangoSupport.TangoDoubleMatrixTransformData glToCameraTransform =
                TangoSupport.getDoubleMatrixTransformAtTime(
                        currentPose.timestamp,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.ROTATION_IGNORED);

        bcoToPixelTransform = new Matrix4(bcoToGlTransform);
        bcoToPixelTransform.leftMultiply(new Matrix4(glToCameraTransform.matrix)).leftMultiply(getRenderer().getCurrentCamera().getProjectionMatrix());

        uiOverlayHolder.updateBcoToPixelTransform(bcoToPixelTransform);
    }

    @Override
    protected boolean callPostPreFrame() {
        return true;
    }

    private void addDebugSphere() {
        this.getRenderer().addSphere(new Vector3(TangoSupport.doubleTransformPoint(bcoToGlTransform, new double[]{0, 0, 0})), Color.RED);
    }

    private boolean isUnitLocationEditable(UnitConfig unitConfig) {
        switch (unitConfig.getType()) {
            case DEVICE:
                return true;
            case LOCATION:
                return false;
            default:
                if (unitConfig.getBoundToUnitHost()) {
                    return false;
                }
                else {
                    return true;
                }
        }
    }

    public SettingValue getCurrentUnitSetting() {
        return currentUnitSetting;
    }

    public SettingValue getCurrentLocationSetting() {
        return currentLocationSetting;
    }
}
