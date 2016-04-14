package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.custom.BaseMapActivity;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;

public class MapActivity extends BaseMapActivity {
    private TextView tvLocX, tvLocY, tvLocZone, tvLocXType, tvLocYType, tvZoneLbl;
    private FloatingActionButton fabMyPos;

    private boolean locUtm, myPosBtn, dispLoc, created;

    private ImageView ivGps;

    //region Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        fabMyPos = (FloatingActionButton)findViewById(R.id.mapFabMyPos);

        tvLocX = (TextView)findViewById(R.id.mapTbTvLocX);
        tvLocXType = (TextView)findViewById(R.id.mapTbTvLocXType);
        tvLocY = (TextView)findViewById(R.id.mapTbTvLocY);
        tvLocYType = (TextView)findViewById(R.id.mapTbTvLocYType);
        tvLocZone = (TextView)findViewById(R.id.mapTbTvLocZone);
        tvZoneLbl = (TextView)findViewById(R.id.mapTbTvLocZoneLabel);

        ivGps = (ImageView)findViewById(R.id.mapIvGps);

        fabMyPos.setVisibility(myPosBtn && getLastPosition() != null ? View.VISIBLE : View.GONE);
        setDisplayLocInfoVisible();

        if (Global.Settings.DeviceSettings.isGpsConfigured()) {
            Global.getGpsBinder().startGps();
        }

        created = true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mapMenuWhereIs: {
                fabMyPos.hide();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);

        return super.onCreateOptionsMenu(menu);
    }

    //endregion

    @Override
    protected void getSettings() {
        super.getSettings();

        myPosBtn = Global.Settings.DeviceSettings.getMapMyPosBtns();
        dispLoc = Global.Settings.DeviceSettings.getMapDisplayGpsLocation();
        locUtm = Global.Settings.DeviceSettings.getMapUseUtmNav();


        if (created) {
            if (fabMyPos != null && getLastPosition() != null) {
                fabMyPos.setVisibility(myPosBtn ? View.VISIBLE : View.GONE);
                fabMyPos.hide();
            }

            setDisplayLocInfoVisible();
        }
    }

    private void setDisplayLocInfoVisible() {
        int vis = dispLoc ? View.VISIBLE : View.INVISIBLE;

        if (dispLoc) {
            if (locUtm) {
                tvLocZone.setVisibility(View.VISIBLE);
                tvZoneLbl.setVisibility(View.VISIBLE);
                tvLocZone.setText(String.format("%d", getZone()));

                tvLocXType.setText(R.string.str_utmx);
                tvLocYType.setText(R.string.str_utmy);
            } else {
                tvLocZone.setVisibility(View.GONE);
                tvZoneLbl.setVisibility(View.GONE);

                tvLocXType.setText(R.string.str_lat);
                tvLocYType.setText(R.string.str_lon);
            }

            setDisplayLocInfo();
        }

        tvLocX.setVisibility(vis);
        tvLocY.setVisibility(vis);
        tvLocXType.setVisibility(vis);
        tvLocYType.setVisibility(vis);
        ivGps.setVisibility(vis);
    }


    public void btnMyLocClick(View view) {
        //onMyLocationButtonClick();

        GeoPosition lastPosition = getLastPosition();

        if (lastPosition == null) {
            lastPosition = Global.getGpsBinder().getLastPosition();
        }

        if (lastPosition != null) {
            moveToLocation(lastPosition, Consts.LocationInfo.GoogleMaps.ZOOM_CLOSE, true);
        }
    }

    @Override
    public void onMapClick(Position position) {
        super.onMapClick(position);
        fabMyPos.show();
    }

    @Override
    public void onMarkerClick(IMultiMapFragment.MarkerData md) {
        super.onMarkerClick(md);
        fabMyPos.hide();
    }

    @Override
    public void nmeaBurstReceived(INmeaBurst nmeaBurst) {
        super.nmeaBurstReceived(nmeaBurst);

        if (getLastPosition() != null) {
            if (myPosBtn && fabMyPos != null && fabMyPos.getVisibility() != View.VISIBLE  && getLastPosition() != null) {
                fabMyPos.setVisibility(View.VISIBLE);
                fabMyPos.show();
            }
        }

        setDisplayLocInfo();
    }


    private void setDisplayLocInfo() {
        GeoPosition lastPosition = getLastPosition();

        if (dispLoc && lastPosition != null) {
            if (locUtm) {
                UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(
                        lastPosition.getLatitudeSignedDecimal(),
                        lastPosition.getLongitudeSignedDecimal(),
                        getZone());

                tvLocX.setText(String.format("%.3f", coords.getX()));
                tvLocY.setText(String.format("%.3f", coords.getY()));
            } else {
                tvLocX.setText(String.format("%.3f", lastPosition.getLatitudeSignedDecimal()));
                tvLocY.setText(String.format("%.3f", lastPosition.getLongitudeSignedDecimal()));
            }
        }
    }


    //endregion
}
