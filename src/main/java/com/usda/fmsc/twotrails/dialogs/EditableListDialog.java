package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.usda.fmsc.twotrails.R;

import java.util.List;

import com.usda.fmsc.utilities.StringEx;

public class EditableListDialog extends DialogFragment {
    private String[] items;
    private String selectedItem = StringEx.Empty;

    private String posBtnText, negBtnText, title;

    public EditableListDialog() {
        posBtnText = negBtnText = StringEx.Empty;
    }

    private DialogInterface.OnClickListener onPosClick, onNegClick;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.diag_editable_list, null);

        builder.setView(view);

        if (selectedItem == null) {
            selectedItem = StringEx.Empty;
        }

        final EditText editText = (EditText)view.findViewById(R.id.diagEitableListTxtValue);
        editText.setText(selectedItem);
        editText.setSelection(selectedItem.length());
        editText.setInputType(InputType.TYPE_CLASS_TEXT);

        final ListView listView = (ListView)view.findViewById(R.id.diagEitableListListValues);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.list_row_diag_editable);


        adapter.addAll(items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = items[position];
                editText.setText(selectedItem);
                editText.setSelection(selectedItem.length());
            }
        });



        if (title != null)
            builder.setTitle(title);

        if (!StringEx.isEmpty(posBtnText))
            builder.setPositiveButton(posBtnText, onPosClick);

        if(!StringEx.isEmpty(negBtnText))
            builder.setNegativeButton(negBtnText, onNegClick);

        return builder.create();
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

    public void setDefaultValue(String value) {
        selectedItem = value;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setItems(List<String> items) {
        this.items = items.toArray(new String[items.size()]);
    }

    public void setItems(String[] items) {
        this.items = items;
    }

    public String getSelectedItem() {
        return selectedItem;
    }
}
