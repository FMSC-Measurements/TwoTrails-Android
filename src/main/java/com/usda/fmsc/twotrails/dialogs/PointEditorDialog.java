package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.adapters.MetadataDetailsAdapter;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtMetadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.usda.fmsc.utilities.StringEx;

public class PointEditorDialog extends DialogFragment {
    private static String CN = "CN";
    private static String PID = "PID";
    private static String METACN = "METACN";
    private static String META = "META";
    int ds = -1;

    private View lastSelected;
    private Button posButton;
    private ListView lvcMeta;

    private String posBtnText, negBtnText, pid, metacn, newMetacn, cn;
    private ArrayList<TtMetadata> meta = null;

    PointEditorListener listener;


    public static PointEditorDialog newInstance(String cn, int pid, String metacn, HashMap<String, TtMetadata> metas) throws IOException {
        PointEditorDialog f = new PointEditorDialog();

        Bundle args = new Bundle();

        args.putString(PID, Integer.toString(pid));
        args.putString(METACN, metacn);
        args.putString(CN, cn);
        args.putParcelableArrayList(META, new ArrayList<Parcelable>(metas.values()));

        f.setArguments(args);

        return f;
    }


    public PointEditorDialog() {
        posBtnText = "Edit";
        negBtnText = "Cancel";
    }

    DialogInterface.OnClickListener onPosClick, onNegClick;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && !bundle.isEmpty()) {
            pid = bundle.getString(PID);
            metacn = bundle.getString(METACN);
            cn = bundle.getString(CN);
            meta = bundle.getParcelableArrayList(META);

            newMetacn = metacn;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.diag_edit_point, null);

        final EditText txtPID = (EditText)view.findViewById(R.id.diagEditPointTxtPID);
        lvcMeta = (ListView)view.findViewById(R.id.diagEditPointLvcMeta);

        txtPID.setText(pid);
        txtPID.setInputType(InputType.TYPE_CLASS_NUMBER);
        txtPID.setSelection(pid.length());

        txtPID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                posButton.setEnabled(true);
            }
        });

        for (int i = 0; i < meta.size(); i++) {
            if (meta.get(i).getCN().equals(metacn)) {
                ds = i;
                break;
            }
        }

        lvcMeta.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvcMeta.setAdapter(new MetadataDetailsAdapter(getContext(), meta, ds));

        lvcMeta.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TtMetadata m = meta.get(position);

                if (!m.getCN().equals(metacn)) {
                    posButton.setEnabled(true);
                }

                newMetacn = m.getCN();

                if (lastSelected == null && ds > -1) {
                    lvcMeta.getChildAt(ds).setBackground(AndroidUtils.UI.getDrawable(getContext(), R.drawable.list_item_selector));
                    ds = -1;
                } else if (lastSelected != null && lastSelected != view) {
                    lastSelected.setSelected(false);
                }

                view.setSelected(true);
                lastSelected = view;
            }
        });

         builder.setTitle("Edit Point");

        builder.setView(view);

        builder.setPositiveButton(posBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (onPosClick != null) {
                    onPosClick.onClick(dialog, which);
                }

                boolean update = false;
                if (!StringEx.isEmpty(newMetacn) && !newMetacn.equals(metacn)) {
                    update = true;
                }

                String p = txtPID.getText().toString();
                if (p.length() > 0) {
                    if (!p.equals(pid)) {
                        update = true;
                    }
                } else {
                    update = false;
                    txtPID.requestFocus();
                }

                if (update) {
                    if (listener != null) {
                        listener.onEdited(cn, Integer.parseInt(p), newMetacn);
                    }
                }
            }
        });

        builder.setNegativeButton(negBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (onNegClick != null) {
                    onNegClick.onClick(dialog, which);
                }

                if (listener != null) {
                    listener.onCanceled();
                }
            }
        });

        return builder.create();
    }


    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();

        if (d != null) {
            posButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
            posButton.setEnabled(false);
        }
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

    public void setEditPointListener(PointEditorListener listener) {
        this.listener = listener;
    }

    public interface PointEditorListener {
        void onEdited(String cn, int pid, String metacn);
        void onCanceled();
    }
}
