package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ValueBar;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.MapSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;

import java.util.Arrays;

public class ColorPickerDialog extends TtBaseDialogFragment {
    public static final String COLOR = "color";

    private @ColorInt int colorSelected;

    private ColorPicker picker;


    private ColorListener listener;

    public static ColorPickerDialog newInstance(@ColorInt int color) {
        ColorPickerDialog dialog = new ColorPickerDialog();

        Bundle bundle = new Bundle();
        bundle.putInt(COLOR, color);
        dialog.setArguments(bundle);

        return dialog;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(COLOR)) {
            colorSelected = bundle.getInt(COLOR);
        } else {
            colorSelected = AndroidUtils.UI.getColor(getActivity(), R.color.black_1000);
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View view = inflater.inflate(R.layout.diag_color_picker, null);

        picker = view.findViewById(R.id.picker);
        ValueBar valueBar = view.findViewById(R.id.valuebar);

        picker.addValueBar(valueBar);
        picker.setColor(colorSelected);
        picker.setOldCenterColor(colorSelected);
        picker.setOnColorChangedListener(color -> colorSelected = color);

        dialog.setView(view)
                .setPositiveButton(R.string.str_ok, (dialog1, which) -> {
                    if (listener != null) {
                        listener.onColorChange(colorSelected);
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);


        return dialog.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();

        if (d != null) {
            Button negButton = d.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (negButton != null) {
                negButton.setOnClickListener(v -> {
                    if (picker != null) {
                        picker.setColor(colorSelected);
                        picker.setOldCenterColor(colorSelected);
                    }
                });
            }
        }
    }


    public ColorPickerDialog setListener(ColorListener listener) {
        this.listener = listener;
        return this;
    }

    public interface ColorListener {
        void onColorChange(@ColorInt int color);
    }
}
