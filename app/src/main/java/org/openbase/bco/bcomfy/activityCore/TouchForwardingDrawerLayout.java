package org.openbase.bco.bcomfy.activityCore;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class TouchForwardingDrawerLayout extends DrawerLayout {

    public TouchForwardingDrawerLayout( Context context, AttributeSet attrs ) {
        super( context, attrs );
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        boolean r = super.onTouchEvent( event );
        getChildAt( 0 ).dispatchTouchEvent( event );
        return r;
    }
}