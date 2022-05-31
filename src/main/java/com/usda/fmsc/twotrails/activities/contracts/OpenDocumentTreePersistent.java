package com.usda.fmsc.twotrails.activities.contracts;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OpenDocumentTreePersistent extends ActivityResultContract<Uri, Uri> {
    private Context _Context;


    @CallSuper
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @Nullable Uri input) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (input != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input);
        }

        _Context = context;

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        return intent;
    }

    @Nullable
    @Override
    public final SynchronousResult<Uri> getSynchronousResult(@NonNull Context context,
                                                             @Nullable Uri input) {
        return null;
    }

    @Nullable
    @Override
    public final Uri parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null || resultCode != Activity.RESULT_OK) return null;

        Uri dir = intent.getData();

        if (dir != null) {
            _Context.getContentResolver().takePersistableUriPermission(dir, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        return dir;
    }
}