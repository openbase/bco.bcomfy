package org.openbase.bco.bcomfy;

import android.app.Application;
import android.content.Context;

public class BComfy extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        BComfy.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return BComfy.context;
    }

}
