package org.openbase.bco.bcomfy.activityInit.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import org.openbase.bco.bcomfy.R;

public class LocationChooser extends DialogFragment {

    public interface LocationChooserListener {
        public CharSequence[] getLocations();
        public void onLocationSelected(String location);
    }

    LocationChooserListener listener;

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
        final CharSequence[] locations = listener.getLocations();


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_location)
                .setItems(locations, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onLocationSelected(locations[which].toString());
                    }
                });

        return builder.create();
    }
}
