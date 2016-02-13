package com.usda.fmsc.twotrails.dialogs;

import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.utilities.AppUnits;

import java.util.List;

import com.usda.fmsc.utilities.StringEx;

public class MoveToPointDialog extends DialogFragment {

    private int currentIndex;
    private List<TtPoint> points;
    private String posBtnText, negBtnText, title;
    ImageButton fButton, lButton;

    public MoveToPointDialog() {
        posBtnText = negBtnText = StringEx.Empty;
    }

    DialogInterface.OnClickListener onPosClick, onNegClick, onFirstClick, onLastClick;
    AdapterView.OnItemClickListener onItemClick;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.diag_move_to_location, null);

        builder.setView(view);

        fButton = (ImageButton)view.findViewById(R.id.diagMoveToLocFirst);
        lButton = (ImageButton)view.findViewById(R.id.diagMoveToLocLast);

        if (currentIndex < 1) {
            fButton.setEnabled(false);
            fButton.setAlpha(Consts.DISABLED_ALPHA);
        }

        if (currentIndex > points.size() - 2) {
            lButton.setEnabled(false);
            lButton.setAlpha(Consts.DISABLED_ALPHA);
        }


        final ListView listView = (ListView)view.findViewById(R.id.diagEitableListListValues);

        PointDetailsAdapter adapter = new PointDetailsAdapter(points, getActivity(), AppUnits.IconColor.Primary);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onItemClick.onItemClick(parent, view, position, id);
                dismiss();
            }
        });


        if (title != null)
            builder.setTitle(title);

        if (!StringEx.isEmpty(posBtnText))
            builder.setPositiveButton(posBtnText, onPosClick);

        if(!StringEx.isEmpty(negBtnText))
            builder.setNegativeButton(negBtnText, onNegClick);

        final AlertDialog dialog = builder.create();

        fButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onFirstClick != null) {
                    onFirstClick.onClick(dialog, 0);
                }
                dismiss();
            }
        });

        lButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onLastClick != null) {
                    onLastClick.onClick(dialog, 0);
                }
                dismiss();
            }
        });

        return dialog;
    }

    public void setPositiveButton(DialogInterface.OnClickListener listener) {
        onPosClick = listener;
    }

    public void setPositiveButton(String text, DialogInterface.OnClickListener listener) {
        onPosClick = listener;
        posBtnText = text;
    }

    public void setNegativeButton(DialogInterface.OnClickListener listener) {
        onNegClick = listener;
    }

    public void setNegativeButton(String text, DialogInterface.OnClickListener listener) {
        onNegClick = listener;
        negBtnText = text;
    }


    public void setFirstListener(DialogInterface.OnClickListener listener) {
        onFirstClick = listener;
    }

    public void setLastListener(DialogInterface.OnClickListener listener) {
        onLastClick = listener;
    }

    public void setOnItemClick(AdapterView.OnItemClickListener listener) {
        onItemClick = listener;
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public void setItems(List<TtPoint> points, int currentIndex) {
        this.points = points;
        this.currentIndex = currentIndex;
    }
}
