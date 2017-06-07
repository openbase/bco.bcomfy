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
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.TangoException;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.TangoActivity;
import org.openbase.bco.bcomfy.TangoRenderer;
import org.openbase.bco.bcomfy.activityInit.measure.Measurer;
import org.openbase.bco.bcomfy.activityInit.measure.Plane;
import org.openbase.bco.bcomfy.activityInit.view.InstructionTextView;
import org.openbase.bco.bcomfy.activityInit.view.LocationChooser;
import org.openbase.bco.bcomfy.activitySettings.SettingsActivity;
import org.openbase.bco.bcomfy.utils.BcoUtils;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.view.SurfaceView;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class InitActivity extends TangoActivity implements View.OnTouchListener, LocationChooser.LocationChooserListener {
    private static final String TAG = InitActivity.class.getSimpleName();

    private Button buttonAddRoom;
    private Button buttonFinishRoom;
    private Button buttonFinishMeasurement;
    private InstructionTextView instructionTextView;

    private Measurer measurer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_init);
        super.onCreate(savedInstanceState);

        measurer = new Measurer(
                Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_INIT_DEFAULT, "1")),
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEY_PREF_INIT_ALIGN, true),
                Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_INIT_ANCHOR, "3")));
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
                    Measurer.MeasureType lastMeasureType = measurer.addPlaneMeasurement(planeFit);
                    updateGuiAfterPlaneMeasurement(planeFit, lastMeasureType);
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

    /**
     * Handle a successful plane measurement
     */
    private void updateGuiAfterPlaneMeasurement(Plane plane, Measurer.MeasureType lastMeasureType) {
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
        instructionTextView = new InstructionTextView((TextView) findViewById(R.id.instructionTextView));

        buttonAddRoom = (Button) findViewById(R.id.buttonAddRoom);
        buttonAddRoom.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_add_circle_outline)
                        .color(Color.BLACK)
                        .sizePx((int) buttonAddRoom.getTextSize())
                , null, null, null);

        buttonFinishRoom = (Button) findViewById(R.id.buttonFinishRoom);
        buttonFinishRoom.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_done)
                        .color(Color.BLACK)
                        .sizePx((int) buttonFinishRoom.getTextSize())
                , null, null, null);

        buttonFinishMeasurement = (Button) findViewById(R.id.buttonFinishMeasurement);
        buttonFinishMeasurement.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_done_all)
                        .color(Color.BLACK)
                        .sizePx((int) buttonFinishMeasurement.getTextSize())
                , null, null, null);

        setSurfaceView((SurfaceView) findViewById(R.id.surfaceview));
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
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(measurer.hasFinishedRoom());
                break;
            case MARK_GROUND:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(false);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_GROUND);
                break;
            case MARK_CEILING:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(false);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_CEILING);
                break;
            case MARK_WALLS:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(false);
                buttonFinishMeasurement.setEnabled(false);
                instructionTextView.updateInstruction(InstructionTextView.Instruction.MARK_WALLS);
                break;
            case ENOUGH_WALLS:
                buttonAddRoom.setEnabled(false);
                buttonFinishRoom.setEnabled(true);
                buttonFinishMeasurement.setEnabled(false);
                break;
        }
    }

    public void onAddRoomClicked(View v) {
        measurer.startNewRoom();
        updateGuiButtons();
    }

    public void onFinishRoomClicked(View v) {
        DialogFragment dialogFragment = new LocationChooser();
        dialogFragment.show(getFragmentManager(), "locationChooser");
    }

    public void onFinishMeasurementClicked(View v) {
        updateGuiButtons();
    }

    @Override
    public void onLocationSelected(final String locationId) {
        measurer.finishRoom();
        updateGuiButtons();

        saveTransformsLocally();

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

    @Deprecated
    private void saveTransformsLocally() {
        String filename = "transform.tmp";
        FileOutputStream outputStream;
        ObjectOutputStream objectOutputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(measurer.getGlToBcoTransform());
            objectOutputStream.writeObject(measurer.getBcoToGlTransform());
            objectOutputStream.close();
            Log.i(TAG, "Transform saved:\n" + Arrays.toString(measurer.getGlToBcoTransform()) + "\n" + Arrays.toString(measurer.getBcoToGlTransform()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
