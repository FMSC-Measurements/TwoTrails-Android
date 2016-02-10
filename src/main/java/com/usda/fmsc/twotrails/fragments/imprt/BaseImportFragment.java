package com.usda.fmsc.twotrails.fragments.imprt;

import android.support.v4.app.Fragment;

import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.utilities.Import;

public abstract class BaseImportFragment extends Fragment {
    private Listener listener;

    public final void importFile(DataAccessLayer dal) {
        if (validate()) {
            runImportTask(dal);
        }
    }

    protected abstract void runImportTask(DataAccessLayer dal);

    public abstract void cancel();

    public abstract boolean validate();

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

    public interface Listener {
        void onTaskComplete(Import.ImportResultCode code);
        void onTaskStart();
    }
}
