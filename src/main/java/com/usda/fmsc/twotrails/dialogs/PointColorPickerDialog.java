package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ValueBar;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;

import java.util.Arrays;

public class PointColorPickerDialog extends DialogFragment {
    public static final String OPTIONS = "Options";
    public static final String NAME = "Name";

    private static final int NUMBER_OF_COLORS = 7;

    private @Size(NUMBER_OF_COLORS) @ColorInt int[] colorOptions;
    private @Size(NUMBER_OF_COLORS) @ColorInt int[] originalColorOptions;
    private String name;

    private @ColorInt int colorTrans, colorSelected;

    private int colorIndex = 0;

    private ColorPicker picker;
    private RelativeLayout lastSelectedView;
    private ImageView[] ivs = new ImageView[NUMBER_OF_COLORS];
    
    
    private PointColorListener listener;

    public static PointColorPickerDialog newInstance(@Size(NUMBER_OF_COLORS) @ColorInt int[] colorOptions, String polygonName) {
        PointColorPickerDialog dialog = new PointColorPickerDialog();

        Bundle bundle = new Bundle();
        bundle.putIntArray(OPTIONS, colorOptions);
        bundle.putString(NAME, polygonName);
        dialog.setArguments(bundle);

        return dialog;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(OPTIONS) && bundle.containsKey(NAME)) {
            colorOptions = bundle.getIntArray(OPTIONS);
            name = bundle.getString(NAME);

            originalColorOptions = Arrays.copyOf(colorOptions, NUMBER_OF_COLORS);
        }
        
        colorTrans = AndroidUtils.UI.getColor(getActivity(), android.R.color.transparent);
        colorSelected = AndroidUtils.UI.getColor(getActivity(), R.color.black_1000);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View view = inflater.inflate(R.layout.diag_point_color_picker, null);

        picker = (ColorPicker) view.findViewById(R.id.picker);
        ValueBar valueBar = (ValueBar) view.findViewById(R.id.valuebar);

        picker.addValueBar(valueBar);
        picker.setColor(originalColorOptions[0]);
        picker.setOldCenterColor(originalColorOptions[0]);
        picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                updateColor(colorIndex, color);
            }
        });

        int[] ids = new int[] {
                R.id.ivPCPAdjBnd,
                R.id.ivPCPAdjNav,
                R.id.ivPCPUnAdjBnd,
                R.id.ivPCPUnadjNav,
                R.id.ivPCPAdjPts,
                R.id.ivPCPUnadjPts,
                R.id.ivPCPWayPts
        };

        String[] descs = new String[] {
                getString(R.string.map_adj_bnd),
                getString(R.string.map_adj_nav),
                getString(R.string.map_unadj_bnd),
                getString(R.string.map_unadj_nav),
                getString(R.string.diag_pcp_adjpts),
                getString(R.string.diag_pcp_unadjpts),
                getString(R.string.map_way_pts)
        };

        for (int i = 0; i < NUMBER_OF_COLORS; i++) {
            ImageView iv = (ImageView)view.findViewById(ids[i]);
            iv.setBackgroundColor(colorOptions[i]);

            final int index = i;
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectPointType(ivs[index], index);
                }
            });

            AndroidUtils.UI.setContentDescToast(iv, descs[i]);

            ivs[i] = iv;
        }
        
        lastSelectedView = (RelativeLayout) ivs[0].getParent();
        
        dialog.setView(view)
        .setTitle(name)
        .setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    listener.onUpdated(colorOptions);
                }
            }
        })
        .setNegativeButton(R.string.str_reset, null)
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
                negButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        colorOptions = Arrays.copyOf(originalColorOptions, NUMBER_OF_COLORS);

                        for (int i = 0; i < NUMBER_OF_COLORS; i++) {
                            ivs[i].setBackgroundColor(colorOptions[i]);
                        }

                        if (picker != null) {
                            picker.setColor(colorOptions[colorIndex]);
                            picker.setOldCenterColor(colorOptions[colorIndex]);
                        }
                    }
                });
            }
        }
    }
    
    
    private void selectPointType(ImageView imageView, int newIndex) {
        colorIndex = newIndex;
        picker.setOldCenterColor(colorOptions[colorIndex]);
        picker.setColor(colorOptions[colorIndex]);
        
        lastSelectedView.setBackgroundColor(colorTrans);
        
        lastSelectedView = (RelativeLayout) imageView.getParent();
        lastSelectedView.setBackgroundColor(colorSelected);
    }
    
    private void updateColor(int index, @ColorInt int color) {
        ivs[index].setBackgroundColor(color);
        colorOptions[index] = color;
    }
    

    public void setListener(PointColorListener listener) {
        this.listener = listener;
    }

    public interface PointColorListener {
        void onUpdated(@Size(7) @ColorInt int[] colorOptions);
    }
}