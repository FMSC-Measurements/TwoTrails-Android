package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.BuildConfig;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.TtProjectAdjusterActivity;
import com.usda.fmsc.twotrails.activities.contracts.CreateZipDocument;
import com.usda.fmsc.twotrails.activities.contracts.OpenDocumentTreePersistent;
import com.usda.fmsc.twotrails.adapters.RecentProjectAdapter;
import com.usda.fmsc.twotrails.data.DataAccessManager;
import com.usda.fmsc.twotrails.data.MediaAccessManager;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.data.UpgradeException;
import com.usda.fmsc.twotrails.fragments.main.MainDataFragment;
import com.usda.fmsc.twotrails.fragments.main.MainFileFragment;
import com.usda.fmsc.twotrails.fragments.main.MainToolsFragment;
import com.usda.fmsc.twotrails.logic.AdjustingException;
import com.usda.fmsc.twotrails.objects.TwoTrailsProject;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.MimeTypes;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends TtProjectAdjusterActivity {
    private static final boolean SYNC_OPTION_ENABLED = false;

    private View progressLayout;

    private MainFileFragment mFragFile;
    private MainDataFragment mFragData;
    private MainToolsFragment mFragTools;

    private ViewPager2 mViewPager;

    private boolean openingProject, exitOnAdjusted, showedCrashed;

    @Override
    public boolean requiresGpsService() { return false; }

    @Override
    public boolean requiresRFService() { return false; }



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

        TabsFragmentAdapter mTabsFragmentAdapter = new TabsFragmentAdapter(getSupportFragmentManager(), getLifecycle());

        mFragFile = MainFileFragment.newInstance();
        mFragData = MainDataFragment.newInstance();
        mFragTools = MainToolsFragment.newInstance();

        progressLayout = findViewById(R.id.progressLayout);

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.mainViewPager);
        if (mViewPager != null) {
            mViewPager.setAdapter(mTabsFragmentAdapter);
        }

        //Setup Tabs
        TabLayout tabLayout = findViewById(R.id.mainTabs);
        new TabLayoutMediator(tabLayout, mViewPager, (tab, position) -> {
            String tabName = "";

            try {
                switch (position) {
                    default:
                    case 0: tabName = getResources().getString(R.string.main_tab_file); break;
                    case 1: tabName = getResources().getString(R.string.main_tab_data); break;
                    case 2: tabName = getResources().getString(R.string.main_tab_tools); break;
                }
            } catch (Exception e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:TabLayoutMediator");
            }

            tab.setText(tabName);
        }).attach();

    }

    private boolean resuming = false;

    @Override
    protected void onResume() {
        super.onResume();

        resuming = true;

        if (progressLayout != null) {
            progressLayout.setVisibility(View.GONE);
        }

        updateAppInfo();

        Intent startIntent = getIntent();
        DeviceSettings ds = getTtAppCtx().getDeviceSettings();

        if (!showedCrashed && startIntent != null && startIntent.hasExtra(Consts.Codes.Data.CRASH)) {
            runOnUiThread(() -> {
                if (AndroidUtils.Device.isInternetAvailable(getTtAppCtx())) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("TwoTrails experienced a crash. Would you like to send an error report to the developer team to help prevent future crashes?")
                            .setPositiveButton("Send", (dialog, which) -> TtUtils.SendCrashEmailToDev(MainActivity.this))
                            .setNeutralButton("Don't Send", null)
                            .show();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("TwoTrails experienced a crash. You can send a crash log to the development team from inside the settings menu.")
                            .setPositiveButton(R.string.str_ok, null)
                            .show();
                }
                showedCrashed = true;

                openProjectOnResume();
            });
        } else {
            if (SYNC_OPTION_ENABLED) {
                if (ds.isExternalSyncEnabled()) {
                    if (getTtAppCtx().hasExternalDirAccess()) {
                        if (getTtAppCtx().getTwoTrailsExternalDir() == null) {
                            Toast.makeText(MainActivity.this, "Error creating external TwoTrails folder", Toast.LENGTH_LONG).show();
                        } else {
                            checkGpsOnResume();
                        }
                    } else {
                        requestExternalDirectoryAccess();
                    }
                } else if (!ds.isExternalSyncEnabledAsked()) {
                    //is not enabled and has not been asked before

                    DontAskAgainDialog dialog = new DontAskAgainDialog(
                            MainActivity.this,
                            DeviceSettings.EXTERNAL_SYNC_ENABLED_ASK,
                            DeviceSettings.EXTERNAL_SYNC_ENABLED,
                            ds.getPrefs());

                    dialog.setMessage("TwoTrails requires access to the external storage for easy importing, exporting and map use." +
                            "Without access all projects will need to be accessed manually. Would you like grant access?");

                    dialog.setPositiveButton("Yes", (dialogInterface, i, value) -> {
                        if (checkExternalDirectoryAccess()) {
                            checkGpsOnResume();
                        }
                    }, false); //dont set ExternalSyncEnabled to true yet

                    dialog.setNegativeButton("No", (dialogInterface, i, value) -> checkGpsOnResume(), false);

                    dialog.show();
                } else {
                    //has been asked but did not enable
                    checkGpsOnResume();
                }
            } else {
                checkGpsOnResume();
            }
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
        int itemId = item.getItemId();
        if (itemId == R.id.mainMenuSettings) {
            //startActivityForResult(new Intent(this, SettingsActivity.class), Consts.Codes.Activities.SETTINGS);
            updateAppInfoOnResult.launch(new Intent(this, SettingsActivity.class));
        } else if (itemId == R.id.mainMenuGpsSettings) {
            startActivity(new Intent(this, SettingsActivity.class).
                    putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE));
        } else if (itemId == R.id.mainMenuRangeFinderSettings) {
            startActivity(new Intent(this, SettingsActivity.class).
                    putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.LASER_SETTINGS_PAGE));
        } else if (itemId == R.id.mainMenuAbout) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(String.format("App: %s\nData: %s", TtUtils.getApplicationVersion(getTtAppCtx()), TwoTrailsSchema.SchemaVersion))
                    .show();
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
        if (isAboutToExit() && getTtAppCtx().isAdjusting()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("Polygons are currently adjusting. Would you like to wait for them to finish?")
                    .setPositiveButton("Wait", (dialog, which) -> {
                        if (!getTtAppCtx().isAdjusting()) {
                            finishAndRemoveTask();
                        } else {
                            exitOnAdjusted = true;
                        }
                    })
                    .setNegativeButton(R.string.str_exit, (dialog, which) -> finishAndRemoveTask())
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSettingsUpdated() {
        updateAppInfo();
    }

    @Override
    public void finishAndRemoveTask() {
        closeProject();
        super.finishAndRemoveTask();
    }

    @Override
    public void onAdjusterStopped(TwoTrailsApp.ProjectAdjusterResult result, AdjustingException.AdjustingError error) {
        super.onAdjusterStopped(result, error);

        if (exitOnAdjusted) {
            finishAndRemoveTask();
        } else if (openingProject) {
            onProjectOpened();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == Consts.Codes.Activities.SEND_EMAIL_TO_DEV)
            Toast.makeText(MainActivity.this, "Thank You for your feedback.", Toast.LENGTH_LONG).show();
        else if (resultCode == Consts.Codes.Results.NO_DAL) {
            if (getTtAppCtx().hasReport()) {
                getTtAppCtx().getReport().writeError("DAL not found", "requestCode:" + requestCode);
            } else {
                Log.e(Consts.LOG_TAG, "DAL not found");
            }
        }
    }

    //endregion


    //region Tabs
    public class TabsFragmentAdapter extends FragmentStateAdapter {


        public TabsFragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
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
        public int getItemCount() {
            return 3;
        }


    }
    //endregion


    //region Actions
    //public final int UPDATE_INFO = 101;
    //public final int UPDATE_INFO_AND_GOTO_DATA_TAB = 102;
    //public final int GOTO_DATA_TAB = 103;

    private final ActivityResultLauncher<Uri> requestExternalDirectoryAccess = registerForActivityResult(new OpenDocumentTreePersistent(), dir ->  {
        if (dir != null) {
            try {
                getTtAppCtx().setExternalRootDir(dir);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error creating external TwoTrails folder", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Cannot create external TwoTrails folder without permissions", Toast.LENGTH_LONG).show();
        }

        if (resuming && requestGpsAccess()) {
            openProjectOnResume();
        }
    });

    private void requestExternalDirectoryAccess() {
        Uri initDir = AndroidUtils.Files.getDocumentsUri(getTtAppCtx());
        requestExternalDirectoryAccess.launch(initDir);
    }

    private boolean checkExternalDirectoryAccess() {
        if (!getTtAppCtx().hasExternalDirAccess()) {

            requestExternalDirectoryAccess();

            return false; //wait for callback
        }

        return true;
    }


    private final ActivityResultLauncher<String[]> requestInternalLocationPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            onLocationRequestResult(TtUtils.Collections.areAllTrue(result.values())));

