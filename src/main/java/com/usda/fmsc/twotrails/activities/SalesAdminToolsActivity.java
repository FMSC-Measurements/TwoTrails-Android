package com.usda.fmsc.twotrails.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;

public class SalesAdminToolsActivity extends AcquireGpsMapActivity {

    private CardView cvGpsInfo;

    private MenuItem miHideGpsInfo;
    private boolean gpsInfoHidden;

    //region Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_admin_tools);

        setUseExitWarning(true);
        setUseLostConnectionWarning(true);

        SheetLayoutEx.enterFromBottomAnimation(this);

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {

        }

        addMapDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                if (isMapDrawerOpen(GravityCompat.END)) {
                    setMapDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.END);
                }
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(""); //TODO SET TITLE
            actionBar.setDisplayShowTitleEnabled(true);

            AndroidUtils.UI.createToastForToolbarTitle(SalesAdminToolsActivity.this, getToolbar());
        }


        cvGpsInfo = findViewById(R.id.take5CardGpsInfo);
    }

//    @Override
//    protected int getMapRightDrawerLayoutId() {
//        return R.layout.content_drawer_media;  //TODO add other tools
//    }

    @Override
    protected void getSettings() {
        super.getSettings();

//        options.Fix = getTtAppCtx().getDeviceSettings().getTake5FilterFix();
//        options.FixType = getTtAppCtx().getDeviceSettings().getTake5FilterFixType();
//        options.DopType = getTtAppCtx().getDeviceSettings().getTake5FilterDopType();
//        options.DopValue = getTtAppCtx().getDeviceSettings().getTake5FilterDopValue();
//        increment = getTtAppCtx().getDeviceSettings().getTake5Increment();
//        takeAmount = getTtAppCtx().getDeviceSettings().getTake5NmeaAmount();
//
//        useVib = getTtAppCtx().getDeviceSettings().getTake5VibrateOnCreate();
//        useRing = getTtAppCtx().getDeviceSettings().getTake5RingOnCreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_take5, menu);

//        miMoveToEnd = menu.findItem(R.id.take5MenuToBottom);
//        miMode = menu.findItem(R.id.take5MenuMode);
        miHideGpsInfo = menu.findItem(R.id.satMenuGpsInfoToggle);
//        miCenterPosition = menu.findItem(R.id.take5MenuCenterPositionToggle);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.satMenuGps: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.satMenuSatSettings: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.POINT_TAKE5_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.take5MenuGpsInfoToggle: {
                if (gpsInfoHidden) {
                    gpsInfoHidden = false;
                    cvGpsInfo.setVisibility(View.VISIBLE);
                    miHideGpsInfo.setTitle(R.string.menu_x_hide_gps_info);
                } else {
                    gpsInfoHidden = true;
                    cvGpsInfo.setVisibility(View.GONE);
                    miHideGpsInfo.setTitle(R.string.menu_x_show_gps_info);
                }
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Consts.Codes.Activites.SETTINGS: {
                getTtAppCtx().getGps().startGps();

                getSettings();
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }

    @Override
    public void onBackPressed() {
        if (isMapDrawerOpen(GravityCompat.END)) {
            setMapDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
            closeMapDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

    @Override
    protected void onPause() {
        SheetLayoutEx.exitToBottomAnimation(this);
        super.onPause();
    }

//    @Override
//    public void finish() {
//        if (validateSideShot()) {
//            if (_Points != null && _Points.size() > 0) {
//                if (!saved || updated) {
//                    savePoint(_CurrentPoint);
//                }
//
//                setResult(Consts.Codes.Results.POINT_CREATED, new Intent().putExtra(Consts.Codes.Data.NUMBER_OF_CREATED_POINTS, _Points.size()));
//            } else {
//                if (_Group != null) {
//                    getTtAppCtx().getDAL().deleteGroup(_Group.getCN());
//                }
//
//                setResult(RESULT_CANCELED);
//            }
//
//            super.finish();
//        }
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            //TODO take point
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (hasPosition()) {
                moveToLocation(getLastPosition(), Consts.Location.ZOOM_CLOSE, true);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onMapReady() {
        super.onMapReady();
        setMapGesturesEnabled(true);
    }
    //endregion



    //region Controls
    public void btnTakePointClick(View view) {
        if (isReceivingNmea()) {
//            if (validateSideShot()) {
//                setupTake5();
//            }
        } else {
            Toast.makeText(SalesAdminToolsActivity.this, "Currently not receiving NMEA data.", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

}
