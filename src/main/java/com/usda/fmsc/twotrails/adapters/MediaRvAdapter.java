package com.usda.fmsc.twotrails.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.SelectableAdapterEx;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtImage;

import java.util.List;

public class MediaRvAdapter extends SelectableAdapterEx<TtMedia, SelectableAdapterEx.SelectableViewHolderEx> {
    private final int PIC = 1;
    private final int VIDEO = 4;

    private int colorTrans;
    private int colorSelected;

    private int maxHeight;

    private LayoutInflater inflater;
    private BitmapManager bitmapManager;
    private MediaChangedListener listener;

    public MediaRvAdapter(Activity activity, List<TtMedia> mediaList, Listener<TtMedia> listener, int maxHeight, BitmapManager bitmapManager) {
        super(activity, mediaList);

        inflater = LayoutInflater.from(activity);

        colorTrans = AndroidUtils.UI.getColor(activity, android.R.color.transparent);
        colorSelected = AndroidUtils.UI.getColor(activity, R.color.primaryLight);

        setListener(listener);
        this.maxHeight = maxHeight;
        this.bitmapManager = bitmapManager;
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
    public MediaViewHolder onCreateViewHolderEx(ViewGroup parent, int viewType) {
        switch (viewType) {
            case PIC: {
                return new ImageViewHolder(inflater.inflate(R.layout.content_point_media_thumbnail, parent, false));
            }
            case VIDEO: {
                break;
            }
        }
        return null;
    }

    @Override
    public SelectableViewHolderEx onCreateHeaderViewHolder(ViewGroup parent) {
        return new SelectableViewHolderExHeaderFooter(inflater.inflate(R.layout.rv_header, parent, false));
    }

    @Override
    public SelectableViewHolderEx onCreateFooterViewHolder(ViewGroup parent) {
        return new SelectableViewHolderExHeaderFooter(inflater.inflate(R.layout.rv_media_footer, parent, false));
    }


    @Override
    public synchronized void add(TtMedia item, boolean notify) {
        super.add(item, notify);

        if (notify && listener != null) {
            listener.onNotifyDataSetChanged();
        }
    }

    @Override
    public synchronized void add(int index, TtMedia item, boolean notify) {
        super.add(index, item, notify);

        if (notify && listener != null) {
            listener.onNotifyDataSetChanged();
        }
    }

    @Override
    public synchronized void remove(TtMedia item, boolean notify) {
        super.remove(item, notify);

        if (notify && listener != null) {
            listener.onNotifyDataSetChanged();
        }
    }

    @Override
    public synchronized TtMedia remove(int index, boolean notify) {
        TtMedia media = super.remove(index, notify);

        if (notify && listener != null) {
            listener.onNotifyDataSetChanged();
        }

        return media;
    }

    @Override
    public synchronized void clear(boolean notify) {
        super.clear(notify);

        if (notify && listener != null) {
            listener.onNotifyDataSetChanged();
        }
    }


    public void setListener(MediaChangedListener listener) {
        this.listener = listener;
    }

    public interface MediaChangedListener {
        void onNotifyDataSetChanged();
    }

    private abstract class MediaViewHolder extends SelectableViewHolderEx {
        public MediaViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class ImageViewHolder extends MediaViewHolder {
        private ImageView ivImage;

        public ImageViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.img);

            ivImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select();
                }
            });
        }

        @Override
        protected void onViewSelected(boolean selected) {
            ivImage.setBackgroundColor(selected ? colorSelected : colorTrans);
        }

        @Override
        public void onBindView(TtMedia item) {
            TtImage pic = (TtImage)getItem();

            String key = pic.getCN() + "_thumb";

            Bitmap thumbnail;

            if (!bitmapManager.containKey(key)) {
                Bitmap img = bitmapManager.get(pic.getCN());

                int width = img.getWidth() * maxHeight / img.getHeight();
                thumbnail = AndroidUtils.UI.resizeBitmap(img, width, maxHeight);
                bitmapManager.put(key, pic.getFilePath(), thumbnail, new BitmapManager.ScaleOptions(maxHeight, BitmapManager.ScaleMode.Max));
            } else {
                thumbnail = bitmapManager.get(key);
            }

            ivImage.setImageBitmap(thumbnail);
            AndroidUtils.UI.setContentDescToast(ivImage, pic.getName());
        }
    }
}
