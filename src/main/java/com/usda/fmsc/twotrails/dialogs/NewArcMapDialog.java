package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.GetMapExtentsActivity;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.ui.CheckMarkAnimatedView;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NewArcMapDialog extends DialogFragment {
    private static final int FILE_SELECTED = 101;

    private static final String CREATE_MODE = "CreateMode";
    private static final String DEFAULT_NAME = "DefaultName";
    private static final String DEFAULT_URI = "DefaultUrI";

    private EditText txtName, txtUri, txtDesc, txtLoc;
    private CheckMarkAnimatedView chkmkavUrlStatus;
    private Button btnPos;
    private MaterialProgressBar progressBar;
    private ImageView ivBadUri;

    private int enabledColor, disabledColor;

    private boolean hasName, validUri;

    private String defaultName, defaultUri;

    private CreateMode mode;

    private PostDelayHandler twDelayHandler;

    private ArcGisMapLayer aLayer;


    public static NewArcMapDialog newInstance(String name, String uri, CreateMode mode) {
        NewArcMapDialog dialog = new NewArcMapDialog();

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

        twDelayHandler = new PostDelayHandler(1000);

        enabledColor = AndroidUtils.UI.getColor(getContext(), R.color.primary);
        disabledColor = AndroidUtils.UI.getColor(getContext(), R.color.grey_400);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(CREATE_MODE)) {
            mode = CreateMode.parse(bundle.getInt(CREATE_MODE));

            defaultName = bundle.getString(DEFAULT_NAME, StringEx.Empty);
            defaultUri = bundle.getString(DEFAULT_URI, StringEx.Empty);

            if (mode == CreateMode.OFFLINE_FROM_OFFLINE_URL || mode == CreateMode.OFFLINE_FROM_ONLINE_URL && defaultUri != null)
                defaultUri = ArcGISTools.getOfflineUrlFromOnlineUrl(defaultUri);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder db = new AlertDialog.Builder(getContext());

        View view = LayoutInflater.from(getContext()).inflate(R.layout.diag_create_arc_map, null);

        txtUri = (EditText)view.findViewById(R.id.txtUri);
        txtName = (EditText)view.findViewById(R.id.txtName);
        txtLoc = (EditText)view.findViewById(R.id.txtLocation);
        txtDesc = (EditText)view.findViewById(R.id.txtDesc);
        chkmkavUrlStatus = (CheckMarkAnimatedView)view.findViewById(R.id.chkmkavUrlStatus);
        progressBar = (MaterialProgressBar)view.findViewById(R.id.progress);
        ivBadUri = (ImageView)view.findViewById(R.id.ivBadUri);

        txtUri.setHint(mode != CreateMode.OFFLINE_FROM_FILE ? "Url" : "File");

        txtName.setText(defaultName);
        txtUri.setText(defaultUri);

        if (!StringEx.isEmpty(defaultName)) {
            hasName = true;
        }

        if (!StringEx.isEmpty(defaultUri)) {
            if (mode != CreateMode.OFFLINE_FROM_FILE) {
                validateUrl(defaultUri);
            } else {
                validateFile(defaultUri);
            }
        }

        txtName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                hasName = s.length() > 0;
                validate();
            }
        });

        txtUri.addTextChangedListener(stwUri);

        db.setTitle("Create Map")
                .setView(view)
                .setPositiveButton(R.string.str_create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (aLayer == null) {
                            aLayer = ArcGISTools.createMapLayer(
                                    txtName.getText().toString(),
                                    txtDesc.getText().toString(),
                                    txtLoc.getText().toString(),
                                    txtUri.getText().toString(),
                                    null,
                                    mode == CreateMode.NEW_ONLINE);
                        } else {
                            aLayer.setName(txtName.getText().toString());
                            aLayer.setDescription(txtDesc.getText().toString());
                            aLayer.setLocation(txtLoc.getText().toString());
                            aLayer.setUrl(txtUri.getText().toString());
                        }

                        if (mode == CreateMode.NEW_ONLINE || mode == CreateMode.OFFLINE_FROM_FILE) {
                            ArcGISTools.addMapLayer(aLayer);
                        } else {
                            Intent intent = new Intent(getContext(), GetMapExtentsActivity.class);

                            Bundle bundle = new Bundle();
                            bundle.putParcelable(GetMapExtentsActivity.MAP_LAYER, aLayer);
                            intent.putExtras(bundle);

                            startActivity(intent);
                        }
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);

        if (mode == CreateMode.OFFLINE_FROM_FILE) {
            db.setNegativeButton("Browse File", null);
        }

        return db.create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECTED && data != null) {
            Uri uri = data.getData();

            if (uri != null) {
                txtUri.setText(uri.getPath());
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            btnPos = d.getButton(Dialog.BUTTON_POSITIVE);

            Button btnNeg = d.getButton(Dialog.BUTTON_NEGATIVE);

            if (btnNeg != null) {
                btnNeg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AndroidUtils.App.openFileIntent(getActivity(), "file/*.tt", FILE_SELECTED);
                    }
                });
            }
            validate();
        }
    }


    private void enablePosButton(boolean enable) {
        btnPos.setEnabled(enable);
        btnPos.setTextColor(enable ? enabledColor : disabledColor);
    }


    private void validateUrl(String url) {
        progressBar.setVisibility(View.VISIBLE);
        chkmkavUrlStatus.setVisibility(View.INVISIBLE);
        ivBadUri.setVisibility(View.INVISIBLE);

        if (Patterns.WEB_URL.matcher(url).matches()) {
            if (!url.startsWith("http")) {
                url = String.format("http://%s", url);
            }

            if (mode == CreateMode.OFFLINE_FROM_ONLINE_URL || mode == CreateMode.OFFLINE_FROM_OFFLINE_URL) {
                url = ArcGISTools.getOfflineUrlFromOnlineUrl(url);
            }

            final String fUrl = url;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArcGISTools.getLayerFromUrl(fUrl, getContext(), new ArcGISTools.IGetArcMapLayerListener() {
                        @Override
                        public void onComplete(ArcGisMapLayer layer) {
                            if (txtDesc.getText().length() < 1 && layer.getDescription().length() > 0) {
                                txtDesc.setText(layer.getDescription());
                            }

                            if (mode == CreateMode.OFFLINE_FROM_ONLINE_URL || mode == CreateMode.OFFLINE_FROM_OFFLINE_URL) {
                                txtUri.removeTextChangedListener(stwUri);
                                txtUri.setText(layer.getUrl());
                                txtUri.addTextChangedListener(stwUri);
                            }

                            aLayer = layer;

                            validUri = true;

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    chkmkavUrlStatus.reset();

                                    progressBar.setVisibility(View.INVISIBLE);
                                    chkmkavUrlStatus.setVisibility(View.VISIBLE);

                                    chkmkavUrlStatus.start();

                                    validate();
                                }
                            });
                        }

                        @Override
                        public void onBadUrl() {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    ivBadUri.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
                }
            }).start();
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            ivBadUri.setVisibility(View.VISIBLE);
        }
    }

    private void validateFile(String filePath) {
        if (TtUtils.fileExists(filePath))
        {
            ivBadUri.setVisibility(View.INVISIBLE);
            chkmkavUrlStatus.reset();
            chkmkavUrlStatus.setVisibility(View.VISIBLE);
            chkmkavUrlStatus.start();
        } else {
            chkmkavUrlStatus.setVisibility(View.INVISIBLE);
            ivBadUri.setVisibility(View.VISIBLE);
        }

        validate();
    }

    private void validate() {
        enablePosButton(hasName && validUri);
    }


    SimpleTextWatcher stwUri = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(final Editable s) {
            validUri = false;

            if (s.length() > 10) {
                final String uri = s.toString();

                twDelayHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mode == CreateMode.OFFLINE_FROM_FILE) {
                            validateFile(uri);
                        } else {
                            validateUrl(uri);
                        }
                    }
                });
            } else {
                ivBadUri.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                chkmkavUrlStatus.setVisibility(View.INVISIBLE);
            }

            validate();
        }
    };


    public enum CreateMode {
        NEW_ONLINE(0),
        OFFLINE_FROM_ONLINE_URL(1),
        OFFLINE_FROM_OFFLINE_URL(2),
        OFFLINE_FROM_FILE(3);

        private final int value;

        private CreateMode(int value) {
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
