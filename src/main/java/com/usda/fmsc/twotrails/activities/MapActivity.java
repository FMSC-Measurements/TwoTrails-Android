package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.base.BaseMapActivity;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.R;

import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.utilities.StringEx;

public class MapActivity extends BaseMapActivity {
    private TextView tvLocX, tvLocY, tvLocZone, tvLocXType, tvLocYType, tvZoneLbl;
    private FloatingActionButton fabMyPos;

    private boolean locUtm, myPosBtn, dispLoc, created;

    private ImageView ivGps;

    //region Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        fabMyPos = findViewById(R.id.mapFabMyPos);

        tvLocX = findViewById(R.id.mapTbTvLocX);
        tvLocXType = findViewById(R.id.mapTbTvLocXType);
        tvLocY = findViewById(R.id.mapTbTvLocY);
        tvLocYType = findViewById(R.id.mapTbTvLocYType);
        tvLocZone = findViewById(R.id.mapTbTvLocZone);
        tvZoneLbl = findViewById(R.id.mapTbTvLocZoneLabel);

        ivGps = findViewById(R.id.mapIvGps);

        //fabMyPos.setVisibility(myPosBtn && getLastPosition() != null ? View.VISIBLE : View.GONE);
        if (myPosBtn && getLastPosition() != null)
            fabMyPos.show();
        else
            fabMyPos.hide();

        setDisplayLocInfoVisible();

        if (getTtAppCtx().getDeviceSettings().isGpsConfigured() && shouldStartGps()) {
            getTtAppCtx().getGps().startGps();
        }

        created = true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mapMenuWhereIs) {
            fabMyPos.hide();
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

        myPosBtn = getTtAppCtx().getDeviceSettings().getMapMyPosBtns();
        dispLoc = getTtAppCtx().getDeviceSettings().getMapDisplayGpsLocation();
        locUtm = getTtAppCtx().getDeviceSettings().getMapUseUtmNav();


        if (created) {
            if (fabMyPos != null && getLastPosition() != null) {
                //fabMyPos.setVisibility(myPosBtn ? View.VISIBLE : View.GONE);
                if (myPosBtn)
                    fabMyPos.show();
                else
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
                tvLocZone.setText(StringEx.toString(getZone()));

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
        Position lastPosition = getLastPosition();

        if (lastPosition == null) {
            lastPosition = getTtAppCtx().getGps().getLastPosition();
        }

        if (lastPosition != null) {
            moveToLocation(lastPosition, Consts.Location.ZOOM_CLOSE, true);
        }
    }

    @Override
    public void onMapReady() {
        super.onMapReady();

        setMapPadding(0, (int)getResources().getDimension(R.dimen.toolbar_height), 0, 0);
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
    public void onNmeaBurstReceived(NmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);

        if (getLastPosition() != null) {
            if (myPosBtn && fabMyPos != null && getLastPosition() != null) {
                //fabMyPos.setVisibility(View.VISIBLE);
                fabMyPos.show();
            }
        }

        setDisplayLocInfo();
    }


    private void setDisplayLocInfo() {
        Position lastPosition = getLastPosition();

        if (dispLoc && lastPosition != null) {
            if (locUtm) {
                UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(
                        lastPosition.getLatitudeSignedDecimal(),
                        lastPosition.getLongitudeSignedDecimal(),
                        getZone());

                tvLocX.setText(StringEx.toString(coords.getX(), 3));
                tvLocY.setText(StringEx.toString(coords.getY(), 3));
            } else {
                tvLocX.setText(StringEx.toString(lastPosition.getLatitudeSignedDecimal(), 6));
                tvLocY.setText(StringEx.toString(lastPosition.getLongitudeSignedDecimal(), 6));
            }
        }
    }

    @Override
    public boolean shouldStartGps() {
        return true; //for position bar
    }
    //endregion
}
