package org.openbase.bco.bcomfy.activityInit.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityInit.measure.Measurer;

public class InstructionTextView {

    private Context context;

    private TextView textView;
    private Animation pulseFast;

    private int colorDefault;
    private int colorPositive;
    private int colorNegative;

    public InstructionTextView(TextView textView, Context context) {
        this.context = context;
        this.textView = textView;

        colorDefault = context.getColor(R.color.background_gray_slightly_transparent);
        colorPositive = context.getColor(R.color.background_green_slightly_transparent);
        colorNegative = context.getColor(R.color.background_red_slightly_transparent);

        textView.setCompoundDrawables(new IconicsDrawable(context)
                .icon(GoogleMaterial.Icon.gmd_touch_app)
                .color(Color.WHITE)
                .sizeDp(64), null, null, null);

        pulseFast = AnimationUtils.loadAnimation(context, R.anim.pulse_fast);
        pulseFast.setFillAfter(false);
    }

    public void updateInstruction(Measurer.MeasurerState measurerState) {
        switch (measurerState) {
            case INIT:
                textView.setVisibility(View.INVISIBLE);
                textView.setText(R.string.no_text);
                break;
            case MARK_GROUND:
                textView.setText(R.string.init_mark_ground);
                textView.setVisibility(View.VISIBLE);
                textView.startAnimation(pulseFast);
                break;
            case MARK_CEILING:
                textView.setText(R.string.init_mark_ceiling);
                textView.setVisibility(View.VISIBLE);
                textView.startAnimation(pulseFast);
                break;
            case MARK_WALLS:
                textView.setText(R.string.init_mark_walls);
                textView.setVisibility(View.VISIBLE);
                textView.startAnimation(pulseFast);
                break;
        }
    }

    public void updateInstruction(Measurer.MeasurerState measurerState, int wallNumber, int measurementsFinished, int measurementsNeeded) {
        updateInstruction(measurerState);
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
            if (measurerState == Measurer.MeasurerState.MARK_WALLS) {
                textView.setText(Html.fromHtml(context.getString(R.string.init_mark_walls_number_measurements, nextWallNumberString, (measurementsNeeded - measurementsFinished))));
            }
            else if (measurerState == Measurer.MeasurerState.ENOUGH_WALLS) {
                textView.setText(Html.fromHtml(context.getString(R.string.init_mark_walls_number_measurements_enough, nextWallNumberString, (measurementsNeeded - measurementsFinished))));
            }
        }
        else {
            if (measurerState == Measurer.MeasurerState.MARK_WALLS) {
                textView.setText(Html.fromHtml(context.getString(R.string.init_mark_walls_number, nextWallNumberString)));
            }
            else if (measurerState == Measurer.MeasurerState.ENOUGH_WALLS) {
                textView.setText(Html.fromHtml(context.getString(R.string.init_mark_walls_number_enough, nextWallNumberString)));
            }
        }
    }

    public void animatePositive() {
        ObjectAnimator objectAnimator = ObjectAnimator.ofArgb(textView, "backgroundColor", colorDefault, colorPositive)
                .setDuration(200);
        objectAnimator.setRepeatCount(1);
        objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
        objectAnimator.start();
    }

    public void animateNegative() {
        ObjectAnimator objectAnimator = ObjectAnimator.ofArgb(textView, "backgroundColor", colorDefault, colorNegative)
                .setDuration(200);
        objectAnimator.setRepeatCount(1);
        objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
        objectAnimator.start();
    }
}
