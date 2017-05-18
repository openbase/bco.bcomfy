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
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.TangoException;
import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.TangoActivity;
import org.openbase.bco.bcomfy.TangoRenderer;
import org.openbase.bco.bcomfy.activityCore.deviceList.Location;
import org.openbase.bco.bcomfy.activityCore.deviceList.LocationAdapter;
import org.openbase.bco.bcomfy.activityCore.serviceList.UnitListViewHolder;
import org.openbase.bco.bcomfy.activityInit.measure.Plane;
import org.openbase.bco.bcomfy.interfaces.OnDeviceClickedListener;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.view.SurfaceView;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.location.LocationConfigType;
import rst.math.Vec3DDoubleType;

public class CoreActivity extends TangoActivity implements View.OnTouchListener, OnDeviceClickedListener {
    private static final String TAG = CoreActivity.class.getSimpleName();

    private DrawerLayout drawerLayout;
    private RecyclerView leftDrawer;
    private LinearLayout rightDrawer;

    private LinearLayout buttonsEdit;
    private Button buttonEditApply;
    private Button buttonEditCancel;

    private View editLocationButton;
    boolean inEditMode = false;

    private UnitListViewHolder unitListViewHolder;

    private double[] glToBcoTransform;
    private double[] bcoToGlTransform;

    private ScheduledThreadPoolExecutor sch;
    private Runnable fetchLocationLabelTask;

    private TextView locationLabelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_core);
        super.onCreate(savedInstanceState);

        loadTransformsLocally();
        initFetchLocationLabelTask();

        Log.i(TAG, "Transform loaded:\n" + Arrays.toString(glToBcoTransform) + "\n" + Arrays.toString(bcoToGlTransform));
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
                Plane planeFit;
                synchronized (this) {
                    //TODO: TangoSupport.getDepth...
                    planeFit = TangoUtils.doFitPlane(u, v, rgbTimestampGlThread, tangoPointCloudManager.getLatestPointCloud(), displayRotation);
                }

                if (planeFit != null) {
                    //TODO: do something!
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
        drawerLayout = (DrawerLayout) findViewById(R.id.activity_core);
        leftDrawer   = (RecyclerView) findViewById(R.id.left_drawer);
        rightDrawer  = (LinearLayout) findViewById(R.id.right_drawer);

        buttonsEdit = (LinearLayout) findViewById(R.id.buttons_edit);
        buttonEditApply = (Button) findViewById(R.id.button_apply);
        buttonEditCancel = (Button) findViewById(R.id.button_cancel);

        buttonEditApply.setOnClickListener(v -> editApply());
        buttonEditCancel.setOnClickListener(v -> leaveEditMode());

        setupLeftDrawer();
        setupRightDrawer();

        locationLabelView = (TextView) findViewById(R.id.locationLabelView);
        drawerLayout.setScrimColor(Color.TRANSPARENT);

        setSurfaceView((SurfaceView) findViewById(R.id.surfaceview_core));
        getSurfaceView().setOnTouchListener(this);
        setRenderer(new TangoRenderer(this));
    }

    private void setupLeftDrawer() {
        List<Location> locations = new ArrayList<>();

        try {
            LocationRegistryRemote remote = Registries.getLocationRegistry();
            remote.waitForData();

            StreamSupport.stream(remote.getLocationConfigs())
                    .filter(locationConfig -> locationConfig.getPlacementConfig().getShape().getFloorCount() > 0)
                    .sorted((o1, o2) -> o1.getLabel().compareTo(o2.getLabel()))
                    .forEach(locationConfig -> locations.add(new Location(locationConfig, remote)));
        } catch (CouldNotPerformException | InterruptedException e) {
            Log.e(TAG, "Could not fetch locations!\n" + Log.getStackTraceString(e));
        }

        for (Iterator<Location> it = locations.iterator(); it.hasNext();) {
            Location location = it.next();
            if (location.getChildList().size() == 0) {
                it.remove();
            }
        }

        LocationAdapter locationAdapter = new LocationAdapter(this, locations, this);
        leftDrawer.setAdapter(locationAdapter);
        leftDrawer.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRightDrawer() {
        unitListViewHolder = new UnitListViewHolder(rightDrawer);
        editLocationButton = findViewById(R.id.edit_location_button);
        editLocationButton.setOnClickListener(v -> enterEditMode());
    }

    private void initFetchLocationLabelTask() {
        sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);

        fetchLocationLabelTask = () -> {
            double[] bcoPosition = TangoSupport.doubleTransformPoint(glToBcoTransform, currentPose.translation);

            Vec3DDoubleType.Vec3DDouble vec3DDouble = Vec3DDoubleType.Vec3DDouble.newBuilder()
                    .setX(bcoPosition[0]).setY(bcoPosition[1]).setZ(bcoPosition[2]).build();

            try {
                Registries.waitForData();
                List<UnitConfigType.UnitConfig> unitConfigs = Registries.getLocationRegistry()
                        .getLocationConfigsByCoordinate(vec3DDouble, LocationConfigType.LocationConfig.LocationType.TILE);

                if (unitConfigs.size() > 0) {
                    runOnUiThread(() -> locationLabelView.setText(unitConfigs.get(0).getLabel()));
                    runOnUiThread(() -> locationLabelView.setVisibility(View.VISIBLE));
                }
                else {
                    runOnUiThread(() -> locationLabelView.setVisibility(View.INVISIBLE));
                }

            } catch (CouldNotPerformException | InterruptedException | ExecutionException | ConcurrentModificationException e) {
                e.printStackTrace();
            }
        };

        sch.scheduleWithFixedDelay(fetchLocationLabelTask, 1, 1, TimeUnit.SECONDS);
    }

    @Deprecated
    private void loadTransformsLocally() {
        String filename = "transform.tmp";
        FileInputStream inputStream;
        ObjectInputStream objectInputStream;

        try {
            inputStream = openFileInput(filename);
            objectInputStream = new ObjectInputStream(inputStream);
            glToBcoTransform = (double[]) objectInputStream.readObject();
            bcoToGlTransform = (double[]) objectInputStream.readObject();
            objectInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeviceClicked(String id) {
        drawerLayout.closeDrawer(leftDrawer);
        unitListViewHolder.displayDevice(this, id);
        drawerLayout.openDrawer(rightDrawer);
    }

    private void enterEditMode() {
        inEditMode = true;
        drawerLayout.closeDrawers();
        buttonsEdit.setVisibility(View.VISIBLE);
    }

    private void leaveEditMode() {
        inEditMode = false;
        drawerLayout.openDrawer(rightDrawer);
        buttonsEdit.setVisibility(View.INVISIBLE);
    }

    private void editApply() {

    }
}
