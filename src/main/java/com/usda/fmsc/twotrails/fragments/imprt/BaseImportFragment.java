package com.usda.fmsc.twotrails.fragments.imprt;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.utilities.Import;

public abstract class BaseImportFragment extends Fragment {
    private Listener listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readyToImport(validate());
    }

    public final void importFile(TwoTrailsApp app) {
        if (validate(true)) {
            runImportTask(app);
        }
    }

    protected abstract void runImportTask(TwoTrailsApp app);

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
