package com.usda.fmsc.twotrails.fragments.imprt;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.utilities.Import;
import com.usda.fmsc.twotrails.utilities.Import.TextImportTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportTextFragment extends BaseImportFragment {
    private static final String FILENAME = "filename";

    SwitchCompat swtAdvanced;

    TableRow rowCN, rowOp, rowIndex, rowPID, rowTime, rowPoly, rowGroup,
            rowCmt, rowBnd, rowX, rowY, rowZ, rowManAcc,
            rowLat, rowLon, rowElev, rowRMSEr, rowFwdAz, rowBkAz, rowSlpDist,
            rowSlpDistType, rowSlpAng, rowSlpAngType, rowParentCN;//, rowMeta, rowAcc

    Spinner spnCN, spnOp, spnIndex, spnPID, spnTime, spnPoly, spnGroup,
            spnCmt, spnBnd, spnX, spnY, spnZ, spnManAcc,
            spnLat, spnLon, spnElev, spnRMSEr, spnFwdAz, spnBkAz, spnSlpDist,
            spnSlpDistType, spnSlpAng, spnSlpAngType, spnParentCN;//, spnMeta, spnAcc
    
    View divOp, divCN, divManAcc, divLat, divLon, divElev, divRMSEr, divFwdAz,
            divBkAz, divSlpDist, divSlpDistType, divSlpAng, divSlpAngType, divParentCN;


    TextImportTask task;

    private String[] _Columns;
    private String _FileName;

    private boolean advImport = false, validFile;


    public static ImportTextFragment newInstance(String fileName) {
        ImportTextFragment fragment = new ImportTextFragment();
        Bundle args = new Bundle();
        
        args.putString(FILENAME, fileName);
        
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(FILENAME)) {
            _FileName = bundle.getString(FILENAME);

            updateFileName(_FileName);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import_text, container, false);

        swtAdvanced = view.findViewById(R.id.importFragSwtAdvanced);

        //region Rows
        rowCN = view.findViewById(R.id.importFragItemRowCN);
        rowOp = view.findViewById(R.id.importFragItemRowOp);
        rowIndex = view.findViewById(R.id.importFragItemRowIndex);
        rowPID = view.findViewById(R.id.importFragItemRowPID);
        rowTime = view.findViewById(R.id.importFragItemRowTime);
        rowPoly = view.findViewById(R.id.importFragItemRowPoly);
        rowGroup = view.findViewById(R.id.importFragItemRowGroup);
        rowCmt = view.findViewById(R.id.importFragItemRowCmt);
        //rowMeta = (TableRow)view.findViewById(R.id.importFragItemRowMeta);
        rowBnd = view.findViewById(R.id.importFragItemRowBnd);
        rowX = view.findViewById(R.id.importFragItemRowX);
        rowY = view.findViewById(R.id.importFragItemRowY);
        rowZ = view.findViewById(R.id.importFragItemRowZ);
        //rowAcc = (TableRow)view.findViewById(R.id.importFragItemRowAcc);
        rowManAcc = view.findViewById(R.id.importFragItemRowManAcc);
        rowLat = view.findViewById(R.id.importFragItemRowLat);
        rowLon = view.findViewById(R.id.importFragItemRowLon);
        rowElev = view.findViewById(R.id.importFragItemRowElev);
        rowRMSEr = view.findViewById(R.id.importFragItemRowRMSEr);
        rowFwdAz = view.findViewById(R.id.importFragItemRowFwdAz);
        rowBkAz = view.findViewById(R.id.importFragItemRowBkAz);
        rowSlpDist = view.findViewById(R.id.importFragItemRowSlpDist);
        rowSlpDistType = view.findViewById(R.id.importFragItemRowSlpDistType);
        rowSlpAng = view.findViewById(R.id.importFragItemRowSlpAng);
        rowSlpAngType = view.findViewById(R.id.importFragItemRowSlpAngType);
        rowParentCN = view.findViewById(R.id.importFragItemRowParentCN);
        //end region
        
        
        //region Diviers
        divCN = view.findViewById(R.id.importFragDivCN);
        divOp = view.findViewById(R.id.importFragDivOp);
        divManAcc = view.findViewById(R.id.importFragDivManAcc);
        divLat = view.findViewById(R.id.importFragDivLat);
        divLon = view.findViewById(R.id.importFragDivLon);
        divElev = view.findViewById(R.id.importFragDivElev);
        divRMSEr = view.findViewById(R.id.importFragDivRMSEr);
        divFwdAz = view.findViewById(R.id.importFragDivFwdAz);
        divBkAz = view.findViewById(R.id.importFragDivBkAz);
        divSlpDist = view.findViewById(R.id.importFragDivSlpDist);
        divSlpDistType = view.findViewById(R.id.importFragDivSlpDistType);
        divSlpAng = view.findViewById(R.id.importFragDivSlpAng);
        divSlpAngType = view.findViewById(R.id.importFragDivSlpAngType);
        divParentCN = view.findViewById(R.id.importFragDivParentCN);
        //endregion
        

        //region Spinners
        spnCN = view.findViewById(R.id.importFragSpnCN);
        spnOp = view.findViewById(R.id.importFragSpnOp);
        spnIndex = view.findViewById(R.id.importFragSpnIndex);
        spnPID = view.findViewById(R.id.importFragSpnPID);
        spnTime = view.findViewById(R.id.importFragSpnTime);
        spnPoly = view.findViewById(R.id.importFragSpnPoly);
        spnGroup = view.findViewById(R.id.importFragSpnGroup);
        spnCmt = view.findViewById(R.id.importFragSpnCmt);
        //spnMeta = (Spinner)view.findViewById(R.id.importFragSpnMeta);
        spnBnd = view.findViewById(R.id.importFragSpnBnd);
        spnX = view.findViewById(R.id.importFragSpnX);
        spnY = view.findViewById(R.id.importFragSpnY);
        spnZ = view.findViewById(R.id.importFragSpnZ);
        //spnAcc = (Spinner)view.findViewById(R.id.importFragSpnAcc);
        spnManAcc = view.findViewById(R.id.importFragSpnManAcc);
        spnLat = view.findViewById(R.id.importFragSpnLat);
        spnLon = view.findViewById(R.id.importFragSpnLon);
        spnElev = view.findViewById(R.id.importFragSpnElev);
        spnRMSEr = view.findViewById(R.id.importFragSpnRMSEr);
        spnFwdAz = view.findViewById(R.id.importFragSpnFwdAz);
        spnBkAz = view.findViewById(R.id.importFragSpnBkAz);
        spnSlpDist = view.findViewById(R.id.importFragSpnSlpDist);
        spnSlpDistType = view.findViewById(R.id.importFragSpnSlpDistType);
        spnSlpAng = view.findViewById(R.id.importFragSpnSlpAng);
        spnSlpAngType = view.findViewById(R.id.importFragSpnSlpAngType);
        spnParentCN = view.findViewById(R.id.importFragSpnParentCN);

        AdapterView.OnItemSelectedListener sl = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                readyToImport(validate());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                readyToImport(validate());
            }
        };

        spnCN.setOnItemSelectedListener(sl);
        spnOp.setOnItemSelectedListener(sl);
        spnIndex.setOnItemSelectedListener(sl);
        spnPID.setOnItemSelectedListener(sl);
        spnTime.setOnItemSelectedListener(sl);
        spnPoly.setOnItemSelectedListener(sl);
        spnGroup.setOnItemSelectedListener(sl);
        spnCmt.setOnItemSelectedListener(sl);
        //spnMeta.setOnItemSelectedListener(sl);
        spnBnd.setOnItemSelectedListener(sl);
        spnX.setOnItemSelectedListener(sl);
        spnY.setOnItemSelectedListener(sl);
        spnZ.setOnItemSelectedListener(sl);
        //spnAcc.setOnItemSelectedListener(sl);
        spnManAcc.setOnItemSelectedListener(sl);
        spnLat.setOnItemSelectedListener(sl);
        spnLon.setOnItemSelectedListener(sl);
        spnElev.setOnItemSelectedListener(sl);
        spnRMSEr.setOnItemSelectedListener(sl);
        spnFwdAz.setOnItemSelectedListener(sl);
        spnBkAz.setOnItemSelectedListener(sl);
        spnSlpDist.setOnItemSelectedListener(sl);
        spnSlpDistType.setOnItemSelectedListener(sl);
        spnSlpAng.setOnItemSelectedListener(sl);
        spnSlpAngType.setOnItemSelectedListener(sl);
        spnParentCN.setOnItemSelectedListener(sl);
        //endregion

        if (_Columns != null && _Columns.length > 2) {
            setupSpinners();
        }


        swtAdvanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                advImport = isChecked;

                int visibility = advImport ? View.VISIBLE : View.GONE;

                rowCN.setVisibility(visibility);
                rowOp.setVisibility(visibility);
                //rowIndex.setVisibility(visibility);
                //rowPID.setVisibility(visibility);
                //rowTime.setVisibility(visibility);
                //rowPoly.setVisibility(visibility);
                //rowGroup.setVisibility(visibility);
                //rowCmt.setVisibility(visibility);
                //rowMeta.setVisibility(visibility);
                //rowBnd.setVisibility(visibility);
                //rowX.setVisibility(visibility);
                //rowY.setVisibility(visibility);
                //rowZ.setVisibility(visibility);
                //rowAcc.setVisibility(visibility);
                rowManAcc.setVisibility(visibility);
                rowLat.setVisibility(visibility);
                rowLon.setVisibility(visibility);
                rowElev.setVisibility(visibility);
                rowRMSEr.setVisibility(visibility);
                rowFwdAz.setVisibility(visibility);
                rowBkAz.setVisibility(visibility);
                rowSlpDist.setVisibility(visibility);
                rowSlpDistType.setVisibility(visibility);
                rowSlpAng.setVisibility(visibility);
                rowSlpAngType.setVisibility(visibility);
                rowParentCN.setVisibility(visibility);

                divCN.setVisibility(visibility);
                divOp.setVisibility(visibility);
                //divIndex.setVisibility(visibility);
                //divPID.setVisibility(visibility);
                //divTime.setVisibility(visibility);
                //divPoly.setVisibility(visibility);
                //divGroup.setVisibility(visibility);
                //divCmt.setVisibility(visibility);
                //divMeta.setVisibility(visibility);
                //divBnd.setVisibility(visibility);
                //divX.setVisibility(visibility);
                //divY.setVisibility(visibility);
                //divZ.setVisibility(visibility);
                //divAcc.setVisibility(visibility);
                divManAcc.setVisibility(visibility);
                divLat.setVisibility(visibility);
                divLon.setVisibility(visibility);
                divElev.setVisibility(visibility);
                divRMSEr.setVisibility(visibility);
                divFwdAz.setVisibility(visibility);
                divBkAz.setVisibility(visibility);
                divSlpDist.setVisibility(visibility);
                divSlpDistType.setVisibility(visibility);
                divSlpAng.setVisibility(visibility);
                divSlpAngType.setVisibility(visibility);
                divParentCN.setVisibility(visibility);

                readyToImport(validate());
            }
        });


        AndroidUtils.UI.hideKeyboardOnTouch(view.findViewById(R.id.importFragSv),
                (EditText)((Activity) view.getContext()).findViewById(R.id.importTxtFile),
                true);

        return view;
    }


    private void setupSpinners() {
        if (_Columns != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.simple_large_spinner_item, _Columns);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spnCN.setAdapter(adapter);
            spnOp.setAdapter(adapter);
            spnIndex.setAdapter(adapter);
            spnPID.setAdapter(adapter);
            spnTime.setAdapter(adapter);
            spnPoly.setAdapter(adapter);
            spnGroup.setAdapter(adapter);
            spnCmt.setAdapter(adapter);
            //spnMeta.setAdapter(adapter);
            spnBnd.setAdapter(adapter);
            spnX.setAdapter(adapter);
            spnY.setAdapter(adapter);
            spnZ.setAdapter(adapter);
            //spnAcc.setAdapter(adapter);
            spnManAcc.setAdapter(adapter);
            spnLat.setAdapter(adapter);
            spnLon.setAdapter(adapter);
            spnElev.setAdapter(adapter);
            spnRMSEr.setAdapter(adapter);
            spnFwdAz.setAdapter(adapter);
            spnBkAz.setAdapter(adapter);
            spnSlpDist.setAdapter(adapter);
            spnSlpDistType.setAdapter(adapter);
            spnSlpAng.setAdapter(adapter);
            spnSlpAngType.setAdapter(adapter);
            spnParentCN.setAdapter(adapter);


            for (int i = 1; i < _Columns.length; i++) {
                String temp = _Columns[i].toLowerCase();

                switch (Import.TextFieldType.parse(temp)) {
                    case CN:
                        spnCN.setSelection(i);
                        break;
                    case OPTYPE:
                        spnOp.setSelection(i);
                        break;
                    case INDEX:
                        spnIndex.setSelection(i);
                        break;
                    case PID:
                        spnPID.setSelection(i);
                        break;
                    case TIME:
                        spnTime.setSelection(i);
                        break;
                    case POLY_NAME:
                        spnPoly.setSelection(i);
                        break;
                    case GROUP_NAME:
                        spnGroup.setSelection(i);
                        break;
                    case COMMENT:
                        spnCmt.setSelection(i);
                        break;
                    //case META_CN:
                     //   spnMeta.setSelection(i);
                    //    break;
                    case ONBND:
                        spnBnd.setSelection(i);
                        break;
                    case UNADJX:
                        spnX.setSelection(i);
                        break;
                    case UNADJY:
                        spnY.setSelection(i);
                        break;
                    case UNADJZ:
                        spnZ.setSelection(i);
                        break;
                    //case ACCURACY:
                    //    spnAcc.setSelection(i);
                    //    break;
                    case MAN_ACC:
                        spnManAcc.setSelection(i);
                        break;
                    case RMSER:
                        spnRMSEr.setSelection(i);
                        break;
                    case LATITUDE:
                        spnLat.setSelection(i);
                        break;
                    case LONGITUDE:
                        spnLon.setSelection(i);
                        break;
                    case ELEVATION:
                        spnElev.setSelection(i);
                        break;
                    case FWD_AZ:
                        spnFwdAz.setSelection(i);
                        break;
                    case BK_AZ:
                        spnBkAz.setSelection(i);
                        break;
                    case SLOPE_DIST:
                        spnSlpDist.setSelection(i);
                        break;
                    case SLOPE_DIST_TYPE:
                        spnSlpDistType.setSelection(i);
                        break;
                    case SLOPE_ANG:
                        spnSlpAng.setSelection(i);
                        break;
                    case SLOPE_ANG_TYPE:
                        spnSlpAngType.setSelection(i);
                        break;
                    case PARENT_CN:
                        spnParentCN.setSelection(i);
                        break;
                }
            }
        }
    }


    private HashMap<Import.TextFieldType, Integer> getColumnsMap(boolean advImport) {
        HashMap<Import.TextFieldType, Integer> map = new HashMap<>();

        int temp;

        temp = spnCN.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.CN, temp);
        }

        temp = spnIndex.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.INDEX, temp);
        }

        temp = spnPID.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.PID, temp);
        }

        temp = spnTime.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.TIME, temp);
        }

        temp = spnPoly.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.POLY_NAME, temp);
        }

        temp = spnGroup.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.GROUP_NAME, temp);
        }

        temp = spnCmt.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.COMMENT, temp);
        }

        //temp = spnMeta.getSelectedItemPosition() - 1;
        //if (temp > -1) {
        //    map.put(Import.TextFieldType.META_CN, temp);
        //}

        temp = spnBnd.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.ONBND, temp);
        }

        temp = spnX.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.UNADJX, temp);
        }

        temp = spnY.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.UNADJY, temp);
        }

        temp = spnZ.getSelectedItemPosition() - 1;
        if (temp > -1) {
            map.put(Import.TextFieldType.UNADJZ, temp);
        }

        //temp = spnAcc.getSelectedItemPosition() - 1;
        //if (temp > -1) {
        //    map.put(Import.TextFieldType.ACCURACY, temp);
        //}

        if (advImport) {
            temp = spnOp.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.OPTYPE, temp);
            }

            temp = spnManAcc.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.MAN_ACC, temp);
            }

            temp = spnLat.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.LATITUDE, temp);
            }

            temp = spnLon.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.LONGITUDE, temp);
            }

            temp = spnElev.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.ELEVATION, temp);
            }

            temp = spnRMSEr.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.RMSER, temp);
            }

            temp = spnFwdAz.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.FWD_AZ, temp);
            }

            temp = spnBkAz.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.BK_AZ, temp);
            }

            temp = spnSlpDist.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.SLOPE_DIST, temp);
            }

            temp = spnSlpDistType.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.SLOPE_DIST_TYPE, temp);
            }

            temp = spnSlpAng.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.SLOPE_ANG, temp);
            }

            temp = spnSlpAngType.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.SLOPE_ANG_TYPE, temp);
            }

            temp = spnParentCN.getSelectedItemPosition() - 1;
            if (temp > -1) {
                map.put(Import.TextFieldType.PARENT_CN, temp);
            }
        }

        return map;
    }

    @Override
    protected void runImportTask(DataAccessLayer dal) {
        task = new TextImportTask();

        task.setListener(new Import.ImportTaskListener() {
            @Override
            public void onTaskFinish(Import.ImportResult result) {
                onTaskComplete(result.getCode());
                task = null;
            }
        });

        Map<Import.TextFieldType, Integer> columnMap = getColumnsMap(advImport);
        List<String> polygonNames = new ArrayList<>();

        TextImportTask.TextImportParams params = new TextImportTask.TextImportParams(_FileName, dal, columnMap, polygonNames, advImport);

        onTaskStart();
        task.execute(params);
    }

    @Override
    public boolean validate(boolean useMessage) {
        if (!validFile) {
            if (useMessage) {
                Toast.makeText(getContext(), "Invalid File", Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        if (spnX.getSelectedItemPosition() < 1) {
            onError("X", spnX, false, useMessage);
            return false;
        }

        if (spnY.getSelectedItemPosition() < 1) {
            onError("Y", spnY, false, useMessage);
            return false;
        }

        if (advImport) {
            if (spnOp.getSelectedItemPosition() < 1) {
                onError("Op Type", spnOp, true, useMessage);
                return false;
            }

            if (spnFwdAz.getSelectedItemPosition() < 1 && spnBkAz.getSelectedItemPosition() < 1) {
                onError("The Forward or Backward Azimuth", spnFwdAz, true, useMessage);
                return false;
            }

            if (spnSlpDist.getSelectedItemPosition() < 1) {
                onError("Slope Distance", spnSlpDist, true, useMessage);
                return false;
            }

            if (spnSlpDistType.getSelectedItemPosition() < 1) {
                onError("Slope Distance Type", spnSlpDistType, true, useMessage);
                return false;
            }

            if (spnSlpAng.getSelectedItemPosition() < 1) {
                onError("Slope Angle", spnSlpAng, true, useMessage);
                return false;
            }

            if (spnSlpAngType.getSelectedItemPosition() < 1) {
                onError("Slope Angle Type", spnSlpAngType, true, useMessage);
                return false;
            }

            if (spnParentCN.getSelectedItemPosition() < 1) {
                onError("Parent CN", spnParentCN, true, useMessage);
                return false;
            }
        }

        return true;
    }

    @Override
    public void updateFileName(String filename) {
        validFile = false;

        _FileName = filename;
        _Columns = new String[0];

        if (!StringEx.isEmpty(_FileName)) {
            File f = new File(_FileName);

            if (f.exists() && !f.isDirectory()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(f));

                    String line = reader.readLine();

                    if (!StringEx.isEmpty(line)) {
                        _Columns = String.format("No Field,%s", line).replaceAll("\"", "").split(",");

                        if (_Columns.length > 2) {
                            validFile = true;
                        }
                    }
                } catch (Exception e) {
                    //
                }
            }

            if (spnCN != null) {
                setupSpinners();
            }
        }
    }

    private void onError(String field, View view, boolean isAdv, boolean showToast) {
        view.requestFocus();

        if (showToast) {
            Toast.makeText(getContext(),
                    String.format("%s Field is required%s", field,
                            isAdv ? " for advanced import" : StringEx.Empty),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void cancel() {
        if (task != null) {
            task.cancel(false);
        }
    }
}
