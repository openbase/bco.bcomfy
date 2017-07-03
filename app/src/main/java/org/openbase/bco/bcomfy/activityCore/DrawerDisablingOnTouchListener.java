package org.openbase.bco.bcomfy.activityCore;

import android.view.MotionEvent;
import android.view.View;

public class DrawerDisablingOnTouchListener implements View.OnTouchListener {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                view.getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        view.onTouchEvent(motionEvent);
        return true;
    }
}
