package org.openbase.bco.bcomfy.activityInit.view;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;

public class InstructionTextView {

    private TextView textView;
    private Animation pulseFast;

    public enum Instruction {
        EMPTY, MARK_GROUND, MARK_CEILING, MARK_WALLS
    }

    public InstructionTextView(TextView textView, Context context) {
        this.textView = textView;

        textView.setCompoundDrawables(new IconicsDrawable(context)
                .icon(GoogleMaterial.Icon.gmd_touch_app)
                .color(Color.WHITE)
                .sizeDp(64), null, null, null);

        pulseFast = AnimationUtils.loadAnimation(context, R.anim.pulse_fast);
        pulseFast.setFillAfter(false);
    }

    public void updateInstruction(Instruction instruction) {
        switch (instruction) {
            case EMPTY:
                textView.setText(R.string.no_text);
                textView.setVisibility(View.INVISIBLE);
                break;
            case MARK_GROUND:
                textView.setText(R.string.init_mark_ground);
                textView.setVisibility(View.VISIBLE);
                break;
            case MARK_CEILING:
                textView.setText(R.string.init_mark_ceiling);
                textView.setVisibility(View.VISIBLE);
                break;
            case MARK_WALLS:
                textView.setText(R.string.init_mark_walls);
                textView.setVisibility(View.VISIBLE);
                break;
        }

        textView.startAnimation(pulseFast);
    }

    public void updateInstruction(Instruction instruction, int wallNumber, int measurementsFinished, int measurementsNeeded) {
        updateInstruction(instruction);
        String nextWallNumberString;
        int nextWallNumber = wallNumber + 1;

        switch (nextWallNumber) {
            case 1:
                nextWallNumberString = "1st";
                break;
            case 2:
                nextWallNumberString = "2nd";
                break;
            case 3:
                nextWallNumberString = "3rd";
                break;
            default:
                nextWallNumberString = nextWallNumber + "th";
        }

        if (measurementsNeeded > 1) {
            textView.setText("Mark the " + nextWallNumberString + " wall by tapping the display at the according point\n\n" +
                                "This wall needs " + (measurementsNeeded - measurementsFinished) + " remaining taps");
        }
        else {
            textView.setText("Mark the " + nextWallNumberString + " wall by tapping the display at the according point");
        }
    }
}
