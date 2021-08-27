package com.usda.fmsc.twotrails.activities.contracts;


import android.content.Context;
import android.content.Intent;
import android.provider.DocumentsContract;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

public class GetImages extends ActivityResultContracts.GetMultipleContents {
    @CallSuper
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull String input) {
        return super.createIntent(context, input)
                .setType("image/*")
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, input);
    }
}