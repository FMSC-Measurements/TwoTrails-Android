package com.usda.fmsc.twotrails.activities.contracts;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.usda.fmsc.utilities.Tuple;


public class CreateDocumentWType extends ActivityResultContract<Tuple<String, String>, Uri> {

    @CallSuper
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Tuple<String, String> input) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType(input.Item2)
                .putExtra(Intent.EXTRA_TITLE, input.Item1);
    }

    @Nullable
    @Override
    public final SynchronousResult<Uri> getSynchronousResult(@NonNull Context context,
                                                             @NonNull Tuple<String, String> input) {
        return null;
    }

    @Nullable
    @Override
    public final Uri parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null || resultCode != Activity.RESULT_OK) return null;
        return intent.getData();
    }
}