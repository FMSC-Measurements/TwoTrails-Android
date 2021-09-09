package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.ui.CheckMarkAnimatedView;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;
import java.io.IOException;

public class NewArcMapDialogTt extends TtBaseDialogFragment {
    private static final String CREATE_MODE = "CreateMode";
    private static final String DEFAULT_NAME = "DefaultName";
    private static final String DEFAULT_URI = "DefaultUrI";

    private EditText txtName, txtUri, txtDesc, txtLoc;
    private CheckMarkAnimatedView chkmkavUrlStatus;
    private Button btnPos;
    private ImageView ivBadUri;
    private Uri tpkPath, defaultUri;

    private int enabledColor, disabledColor;

    private boolean hasName, validUri;

    private String defaultName;

    private CreateMode mode;

    private ArcGisMapLayer aLayer;


    private final ActivityResultLauncher<String> getTpkFile = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        if (result != null && result.getPath() != null) {
            String filePath = result.getPath();

            if (filePath.toLowerCase().endsWith(Consts.FileExtensions.TPK)) {
                tpkPath = result;
                String filename = FileUtils.getFileName(filePath);
                txtUri.setText(filename);
                txtName.requestFocus();
                txtName.setSelection(txtName.getText().toString().length());
                validate();
            } else {
                Toast.makeText(getActivity(), "Invalid TPK File", Toast.LENGTH_LONG).show();
            }
        }
    });


    public static NewArcMapDialogTt newInstance(String name, String uri, CreateMode mode) {
        NewArcMapDialogTt dialog = new NewArcMapDialogTt();

        Bundle bundle = new Bundle();
        bundle.putInt(CREATE_MODE, mode.getValue());
        bundle.putString(DEFAULT_NAME, name);
        bundle.putString(DEFAULT_URI, uri);

        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        twDelayHandler = new PostDelayHandler(1000);

        enabledColor = AndroidUtils.UI.getColor(getContext(), R.color.primary);
        disabledColor = AndroidUtils.UI.getColor(getContext(), R.color.grey_400);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(CREATE_MODE)) {
            mode = CreateMode.parse(bundle.getInt(CREATE_MODE));

            defaultName = bundle.getString(DEFAULT_NAME, StringEx.Empty);

            if (bundle.containsKey(DEFAULT_URI)) {
                String dus = bundle.getString(DEFAULT_URI);
                if (dus != null && dus.length() > 0) {
                    defaultUri = Uri.parse(dus);
                }
            }

            if (mode == CreateMode.OFFLINE_FROM_OFFLINE_URL || mode == CreateMode.OFFLINE_FROM_ONLINE_URL && defaultUri != null)
                defaultUri = getTtAppCtx().getArcGISTools().getOfflineUrlFromOnlineUrl(defaultUri);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder db = new AlertDialog.Builder(getActivity());

        View view = LayoutInflater.from(getContext()).inflate(R.layout.diag_create_arc_map, null);

        txtUri = view.findViewById(R.id.txtUri);
        txtName = view.findViewById(R.id.txtName);
        txtLoc = view.findViewById(R.id.txtLocation);
        txtDesc = view.findViewById(R.id.txtDesc);
        chkmkavUrlStatus = view.findViewById(R.id.chkmkavUrlStatus);
        ivBadUri = view.findViewById(R.id.ivBadUri);

        txtUri.setHint(mode != CreateMode.OFFLINE_FROM_FILE ? "Url" : "File");

        txtName.setText(defaultName);
        if (defaultUri != null)
            txtUri.setText(defaultUri.getPath());

        if (!StringEx.isEmpty(defaultName)) {
            hasName = true;
        }

        if (defaultUri != null) {
//            if (mode != CreateMode.OFFLINE_FROM_FILE) {
//                validateUrl(defaultUri);
//            } else {
                validateFile(defaultUri);
//            }
        }

        txtName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                hasName = s.length() > 0;
                validate();
            }
        });

        db.setTitle("Create Map")
                .setView(view)
                .setPositiveButton(R.string.str_create, (dialog, which) -> {

                    if (aLayer == null) {
                        aLayer = getTtAppCtx().getArcGISTools().createMapLayer(
                                txtName.getText().toString().trim(),
                                txtDesc.getText().toString().trim(),
                                txtLoc.getText().toString().trim(),
                                mode == CreateMode.OFFLINE_FROM_FILE ? "" : tpkPath.getPath(),
                                mode == CreateMode.OFFLINE_FROM_FILE ?  FileUtils.getFileName(tpkPath.getPath()) : "",
                                mode == CreateMode.NEW_ONLINE);


                    } else {
                        aLayer.setName(txtName.getText().toString().trim());
                        aLayer.setDescription(txtDesc.getText().toString().trim());
                        aLayer.setLocation(txtLoc.getText().toString().trim());

                        if (mode == CreateMode.OFFLINE_FROM_FILE) {
                            aLayer.setFileName(FileUtils.getFileName(tpkPath.getPath()));
                        } else {
                            aLayer.setUrl(tpkPath != null ? tpkPath.getPath() : "");
                        }
                    }

                    if (mode == CreateMode.NEW_ONLINE || mode == CreateMode.OFFLINE_FROM_FILE) {

                        if (mode == CreateMode.OFFLINE_FROM_FILE) {
                            File offlineMapsDir = getTtAppCtx().getOfflineMapsDir();
                            File internalCopy = new File(offlineMapsDir, aLayer.getFileName());

                            if (!FileUtils.fileOrFolderExists(offlineMapsDir.getPath())) {
                                if (!offlineMapsDir.mkdirs()) {
                                    getTtAppCtx().getReport().writeError("Unable to create OfflineMapsDir", "NewArcMapDialogTt:CreateMap");
                                }
                            }

                            try {
                                AndroidUtils.Files.copyFile(getActivity(), tpkPath, Uri.fromFile(internalCopy));
                            } catch (IOException e) {
                                getTtAppCtx().getReport().writeError("Unable to copy tpk to internal folder", "NewArcMapDialogTt:CreateMap");
                                Toast.makeText(getActivity(), "Unable to create map. Please see log for details", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        getTtAppCtx().getArcGISTools().addMapLayer(aLayer);
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);

        if (mode == CreateMode.OFFLINE_FROM_FILE) {
            db.setNegativeButton("Browse File", null);
        }

        return db.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            btnPos = d.getButton(Dialog.BUTTON_POSITIVE);

            Button btnNeg = d.getButton(Dialog.BUTTON_NEGATIVE);

            if (btnNeg != null) {
                btnNeg.setOnClickListener(v -> getTpkFile.launch("*/*"));
            }
            validate();
        }
    }


    private void enablePosButton(boolean enable) {
        btnPos.setEnabled(enable);
        btnPos.setTextColor(enable ? enabledColor : disabledColor);
    }

    private boolean validateFile(Uri filePath) {
        if (filePath != null && filePath.getPath() != null && filePath.getPath().endsWith(".tpk") && AndroidUtils.Files.fileExists(getContext(), filePath)) {
            ivBadUri.setVisibility(View.INVISIBLE);
            chkmkavUrlStatus.reset();
            chkmkavUrlStatus.setVisibility(View.VISIBLE);
            chkmkavUrlStatus.start();
            validUri = true;
        } else {
            chkmkavUrlStatus.setVisibility(View.INVISIBLE);
            ivBadUri.setVisibility(View.VISIBLE);
            validUri = false;
        }

        return validUri;
    }

    private void validate() {
        enablePosButton(hasName && validateFile(tpkPath));
    }


    public enum CreateMode {
        NEW_ONLINE(0),
        OFFLINE_FROM_ONLINE_URL(1),
        OFFLINE_FROM_OFFLINE_URL(2),
        OFFLINE_FROM_FILE(3);

        private final int value;

        CreateMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static CreateMode parse(int id) {
            CreateMode[] dists = values();
            if(dists.length > id && id > -1)
                return dists[id];
            throw new IllegalArgumentException("Invalid CreateMode id: " + id);
        }

        @Override
        public String toString() {
            switch(this) {
                case NEW_ONLINE:
                    return "NEW ONLINE";
                case OFFLINE_FROM_ONLINE_URL:
                    return "OFFLINE FROM ONLINE URL";
                case OFFLINE_FROM_OFFLINE_URL:
                    return "OFFLINE FROM OFFLINE URL";
                case OFFLINE_FROM_FILE:
                    return "OFFLINE FROM FILE";
                default: throw new IllegalArgumentException();
            }
        }
    }
}
