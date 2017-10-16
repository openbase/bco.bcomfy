package org.openbase.bco.bcomfy.activityCore;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import org.openbase.bco.bcomfy.activityCore.ListSettingsDialogFragment.OnSettingsChosenListener;
import org.openbase.bco.bcomfy.activityCore.ListSettingsDialogFragment.SettingValue;
import org.openbase.bco.bcomfy.activityCore.deviceList.FetchDeviceListTask;
import org.openbase.bco.bcomfy.activityCore.deviceList.Location;
import org.openbase.bco.bcomfy.activityCore.deviceList.LocationAdapter;
import org.openbase.bco.bcomfy.activityCore.serviceList.UnitListViewHolder;
import org.openbase.bco.bcomfy.activityCore.uiOverlay.UiOverlayHolder;
import org.openbase.bco.bcomfy.activitySettings.SettingsActivity;
import org.openbase.bco.bcomfy.interfaces.OnDeviceSelectedListener;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.bcomfy.utils.AndroidUtils;
import org.openbase.bco.bcomfy.utils.BcoUtils;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationConfigType;
import rst.math.Vec3DDoubleType;
import rst.spatial.PlacementConfigType;

public class CoreActivity extends TangoActivity implements View.OnTouchListener, OnDeviceSelectedListener, OnSettingsChosenListener, OnTaskFinishedListener<Boolean> {
    private static final String TAG = CoreActivity.class.getSimpleName();

    private DrawerLayout drawerLayout;
    private RelativeLayout leftDrawer;
    private RecyclerView recyclerView;
    private ProgressBar progressBarLeftDrawer;
    private LinearLayout rightDrawer;

    private FloatingActionButton fabExpandDrawer;
    private FloatingActionButton fabSettings;
    private FloatingActionButton fabEditLocation;
    private FloatingActionButton fabHelpView;
    private TextView locationEditHelpText;
    private TextView relocationInstructionTextView;
    private TextView noPoseTextView;

    private SettingValue currentUnitSetting;
    private SettingValue currentLocationSetting;

    private ListSettingsDialogFragment listSettings;

    private LinearLayout updatingPositionView;
    private LinearLayout buttonsEdit;
    private Button buttonEditApply;
    private Button buttonEditCancel;
    private Button buttonEditClear;

    private ImageView helpView;

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

