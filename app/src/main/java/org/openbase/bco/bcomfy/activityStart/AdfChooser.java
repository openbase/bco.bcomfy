package org.openbase.bco.bcomfy.activityStart;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;

import org.openbase.bco.bcomfy.R;

import java.util.ArrayList;

import java8.util.stream.StreamSupport;

public class AdfChooser extends DialogFragment {

    public interface AdfChooserListener {
        void onAdfSelected(String adfUuid);
    }

    AdfChooserListener listener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Verify that the host activity implements the callback interface
        if (context instanceof AdfChooserListener) {
            listener = (AdfChooserListener) context;
        }
        else {
            throw new ClassCastException(context.toString()
                    + " must implement AdfChooserListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ArrayList<Pair<String, String>> adfList = (ArrayList<Pair<String, String>>) getArguments().getSerializable("adfList");

        String[] adfListStrings = StreamSupport.stream(adfList)
                .map(stringStringPair -> stringStringPair.second + "\n-> " + stringStringPair.first)
                .toArray(String[]::new);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_adf)
                .setItems(adfListStrings, (dialog, which) -> listener.onAdfSelected(adfList.get(which).first));

        return builder.create();
    }
}
