package org.openbase.bco.bcomfy.activityCore;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.TangoException;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.TangoActivity;
import org.openbase.bco.bcomfy.TangoRenderer;
import org.openbase.bco.bcomfy.activityInit.measure.Plane;
import org.openbase.bco.bcomfy.utils.TangoUtils;
import org.rajawali3d.view.SurfaceView;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;

public class CoreActivity extends TangoActivity implements View.OnTouchListener {
    private static final String TAG = CoreActivity.class.getSimpleName();

    private DrawerLayout drawerLayout;
    private RecyclerView leftDrawer;
    private LinearLayout rightDrawer;

    private double[] glToBcoTransform;
    private double[] bcoToGlTransform;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_core);
        super.onCreate(savedInstanceState);

        loadTransformsLocally();

        Log.i(TAG, "Transform loaded:\n" + Arrays.toString(glToBcoTransform) + "\n" + Arrays.toString(bcoToGlTransform));
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

        drawerLayout.setScrimColor(Color.TRANSPARENT);

        setSurfaceView((SurfaceView) findViewById(R.id.surfaceview_core));
        getSurfaceView().setOnTouchListener(this);
        setRenderer(new TangoRenderer(this));
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

}
