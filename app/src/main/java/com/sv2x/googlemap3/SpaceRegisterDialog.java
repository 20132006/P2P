package com.sv2x.googlemap3;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

/**
 * Created by netlab on 2/17/16.
 */
public class SpaceRegisterDialog extends DialogFragment {

    CheckBox actual;
    CheckBox matched;
    EditText space_name;
    int decision=0;
    public boolean whichOneIsChecked()
    {
        if (actual.isChecked())
            return false;
        return true;
    }

    public String get_name()
    {
        return String.valueOf(space_name.getText());
    }

    public int get_decision()
    {
        return decision;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater;

        inflater = getActivity().getLayoutInflater();
        View view;

        view = inflater.inflate(R.layout.dialog_builder_layout_for_choosing_location_type, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        space_name = (EditText) view.findViewById(R.id.space_name);
        actual = (CheckBox) view.findViewById(R.id.actual);
        matched = (CheckBox) view.findViewById(R.id.matched);


        final boolean[] error_call = {false};

        matched.setChecked(true);

        actual.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (error_call[0])
                {
                    error_call[0]=false;
                }
                else if (!actual.isChecked() && !matched.isChecked())
                {
                    actual.setChecked(true);
                }
                else if (actual.isChecked() && matched.isChecked())
                {

                    error_call[0] = true;
                    matched.setChecked(false);
                }
            }
        });
        matched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (error_call[0]) {
                    error_call[0] = false;
                } else if (!actual.isChecked() && !matched.isChecked()) {
                    matched.setChecked(true);
                } else if (matched.isChecked() && actual.isChecked()) {
                    error_call[0] = true;
                    actual.setChecked(false);
                }
            }
        });

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                decision = 1;
            }
        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                decision = 2;
            }
        });
        return builder.create();
    }

    @Override
    public void onDismiss (DialogInterface dialog)
    {
        if (decision != 1)
            decision = 3;
    }


}
