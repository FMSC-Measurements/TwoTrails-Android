package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.BuildConfig;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.base.TtAjusterCustomToolbarActivity;
import com.usda.fmsc.twotrails.adapters.RecentProjectAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.fragments.main.MainDataFragment;
import com.usda.fmsc.twotrails.fragments.main.MainFileFragment;
import com.usda.fmsc.twotrails.fragments.main.MainToolsFragment;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.RecentProject;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.File;
import java.util.ArrayList;

import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;


public class MainActivity extends TtAjusterCustomToolbarActivity {
    private View progressLayout;

    private MainFileFragment mFragFile;
    private MainDataFragment mFragData;
    private MainToolsFragment mFragTools;

    private ViewPager mViewPager;

    private boolean _fileOpen = false, exitOnAdjusted, askLocation;

    private String tmpFile;


    //region Main Activity Functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        setUseExitWarning(true);

        TabsPagerAdapter mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        mFragFile = MainFileFragment.newInstance();
        mFragData = MainDataFragment.newInstance();
        mFragTools = MainToolsFragment.newInstance();

        progressLayout = findViewById(R.id.progressLayout);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.mainViewPager);
        if (mViewPager != null) {
            mViewPager.setAdapter(mTabsPagerAdapter);
        }

        //Setup Tabs
        TabLayout tabLayout = (TabLayout)findViewById(R.id.mainTabs);
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(mViewPager);
        }

        //setup values
        new Thread(new Runnable() {
            @Override
            public void run() {
                Global.init(getApplicationContext(), MainActivity.this);

                final Intent intent = getIntent();
                final String action = intent.getAction();

                if (Intent.ACTION_VIEW.equals(action)){
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
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAppInfo();

        if (!askLocation && !AndroidUtils.App.requestLocationPermission(MainActivity.this, Consts.Codes.Requests.LOCATION)) {
            askLocation = true;
        }

        if (progressLayout != null) {
            progressLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeFile();
        Global.destroy();
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        inflateMenu(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.mainMenuSettings:
                startActivityForResult(new Intent(this, PreferenceActivity.class), Consts.Codes.Activites.SETTINGS);
                break;
            case R.id.mainMenuGpsSettings:
                startActivity(new Intent(this, SettingsActivity.class).
                        putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE));
                break;
            case R.id.mainMenuRangeFinderSettings:
                startActivity(new Intent(this, SettingsActivity.class).
                        putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.LASER_SETTINGS_PAGE));
                break;
            case R.id.mainMenuAbout:
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(String.format("App: %s\nData: %s", AndroidUtils.App.getVersionName(MainActivity.this), TwoTrailsSchema.SchemaVersion))
                        .show();
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

        if (isAboutToExit() && PolygonAdjuster.isProcessing()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("Polygons are currently adjusting. Would you like to wait for them to finish?")
                    .setPositiveButton("Wait", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!PolygonAdjuster.isProcessing()) {
                                finish();
                            } else {
                                exitOnAdjusted = true;
                            }
                        }
                    })
                    .setNegativeButton(R.string.str_exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onAdjusterStopped(PolygonAdjuster.AdjustResult result) {
        super.onAdjusterStopped(result);

        if (exitOnAdjusted) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case Consts.Codes.Requests.INTERNET:
                    startActivity(new Intent(this, MapActivity.class));
                    break;
                case Consts.Codes.Requests.CREATE_FILE:
                    if (tmpFile != null)
                        createFile(tmpFile);
                    break;
                case Consts.Codes.Requests.OPEN_FILE:
                    if (tmpFile != null)
                        openFile(tmpFile);
                    break;
            }
        } else {
            if (requestCode == Consts.Codes.Requests.LOCATION) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("TwoTrails requires Location Services in order to work.")
                        .setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .show();
            }
        }
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
    public final int UPDATE_INFO = 101;
    public final int UPDATE_INFO_AND_GOTO_DATA_TAB = 102;
    public final int GOTO_DATA_TAB = 103;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Consts.Codes.Dialogs.REQUEST_FILE: {
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
            case Consts.Codes.Activites.SETTINGS: {
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
        if (PolygonAdjuster.isProcessing()) {
            Toast.makeText(this, "Currently Adjusting Polygons.", Toast.LENGTH_LONG).show();
        } else {
            if (_fileOpen) {
                closeFile();
            }

            if (!AndroidUtils.App.requestStoragePermission(MainActivity.this, Consts.Codes.Requests.OPEN_FILE)) {
                tmpFile = filePath;
                return;
            } else {
                tmpFile = null;
            }

            try {
                if (!FileUtils.fileExists(filePath)) {
                    createFile(filePath);
                } else {
                    Global.setDAL(new DataAccessLayer(filePath));

                    if (Global.getDAL().getDalVersion().toIntVersion() < TwoTrailsSchema.SchemaVersion.toIntVersion()) {
                        //upgrade?

                        //if upgrade

                        //else
                        Global.setDAL(null);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Upgrade Canceled", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gotoDataTab();
                                _fileOpen = true;
                                updateAppInfo();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        _fileOpen = false;
                        Toast.makeText(MainActivity.this, "File Failed to Open", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void createFile(String filePath) {
        closeFile();

        if (!AndroidUtils.App.requestStoragePermission(MainActivity.this, Consts.Codes.Requests.CREATE_FILE)) {
            tmpFile = filePath;
            return;
        } else {
            tmpFile = null;
        }

        File file = new File(filePath);

        CharSequence toastMessage = "Project Created";

        if (file.exists() && !file.isDirectory()) {
            if (!file.delete()) {
                Toast.makeText(this, "Unable to overwrite current file.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            Global.setDAL(new DataAccessLayer(filePath));

            Global.Settings.ProjectSettings.initProjectSettings(Global.getDAL());

            startActivityForResult(new Intent(this, ProjectActivity.class), UPDATE_INFO_AND_GOTO_DATA_TAB);

        } catch (Exception e) {
            TtUtils.TtReport.writeError("MainActivity:CreateFile", e.getMessage(), e.getStackTrace());
            e.printStackTrace();
            toastMessage = "Project creation failed";
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
    }

    private void closeFile() {
        if (Global.getDAL() != null) {
            Global.getDAL().close();
            Global.setDAL(null);
        }
    }

    private void updateAppInfo() {
        boolean enable = false;
        if(Global.getDAL() != null) {
            Global.Settings.ProjectSettings.updateRecentProjects(
                    new RecentProject(Global.getDAL().getProjectID(), Global.getDAL().getFilePath()));
            
            setTitle("TwoTrails - " + Global.getDAL().getProjectID());
            enable = true;

            mFragFile.updateInfo(Global.getDAL());
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

    private void duplicateFile(final String fileName) {
        if (Global.getDAL().duplicate(fileName)) {
            View view = findViewById(R.id.parent);
            if (view != null) {
                Snackbar snackbar = Snackbar.make(view, "File duplicated", Snackbar.LENGTH_LONG).setAction("Open", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openFile(fileName);
                    }
                })
                .setActionTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.primaryLighter));

                AndroidUtils.UI.setSnackbarTextColor(snackbar, Color.WHITE);

                snackbar.show();
            } else {
                Toast.makeText(this, "File duplicated", Toast.LENGTH_SHORT).show();
            }
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

                if (value.length() > 0) {
                    String filePath = Global.getTtFilePath(value);

                    if (FileUtils.fileExists(filePath)) {
                        OverwriteFileDialog(filePath);
                    } else {
                        createFile(filePath);
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Invalid Filename", Toast.LENGTH_LONG).show();
                        }
                    });
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

    private void askToAdjust() {
        new AlertDialog.Builder(MainActivity.this)
        .setMessage("It looks like the data layer needs to be adjusted. Would you like to try and adjust it now?")
            .setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PolygonAdjuster.adjust(Global.getDAL(), MainActivity.this, true);
                }
            })
            .setNeutralButton(R.string.str_no, null)
            .show();
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
        AndroidUtils.App.openFileIntent(this, Consts.FileExtensions.TWO_TRAILS, Consts.Codes.Dialogs.REQUEST_FILE);
    }

    public void btnOpenRecClick(View view) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                MainActivity.this);

        builderSingle.setTitle("Recently Opened");

        ArrayList<RecentProject> recentProjects = Global.Settings.ProjectSettings.getRecentProjects();

        if (recentProjects.size() > 0) {
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

                            if(FileUtils.fileExists(project.File))
                                openFile(project.File);
                            else
                                Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_LONG).show();
                        }
                    });
            builderSingle.show();
        } else {
            Toast.makeText(MainActivity.this, "No Recent Projects.", Toast.LENGTH_LONG).show();
        }
    }

    public void btnImportClick(View view) {
        startActivityForResult(new Intent(this, ImportActivity.class), UPDATE_INFO);
    }

    public void btnDupClick(View view) {
        if (PolygonAdjuster.isProcessing()) {
            Toast.makeText(this, "Currently Adjusting Polygons.", Toast.LENGTH_LONG).show();
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            String filepath = Global.getDAL().getFilePath();

            String dupFile = String.format("%s_bk%s", filepath.substring(0, filepath.length() - 3), Consts.FILE_EXTENSION);
            int inc = 2;

            while (true) {
                if (FileUtils.fileExists(dupFile)) {
                    dupFile = String.format("%s_bk%d%s", filepath.substring(0, filepath.length() - 3), inc, Consts.FILE_EXTENSION);
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

                        Global.getDAL().clean();

                        pd.cancel();
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);
    }
    //endregion

    //region Data
    public void btnPointsClick(View view) {
        if (Global.getDAL().hasPolygons()) {
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
        if (Global.getDAL().getItemCount(TwoTrailsSchema.PointSchema.TableName) > 0) {
            startActivityForResult(new Intent(this, TableViewActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Points in Project", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    //region Tools
    public void btnMapClick(View view) {
        if (AndroidUtils.App.requestNetworkPermission(this, Consts.Codes.Requests.INTERNET)) {
            if (Global.getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                startActivity(new Intent(this, MapActivity.class));
            }
        }
    }

    public void btnGoogleEarthClick(View view) {
        final String gEarth = "com.google.earth";

        if (AndroidUtils.App.isPackageInstalled(MainActivity.this, gEarth)) {
            if (Global.getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                progressLayout.setVisibility(View.VISIBLE);

                String kmlPath = Export.kml(Global.getDAL(), Global.getTtFileDir());

                progressLayout.setVisibility(View.GONE);

                if (!StringEx.isEmpty(kmlPath)) {
                    File KML = new File(kmlPath);
                    Intent i = getPackageManager().getLaunchIntentForPackage(gEarth);

                    i.setDataAndType(FileProvider.getUriForFile(
                                MainActivity.this,
                                BuildConfig.APPLICATION_ID + ".provider",
                                KML),
                            "application/vnd.google-earth.kml+xml");
                    startActivity(i);
                }
            }
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setMessage("Google Earth is not installed.")
            .setPositiveButton("Install", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AndroidUtils.App.navigateAppStore(MainActivity.this, gEarth);
                }
            })
            .setNeutralButton(R.string.str_cancel, null)
            .show();
        }
    }

    public void btnHAIDClick(View view) {
        if (Global.getDAL().hasPolygons()) {
            if (Global.getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                startActivity(new Intent(this, HaidActivity.class));
            }
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnExportClick(View view) {
        if (Global.getDAL().hasPolygons()) {
            if (Global.getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                startActivity(new Intent(this, ExportActivity.class));
            }
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnPlotGridClick(View view) {
        if(Global.getDAL().hasPolygons()) {
            startActivityForResult(new Intent(this, PlotGridActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnMultiEdit(View view) {
        /*
        if(Global.getDAL().hasPolygons()) {
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
