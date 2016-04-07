package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
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
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.GetMapExtentsActivity;
import com.usda.fmsc.twotrails.objects.ArcGisMapLayer;
import com.usda.fmsc.twotrails.ui.CheckMarkAnimatedView;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.utilities.StringEx;

import java.net.MalformedURLException;
import java.net.URL;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class ArcMapFromUrlDialog extends DialogFragment {
    private static final String IS_ONLINE = "IsOnline";
    private static final String DEFAULT_NAME = "DefaultName";
    private static final String DEFAULT_URL = "DefaultUrl";

    EditText txtName, txtUrl, txtDesc, txtLoc;
    CheckMarkAnimatedView chkmkavUrlStatus;
    Button btnPos;
    MaterialProgressBar progressBar;
    ImageView ivBadUrl;

    int enabledColor, disabledColor;

    PostDelayHandler delayHandler;

    boolean hasName, validUrl, isOnline;

    ArcGisMapLayer aLayer;

    String defaultName, defaultUrl;


    public static ArcMapFromUrlDialog newInstance(String name, String url, boolean isOnline) {
        ArcMapFromUrlDialog dialog = new ArcMapFromUrlDialog();

        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_ONLINE, isOnline);
        bundle.putString(DEFAULT_NAME, name);
        bundle.putString(DEFAULT_URL, url);

        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        delayHandler = new PostDelayHandler(1000);

        enabledColor = AndroidUtils.UI.getColor(getContext(), R.color.primary);
        disabledColor = AndroidUtils.UI.getColor(getContext(), R.color.grey_400);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(IS_ONLINE)) {
            isOnline = bundle.getBoolean(IS_ONLINE);

            defaultName = bundle.getString(DEFAULT_NAME, StringEx.Empty);
            defaultUrl = bundle.getString(DEFAULT_URL, StringEx.Empty);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder db = new AlertDialog.Builder(getContext());

        View view = LayoutInflater.from(getContext()).inflate(R.layout.diag_create_arc_map, null);

        txtUrl = (EditText)view.findViewById(R.id.txtUrl);
        txtName = (EditText)view.findViewById(R.id.txtName);
        txtLoc = (EditText)view.findViewById(R.id.txtLocation);
        txtDesc = (EditText)view.findViewById(R.id.txtDesc);
        chkmkavUrlStatus = (CheckMarkAnimatedView)view.findViewById(R.id.chkmkavUrlStatus);
        progressBar = (MaterialProgressBar)view.findViewById(R.id.progress);
        ivBadUrl = (ImageView)view.findViewById(R.id.ivBadUrl);

        txtName.setText(defaultName);
        txtUrl.setText(defaultUrl);

        if (!StringEx.isEmpty(defaultName)) {
            hasName = true;
        }

        if (!StringEx.isEmpty(defaultUrl)) {
            validateUrl(defaultUrl);
        }

        txtName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                hasName = s.length() > 0;
                validate();
            }
        });

        txtUrl.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(final Editable s) {
                validUrl = false;

                if (s.length() > 10) {
                    final String url = s.toString();

                    delayHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            validateUrl(url);
                        }
                    });
                } else {
                    ivBadUrl.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    chkmkavUrlStatus.setVisibility(View.INVISIBLE);
                }

                validate();
            }
        });

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
                                    txtUrl.getText().toString(),
                                    isOnline);
                        } else {
                            aLayer.setName(txtName.getText().toString());
                            aLayer.setDescription(txtDesc.getText().toString());
                            aLayer.setLocation(txtLoc.getText().toString());
                            aLayer.setUri(txtUrl.getText().toString());
                        }

                        if (isOnline) {
                            ArcGISTools.addLayer(aLayer);
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

        return db.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            btnPos = d.getButton(Dialog.BUTTON_POSITIVE);
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
        ivBadUrl.setVisibility(View.INVISIBLE);

        if (Patterns.WEB_URL.matcher(url).matches()) {
            if (!url.startsWith("http")) {
                url = String.format("http://%s", url);
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

                            aLayer = layer;

                            validUrl = true;

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
                                    ivBadUrl.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
                }
            }).start();
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            ivBadUrl.setVisibility(View.VISIBLE);
        }
    }

    private void validate() {
        enablePosButton(hasName && validUrl);
    }
}
