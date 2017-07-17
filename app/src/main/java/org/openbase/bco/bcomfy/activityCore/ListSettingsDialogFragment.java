package org.openbase.bco.bcomfy.activityCore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;

public class ListSettingsDialogFragment extends DialogFragment {

    public enum SettingValue {
        ALL, LOCATED, UNLOCATED
    }

    public interface OnSettingsChosenListener {
        void onSettingsChosen(SettingValue unitSetting, SettingValue locationSetting);
    }

    private OnSettingsChosenListener onSettingsChosenListener;

    private RadioButton radioButtonUnitsAll;
    private RadioButton radioButtonUnitsLocated;
    private RadioButton radioButtonUnitsUnlocated;
    private RadioButton radioButtonLocationsAll;
    private RadioButton radioButtonLocationsLocated;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            onSettingsChosenListener = (OnSettingsChosenListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement OnSettingsChosenListener");
        }
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_list_settings, null);

        radioButtonUnitsAll = dialogView.findViewById(R.id.radioButtonUnitsAll);
        radioButtonUnitsLocated = dialogView.findViewById(R.id.radioButtonUnitsLocated);
        radioButtonUnitsUnlocated = dialogView.findViewById(R.id.radioButtonUnitsUnlocated);
        radioButtonLocationsAll = dialogView.findViewById(R.id.radioButtonLocationsAll);
        radioButtonLocationsLocated = dialogView.findViewById(R.id.radioButtonLocationsLocated);

        switch (((CoreActivity) getActivity()).getCurrentUnitSetting()) {
            case ALL:
                radioButtonUnitsAll.setChecked(true);
                break;
            case LOCATED:
                radioButtonUnitsLocated.setChecked(true);
                break;
            case UNLOCATED:
                radioButtonUnitsUnlocated.setChecked(true);
        }

        switch (((CoreActivity) getActivity()).getCurrentLocationSetting()) {
            case ALL:
                radioButtonLocationsAll.setChecked(true);
                break;
            case LOCATED:
                radioButtonLocationsLocated.setChecked(true);
        }

        builder.setView(dialogView)
                .setIcon(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_settings).color(Color.WHITE).sizeDp(24))
                .setTitle(R.string.list_settings_title)
                .setPositiveButton(R.string.gui_apply, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        RadioButton radioButtonUnitsAll;
//                        RadioButton radioButtonUnitsLocated;
//                        RadioButton radioButtonUnitsUnlocated;
//                        RadioButton radioButtonLocationsAll;
//                        RadioButton radioButtonLocationsLocated;
//
//                        radioButtonUnitsAll = ((AlertDialog) dialog).findViewById(R.id.radioButtonUnitsAll);
//                        radioButtonUnitsLocated = ((AlertDialog) dialog).findViewById(R.id.radioButtonUnitsLocated);
//                        radioButtonUnitsUnlocated = ((AlertDialog) dialog).findViewById(R.id.radioButtonUnitsUnlocated);
//                        radioButtonLocationsAll = ((AlertDialog) dialog).findViewById(R.id.radioButtonLocationsAll);
//                        radioButtonLocationsLocated = ((AlertDialog) dialog).findViewById(R.id.radioButtonLocationsLocated);

                        SettingValue unitSetting;
                        SettingValue locationSetting;

                        if (radioButtonUnitsAll.isChecked()) {
                            unitSetting = SettingValue.ALL;
                        } else if (radioButtonUnitsLocated.isChecked()) {
                            unitSetting = SettingValue.LOCATED;
                        } else {
                            unitSetting = SettingValue.UNLOCATED;
                        }

                        if (radioButtonLocationsAll.isChecked()) {
                            locationSetting = SettingValue.ALL;
                        } else {
                            locationSetting = SettingValue.LOCATED;
                        }

                        onSettingsChosenListener.onSettingsChosen(unitSetting, locationSetting);
                    }
                })
                .setNegativeButton(R.string.gui_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

}
