package org.openbase.bco.bcomfy.activityInit.view;

import android.view.View;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;

public class InstructionTextView {

    private TextView textView;

    public enum Instruction {
        EMPTY, MARK_GROUND, MARK_CEILING, MARK_WALLS
    }

    public InstructionTextView(TextView textView) {
        this.textView = textView;
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
            textView.setText("Please mark the " + nextWallNumberString + " wall in a clockwise order\n\n" +
                                "Measurements left: " + (measurementsNeeded - measurementsFinished));
        }
        else {
            textView.setText("Please mark the " + nextWallNumberString + " wall in a clockwise order");
        }
    }
}
