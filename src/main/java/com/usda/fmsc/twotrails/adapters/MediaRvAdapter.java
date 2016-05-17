package com.usda.fmsc.twotrails.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.BitmapCacher;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtPicture;

import java.util.List;

public class MediaRvAdapter extends RecyclerViewEx.BaseAdapterEx {
    private final int PIC = 1;
    private final int VIDEO = 4;

    private int colorTrans;
    private int colorSelected;

    private int maxHeight;

    private LayoutInflater inflater;
    private Listener listener;

    private List<TtMedia> mediaList;

    private MediaViewHolder lastSelectedHolder;

    BitmapManager bitmapManager;

    Activity activity;

    public MediaRvAdapter(Activity activity, List<TtMedia> mediaList, Listener listener, int maxHeight, BitmapManager bitmapManager) {
        super(activity);

        this.activity = activity;

        inflater = LayoutInflater.from(activity);
        this.mediaList = mediaList;

        colorTrans = AndroidUtils.UI.getColor(activity, android.R.color.transparent);
        colorSelected = AndroidUtils.UI.getColor(activity, R.color.primaryLight);

        this.listener = listener;
        this.maxHeight = maxHeight;
        this.bitmapManager = bitmapManager;
    }

    private TtMedia getItem(int position) {
        return mediaList.get(position);
    }

    @Override
    public int getItemViewTypeEx(int position) {
        TtMedia m = getItem(position);

        switch (m.getMediaType()) {
            case Picture:
                return PIC;
            case Video:
                return VIDEO;
        }

        return INVALID_TYPE;
    }

    @Override
    public RecyclerViewEx.ViewHolderEx onCreateViewHolderEx(ViewGroup parent, int viewType) {
        switch (viewType) {
            case PIC: {
                return new ImageViewHolder(inflater.inflate(R.layout.point_media_image, parent, false));
            }
            case VIDEO: {
                //TODO implement video card
                break;
            }
        }
        return null;
    }

    @Override
    public RecyclerViewEx.ViewHolderEx onCreateFooterViewHolder(ViewGroup parent) {
        return new RecyclerViewEx.ViewHolderEx(inflater.inflate(R.layout.rv_media_footer, parent, false));
    }

    @Override
    public void onBindViewHolderEx(RecyclerViewEx.ViewHolderEx holder, int position) {
        TtMedia m = getItem(position);

        switch (m.getMediaType()) {
            case Picture:
                ImageViewHolder ivh = (ImageViewHolder) holder;
                ivh.bind((TtPicture) m);
                break;
            case Video:
                //TODO implement video bind
                break;
        }
    }

    @Override
    public int getItemCountEx() {
        return mediaList.size();
    }

    private void onMediaViewSelected(MediaViewHolder mvh) {
        if (listener != null) {
            listener.onMediaSelected(mvh.getMedia());
        }

        if (lastSelectedHolder != null)
            lastSelectedHolder.deselect();

        lastSelectedHolder = mvh;
    }


    public abstract class MediaViewHolder extends RecyclerViewEx.ViewHolderEx {
        public MediaViewHolder(View itemView) {
            super(itemView);
        }

        public abstract void select();
        public abstract void deselect();
        public abstract TtMedia getMedia();
    }

    public class ImageViewHolder extends MediaViewHolder {
        private ImageView ivImage;
        private TtPicture picture;

        public ImageViewHolder(View itemView) {
            super(itemView);

            ivImage = (ImageView) itemView.findViewById(R.id.img);
            ivImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            ivImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select();
                }
            });
        }

        @Override
        public void select() {
            ivImage.setBackgroundColor(colorSelected);
            onMediaViewSelected(ImageViewHolder.this);
        }

        @Override
        public void deselect() {
            ivImage.setBackgroundColor(colorTrans);
        }

        @Override
        public TtMedia getMedia() {
            return picture;
        }

        public void bind(TtPicture pic) {
            this.picture = pic;

            String key = pic.getFilePath() + "_tumb";

            Bitmap thumbnail;

            if (!bitmapManager.containKey(key)) {
                Bitmap img = bitmapManager.get(pic.getFilePath());

                int width = img.getWidth() * maxHeight / img.getHeight();
                thumbnail = AndroidUtils.UI.resizeBitmap(img, width, maxHeight);
                bitmapManager.put(key, pic.getFilePath(), thumbnail, new BitmapManager.ScaleOptions(maxHeight, BitmapManager.ScaleMode.Max));
            } else {
                thumbnail = bitmapManager.get(key);
            }

            ivImage.setImageBitmap(thumbnail);
        }
    }



    public interface Listener {
        void onMediaSelected(TtMedia media);
    }
}
