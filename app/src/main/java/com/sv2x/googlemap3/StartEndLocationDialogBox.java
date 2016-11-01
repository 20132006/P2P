package com.sv2x.googlemap3;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

/**
 * Created by netlab on 10/25/16.
 */
public class StartEndLocationDialogBox extends DialogFragment {

    int decision=0;
    Boolean S_exist=false;
    Boolean D_exist=false;
    Button starting_selected;
    Button destination_selected;

    public int get_decision()
    {
        return decision;
    }
    public void setS_exist()
    {
        S_exist = true;
    }
    public void setD_exist()
    {
        D_exist = true;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater;

        inflater = getActivity().getLayoutInflater();
        View view;

        view = inflater.inflate(R.layout.dialog_for_choosing_location_option, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        starting_selected = (Button) view.findViewById(R.id.start_id);
        destination_selected = (Button) view.findViewById(R.id.dest_id);

        if (S_exist)
        {
            starting_selected.setVisibility(View.GONE);
        }
        else if (!S_exist)
        {
            starting_selected.setVisibility(View.VISIBLE);
        }

        if (D_exist)
        {
            destination_selected.setVisibility(View.GONE);
        }
        else if (!D_exist)
        {
            destination_selected.setVisibility(View.VISIBLE);
        }

        starting_selected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                S_exist = true;
                decision = 4;
            }
        });
        destination_selected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                D_exist = true;
                decision = 5;
            }
        });


        builder.setView(view).setPositiveButton("RESET", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                S_exist = false;
                D_exist = false;
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
        if (decision!=5 && decision!=1 && decision!=2 && decision!=4)
            decision = 3;
    }
}
