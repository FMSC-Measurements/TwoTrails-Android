package com.usda.fmsc.twotrails.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.BuildConfig;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.TtAdjusterCustomToolbarActivity;
import com.usda.fmsc.twotrails.adapters.RecentProjectAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.DataAccessUpgrader;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.fragments.main.MainDataFragment;
import com.usda.fmsc.twotrails.fragments.main.MainFileFragment;
import com.usda.fmsc.twotrails.fragments.main.MainToolsFragment;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.RecentProject;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends TtAdjusterCustomToolbarActivity {
    private View progressLayout;

    private MainFileFragment mFragFile;
    private MainDataFragment mFragData;
    private MainToolsFragment mFragTools;

    private ViewPager mViewPager;

    private boolean _fileOpen = false, exitOnAdjusted, askLocation, showedCrashed;

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
        mViewPager = findViewById(R.id.mainViewPager);
        if (mViewPager != null) {
            mViewPager.setAdapter(mTabsPagerAdapter);
        }

        //Setup Tabs
        TabLayout tabLayout = findViewById(R.id.mainTabs);
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(mViewPager);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAppInfo();

        if (!AndroidUtils.App.checkStoragePermission(MainActivity.this)) {
            AndroidUtils.App.requestPermission(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    Consts.Codes.Requests.CREATE_FILE, "TwoTrails needs storage permissions in order to Open and Create files.");
        }
        else if (!askLocation && !AndroidUtils.App.requestPermission(MainActivity.this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                Consts.Codes.Requests.LOCATION, "TwoTrails needs location permissions in order to create collect location information.")) {
            askLocation = true;
        }

        if (progressLayout != null) {
            progressLayout.setVisibility(View.GONE);
        }

        if (getTtAppCtx().areFoldersInitiated()) {
            final Intent intent = getIntent();
            final String action = intent.getAction();

            if (Intent.ACTION_VIEW.equals(action)){
                Uri uri = intent.getData();

                if (uri != null) {
                    openFile(uri);
                }
            }

            if (!getTtAppCtx().hasDAL() && getTtAppCtx().getDeviceSettings().getAutoOpenLastProject()) {
                ArrayList<RecentProject> recentProjects = getTtAppCtx().getProjectSettings().getRecentProjects();
                if (recentProjects.size() > 0) {
                    openFile(recentProjects.get(0).File);
                }
            }
        }


        Intent startIntent = getIntent();

        if (!showedCrashed && startIntent != null && startIntent.hasExtra(Consts.Codes.Data.CRASH)) {
            AndroidUtils.Device.isInternetAvailable(new AndroidUtils.Device.InternetAvailableCallback() {
                @Override
                public void onCheckInternet(final boolean internetAvailable) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (internetAvailable) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage("TwoTrails experienced a crash. Would you like to send an error report to the developer team to help prevent future crashes?")
                                        .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                TtUtils.SendCrashEmailToDev(MainActivity.this);
                                            }
                                        })
                                        .setNeutralButton("Don't Send", null)
                                        .show();
                                showedCrashed = true;
                            } else {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage("TwoTrails experienced a crash. You can send a crash log to the development team from inside the settings menu.")
                                        .setPositiveButton(R.string.str_ok, null)
                                        .show();
                                showedCrashed = true;
                            }
                        }
                    });
                }
            });
        }
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        //closeFile();
//        //finishAndRemoveTask();
//    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        inflateMenu(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.mainMenuSettings:
                startActivityForResult(new Intent(this, SettingsActivity.class), Consts.Codes.Activites.SETTINGS);
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
                                finishAndRemoveTask();
                            } else {
                                exitOnAdjusted = true;
                            }
                        }
                    })
                    .setNegativeButton(R.string.str_exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finishAndRemoveTask();
                        }
                    })
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finishAndRemoveTask() {
        closeFile();
        super.finishAndRemoveTask();
    }

    @Override
    protected void onAdjusterStopped(PolygonAdjuster.AdjustResult result) {
        super.onAdjusterStopped(result);

        if (exitOnAdjusted) {
            finishAndRemoveTask();
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
                    if (!getTtAppCtx().areFoldersInitiated())
                        getTtAppCtx().initFolders();

                    if (tmpFile != null)
                        createFile(tmpFile);

                    if (!askLocation && !AndroidUtils.App.requestPermission(MainActivity.this,
                            new String[] { Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            Consts.Codes.Requests.LOCATION, "TwoTrails needs location permissions in order to create collect location information.")) {
                        askLocation = true;
                    }
                    break;
                case Consts.Codes.Requests.OPEN_FILE:
                    if (!getTtAppCtx().areFoldersInitiated())
                        getTtAppCtx().initFolders();

                    if (tmpFile != null)
                        openFile(tmpFile);
                    break;
                case  Consts.Codes.Requests.LOCATION:
                    getTtAppCtx().startGpsService();
                    break;
            }
        } else {
            if (requestCode == Consts.Codes.Requests.LOCATION) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("TwoTrails requires Location Services in order to work.")
                        .setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AndroidUtils.App.requestLocationPermission(MainActivity.this, Consts.Codes.Requests.LOCATION);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finishAndRemoveTask();
                            }
                        })
                        .show();
            } else if (requestCode == Consts.Codes.Requests.CREATE_FILE) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("TwoTrails requires Storage Permission in order to work.")
                        .setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AndroidUtils.App.requestStoragePermission(MainActivity.this, Consts.Codes.Requests.STORAGE);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finishAndRemoveTask();
                            }
                        })
                        .show();
            }
        }
    }


    //endregion


    //region Tabs
    public class TabsPagerAdapter extends FragmentPagerAdapter {

        private TabsPagerAdapter(FragmentManager fm) {
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
                if(data != null && data.getData() != null) {
                    openFile(data.getData());
                }
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
            case Consts.Codes.Activites.SEND_EMAIL_TO_DEV: {
                Toast.makeText(MainActivity.this, "Thank You for your feedback.", Toast.LENGTH_LONG).show();
                break;
            }
        }

        if (resultCode == Consts.Codes.Results.NO_DAL) {
            if (getTtAppCtx().getReport() != null) {
                getTtAppCtx().getReport().writeError("DAL not found", "requestCode:" + requestCode);
            } else {
                Log.e(Consts.LOG_TAG, "DAL not found");
            }
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
                    getTtAppCtx().setDAL(new DataAccessLayer(filePath, getTtAppCtx()));

                    if (getTtAppCtx().getDAL().getVersion().toIntVersion() < TwoTrailsSchema.SchemaVersion.toIntVersion()) {
                        switch (DataAccessUpgrader.UpgradeDAL(getTtAppCtx().getDAL())) {
                            case Successful:
                                openFile(filePath); break;
                            case Failed:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Upgrade Failed. See Log File for details.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case VersionUnsupported: {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "This file needs an Upgrade that needs to be done on the PC", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            }
                        }
                    } else {
                        if (getTtAppCtx().getDAL().needsAdjusting())
                            PolygonAdjuster.adjust(getTtAppCtx().getDAL());

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
                getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:openFile");

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
            getTtAppCtx().setDAL(new DataAccessLayer(filePath, getTtAppCtx()));

            getTtAppCtx().getProjectSettings().initProjectSettings(getTtAppCtx().getDAL());

            startActivityForResult(new Intent(this, ProjectActivity.class), UPDATE_INFO_AND_GOTO_DATA_TAB);

        } catch (Exception e) {
            getTtAppCtx().getReport().writeError("MainActivity:CreateFile", e.getMessage(), e.getStackTrace());
            e.printStackTrace();
            toastMessage = "Project creation failed";
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
    }

    private void closeFile() {
        if (getTtAppCtx().hasDAL()) {
            getTtAppCtx().getDAL().close();
            getTtAppCtx().setDAL(null);
        }

        if (getTtAppCtx().hasMAL()) {
            getTtAppCtx().getMAL().close();
            getTtAppCtx().setMAL(null);
        }
    }

    private void updateAppInfo() {
//        if (viewCreated) {
//            boolean updatePagerAdapter = false;
//            if (!mFragFile.isViewCreated()) {
//                mFragFile = MainFileFragment.newInstance();
//                updatePagerAdapter = true;
//            }
//
//            if (!mFragData.isViewCreated()) {
//                mFragData = MainDataFragment.newInstance();
//                mTabsPagerAdapter.notifyDataSetChanged();
//                updatePagerAdapter = true;
//            }
//
//            if (!mFragTools.isViewCreated()) {
//                mFragTools = MainToolsFragment.newInstance();
//                mTabsPagerAdapter.notifyDataSetChanged();
//                updatePagerAdapter = true;
//            }
//
//            if (updatePagerAdapter) {
//                mTabsPagerAdapter.notifyDataSetChanged();
//
//                if (mViewPager != null) {
//                    mViewPager.setAdapter(mTabsPagerAdapter);
//                }
//            }
//        }

        boolean enable = false;
        if(getTtAppCtx().hasDAL()) {
            getTtAppCtx().getProjectSettings().updateRecentProjects(
                    new RecentProject(getTtAppCtx().getDAL().getProjectID(), getTtAppCtx().getDAL().getFilePath()));
            
            setTitle("TwoTrails - " + getTtAppCtx().getDAL().getProjectID());
            enable = true;

            mFragFile.updateInfo(getTtAppCtx().getDAL());
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
        if (getTtAppCtx().getDAL().duplicate(fileName)) {
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
                    String filePath = TtUtils.getTtFilePath(value);

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
                    PolygonAdjuster.adjust(getTtAppCtx().getDAL(), true, MainActivity.this);
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
        //AndroidUtils.App.openFileIntent(this, "file/*",
        //        new String[] { "file/*.ttx", "ttx", "*ttx", "*.ttx", "*/*", "*"},
        //        Consts.Codes.Dialogs.REQUEST_FILE);
        //AndroidUtils.App.openFileIntent(this, MimeTypeMap.getSingleton().getExtensionFromMimeType("ttx"), Consts.Codes.Dialogs.REQUEST_FILE);
        //AndroidUtils.App.openFileIntent(this, Consts.FileExtensions.TWO_TRAILS, Consts.Codes.Dialogs.REQUEST_FILE)
        AndroidUtils.App.openFileIntent(this, "*/*", Consts.Codes.Dialogs.REQUEST_FILE);
    }

    public void btnOpenRecClick(View view) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                MainActivity.this);

        builderSingle.setTitle("Recently Opened");

        ArrayList<RecentProject> recentProjects = getTtAppCtx().getProjectSettings().getRecentProjects();

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

            String filepath = getTtAppCtx().getDAL().getFilePath();

            String dupFile = String.format("%s_bk%s", FileUtils.getFilePathWoExt(filepath), Consts.FILE_EXTENSION);
            int inc = 2;

            while (true) {
                if (FileUtils.fileExists(dupFile)) {
                    dupFile = StringEx.format("%s_bk%d%s", FileUtils.getFilePathWoExt(filepath), inc, Consts.FILE_EXTENSION);
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

                        getTtAppCtx().getDAL().clean();

                        pd.cancel();
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);
    }
    //endregion

    //region Data
    public void btnPointsClick(View view) {
        if (getTtAppCtx().getDAL().hasPolygons()) {
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
        if (getTtAppCtx().getDAL().getItemCount(TwoTrailsSchema.PointSchema.TableName) > 0) {
            startActivityForResult(new Intent(this, TableViewActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Points in Project", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    //region Tools
    public void btnMapClick(View view) {
        if (AndroidUtils.App.requestNetworkPermission(this, Consts.Codes.Requests.INTERNET)) {
            if (getTtAppCtx().getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                startActivity(new Intent(this, MapActivity.class));
            }
        }
    }

    public void btnGoogleEarthClick(View view) {
        final String gEarth = "com.google.earth";

        if (AndroidUtils.App.isPackageInstalled(MainActivity.this, gEarth)) {
            if (getTtAppCtx().getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                progressLayout.setVisibility(View.VISIBLE);

                String kmlPath = Export.kml(getTtAppCtx().getDAL(), TtUtils.getTtFileDir());

                progressLayout.setVisibility(View.GONE);

                if (!StringEx.isEmpty(kmlPath)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(AndroidUtils.Files.getUri(MainActivity.this,BuildConfig.APPLICATION_ID, kmlPath), "application/vnd.google-earth.kml+xml");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(MainActivity.this, "Error opening Google Earth", Toast.LENGTH_LONG).show();
                    }
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
        if (getTtAppCtx().getDAL().hasPolygons()) {
            if (getTtAppCtx().getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                startActivity(new Intent(this, HaidActivity.class));
            }
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnExportClick(View view) {
        if (getTtAppCtx().getDAL().hasPolygons()) {
            if (getTtAppCtx().getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                startActivity(new Intent(this, ExportActivity.class));
            }
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnPlotGridClick(View view) {
        if(getTtAppCtx().getDAL().hasPolygons()) {
            startActivityForResult(new Intent(this, PlotGridActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnMultiEdit(View view) {
        /*
        if(getTtAppCtx().getDAL().hasPolygons()) {
            startActivityForResult(new Intent(this, MultiEditActivity.class), UPDATE_INFO);
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
        */
    }

    public void btnGpsLoggerClick(View view) {
        startActivity(new Intent(this, GpsLoggerActivity.class));
    }

    public void btnGpsStatusClick(View view) {
        startActivity(new Intent(this, GpsStatusActivity.class));
    }

    public void btnMapManagerClick(View view) {
        startActivity(new Intent(this, MapManagerActivity.class));
    }

    public void btnTest(View view) {

        throw  new RuntimeException("Crash on purpose");
        //startActivity(new Intent(this, TestActivity.class));
    }
    //endregion
    //endregion
}
