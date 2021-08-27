package com.usda.fmsc.twotrails.activities.contracts;


import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.usda.fmsc.utilities.MimeTypes;

public class CreateZipDocument extends ActivityResultContracts.CreateDocument {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull String input) {
        Intent intent = super.createIntent(context, input);
        intent.setType(MimeTypes.Application.ZIP);
        return intent;
    }
}
