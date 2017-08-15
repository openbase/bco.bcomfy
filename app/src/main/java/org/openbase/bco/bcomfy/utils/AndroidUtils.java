package org.openbase.bco.bcomfy.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public final class AndroidUtils {

    private static final String TAG = AndroidUtils.class.getSimpleName();

    public static void showShortToastTop(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.show();
    }
}
