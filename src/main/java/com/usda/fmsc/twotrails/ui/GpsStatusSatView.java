package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.GnssType;
import com.usda.fmsc.geospatial.nmea41.Satellite;
import com.usda.fmsc.twotrails.R;

import java.util.HashMap;

public class GpsStatusSatView extends GpsStatusView {
    private Context context;

    private Paint paintVis, paintVisOld, paintUsed, paintUsedOld, paintWaas, paintWassOld, paintBackground, flagPaint, paintText;

    private HashMap<GnssType, Bitmap> flags;
    private HashMap<GnssType, Integer> flagsSize;

    private int lastFlagWidth = 0;


    public GpsStatusSatView(Context context) {
        this(context, null, 0);
    }

    public GpsStatusSatView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GpsStatusSatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.context = context;

        paintBackground = new Paint();
        paintBackground.setColor(Color.LTGRAY);
        paintBackground.setStyle(Paint.Style.FILL);
        paintBackground.setAntiAlias(true);

        paintVis = new Paint();
        paintVis.setColor(AndroidUtils.UI.getColor(context, R.color.grey_600));
        paintVis.setStyle(Paint.Style.FILL);
        paintVis.setAntiAlias(true);

        paintVisOld = new Paint();
        paintVisOld.setColor(AndroidUtils.UI.getColor(context, R.color.grey_600));
        paintVisOld.setAlpha(128);
        paintVisOld.setStyle(Paint.Style.FILL);
        paintVisOld.setAntiAlias(true);

        paintUsed = new Paint();
        paintUsed.setColor(AndroidUtils.UI.getColor(context, R.color.green_500));
        paintUsed.setStyle(Paint.Style.FILL);
        paintUsed.setAntiAlias(true);

        paintUsedOld = new Paint();
        paintUsedOld.setColor(AndroidUtils.UI.getColor(context, R.color.green_500));
        paintUsedOld.setAlpha(128);
        paintUsedOld.setStyle(Paint.Style.FILL);
        paintUsedOld.setAntiAlias(true);

        paintWaas = new Paint();
        paintWaas.setColor(AndroidUtils.UI.getColor(context, R.color.blue_500));
        paintWaas.setStyle(Paint.Style.FILL);
        paintWaas.setAntiAlias(true);

        paintWassOld = new Paint();
        paintWassOld.setColor(AndroidUtils.UI.getColor(context, R.color.blue_500));
        paintWassOld.setAlpha(128);
        paintWassOld.setStyle(Paint.Style.FILL);
        paintWassOld.setAntiAlias(true);

        paintText = new Paint(Color.BLACK);
        paintText.setFakeBoldText(true);
        paintText.setTextSize(22);
        paintText.setAntiAlias(true);

        flagPaint = new Paint();
        flagPaint.setAntiAlias(true);

        flags = new HashMap<>();
        flagsSize = new HashMap<>();

        for (GnssType type : GnssType.values()) {
            flags.put(type, null);
            flagsSize.put(type, null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int vHeight = getHeight();

        if (getValidSatelliteCount() > 0) {
            int width = getWidth() / getValidSatelliteCount();

            int flagWidth = width / 3;
            if (flagWidth > 46)
                flagWidth = 46;

            int offset = 0;
            Bitmap flag = null;
            GnssType lastGnss = null;

            int txtX = width / 2 - 7;
            int txtY = vHeight / 2 + 7;

            int count = 0;

            for (Satellite sat : getSatellites().values()) {
                if (getSatellitesValid().containsKey(sat.getNmeaID()) && getSatellitesValid().get(sat.getNmeaID())) {
                    float height = ((sat.getSRN() != null ? Math.abs(sat.getSRN()) : 0) / 99f) * vHeight;

                    Paint paint;

                    if (sat.isSBAS()) {
                        paint = getSatellitesVisibility().get(sat.getNmeaID()) ? paintWaas : paintWassOld;
                    } else {
                        paint = getSatellitesUsed().get(sat.getNmeaID()) ?
                                (getSatellitesVisibility().get(sat.getNmeaID()) ? paintUsed : paintUsedOld) :
                                (getSatellitesVisibility().get(sat.getNmeaID()) ? paintVis : paintVisOld);
                    }

                    canvas.drawRect(offset,
                            vHeight - height,
                            ++count == getValidSatelliteCount() ? getWidth() : (offset + width),
                            vHeight,
                            paint);

                    if (lastGnss == null || lastFlagWidth != flagWidth || lastGnss != sat.getGnssType() ) {
                        lastGnss = sat.getGnssType();
                        flag = getFlag(flagWidth, lastGnss);
                    }

                    if (flag != null) {
                        canvas.drawBitmap(flag, offset, 0, flagPaint);
                    }

                    canvas.drawText(String.valueOf(sat.getNmeaID()), offset + txtX, txtY, paintText);


                    offset += width;
                }
            }

            lastFlagWidth = flagWidth;
        }
    }


    private Bitmap getFlag(int flagWidth, GnssType gnssType) {
        if (flagsSize.get(gnssType) != null && flagsSize.get(gnssType) != flagWidth) {
            return flags.get(gnssType);
        } else {
            int res;
            switch (gnssType) {
                case GPS:
                case WAAS:
                    res = R.drawable.flag_usa;
                    break;
                case GLONASS:
                case SDCM:
                    res = R.drawable.flag_russia;
                    break;
                case GALILEO:
                case EGNOS:
                    res = R.drawable.flag_eu;
                    break;
                case BEIDOU:
                    res = R.drawable.flag_china;
                    break;
                case QZSS:
                case MSAS:
                    res = R.drawable.flag_japan;
                    break;
                case GAGAN:
                    res = R.drawable.flag_india;
                    break;
                case Unknown:
                case UnknownSBAS:
                default:
                    return null;
            }

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), res);

            if (flagWidth != 46) {
                bitmap = AndroidUtils.UI.scaleBitmap(bitmap, flagWidth, false);
            }

            flagsSize.put(gnssType, flagWidth);
            flags.put(gnssType, bitmap);

            return bitmap;
        }
    }
}
