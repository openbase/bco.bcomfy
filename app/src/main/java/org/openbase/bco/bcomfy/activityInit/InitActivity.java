package org.openbase.bco.bcomfy.activityInit;

import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.atap.tangoservice.TangoException;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.TangoActivity;
import org.openbase.bco.bcomfy.TangoRenderer;
import org.openbase.bco.bcomfy.activityInit.measure.AnchorRoom;
import org.openbase.bco.bcomfy.activityInit.measure.Measurer;
import org.openbase.bco.bcomfy.activityInit.measure.Plane;
import org.openbase.bco.bcomfy.activityInit.view.InstructionTextView;
import org.openbase.bco.bcomfy.activityInit.view.LocationChooser;
import org.openbase.bco.bcomfy.activitySettings.SettingsActivity;
import org.openbase.bco.bcomfy.utils.AndroidUtils;
import org.openbase.bco.bcomfy.utils.AndroidUtils.RoomData;
import org.openbase.bco.bcomfy.utils.BcoUtils;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.vector.Vector3;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class InitActivity extends TangoActivity implements View.OnTouchListener, LocationChooser.LocationChooserListener {
    private static final String TAG = InitActivity.class.getSimpleName();

    private Button buttonAddRoom;
    private Button buttonUndoMeasurement;
    private Button buttonFinishRoom;
    private Button buttonFinishMeasuring;
    private InstructionTextView instructionTextView;

    private Measurer measurer;

    private boolean recalcTransform;
    private boolean scanContinue;
    private String adfUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_init);
        super.onCreate(savedInstanceState);

        recalcTransform = getIntent().getBooleanExtra("recalcTransform", false);
        scanContinue = getIntent().getBooleanExtra("scanContinue", false);
        adfUuid = getIntent().getStringExtra("adfUuid");

        if (scanContinue) {
            try {
                RoomData roomData = AndroidUtils.loadLocalData(adfUuid, this);

                Vector3[] anchorNormals = new Vector3[4];
                anchorNormals[0] = new Vector3(roomData.anchorNormal0);
                anchorNormals[1] = new Vector3(roomData.anchorNormal1);
                anchorNormals[2] = new Vector3(roomData.anchorNormal2);
                anchorNormals[3] = new Vector3(roomData.anchorNormal3);

                measurer = new Measurer(
                        Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_INIT_DEFAULT, "1")),
                        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEY_PREF_INIT_ALIGN, true),
                        Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_INIT_ANCHOR, "3")),
                        recalcTransform,
                        anchorNormals,
                        roomData.glToBcoTransform,
                        roomData.bcoToGlTransform);
            } catch (CouldNotPerformException e) {
                Log.e(TAG, "Unable to load room data for selected adf!", e);
                onStop();
                finish();
            }
        }
        else {
            measurer = new Measurer(
                    Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_INIT_DEFAULT, "1")),
                    PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEY_PREF_INIT_ALIGN, true),
                    Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_INIT_ANCHOR, "3")),
                    recalcTransform);
        }
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
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // Ignore onTouch Event if we don't want to measure something
        if (measurer.getMeasurerState() == Measurer.MeasurerState.INIT) {
            return true;
        }

        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            // Calculate click location in u,v (0;1) coordinates.
            float u = motionEvent.getX() / view.getWidth();
            float v = motionEvent.getY() / view.getHeight();

            try {
                // Fit a plane on the clicked point using the latest point cloud data
                // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
                // and a possible service disconnection due to an onPause event.
                Plane planeFit;
                synchronized (this) {
                    planeFit = TangoUtils.doFitPlane(u, v, rgbTimestampGlThread, tangoPointCloudManager.getLatestPointCloud(), displayRotation);
                }

                if (planeFit != null) {
                    Measurer.MeasureType lastMeasureType = measurer.addPlaneMeasurement(planeFit, currentPose.translation);

                    if (lastMeasureType == Measurer.MeasureType.INVALID) {
                        runOnUiThread(() -> instructionTextView.animateNegative());
                        AndroidUtils.showLongToastTop(getApplicationContext(), R.string.init_invalid_wall);
                        Log.w(TAG, getString(R.string.init_invalid_wall), new CouldNotPerformException(getString(R.string.init_invalid_wall)));
                    }
                    else if (lastMeasureType == Measurer.MeasureType.TOO_CLOSE) {
                        runOnUiThread(() -> instructionTextView.animateNegative());
                        AndroidUtils.showLongToastTop(getApplicationContext(), R.string.init_too_close);
                        Log.w(TAG, getString(R.string.init_too_close), new CouldNotPerformException(getString(R.string.init_too_close)));
                    }
                    else {
                        runOnUiThread(() -> instructionTextView.animatePositive());
                    }

                    updateGuiAfterPlaneMeasurement(planeFit, lastMeasureType);
                }

            } catch (TangoException t) {
                runOnUiThread(() -> instructionTextView.animateNegative());
                AndroidUtils.showLongToastTop(getApplicationContext(), R.string.tango_error);
                Log.w(TAG, getString(R.string.tango_error), t);
            } catch (SecurityException t) {
                runOnUiThread(() -> instructionTextView.animateNegative());
                AndroidUtils.showShortToastTop(getApplicationContext(), R.string.no_permissions);
                Log.w(TAG, getString(R.string.no_permissions), t);
            }
        }
        return true;
    }

    /**
     * Handle a successful plane measurement
     */
    private void updateGuiAfterPlaneMeasurement(Plane plane, Measurer.MeasureType lastMeasureType) {
        if (recalcTransform && measurer.isAnchorFinished()) {
            updateLocalData();
            finish();
        }

        switch (lastMeasureType) {
            case INVALID:
                return;
            case GROUND:
                getRenderer().addGroundPlane(plane.getPosition(), plane.getNormal());
                break;
            case CEILING:
                getRenderer().addCeilingPlane(plane.getPosition(), plane.getNormal());
                break;
            case WALL:
                getRenderer().addWallPlane(plane.getPosition(), plane.getNormal());
                break;
        }

        updateGuiButtons();
    }

    @Override
    protected void setupGui() {
        instructionTextView = new InstructionTextView(findViewById(R.id.instructionTextView), this);

        buttonAddRoom = findViewById(R.id.buttonAddRoom);
        buttonAddRoom.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_add_circle_outline)
                        .color(Color.WHITE)
                        .sizeDp(24)
                , null, null, null);

        buttonUndoMeasurement = findViewById(R.id.buttonUndoMeasurement);
        buttonUndoMeasurement.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_undo)
                        .color(Color.WHITE)
                        .sizeDp(24)
                , null, null, null);

        buttonFinishRoom = findViewById(R.id.buttonFinishRoom);
        buttonFinishRoom.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_done)
                        .color(Color.WHITE)
                        .sizeDp(24)
                , null, null, null);

        buttonFinishMeasuring = findViewById(R.id.buttonFinishMeasurement);
        buttonFinishMeasuring.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_done_all)
                        .color(Color.WHITE)
                        .sizeDp(24)
                , null, null, null);

        if (recalcTransform) {
            buttonAddRoom.setVisibility(View.GONE);
            buttonFinishRoom.setVisibility(View.GONE);
            buttonFinishMeasuring.setVisibility(View.GONE);
        }

        setSurfaceView(findViewById(R.id.surfaceview));
        getSurfaceView().setOnTouchListener(this);
        setRenderer(new TangoRenderer(this));
    }

    /**
     * Update Buttons and InstructionTextView based on the current state of the {@link Measurer}
     */
    private void updateGuiButtons() {
        switch (measurer.getMeasurerState()) {
            case INIT:
                buttonAddRoom.setEnabled(true);
                buttonUndoMeasurement.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasuring.setEnabled(measurer.hasFinishedRoom());
                instructionTextView.updateInstruction(Measurer.MeasurerState.INIT);
                break;
            case MARK_GROUND:
                buttonAddRoom.setEnabled(false);
                buttonUndoMeasurement.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasuring.setEnabled(false);
                instructionTextView.updateInstruction(Measurer.MeasurerState.MARK_GROUND);
                break;
            case MARK_CEILING:
                buttonAddRoom.setEnabled(false);
                buttonUndoMeasurement.setEnabled(true);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasuring.setEnabled(false);
                instructionTextView.updateInstruction(Measurer.MeasurerState.MARK_CEILING);
                break;
            case MARK_WALLS:
                buttonAddRoom.setEnabled(false);
                buttonUndoMeasurement.setEnabled(true);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasuring.setEnabled(false);
                instructionTextView.updateInstruction(Measurer.MeasurerState.MARK_WALLS,
                        measurer.getCurrentFinishedWallCount(),
                        measurer.getCurrentMeasurementCount(),
                        measurer.getNeededMeasurementCount());
                break;
            case ENOUGH_WALLS:
                buttonAddRoom.setEnabled(false);
                buttonUndoMeasurement.setEnabled(true);
                buttonFinishRoom.setEnabled(true);
                buttonFinishMeasuring.setEnabled(false);
                instructionTextView.updateInstruction(Measurer.MeasurerState.ENOUGH_WALLS,
                        measurer.getCurrentFinishedWallCount(),
                        measurer.getCurrentMeasurementCount(),
                        measurer.getNeededMeasurementCount());
                break;
        }
    }

    public void onAddRoomClicked(View v) {
        measurer.startNewRoom();
        updateGuiButtons();
    }

    public void onUndoMeasurementClicked(View v) {
        try {
            measurer.undoLastMeasurement();
            getRenderer().removeRecentPlane();
            updateGuiButtons();
        } catch (CouldNotPerformException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void onFinishRoomClicked(View v) {
        DialogFragment dialogFragment = new LocationChooser();
        dialogFragment.show(getFragmentManager(), "locationChooser");
    }

    public void onFinishMeasurementClicked(View v) {
        Log.i(TAG, "Measurement finished. Closing InitActivity...");
        this.finish();
    }

    @Override
    public void onLocationSelected(final String locationId) {
        measurer.finishRoom();
        updateGuiButtons();

        updateLocalData();

        ArrayList<Vector3> ceiling = measurer.getLatestCeilingVertices();
        final ArrayList<Vector3> ground  = measurer.getLatestGroundVertices();

        for (Vector3 vertex : ground) {
            getRenderer().addSphere(vertex, Color.BLUE);
        }
        for (Vector3 vertex : ceiling) {
            getRenderer().addSphere(vertex, Color.RED);
        }

        getRenderer().clearPlanes();

        new BcoUtils.UpdateLocationShapeTask(
                locationId,
                ground,
                measurer.getGlToBcoTransform(),
                returnObject -> Log.i(TAG, "Updated shape of location: " + locationId))
                .execute();
    }

    private void updateLocalData() {
        try {
            RoomData roomData = new RoomData();
            roomData.glToBcoTransform = measurer.getGlToBcoTransform();
            roomData.bcoToGlTransform = measurer.getBcoToGlTransform();
            roomData.anchorNormal0 = measurer.getAnchorNormals()[0].toArray();
            roomData.anchorNormal1 = measurer.getAnchorNormals()[1].toArray();
            roomData.anchorNormal2 = measurer.getAnchorNormals()[2].toArray();
            roomData.anchorNormal3 = measurer.getAnchorNormals()[3].toArray();
            AndroidUtils.updateLocalData(adfUuid, roomData, this);
        } catch (CouldNotPerformException e) {
            Log.e(TAG, "Error while saving room data to local storage!", e);
        }
    }
}