    private Animation shakeAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_core);
        currentUnitSetting = SettingValue.ALL;
        currentLocationSetting = SettingValue.LOCATED;

        super.onCreate(savedInstanceState);

        adfUuid = PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_MISC_ADF, "INVALID!");
        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake);

        try {
            AndroidUtils.RoomData roomData = AndroidUtils.loadLocalData(adfUuid, this);

            glToBcoTransform = roomData.glToBcoTransform;
            bcoToGlTransform = roomData.bcoToGlTransform;
        } catch (CouldNotPerformException e) {
            Log.e(TAG, "Unable to load data for adf " + adfUuid, e);
            onStop();
            finish();
        }
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
                AndroidUtils.showLongToastTop(getApplicationContext(), R.string.tango_error);
                Log.w(TAG, getString(R.string.tango_error), t);
            } catch (SecurityException t) {
                AndroidUtils.showShortToastTop(getApplicationContext(), R.string.no_permissions);
                Log.w(TAG, getString(R.string.no_permissions), t);
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
        locationEditHelpText  = findViewById(R.id.location_edit_help_text);

        noPoseTextView        = findViewById(R.id.noPoseTextView);
        noPoseTextView.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_warning)
                        .color(Color.WHITE)
                        .sizeDp(64),null,
                new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_warning)
                        .color(Color.WHITE)
                        .sizeDp(64),null);

        relocationInstructionTextView = findViewById(R.id.relocation_instruction);
        relocationInstructionTextView.setCompoundDrawables(new IconicsDrawable(this)
                .icon(GoogleMaterial.Icon.gmd_touch_app)
                .color(Color.WHITE)
                .sizeDp(64), null, null, null);

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

        fabHelpView = findViewById(R.id.fab_help_view);
        fabHelpView.setImageDrawable(new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_help_outline).color(Color.WHITE).sizeDp(24));
        fabHelpView.setOnClickListener(v -> showHelpView());

        helpView = findViewById(R.id.helpView);
        helpView.setOnClickListener(v -> hideHelpView());

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (drawerView == leftDrawer) {
                    fabExpandDrawer.setAlpha(1 - slideOffset);
                    fabSettings.setAlpha(slideOffset);
                    fabSettings.setTranslationX(leftDrawer.getMeasuredWidth() * slideOffset);
                }
                else {
                    locationEditHelpText.setTranslationX(-rightDrawer.getMeasuredWidth() * slideOffset);
                    locationEditHelpText.setAlpha(slideOffset);
                    fabEditLocation.setTranslationX(-rightDrawer.getMeasuredWidth() * slideOffset);
                    fabEditLocation.setAlpha(slideOffset);
                    fabHelpView.setTranslationX(-rightDrawer.getMeasuredWidth() * slideOffset);
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

        updatingPositionView = findViewById(R.id.updatingPositionView);
        buttonsEdit         = findViewById(R.id.buttons_edit);

        buttonEditApply     = findViewById(R.id.button_apply);
        buttonEditApply.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_check)
                        .color(Color.WHITE)
                        .sizeDp(18)
                , null, null, null);

        buttonEditCancel    = findViewById(R.id.button_cancel);
        buttonEditCancel.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_close)
                        .color(Color.WHITE)
                        .sizeDp(18)
                , null, null, null);

        buttonEditClear     = findViewById(R.id.button_clear);
        buttonEditClear.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_location_off)
                        .color(Color.WHITE)
                        .sizeDp(18)
                , null, null, null);

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
            if (inEditMode) {
                return;
            }

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

    @Override
    public void onDeviceSelected(UnitConfig unitConfig) {
        drawerLayout.closeDrawer(leftDrawer);

        currentDevice = unitConfig;
        unitListViewHolder.displayUnit(this, unitConfig);
        uiOverlayHolder.onDeviceSelected(unitConfig);

        if (isUnitLocationEditable(unitConfig)) {
            fabEditLocation.setVisibility(View.VISIBLE);
            if (!unitConfig.getPlacementConfig().hasPosition()) {
                fabEditLocation.startAnimation(shakeAnimation);
                locationEditHelpText.setVisibility(View.VISIBLE);
            }
            else {
                locationEditHelpText.setVisibility(View.INVISIBLE);
            }
        }
        else {
            fabEditLocation.setVisibility(View.GONE);
            locationEditHelpText.setVisibility(View.INVISIBLE);
        }

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, rightDrawer);
        drawerLayout.openDrawer(rightDrawer);
    }

    public void onLocationLabelButtonClicked(View view) {
        if (currentLocation != null) {
            onDeviceSelected(currentLocation);
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

    private void showHelpView() {
        helpView.setVisibility(View.VISIBLE);
    }

    private void hideHelpView() {
        helpView.setVisibility(View.INVISIBLE);
    }

    private void enterEditMode() {
        inEditMode = true;
        drawerLayout.closeDrawers();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, leftDrawer);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, rightDrawer);
        fabExpandDrawer.setVisibility(View.INVISIBLE);
        buttonsEdit.setVisibility(View.VISIBLE);
        relocationInstructionTextView.setVisibility(View.VISIBLE);
        locationLabelButton.setVisibility(View.GONE);
        uiOverlayHolder.setUiOverlayVisibility(View.INVISIBLE);
    }

    private void leaveEditMode() {
        inEditMode = false;
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, leftDrawer);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, rightDrawer);
        drawerLayout.openDrawer(rightDrawer);
        fabExpandDrawer.setVisibility(View.VISIBLE);
        buttonsEdit.setVisibility(View.INVISIBLE);
        relocationInstructionTextView.setVisibility(View.INVISIBLE);
        locationLabelButton.setVisibility(View.VISIBLE);
        uiOverlayHolder.setUiOverlayVisibility(View.VISIBLE);

        getRenderer().clearSpheres();
        buttonEditApply.setEnabled(false);
    }

    private void editApply() {
        updatingPositionView.setVisibility(View.VISIBLE);
        relocationInstructionTextView.setVisibility(View.INVISIBLE);
        buttonEditApply.setEnabled(false);
        buttonEditCancel.setEnabled(false);
        buttonEditClear.setEnabled(false);

        new BcoUtils.UpdateUnitPositionTask(currentDevice, glToBcoTransform, currentEditPosition.toArray(), this).execute((Void) null);
    }

    @Override
    public void taskFinishedCallback(Boolean updateSuccessful) {
        if (updateSuccessful) {
            try {
                uiOverlayHolder.checkAndAddNewUnit(currentDevice);
            } catch (CouldNotPerformException | InterruptedException ex) {
                Log.e(TAG, "Error while updating unit in overlay after updating its position!", ex);
            }

            updatingPositionView.setVisibility(View.GONE);
            buttonEditCancel.setEnabled(true);
            buttonEditClear.setEnabled(true);

            leaveEditMode();
        }
        else {
            updatingPositionView.setVisibility(View.GONE);
            relocationInstructionTextView.setVisibility(View.VISIBLE);
            buttonEditApply.setEnabled(true);
            buttonEditCancel.setEnabled(true);
            buttonEditClear.setEnabled(true);

            AndroidUtils.showShortToastTop(this, R.string.toast_position_update_error);
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

    @Override
    protected void onPoseAvailableChange(boolean poseAvailable) {
        if (poseAvailable) {
            noPoseTextView.setVisibility(View.INVISIBLE);

            if (inEditMode) {
                relocationInstructionTextView.setVisibility(View.VISIBLE);
            }
        }
        else {
            noPoseTextView.setVisibility(View.VISIBLE);
            relocationInstructionTextView.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isUnitLocationEditable(UnitConfig unitConfig) {
        switch (unitConfig.getType()) {
            case DEVICE:
                return true;
            case LOCATION:
                return false;
            default:
                return !unitConfig.getBoundToUnitHost();
        }
    }

    public SettingValue getCurrentUnitSetting() {
        return currentUnitSetting;
    }

    public SettingValue getCurrentLocationSetting() {
        return currentLocationSetting;
    }
}
