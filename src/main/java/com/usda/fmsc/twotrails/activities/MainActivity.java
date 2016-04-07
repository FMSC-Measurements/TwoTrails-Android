package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.custom.TtAjusterCustomToolbarActivity;
import com.usda.fmsc.twotrails.adapters.RecentProjectAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.fragments.main.MainDataFragment;
import com.usda.fmsc.twotrails.fragments.main.MainFileFragment;
import com.usda.fmsc.twotrails.fragments.main.MainToolsFragment;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.GpsPoint;
import com.usda.fmsc.twotrails.objects.RecentProject;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.File;
import java.util.ArrayList;

import com.usda.fmsc.utilities.StringEx;


public class MainActivity extends TtAjusterCustomToolbarActivity {
    private View progressLayout;

    private MainFileFragment mFragFile;
    private MainDataFragment mFragData;
    private MainToolsFragment mFragTools;

    private ViewPager mViewPager;

    private boolean _fileOpen = false;


    //region Main Activity Functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setUseExitWarning(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        TabsPagerAdapter mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        mFragFile = MainFileFragment.newInstance();
        mFragData = MainDataFragment.newInstance();
        mFragTools = MainToolsFragment.newInstance();

        progressLayout = findViewById(R.id.progressLayout);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.mainViewPager);
        mViewPager.setAdapter(mTabsPagerAdapter);

        //Setup Tabs
        TabLayout tabLayout = (TabLayout)findViewById(R.id.mainTabs);
        tabLayout.setupWithViewPager(mViewPager);


        //TwoTrails creation

        //setup values
        Global.init(getApplicationContext(), this);  //setup all the values using the application context

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if(Intent.ACTION_VIEW.equals(action)){
            Uri uri = intent.getData();
            openFile(uri);
        }

        if (Global.Settings.DeviceSettings.getAutoOpenLastProject()) {
            ArrayList<RecentProject> recentProjects = Global.Settings.ProjectSettings.getRecentProjects();
            if (recentProjects.size() > 0) {
                openFile(recentProjects.get(0).File);
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        //if(Values.Dal != null && !Values.Dal.isOpen())
        //    Values.Dal.open();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAppInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //if(Values.Dal != null)
        //    Values.Dal.close();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //CloseFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeFile();
        Global.destroy();
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        AndroidUtils.UI.addIconsToMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.mainMenuSettings:
                startActivityForResult(new Intent(this, PreferenceActivity.class), Consts.Activities.SETTINGS);
                break;
            case R.id.mainMenuGpsSettings:
                startActivity(new Intent(this, SettingsActivity.class).
                        putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE));
                break;
            case R.id.mainMenuRangeFinderSettings:
                startActivity(new Intent(this, SettingsActivity.class).
                        putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.LASER_SETTINGS_PAGE));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        /*
        * Check if polys are calculating
        * if calculating ask to wait
        *   if yes, return
        *   if no cancel calc and finish*/

        if (PolygonAdjuster.isProcessing()) {
            return;
        }

        super.onBackPressed();
    }

    //endregion


    //region Tabs
    public class TabsPagerAdapter extends FragmentPagerAdapter {

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                default:
                case 0:
                    return mFragFile;
                case 1:
                    return mFragData;
                case 2:
                    return mFragTools;
                //case 3:
                    //return mFragDev;
            }
        }

        @Override
        public int getCount() {
            return 3;
            //return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                default:
                case 0: return getString(R.string.main_tab_file);
                case 1: return getString(R.string.main_tab_data);
                case 2: return getString(R.string.main_tab_tools);
                case 3: return getString(R.string.main_tab_devices);
            }
        }
    }
    //endregion


    //region Actions
    public final int OPEN_TT_FILE = 101;
    //public final int CREATE_TT_FILE = 102;
    public final int UPDATE_INFO = 103;
    public final int UPDATE_INFO_AND_GOTO_DATA_TAB = 104;
    public final int GOTO_DATA_TAB = 105;

    public final int OPEN_SHP_FILE = 111;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case OPEN_TT_FILE: {
                if(data == null)
                    return;
                openFile(data.getData());
                break;
            }
            case UPDATE_INFO_AND_GOTO_DATA_TAB:
                gotoDataTab();
            case UPDATE_INFO: {
                updateAppInfo();
                break;
            }
            case GOTO_DATA_TAB: {
                gotoDataTab();
                break;
            }
            case Consts.Activities.SETTINGS: {
                updateAppInfo();
                break;
            }

            default:
                break;
        }
    }


    private void openFile(Uri uri) {
        openFile(uri.getPath());
    }

    private void openFile(String filePath) {
        if(PolygonAdjuster.isProcessing()) {
            Toast.makeText(this, "Currently Adjusting Polygons.", Toast.LENGTH_LONG).show();
        } else {

            if (_fileOpen) {
                closeFile();
            }

            try {
                if (!TtUtils.fileExists(filePath)) {
                    createFile(filePath);
                } else {
                    Global.DAL = new DataAccessLayer(filePath);

                    if (Global.DAL.getDalVersion().toIntVersion() < TwoTrailsSchema.SchemaVersion.toIntVersion()) {
                        //upgrade?

                        //if upgrade

                        //else
                        Global.DAL = null;
                        Toast.makeText(this, "Upgrade Canceled", Toast.LENGTH_SHORT).show();

                    } else {
                        gotoDataTab();
                        _fileOpen = true;
                    }

                    updateAppInfo();
                }
            } catch (Exception e) {
                Toast.makeText(this, "File Failed to Open", Toast.LENGTH_SHORT).show();
                _fileOpen = false;
            }
        }
    }

    private void createFile(String filePath) {
        closeFile();

        File file = new File(filePath);

        CharSequence toastMessage = "Project Created";

        if (file.exists() && !file.isDirectory()) {
            if (!file.delete()) {
                Toast.makeText(this, "Unable to overwrite current file.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            Global.DAL = new DataAccessLayer(filePath);

            Global.Settings.ProjectSettings.initProjectSettings(null);

            //for test values *****
            //testDAL(Global.DAL);
            //Global.DAL.setProjectID(new File(filePath).getName());
            //******


            startActivityForResult(new Intent(this, ProjectActivity.class), UPDATE_INFO_AND_GOTO_DATA_TAB);

        } catch (Exception e) {
            TtUtils.TtReport.writeError("MainActivity:CreateFile", e.getMessage(), e.getStackTrace());
            e.printStackTrace();
            toastMessage = "Project creation failed";
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
    }

    private void closeFile() {
        if(Global.DAL != null) {
            Global.DAL.close();
            Global.DAL = null;
        }
    }


    private void testDAL(DataAccessLayer dal){
        try{

            TtMetadata metaData = dal.getDefaultMetadata();
            TtGroup mainGroup = Global.getMainGroup();

            TtPolygon poly = new TtPolygon();
            poly.setName("testPoly");
            poly.setPointStartIndex(1010);
            poly.setIncrementBy(5);
            poly.setDescription("this is a test poly");
            poly.setAccuracy(5.0);

            dal.insertPolygon(poly);

            TtPoint point = new GpsPoint(), lastPoint;
            GpsPoint gps = (GpsPoint)point;

            gps.setCN(java.util.UUID.randomUUID().toString());
            gps.setManualAccuracy(2.0);
            gps.setUnAdjX(100);
            gps.setUnAdjY(100);
            gps.setUnAdjZ(0);
            gps.setPolyName(poly.getName());
            gps.setPolyCN(poly.getCN());
            gps.setComment("Point 100,100");
            gps.setGroupCN(mainGroup.getCN());
            gps.setGroupName(mainGroup.getName());
            gps.setIndex(0);
            gps.setMetadataCN(metaData.getCN());
            gps.setOnBnd(true);

            gps.setPID(PointNamer.nameFirstPoint(poly));
            dal.insertPoint(point);

            lastPoint = point;
            point = new GpsPoint();
            gps = (GpsPoint)point;

            gps.setCN(java.util.UUID.randomUUID().toString());
            gps.setManualAccuracy(2.0);
            gps.setUnAdjX(100);
            gps.setUnAdjY(200);
            gps.setUnAdjZ(10);
            gps.setPolyName(poly.getName());
            gps.setPolyCN(poly.getCN());
            gps.setComment("Point 100,200");
            gps.setGroupCN(mainGroup.getCN());
            gps.setGroupName(mainGroup.getName());
            gps.setIndex(1);
            gps.setMetadataCN(metaData.getCN());
            gps.setOnBnd(true);

            gps.setPID(PointNamer.namePoint(lastPoint, poly));
            dal.insertPoint(point);


            lastPoint = point;
            point = new GpsPoint();
            gps = (GpsPoint)point;

            gps.setCN(java.util.UUID.randomUUID().toString());
            gps.setUnAdjX(200);
            gps.setUnAdjY(200);
            gps.setUnAdjZ(20);
            gps.setPolyName(poly.getName());
            gps.setPolyCN(poly.getCN());
            gps.setComment("Point 200,200");
            gps.setGroupCN(mainGroup.getCN());
            gps.setGroupName(mainGroup.getName());
            gps.setIndex(2);
            gps.setMetadataCN(metaData.getCN());
            gps.setOnBnd(true);

            gps.setPID(PointNamer.namePoint(lastPoint, poly));

            dal.insertPoint(point);


            lastPoint = point;
            point = new GpsPoint();
            gps = (GpsPoint)point;

            gps.setCN(java.util.UUID.randomUUID().toString());
            gps.setManualAccuracy(2.0);
            gps.setUnAdjX(200);
            gps.setUnAdjY(100);
            gps.setUnAdjZ(2);
            gps.setPolyName(poly.getName());
            gps.setPolyCN(poly.getCN());
            gps.setComment("Point 200,100");
            gps.setGroupCN(mainGroup.getCN());
            gps.setGroupName(mainGroup.getName());
            gps.setIndex(3);
            gps.setMetadataCN(metaData.getCN());
            gps.setOnBnd(true);

            gps.setPID(PointNamer.namePoint(lastPoint, poly));

            dal.insertPoint(point);

            Thread.sleep(1000);
            poly = new TtPolygon();
            poly.setName("Poly 2");
            poly.setPointStartIndex(2010);
            poly.setIncrementBy(10);
            poly.setDescription("this is a test poly 2");
            poly.setAccuracy(1.0);

            dal.insertPolygon(poly);


            PolygonAdjuster.adjust(dal, this);




        } catch (Exception ex){
            ex.printStackTrace();
            Log.d("TT", ex.getMessage());
        }
    }



    private void updateAppInfo() {
        saveProjectSettings();

        boolean enable = false;
        if(Global.DAL != null) {
            Global.Settings.ProjectSettings.updateRecentProjects(
                    new RecentProject(Global.DAL.getProjectID(), Global.DAL.getFilePath()));
            
            setTitle("TwoTrails - " + Global.DAL.getProjectID());
            enable = true;

            mFragFile.updateInfo(Global.DAL);
        } else {
            setTitle(R.string.app_name);
        }

        mFragFile.enableButtons(enable);
        mFragData.enableButtons(enable);
        mFragTools.enableButtons(enable);
        //mFragDev.enableButtons(enable);
    }

    private void gotoDataTab() {
        mViewPager.setCurrentItem(1);
    }


    private void saveProjectSettings() {
        if(Global.DAL != null) {
            Global.DAL.setProjectID(Global.Settings.ProjectSettings.getProjectId());
            Global.DAL.setProjectDescription(Global.Settings.ProjectSettings.getDescription());
            Global.DAL.setProjectDistrict(Global.Settings.ProjectSettings.getDistrict());
            Global.DAL.setProjectForest(Global.Settings.ProjectSettings.getForest());
            Global.DAL.setProjectRegion(Global.Settings.ProjectSettings.getRegion());
        }
    }

    private void duplicateFile(final String fileName) {
        if (Global.DAL.duplicate(fileName)) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.parent), "File duplicated", Snackbar.LENGTH_LONG).setAction("Open", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFile(fileName);
                }
            })
            .setActionTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.primaryLighter));

            AndroidUtils.UI.setSnackbarTextColor(snackbar, Color.WHITE);

            snackbar.show();
        } else {
            Toast.makeText(this, "File failed to duplicate", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion


    //region Dialogs
    private void CreateFileDialog() {
        final InputDialog inputDialog = new InputDialog(this);
        inputDialog.setTitle("File Name");

        inputDialog.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = inputDialog.getText().trim();

                //filePath = TtUtils.getTtFilePath(inputDialog.getContext(), value);
                String filePath = TtUtils.getTtFilePath(value);

                if(TtUtils.fileExists(filePath)) {
                    OverwriteFileDialog(filePath);
                } else {
                    createFile(filePath);
                }
            }
        });

        inputDialog.setNegativeButton("Cancel", null);

        inputDialog.show();
    }

    private void OverwriteFileDialog(final String filePath) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("File Exists");
        alert.setMessage("Do you want to overwrite the file?");

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                createFile(filePath);
            }
        });

        alert.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CreateFileDialog();
                    }
                });
        alert.show();
    }
    //endregion


    //region Fragment Controls
    //region File
    public void btnNewClick(View view) {
        if(PolygonAdjuster.isProcessing()) {
            Toast.makeText(this, "Currently Adjusting Polygons.", Toast.LENGTH_LONG).show();
        } else {
            CreateFileDialog();
        }
    }

    public void btnOpenClick(View view) {
        /*
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);

        fileIntent.setType("file/*.tt");
        startActivityForResult(fileIntent, OPEN_TT_FILE);
        */

        AndroidUtils.App.openFileIntent(this, "file/*.tt", OPEN_TT_FILE);
    }

    public void btnOpenRecClick(View view) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                MainActivity.this);

        //builderSingle.setIcon(R.drawable.ic_launcher);

        builderSingle.setTitle("Recently Opened");

        ArrayList<RecentProject> recentProjects = Global.Settings.ProjectSettings.getRecentProjects();

        final RecentProjectAdapter adapter = new RecentProjectAdapter(MainActivity.this, recentProjects);

        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(adapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RecentProject project = adapter.getItem(which);

                        if(TtUtils.fileExists(project.File))
                            openFile(project.File);
                        else
                            Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_LONG).show();
                    }
                });
        builderSingle.show();
    }

    public void btnImportClick(View view) {

        startActivityForResult(new Intent(this, ImportActivity.class), UPDATE_INFO);

        //AndroidUtils.App.openFileIntent(this, "file/*.shp", OPEN_SHP_FILE);

        //Global.DAL.clean();
    }

    public void btnDupClick(View view) {
        if (PolygonAdjuster.isProcessing()) {
            Toast.makeText(this, "Currently Adjusting Polygons.", Toast.LENGTH_LONG).show();
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            String filepath = Global.DAL.getFilePath();

            String dupFile = String.format("%s_bk.tt", filepath.substring(0, filepath.length() - 3));
            int inc = 2;

            while (true) {
                if (TtUtils.fileExists(dupFile)) {
                    dupFile = String.format("%s_bk%d.tt", filepath.substring(0, filepath.length() - 3), inc);
                    inc++;
                    continue;
                }
                break;
            }

            final String filename = dupFile;

            dialog.setMessage(String.format("Duplicate File to: %s", dupFile.substring(dupFile.lastIndexOf("/") + 1)));

            dialog.setPositiveButton(R.string.main_btn_duplicate, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            duplicateFile(filename);
                        }
                    })
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        }
    }

    public void btnCleanDb(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("This operation will remove data that is not properly connected within the database. Points, Polygons, Groups and NMEA may be deleted. "+
                            "It is suggested that you backup your project before continuing.")
                .setPositiveButton("Clean", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ProgressDialog pd = new ProgressDialog(getBaseContext());
                        pd.setTitle("Cleaning..");
                        pd.setMessage("This operation may take a few minutes.");

                        pd.show();

                        Global.DAL.clean();

                        pd.cancel();
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);
    }
    //endregion

    //region Data
    public void btnPointsClick(View view) {
        if(Global.DAL.getItemCount(TwoTrailsSchema.PolygonSchema.TableName) > 0) {
            startActivityForResult(new Intent(this, PointsActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnPolygonsClick(View view) {
        startActivityForResult(new Intent(this, PolygonsActivity.class), UPDATE_INFO);
    }

    public void btnMetadataClick(View view) {
        startActivityForResult(new Intent(this, MetadataActivity.class), UPDATE_INFO);
    }

    public void btnProjectInfoClick(View view) {
        startActivityForResult(new Intent(this, ProjectActivity.class), UPDATE_INFO);
    }

    public void btnPointTableClick(View view) {
        if(Global.DAL.getItemCount(TwoTrailsSchema.PointSchema.TableName) > 0) {
            startActivityForResult(new Intent(this, TableEditActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Points in Project", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    //region Tools
    public void btnMapClick(View view) {
        startActivity(new Intent(this, MapActivity.class));
    }

    public void btnGoogleEarthClick(View view) {
        final String gEarth = "com.google.earth";
        final Activity activity = this;

        if (AndroidUtils.Device.isPackageInstalled(this, gEarth)) {

            progressLayout.setVisibility(View.VISIBLE);

            String kmlPath = Export.kml(Global.DAL, TtUtils.getTtFileDir());

            progressLayout.setVisibility(View.GONE);

            if (!StringEx.isEmpty(kmlPath)) {
                File KML = new File(kmlPath);
                Intent i = getPackageManager().getLaunchIntentForPackage(gEarth);
                i.setDataAndType(Uri.fromFile(KML), "application/vnd.google-earth.kml+xml");
                startActivity(i);
            }


            //Toast.makeText(this, "Earth is installed.", Toast.LENGTH_SHORT).show();

        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setMessage("Google Earth is not installed.")
            .setPositiveButton("Install", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AndroidUtils.Device.navigateAppStore(activity, gEarth);
                }
            })
            .setNeutralButton(R.string.str_cancel, null)
            .show();
        }
    }

    public void btnHAIDClick(View view) {
        if(Global.DAL.getItemCount(TwoTrailsSchema.PolygonSchema.TableName) > 0) {
            startActivity(new Intent(this, HaidActivity.class));
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnExportClick(View view) {
        if(Global.DAL.getItemCount(TwoTrailsSchema.PolygonSchema.TableName) > 0) {
            startActivity(new Intent(this, ExportActivity.class));
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnPlotGridClick(View view) {
        if(Global.DAL.getItemCount(TwoTrailsSchema.PolygonSchema.TableName) > 0) {
            startActivityForResult(new Intent(this, PlotGridActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnMultiEdit(View view) {
        /*
        if(Global.DAL.getItemCount(TwoTrailsSchema.PolygonSchema.TableName) > 0) {
            startActivityForResult(new Intent(this, MultiEditActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
        */
    }

    public void btnGpsLoggerClick(View view) {
        startActivity(new Intent(this, GpsLoggerActivity.class));
    }

    public void btnMapManagerClick(View view) {
        startActivity(new Intent(this, MapManagerActivity.class));
    }

    public void btnTest(View view) {
        startActivity(new Intent(this, TestActivity.class));
    }
    //endregion
    //endregion
}
