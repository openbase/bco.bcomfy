package org.openbase.bco.bcomfy.activityStart;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.CoreActivity;
import org.openbase.bco.bcomfy.activityInit.InitActivity;
import org.openbase.bco.bcomfy.activitySettings.SettingsActivity;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.type.domotic.registry.TemplateRegistryDataType;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

public class StartActivity extends Activity {

    private static final String TAG = StartActivity.class.getSimpleName();
    private static Context applicationContext;

    private static final int PERMISSION_CAMERA = 1;
    private static final int PERMISSION_READ_EXTERNAL = 2;
    private static final int PERMISSION_WRITE_EXTERNAL = 3;

    private StartActivityState state = StartActivityState.INIT_BCO;

    private InitBcoTask initBcoTask;

    private ProgressBar progressBar;
    private TextView infoMessage;
    private Button buttonScanInit;
    private Button buttonScanContinue;
    private Button buttonCancel;
    private Button buttonRetry;
    private Button buttonSettings;
    private Button buttonPublish;
    private Button buttonRecord;
    private Button buttonManage;
    private Button buttonDebugStart;
    private Button buttonDebugRecalc;

    private boolean debugRecalc;
    private boolean scanContinue;

    public static Tango tango;
    private TangoConfig tangoConfig;
    private List<Pair<String, String>> adfList;
    private String[] adfNameList;
    private String[] adfIdList;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_start);
        applicationContext = getApplicationContext();

        // load jps default values
        SettingsActivity.setupJPDefaultValues(applicationContext);

        initGui();

        debugRecalc = false;
        scanContinue = false;

        // Set default preferences if not already set.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Set system property. This workaround is needed for RSB.
        System.setProperty("sun.arch.data.model", "32");

        // Check for needed permissions
        checkAndRequestPermissions();
    }

    @Override
    public void onStart() {
        super.onStart();
        initBcoTask = new InitBcoTask(returnObject -> StartActivity.this.changeState(StartActivityState.INIT_TANGO));
        initBcoTask.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void checkAndRequestPermissions() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        }

        // Check read external permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL);
        }

        // Check write external permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL);
        }

        // Check Tango related permission
        if (!Tango.hasPermission(this, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)) {
            startActivityForResult(
                    Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);
        }
    }

    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private void initTangoService() {
        tango = new Tango(StartActivity.this, () -> {
            // Synchronize against disconnecting while the service is being used in the
            // OpenGL thread or in the UI thread.
            synchronized (StartActivity.this) {
                try {
                    TangoSupport.initialize(tango);
                    fetchLocalAdfList();
                    runOnUiThread(() -> changeState(StartActivityState.GET_ADF_FAILED));
                } catch (TangoOutOfDateException e) {
                    Log.e(TAG, getString(R.string.tango_out_of_date_exception), e);
                    changeState(StartActivityState.INIT_TANGO_FAILED);
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.tango_error), e);
                    changeState(StartActivityState.INIT_TANGO_FAILED);
                } catch (TangoInvalidException e) {
                    Log.e(TAG, getString(R.string.tango_invalid), e);
                    changeState(StartActivityState.INIT_TANGO_FAILED);
                }
            }
        });
    }


    /**
     * Connects the tango service.
     */
    private void connectTangoService() {
        new Thread(() -> {
            tangoConfig = setupTangoConfig(tango);
            tango.connect(tangoConfig);
            runOnUiThread(() -> changeState(StartActivityState.SETTINGS));
            if (state == StartActivityState.CONNECT_TANGO_TO_CORE) {
                startCoreActivity();
            }
            else {
                startInitActivity();
            }
        }).start();
    }

    /**
     * Sets up the tango configuration object.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use default configuration for Tango Service, plus color camera, low latency
        // IMU integration and drift correction.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        // NOTE: Low latency integration is necessary to achieve a precise alignment of
        // virtual objects with the RBG image and produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        // Drift correction allows motion tracking to recover after it loses tracking.
        // The drift corrected pose is is available through the frame pair with
        // base frame AREA_DESCRIPTION and target frame DEVICE.
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.KEY_PREF_MISC_ADF, "INVALID!"));

        return config;
    }

    private void changeState(StartActivityState startActivityState) {
        state = startActivityState;

        switch (state) {
            case INIT_BCO:
                infoMessage.setText(R.string.gui_connect_bco);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case INIT_TANGO:
                infoMessage.setText(R.string.gui_init_tango);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                initTangoService();
                break;
            case GET_ADF:
                infoMessage.setText(R.string.gui_update_adf);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
//                fetchLocalAdfList(); //implement ADF registry fetching here
                break;
            case GET_ADF_FAILED:
                infoMessage.setText(R.string.gui_update_adf_failed);
                setVisibilities(View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE);
                break;
            case CONNECT_TANGO_TO_INIT:
            case CONNECT_TANGO_TO_CORE:
                infoMessage.setText(R.string.gui_connect_tango);
                setVisibilities(View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case INIT_TANGO_FAILED:
                infoMessage.setText(R.string.gui_init_tango_failed);
                setVisibilities(View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case SETTINGS:
                setVisibilities(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            default:
                break;
        }
    }

    public void onButtonScanInitClicked(View view) {
        debugRecalc = false;
        scanContinue = false;
        changeState(StartActivityState.CONNECT_TANGO_TO_INIT);
        connectTangoService();
    }

    public void onButtonScanContinueClicked(View view) {
        debugRecalc = false;
        scanContinue = true;
        changeState(StartActivityState.CONNECT_TANGO_TO_INIT);
        connectTangoService();
    }

    public void onButtonCancelClicked(View view) {
        changeState(StartActivityState.SETTINGS);

        // bco connection needs to be restarted at this point!
    }

    @SuppressWarnings("unchecked")
    public void onButtonRetryClicked(View view) {
        if (initBcoTask.getStatus() != AsyncTask.Status.FINISHED) {
            Log.i(TAG, "Cancel initialization task...");
            initBcoTask.cancel(true);
        }

        changeState(StartActivityState.INIT_BCO);

        initBcoTask = new InitBcoTask(returnObject -> changeState(StartActivityState.INIT_TANGO));
        initBcoTask.execute();
    }

    public void onButtonSettingsClicked(View view) {
        startSettingsActivity();
    }

    public void onButtonPublishClicked(View view) {
        Log.e(TAG, "Operation not implemented: onButtonPublishClicked");
    }

    public void onButtonRecordClicked(View v) {
        Log.e(TAG, "Operation not implemented: onButtonRecordClicked");
    }

    public void onButtonManageClicked(View view) {
        Log.e(TAG, "Operation not implemented: onButtonManageClicked");
    }

    public void onButtonDebugStartClicked(View view) {
        changeState(StartActivityState.CONNECT_TANGO_TO_CORE);
        connectTangoService();
    }

    public void onButtonDebugTransformClicked(View view) {
        debugRecalc = true;
        changeState(StartActivityState.CONNECT_TANGO_TO_INIT);
        connectTangoService();
    }

    private void setVisibilities(int progress, int info, int init, int cont, int cancel, int retry, int settings, int publish, int record, int manage, int debugStart, int debugCalc) {
        progressBar.setVisibility(progress);
        infoMessage.setVisibility(info);
        buttonScanInit.setVisibility(init);
        buttonScanContinue.setVisibility(cont);
        buttonCancel.setVisibility(cancel);
        buttonRetry.setVisibility(retry);
        buttonSettings.setVisibility(settings);
        buttonPublish.setVisibility(publish);
        buttonRecord.setVisibility(record);
        buttonManage.setVisibility(manage);
        buttonDebugStart.setVisibility(debugStart);
        buttonDebugRecalc.setVisibility(debugCalc);
    }

    private void initGui() {
        progressBar        = findViewById(R.id.progressBar);
        infoMessage        = findViewById(R.id.infoMessage);
        buttonScanInit     = findViewById(R.id.button_scan_init);
        buttonScanContinue = findViewById(R.id.button_scan_continue);
        buttonCancel       = findViewById(R.id.button_cancel);
        buttonRetry        = findViewById(R.id.button_retry);
        buttonSettings     = findViewById(R.id.button_settings);
        buttonPublish      = findViewById(R.id.button_publish);
        buttonRecord       = findViewById(R.id.button_record);
        buttonManage       = findViewById(R.id.button_manage);
        buttonDebugStart   = findViewById(R.id.button_debug_start);
        buttonDebugRecalc  = findViewById(R.id.button_debug_calc_transform);
    }

    private void fetchLocalAdfList() {
        ArrayList<String> uuidList = tango.listAreaDescriptions();

        adfIdList = new String[uuidList.size()];
        adfNameList = new String[uuidList.size()];

        for (int i = 0; i < uuidList.size(); i++) {
            adfIdList[i] = uuidList.get(i);
            adfNameList[i] = new String(tango.loadAreaDescriptionMetaData(uuidList.get(i)).get(TangoAreaDescriptionMetaData.KEY_NAME));
        }
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("adfIdList", adfIdList);
        intent.putExtra("adfNameList", adfNameList);
        startActivity(intent);
    }

    private void startInitActivity() {
        Intent intent = new Intent(this, InitActivity.class);
        intent.putExtra("recalcTransform", debugRecalc);
        intent.putExtra("scanContinue", scanContinue);
        startActivity(intent);
    }

    private void startCoreActivity() {
        Intent intent = new Intent(this, CoreActivity.class);
        startActivity(intent);
    }

    private static class InitBcoTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = InitBcoTask.class.getSimpleName();
        private OnTaskFinishedListener<Void> listener;

        InitBcoTask(OnTaskFinishedListener<Void> listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Set JPS values used by BCO API.
                SettingsActivity.updateJPServiceProperties(applicationContext);


                Registries.getUnitRegistry().addDataObserver((templateRegistryDataDataProvider, templateRegistryData) -> {
                    Log.i(TAG, "DataObserver triggered!");

                    int size = Registries.getUnitRegistry(true).getUserUnitConfigRemoteRegistry(true).getEntries().size();
                    Log.i(TAG, "Sizes "+size+" = "+templateRegistryData.getUserUnitConfigCount()+" ?");
                });

                // Initiate registries
                while (!Registries.isDataAvailable()) {
                    Log.i(TAG, "Calling data...");
                    try {
                        Registries.waitForData(20, TimeUnit.SECONDS);
                    } catch (NotAvailableException ex) {
                        Log.i(TAG, "TIMEOUT!");
                    }
                }

                SessionManager.getInstance().loginUser(this.getAdminUserId(), "admin", true);
            } catch (CouldNotPerformException | InterruptedException e) {
                Log.e(TAG, "Error while initializing BCO!" + "\n" + Log.getStackTraceString(e));
            }
            return null;
        }

        private String getAdminUserId() {
            try {
                return Registries.getUnitRegistry().getUserUnitIdByUserName("admin");
            } catch (Exception ex) {
                Log.i(TAG, "Try again...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex2) {
                    return "";
                }
                return this.getAdminUserId();
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            Log.i(TAG, "Connection to BCO initialized.");
            Log.i(TAG, BCO.BCO_LOGO_ASCI_ARTS);

            listener.taskFinishedCallback(null);
        }

        @Override
        protected void onCancelled() {
            // what needs to be done here?
        }
    }
}
