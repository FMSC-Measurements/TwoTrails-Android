package com.usda.fmsc.twotrails.fragments.imprt;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.utilities.Import;

public abstract class BaseImportFragment extends Fragment {
    private Listener listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readyToImport(validate());
    }

    public final void importFile(DataAccessLayer dal) {
        if (validate(true)) {
            runImportTask(dal);
        }
    }

    protected abstract void runImportTask(DataAccessLayer dal);

    public abstract void cancel();

    public final boolean validate() {
        return validate(false);
    }

    public abstract boolean validate(boolean useMessage);

    public abstract void updateFileName(String filename);


    public void setListener(Listener listener) {
        this.listener = listener;
    }


    protected void onTaskComplete(Import.ImportResultCode code) {
        if (listener != null) {
            listener.onTaskComplete(code);
        }
    }

    protected void onTaskStart() {
        if (listener != null) {
            listener.onTaskStart();
        }
    }

    protected void readyToImport(boolean ready) {
        if (listener != null) {
            listener.onReadyToImport(ready);
        }
    }

    public interface Listener {
        void onTaskComplete(Import.ImportResultCode code);
        void onTaskStart();
        void onReadyToImport(boolean ready);
    }
}
