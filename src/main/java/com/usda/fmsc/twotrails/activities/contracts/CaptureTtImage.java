package com.usda.fmsc.twotrails.activities.contracts;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.TtCameraActivity;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.utilities.Tuple;

public class CaptureTtImage extends ActivityResultContract<Tuple<String, Uri>, CaptureTtImage.TtImageResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, Tuple<String, Uri> input) {
        return new Intent(context, TtCameraActivity.class)
                .putExtra(Consts.Codes.Data.POINT_CN, input.Item1)
                .putExtra(Consts.Codes.Data.TTIMAGE_URI, input.Item2);
    }

    @Override
    public CaptureTtImage.TtImageResult parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode == Consts.Codes.Results.IMAGE_CAPTURED && intent != null) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                if (bundle.containsKey(Consts.Codes.Data.TTIMAGE)) {
                    TtImage image = bundle.getParcelable(Consts.Codes.Data.TTIMAGE);

                    if (image != null && bundle.containsKey(Consts.Codes.Data.TTIMAGE_URI)) {
                        return new TtImageResult(image, bundle.getParcelable(Consts.Codes.Data.TTIMAGE_URI));
                    }
                }
            }
        }

        return null;
    }

    public static class TtImageResult {
        private final Uri uri;
        private final TtImage image;

        public TtImageResult(@NonNull TtImage image, Uri uri) {
            this.image = image;
            this.uri = uri;
        }

        public Uri getUri() {
            return uri;
        }

        public TtImage getImage() {
            return image;
        }
    }
}