//    private final ActivityResultLauncher<String[]> requestBackgroundLocationPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
//            onBackgroundLocationRequestResult(TtUtils.Collections.areAllTrue(result.values())));

    private final ActivityResultLauncher<String> requestBackgroundLocationPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onBackgroundLocationRequestResult);


    private void onLocationRequestResult(boolean hasPermissions) {
        if (hasPermissions) {
            if (getTtAppCtx().isGpsServiceStarted()) {
                getTtAppCtx().getGps().startGps();
            } else {
                getTtAppCtx().startGpsService();
            }

            AndroidUtils.App.requestBackgroundLocationPermission(MainActivity.this,
                    requestBackgroundLocationPermissionOnResult,
                    getString(R.string.diag_back_loc));
        } else {
            Toast.makeText(MainActivity.this, "Cannot use GPS without permissions", Toast.LENGTH_LONG).show();
        }

        if (resuming) {
            openProjectOnResume();
        }
    }

    private void onBackgroundLocationRequestResult(boolean hasPermissions) {
        if (!(hasPermissions || getTtAppCtx().getDeviceSettings().getKeepScreenOn())) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.diag_keep_on_req)
                    .setPositiveButton(R.string.str_ok, (dialog, which) -> {
                        getTtAppCtx().getDeviceSettings().setKeepScreenOn(true);
                    })
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        }
    }

    private boolean requestGpsAccess() {
        return AndroidUtils.App.requestLocationPermission(MainActivity.this,
                requestInternalLocationPermissionOnResult,
                getString(R.string.diag_loc));
    }

    private void checkGpsOnResume() {
        if (resuming) {
            DeviceSettings ds = getTtAppCtx().getDeviceSettings();

            if (StringEx.isEmpty(ds.getGpsDeviceID()) && ds.isGpsAlwaysOn()) {
                if (requestGpsAccess()) {
                    openProjectOnResume();
                }//else wait for callback
            } else  {
                openProjectOnResume();
            }
        }
    }


    private void openProjectOnResume() {
        if (resuming) {
            resuming = false;

            final Intent startIntent = getIntent();
            final DeviceSettings ds = getTtAppCtx().getDeviceSettings();

            new Thread(() -> {
                final String action = startIntent.getAction();

                if (Intent.ACTION_VIEW.equals(action)){
                    openProjectFromIntent(startIntent);
                } else if (!getTtAppCtx().hasDAL() && ds.getAutoOpenLastProject()) {
                    openMostRecentProject();
                }


            }).start();
        }
    }

    private void openProjectFromIntent(Intent intent) {
        Uri uri = intent.getData();

        if (uri != null) {
            importProject(uri);
        }
    }

    private void openMostRecentProject() {
        ArrayList<TwoTrailsProject> twoTrailsProjects = getTtAppCtx().getProjectSettings().getRecentProjects();
        if (twoTrailsProjects.size() > 0) {
            TwoTrailsProject rp = twoTrailsProjects.get(0);
            openProject(rp.Name, rp.TTXFile);
        }
    }


    private void openProject(String projectName, String projectFileName) {
        if (continueIfNotProcessing()) {
            TwoTrailsApp app = getTtAppCtx();
            if (app.hasDAL()) {
                closeProject();
            }

            try {
                openingProject = true;
                if (projectFileName == null) {
                    projectFileName = TtUtils.projectToFileNameTTX(projectName);
                }

                DataAccessManager dam = DataAccessManager.openDAL(app, projectFileName, projectName);

                if (dam.dbHasErrors()) {
                    //TODO has errors
                    Toast.makeText(MainActivity.this, "Database has Errors", Toast.LENGTH_LONG).show();
                } else {
                    app.setDAM(dam);

                    if (dam.justCreated()) {
                        Toast.makeText(MainActivity.this, "Project Created", Toast.LENGTH_LONG).show();
                        updateAppInfoAndGotoDataTabOnResult.launch(new Intent(this, ProjectActivity.class));
                    } else {
                        if (app.getDAL().needsAdjusting()) {
                            Toast.makeText(MainActivity.this, "Adjusting polygons in project", Toast.LENGTH_LONG).show();
                            app.adjustProject(true);
                        } else {
                            if (dam.justUpgraded()) {
                                Toast.makeText(MainActivity.this, "Project Upgraded", Toast.LENGTH_LONG).show();
                            }

                            onProjectOpened();
                        }
                    }
                }
            } catch (UpgradeException ue) {
                app.getReport().writeError(ue.getMessage(), "MainActivity:openProject:upgradeFailed", ue.getStackTrace());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Upgrade Failed. See Log File for details.", Toast.LENGTH_SHORT).show());

                if (openingProject) {
                    closeProject();
                }

                openingProject = false;
            } catch (Exception e) {
                app.getReport().writeError(e.getMessage(), "MainActivity:openProject", e.getStackTrace());

                openingProject = false;

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Project Failed to Open", Toast.LENGTH_SHORT).show());

                if (openingProject) {
                    closeProject();
                }
            }
        }
    }

    private void onProjectOpened() {
        TwoTrailsApp app = getTtAppCtx();
        openingProject = false;

        runOnUiThread(() -> {
            gotoDataTab();
            updateAppInfo();
        });

        if (getTtAppCtx().hasMAL()) {
            try {
                MediaAccessManager mam = MediaAccessManager.openMAL(app,
                        app.getCurrentProject().TTXFile.replace(Consts.FILE_EXTENSION, Consts.MEDIA_PACKAGE_EXTENSION));

                if (mam.dbHasErrors()) {
                    //TODO has errors
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Media Database has Errors", Toast.LENGTH_LONG).show());
                }

                app.setMAM(mam);
            } catch (UpgradeException ue) {
                runOnUiThread(() -> app.getReport().writeError(ue.getMessage(), "MainActivity:onProjectOpened:upgradeFailed", ue.getStackTrace()));
            } catch (Exception e) {
                app.getReport().writeError(e.getMessage(), "MainActivity:onProjectOpened", e.getStackTrace());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to Open Media Package", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void createProject(String projectName) {
        createProject(projectName, false);
    }

    private void createProject(String projectName, boolean overwrite) {
        if (continueIfNotProcessing()) {
            closeProject();

            String projectFileName = TtUtils.projectToFileNameTTX(projectName);

            File projectFile = getDatabasePath(projectFileName);

            if (projectFile != null && projectFile.exists()) {
                if (overwrite) {
                    if (!projectFile.delete()) {
                        Toast.makeText(MainActivity.this, "Unable to delete project.", Toast.LENGTH_LONG).show();
                        return;
                    }
                } else {
                    Toast.makeText(MainActivity.this, String.format("Project %s already exists", projectName), Toast.LENGTH_LONG).show();
                    return;
                }
            }

            openProject(projectName, projectFileName);
        }
    }


    private void closeProject() {
        if (getTtAppCtx().hasDAL()) {
            getTtAppCtx().getDAL().close();
            getTtAppCtx().setDAM(null);
        }

        if (getTtAppCtx().hasMAL()) {
            getTtAppCtx().getMAL().close();
            getTtAppCtx().setMAM(null);
        }

        updateAppInfo();
    }


    private boolean continueIfNotProcessing() {
        if (getTtAppCtx().isAdjusting()) {
            Toast.makeText(this, "Currently Adjusting Polygons.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }


    private final ActivityResultLauncher<Intent> updateAppInfoOnResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> updateAppInfo());
    private void updateAppInfo() {
        boolean enable = false;
        if(getTtAppCtx().hasDAL()) {
            getTtAppCtx().getProjectSettings().updateRecentProjects(
                    new TwoTrailsProject(getTtAppCtx().getDAL().getProjectID(), getTtAppCtx().getDAL().getFileName()));
            
            setTitle("TwoTrails - " + getTtAppCtx().getDAL().getProjectID());
            enable = true;

            mFragFile.updateInfo();
        } else {
            setTitle(R.string.app_name);
        }

        mFragFile.enableButtons(enable);
        mFragData.enableButtons(enable);
        mFragTools.enableButtons(enable);
        //mFragDev.enableButtons(enable);
    }


    private final ActivityResultLauncher<Intent> gotoDataTabOnResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> gotoDataTab());
    private void gotoDataTab() {
        mViewPager.setCurrentItem(1);
    }

    private final ActivityResultLauncher<Intent> updateAppInfoAndGotoDataTabOnResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        updateAppInfo();
        gotoDataTab();
    });


    private final ActivityResultLauncher<String> importFileLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            importProject(uri);
        } else {
            Toast.makeText(MainActivity.this, "Error opening file for import", Toast.LENGTH_LONG).show();
        }
    });
    private void importProject(Uri filePath) {
        try {
            String fp = filePath.getPath();
            if (fp == null)
                throw new RuntimeException("Invalid Filepath");

            //TODO option for zip files (ttx & ttmpx & media dir)

            String fileName = FileUtils.getFileName(fp);

            if (DataAccessManager.localDALExists(getTtAppCtx(), fileName)) {
                overwriteLocalProjectByImportDialog(fileName, filePath);
            } else {
                DataAccessManager dam = DataAccessManager.importDAL(getTtAppCtx(), filePath);

                String projectName = dam.getDAL().getProjectID();
                dam.close();

                openProject(projectName, fileName);
            }
        } catch (IOException e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:importProject", e.getStackTrace());
            Toast.makeText(MainActivity.this, "Error Importing Project", Toast.LENGTH_LONG).show();
        }
    }


    private final ActivityResultLauncher<String> exportProjectLauncher = registerForActivityResult(new CreateZipDocument(),
            uri -> {
                if (uri != null) {
                    exportProject(uri);
                } else {
                    Toast.makeText(MainActivity.this, "Error selecting file for export", Toast.LENGTH_LONG).show();
                }
            });
    private void exportProject(Uri filePath) {
        try {
            File pcPkgFile = Export.exportProjectPackage(getTtAppCtx(), getTtAppCtx().getDAL(), getTtAppCtx().getMAL());

            DocumentFile df = DocumentFile.fromSingleUri(getTtAppCtx(), filePath);

            if (df != null) {
                DocumentFile extPcPkgFile = df.createFile(MimeTypes.Application.ZIP, pcPkgFile.getName());

                if (extPcPkgFile != null) {
                    AndroidUtils.Files.copyFile(getTtAppCtx(), Uri.fromFile(pcPkgFile), extPcPkgFile.getUri());
                } else {
                    throw new Exception("Unable to create file");
                }
            } else {
                throw new Exception("Unable to get file path from uri");
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:exportProject", e.getStackTrace());
            Toast.makeText(MainActivity.this, "Error Exporting Project", Toast.LENGTH_LONG).show();
        }
    }


//    private void duplicateFile(final String fileName) {
//        if (getTtAppCtx().getDAL().duplicate(fileName)) {
//            View view = findViewById(R.id.parent);
//            if (view != null) {
//                Snackbar snackbar = Snackbar.make(view, "File duplicated", Snackbar.LENGTH_LONG).setAction("Open", v -> openFile(fileName))
//                .setActionTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.primaryLighter));
//
//                AndroidUtils.UI.setSnackbarTextColor(snackbar, Color.WHITE);
//
//                snackbar.show();
//            } else {
//                Toast.makeText(this, "File duplicated", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Toast.makeText(this, "File failed to duplicate", Toast.LENGTH_SHORT).show();
//        }
//    }
    //endregion


    //region Dialogs
    private void createProjectDialog() {
        final InputDialog inputDialog = new InputDialog(MainActivity.this);
        inputDialog.setTitle("Project Name");

        inputDialog.setPositiveButton("Create", (dialog, whichButton) -> {
            String projectName = inputDialog.getText().trim();

            if (projectName.length() > 0) {
                if (DataAccessManager.localDALExists(getTtAppCtx(), TtUtils.projectToFileNameTTX(projectName))) {
                    overwriteLocalProjectByCreateDialog(projectName);
                } else {
                    createProject(projectName);
                }
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Invalid Project Name", Toast.LENGTH_LONG).show());
            }
        });

        inputDialog.setNegativeButton("Cancel", null);

        inputDialog.show();
    }

    private void overwriteLocalProjectByCreateDialog(final String projectName) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Project Exists");
        alert.setMessage(String.format("The current project '%s' already exists. Do you want to overwrite the project?", projectName));

        alert.setPositiveButton("Yes", (dialog, whichButton) -> createProject(projectName, true));

        alert.setNegativeButton("No",
                (dialog, which) -> createProjectDialog());
        alert.show();
    }

    private void overwriteLocalProjectByImportDialog(final String fileName, Uri file) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Project Exists");
        alert.setMessage(String.format("The current file '%s' already exists. Do you want to overwrite or rename the project?", fileName));

        alert.setPositiveButton("Overwrite", (dialog, which) -> {
            try {
                DataAccessManager dam = DataAccessManager.importDAL(getTtAppCtx(), file);

                String projectName = dam.getDAL().getProjectID();
                dam.close();

                openProject(projectName, fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        alert.setNegativeButton("Rename", (dialog, which) -> {
            final InputDialog inputDialog = new InputDialog(MainActivity.this);
            inputDialog.setTitle("New File Name");

            inputDialog.setPositiveButton("Rename", (d2, wb2) -> {
                try {
                    String newFileName = inputDialog.getText();

                    if (!newFileName.endsWith(Consts.FILE_EXTENSION)) {
                        newFileName += Consts.FILE_EXTENSION;
                    }

                    DataAccessManager dam = DataAccessManager.importDAL(getTtAppCtx(), file, newFileName);

                    String projectName = dam.getDAL().getProjectID();
                    dam.close();

                    openProject(projectName, fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            inputDialog.setNegativeButton("Cancel", null);

            inputDialog.show();
        });

        alert.setNeutralButton(R.string.str_cancel, null);
        alert.show();
    }


    private void askToAdjust() {
        new AlertDialog.Builder(MainActivity.this)
        .setMessage("It looks like the data layer needs to be adjusted. Would you like to try and adjust it now?")
            .setPositiveButton(R.string.str_yes, (dialog, which) -> getTtAppCtx().adjustProject(true))
            .setNeutralButton(R.string.str_no, null)
            .show();
    }
    //endregion


    //region Fragment Controls
    //region File
    public void btnCreateClick(View view) {
        if (continueIfNotProcessing()) {
            createProjectDialog();
        }
    }

    public void btnOpenClick(View view) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                MainActivity.this);

        builderSingle.setTitle("Available Projects");

        ArrayList<TwoTrailsProject> twoTrailsProjects = getTtAppCtx().getProjectSettings().getRecentProjects();

        if (twoTrailsProjects.size() > 0) {
            final RecentProjectAdapter adapter = new RecentProjectAdapter(MainActivity.this, twoTrailsProjects);

            builderSingle.setNegativeButton("Cancel",
                    (dialog, which) -> dialog.dismiss());

            builderSingle.setAdapter(adapter,
                    (dialog, which) -> {
                        TwoTrailsProject project = adapter.getItem(which);

                        if (DataAccessManager.localDALExists(getTtAppCtx(), project.TTXFile))
                            openProject(project.Name, project.TTXFile);
                        else
                            Toast.makeText(getApplicationContext(), "Project not found", Toast.LENGTH_LONG).show();
                    });
            builderSingle.show();
        } else {
            Toast.makeText(MainActivity.this, "No Projects.", Toast.LENGTH_LONG).show();
        }
    }

    public void btnImportProjectClick(View view) {
        if (continueIfNotProcessing()) {
            //TODO option for zip files (ttx & ttmpx & media dir)
            importFileLauncher.launch(Consts.FILE_MIME);
        }
    }

    public void btnExportProjectClick(View view) {
        if (continueIfNotProcessing()) {

            String filename = String.format("%s_%s.zip", TtUtils.projectToFileName(
                    getTtAppCtx().getDAL().getProjectID()),
                    new Date(getTtAppCtx().getDAM().getDBFile().lastModified()).toString()
            );

            exportProjectLauncher.launch(filename);
        }
    }

    public void btnImportDataClick(View view) {
        updateAppInfoOnResult.launch(new Intent(this, ImportActivity.class));
    }

    public void btnRemoveProject(View view) {
        //TODO remove project
        //ask to export first
    }


//    public void btnBackupClick(View view) {
        //ask for new file name, copy project and open, auto export if avail



//        if (!AndroidUtils.App.checkStoragePermission(MainActivity.this)) {
//            getFilePermissions();
//        } else {
//            if (PolygonAdjuster.isProcessing()) {
//                Toast.makeText(this, "Currently Adjusting Polygons.", Toast.LENGTH_LONG).show();
//            } else {
//                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//
//                String filepath = getTtAppCtx().getDAL().getFilePath();
//
//                String dupFile = String.format("%s_bk%s", FileUtils.getFilePathWoExt(filepath), Consts.FILE_EXTENSION);
//                int inc = 2;
//
//                while (true) {
//                    if (FileUtils.fileExists(dupFile, MainActivity.this)) {
//                        dupFile = String.format("%s_bk%d%s", FileUtils.getFilePathWoExt(filepath), inc, Consts.FILE_EXTENSION);
//                        inc++;
//                        continue;
//                    }
//                    break;
//                }
//
//                final String filename = dupFile;
//
//                dialog.setMessage(String.format("Duplicate File to: %s", dupFile.substring(dupFile.lastIndexOf("/") + 1)));
//
//                dialog.setPositiveButton(R.string.main_btn_duplicate, (dialog1, which) -> duplicateFile(filename))
//                        .setNeutralButton(R.string.str_cancel, null)
//                        .show();
//            }
//        }
//    }

    public void btnCleanDb(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("This operation will remove data that is not properly connected within the database. Points, Polygons, Groups and NMEA may be deleted. " +
                "It is suggested that you backup your project before continuing.")
                .setPositiveButton("Clean", (dialog1, which) -> {
                    if (getTtAppCtx().getDAL().clean()) {
                        Toast.makeText(MainActivity.this, "Database Cleaned", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error Cleaning Database", Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);
    }
    //endregion

    //region Data
    public void btnPointsClick(View view) {
        if (getTtAppCtx().getDAL().hasPolygons()) {
            //startActivityForResult(new Intent(this, PointsActivity.class), UPDATE_INFO);
            updateAppInfoOnResult.launch(new Intent(this, PointsActivity.class));
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnPolygonsClick(View view) {
        //startActivityForResult(new Intent(this, PolygonsActivity.class), UPDATE_INFO);
        updateAppInfoOnResult.launch(new Intent(this, PolygonsActivity.class));
    }

    public void btnMetadataClick(View view) {
        //startActivityForResult(new Intent(this, MetadataActivity.class), UPDATE_INFO);
        updateAppInfoOnResult.launch(new Intent(this, MetadataActivity.class));
    }

    public void btnProjectInfoClick(View view) {
        //startActivityForResult(new Intent(this, ProjectActivity.class), UPDATE_INFO);
        updateAppInfoOnResult.launch(new Intent(this, ProjectActivity.class));
    }

    public void btnPointTableClick(View view) {
        if (getTtAppCtx().getDAL().getItemsCount(TwoTrailsSchema.PointSchema.TableName) > 0) {
            //startActivityForResult(new Intent(this, TableViewActivity.class), UPDATE_INFO);
            updateAppInfoOnResult.launch(new Intent(this, TableViewActivity.class));
        } else {
            Toast.makeText(this, "No Points in Project", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    //region Tools
    public void btnMapClick(View view) {
        //if (AndroidUtils.App.requestNetworkPermission(this, Consts.Codes.Requests.INTERNET)) {
            if (getTtAppCtx().getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                startActivity(new Intent(this, MapActivity.class));
            }
        //}
    }

    public void btnGoogleEarthClick(View view) {
        final String gEarth = "com.google.earth";

        if (AndroidUtils.App.isPackageInstalled(MainActivity.this, gEarth)) {
            if (getTtAppCtx().getDAL().needsAdjusting()) {
                askToAdjust();
            } else {
                progressLayout.setVisibility(View.VISIBLE);

                try {
                    File kmlFile = Export.kml(getTtAppCtx(), getTtAppCtx().getDAL(), null);

                    progressLayout.setVisibility(View.GONE);

                    if (kmlFile != null) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(AndroidUtils.Files.getUri(MainActivity.this, BuildConfig.APPLICATION_ID, kmlFile), "application/vnd.google-earth.kml+xml");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(MainActivity.this, "Error opening Google Earth", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error creating kml file", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setMessage("Google Earth is not installed.")
                .setPositiveButton("Install", (dialog1, which) -> AndroidUtils.App.navigateAppStore(MainActivity.this, gEarth))
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
            startActivity(new Intent(this, ExportActivity.class));
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnPlotGridClick(View view) {
        if(getTtAppCtx().getDAL().hasPolygons()) {
            //startActivityForResult(new Intent(this, PlotGridActivity.class), UPDATE_INFO);
            updateAppInfoOnResult.launch(new Intent(this, PlotGridActivity.class));
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
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

    public void btnSATClick(View view) {
        if(getTtAppCtx().getDAL().hasPolygons()) {
            startActivity(new Intent(this, SalesAdminToolsActivity.class));
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnTest(View view) {

        //throw  new RuntimeException("Crash on purpose");
        startActivity(new Intent(this, TestActivity.class));
    }
    //endregion
    //endregion
}
