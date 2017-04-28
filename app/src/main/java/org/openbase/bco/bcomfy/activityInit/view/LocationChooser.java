package org.openbase.bco.bcomfy.activityInit.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;

import java8.util.Comparators;
import java8.util.J8Arrays;
import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitConfigType;

public class LocationChooser extends DialogFragment {

    public interface LocationChooserListener {
        void onLocationSelected(String locationId);
    }

    LocationChooserListener listener;
    UnitConfigType.UnitConfig[] locations;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Verify that the host activity implements the callback interface
        if (context instanceof LocationChooserListener) {
            listener = (LocationChooserListener) context;
        }
        else {
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            locations = StreamSupport.stream(Registries.getLocationRegistry().getLocationConfigs())
                    .sorted(Comparators.comparing(UnitConfigType.UnitConfig::getLabel))
                    .toArray(UnitConfigType.UnitConfig[]::new);

            String[] locationStrings = J8Arrays.stream(locations)
                    .map(UnitConfigType.UnitConfig::getLabel)
                    .toArray(String[]::new);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.choose_location)
                    .setItems(locationStrings, (dialog, which) -> listener.onLocationSelected(locations[which].getId()));

            return builder.create();
        } catch (InterruptedException | CouldNotPerformException e) {
            e.printStackTrace();
            return null;
        }
    }
}
