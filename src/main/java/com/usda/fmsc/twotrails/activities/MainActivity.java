package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import com.usda.fmsc.twotrails.activities.base.ProjectAdjusterActivity;
import com.usda.fmsc.twotrails.activities.contracts.CreateZipDocument;
import com.usda.fmsc.twotrails.activities.contracts.OpenDocumentTreePersistent;
import com.usda.fmsc.twotrails.adapters.RecentProjectAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.DataAccessManager;
import com.usda.fmsc.twotrails.data.MediaAccessManager;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.data.UpgradeException;
import com.usda.fmsc.twotrails.fragments.main.MainDataFragment;
import com.usda.fmsc.twotrails.fragments.main.MainFileFragment;
import com.usda.fmsc.twotrails.fragments.main.MainToolsFragment;
import com.usda.fmsc.twotrails.logic.AdjustingException;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.TwoTrailsProject;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtReport;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;


public class MainActivity extends ProjectAdjusterActivity {
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

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        inflateMenu(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.mainMenuSettings) {
            updateAppInfoOnResult.launch(new Intent(this, SettingsActivity.class));
        } else if (itemId == R.id.mainMenuGpsSettings) {
            startActivity(new Intent(this, SettingsActivity.class).
                    putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE));
        } else if (itemId == R.id.mainMenuRangeFinderSettings) {
            startActivity(new Intent(this, SettingsActivity.class).
                    putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.LASER_SETTINGS_PAGE));
        } else if (itemId == R.id.mainMenuPrivacyPolicy) {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
        }else if (itemId == R.id.mainMenuAbout) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(String.format("App: %s\nData: %s", TtUtils.getAndroidApplicationVersion(getTtAppCtx()), TwoTrailsSchema.SchemaVersion))
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
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onAppSettingsUpdated() {
        updateAppInfo();
    }

    @Override
    protected void onDestroy() {
        if (getTtAppCtx() != null) {
            getTtAppCtx().close();
        }

        super.onDestroy();
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

    private final ActivityResultLauncher<String> requestBackgroundLocationPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onBackgroundLocationRequestResult);


    private void onLocationRequestResult(boolean hasPermissions) {
        if (hasPermissions) {
            startGps();

            AndroidUtils.App.requestBackgroundLocationPermission(MainActivity.this,
                    requestBackgroundLocationPermissionOnResult,
                    getString(R.string.diag_back_loc));
        } else {
            Toast.makeText(MainActivity.this, "Cannot use GPS without location permissions", Toast.LENGTH_LONG).show();
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

    private final ActivityResultLauncher<String[]> requestBluetoothPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            onBluetoothRequestResult(TtUtils.Collections.areAllTrue(result.values())));

    private void onBluetoothRequestResult(boolean hasPermissions) {
        if (hasPermissions) {
            startGps();
        } else {
            Toast.makeText(MainActivity.this, "Cannot use GPS without bluetooth permissions", Toast.LENGTH_LONG).show();
        }

        if (resuming) {
            openProjectOnResume();
        }
    }

    private boolean requestBluetoothPermissions() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                AndroidUtils.App.requestBluetoothScanPermission(MainActivity.this,
                        requestBluetoothPermissionOnResult,
                        getString(R.string.diag_bt_loc))
                :
                AndroidUtils.App.requestBluetoothPermission(MainActivity.this,
                        requestBluetoothPermissionOnResult,
                        getString(R.string.diag_bt_loc));
    }

    private void checkGpsOnResume() {
        if (resuming) {
            DeviceSettings ds = getTtAppCtx().getDeviceSettings();
            String devId = ds.getGpsDeviceID();

            if (ds.isGpsAlwaysOn() && (StringEx.isEmpty(devId) ? requestGpsAccess() : requestBluetoothPermissions())) {
                openProjectOnResume();
            } else {
                openProjectOnResume();
            }
        }
    }

    private void startGps() {
        if (getTtAppCtx().isGpsServiceStarted()) {
            getTtAppCtx().getGps().startGps();
        } else {
            getTtAppCtx().startGpsService();
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

                //just checking for basic table structure for now
                if (dam.dbHasErrors()) {
                    //TODO has errors
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Project file is corrupted. Please try opening on the PC application or contact the developer.", Toast.LENGTH_LONG).show());
                    closeProject();
                } else {
                    app.setDAM(dam);

                    if (dam.justCreated()) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Project Created", Toast.LENGTH_LONG).show());
                        updateAppInfoAndGotoDataTabOnResult.launch(new Intent(this, ProjectActivity.class));
                    } else {
                        if (app.getDAL().needsAdjusting()) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Adjusting polygons in project", Toast.LENGTH_LONG).show());
                            app.adjustProject(true);
                        } else {
                            if (dam.justUpgraded()) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Project Upgraded", Toast.LENGTH_LONG).show());
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
                        app.getCurrentProject().TTXFile.replace(Consts.FileExtensions.TWO_TRAILS, Consts.FileExtensions.TWO_TRAILS_MEDIA_PACKAGE));

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
        TwoTrailsApp app = getTtAppCtx();

        if (app != null) {
            if (app.hasDAL()) {
                DataAccessLayer dal = getTtAppCtx().getDAL();
                String projectID = dal.getProjectID();
                app.getProjectSettings().updateRecentProjects(
                        new TwoTrailsProject(projectID, dal.getFileName()));

                setTitle("TwoTrails - " + projectID);
                enable = true;

                mFragFile.updateInfo();
            } else {
                setTitle(R.string.app_name);
            }
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


    private final ActivityResultLauncher<String> importFileOnResult = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            importProject(uri);
        }
    });
    private void importProject(Uri filePath) {
        String fp = filePath.getPath();
        if (fp == null)
            throw new RuntimeException("Invalid Filepath");

        String ext = fp.contains(".") ? fp.toLowerCase().substring(fp.lastIndexOf('.')) : "";

        switch (ext) {
            case Consts.FileExtensions.TWO_TRAILS: importTTX(filePath); break;
            case Consts.FileExtensions.TWO_TRAILS_MEDIA_PACKAGE: importTTMPX(filePath); break;
            case ".zip": importTtPackage(filePath); break;
            default: {
                //check if in downloads, prompt for name or ask to select from another location
                if (fp.toLowerCase().contains("msf:")) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Importing directly from a protected folder (such as Downloads) does not allow the app to see the filename. Would you like to set the filename or import from the another folder?")
                            .setPositiveButton("Import from SD", (dialogInterface, i) -> importFileOnResult.launch("*/*"))
                            .setNegativeButton("Set Filename", (dialogInterface, i) -> importFromProtectedFolderDialog(filePath))
                            .setNeutralButton(R.string.str_cancel, null)
                            .show();
                } else {
                    getTtAppCtx().getReport().writeWarn(String.format(Locale.getDefault(), "Invalid import type: '%s'", fp), "MainActivity:importProject");
                    Toast.makeText(MainActivity.this, "Invalid File Type", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void importTTX(Uri filePath) {
        try {
            String fp = filePath.getPath();
            if (fp == null)
                throw new RuntimeException("Invalid Filepath");

            if (!fp.endsWith(Consts.FileExtensions.TWO_TRAILS))
                throw new RuntimeException("Invalid File Type");

            String fileName = FileUtils.getFileName(fp);

            if (DataAccessManager.localDALExists(getTtAppCtx(), fileName)) {
                overwriteLocalProjectByImportDialog(fileName, filePath);
            } else {
                DataAccessManager dam = DataAccessManager.importDAL(getTtAppCtx(), filePath);

                String projectName = dam.getDAL().getProjectID();
                dam.close();

                openProject(projectName, fileName);
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:importTTX", e.getStackTrace());
            Toast.makeText(MainActivity.this, "Error Importing Project", Toast.LENGTH_LONG).show();
        }
    }

    private void importTTMPX(Uri filePath) {
        try {
            String fp = filePath.getPath();
            if (fp == null)
                throw new RuntimeException("Invalid Filepath");

            if (!fp.endsWith(Consts.FileExtensions.TWO_TRAILS_MEDIA_PACKAGE))
                throw new RuntimeException("Invalid File Type");

            String fileName = FileUtils.getFileName(fp);

            if (MediaAccessManager.localMALExists(getTtAppCtx(), fileName)) {
                overwriteLocalMediaPackageByImportDialog(fileName, filePath);
            } else {
                MediaAccessManager mam = MediaAccessManager.importMAL(getTtAppCtx(), filePath);

                if (fileName.equals(getTtAppCtx().getDAM().getDBFile().getName())) {
                    getTtAppCtx().setMAM(mam);
                }
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:importTTMPX", e.getStackTrace());
            Toast.makeText(MainActivity.this, "Error Importing Media Package", Toast.LENGTH_LONG).show();
        }
    }

    //Todo add import of media files, option to not open project after import if multiple projects detected
    private void importTtPackage(Uri filePath) {
        String fp = filePath.getPath();
        if (fp == null)
            throw new RuntimeException("Invalid Filepath");

        if (!fp.endsWith(".zip"))
            throw new RuntimeException("Invalid File Type");

        File tmpInternalZip = new File(getTtAppCtx().getCacheDir(), FileUtils.getFileName(fp));
        File tmpInternalZipDir = new File(getTtAppCtx().getCacheDir(), FileUtils.getFileNameWoExt(fp));

        try {
            if (tmpInternalZipDir.exists()) {
                if (!FileUtils.deleteDirectory(tmpInternalZipDir)) {
                    throw new RuntimeException("Unable to delete unzipped folder");
                }
            }

            AndroidUtils.Files.copyFile(getTtAppCtx(), filePath, Uri.fromFile(tmpInternalZip));

            FileUtils.unzip(tmpInternalZip, tmpInternalZipDir);

            for (File file : tmpInternalZipDir.listFiles()) {
                importFromFile(file);
            }
        } catch (IOException e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:importTtPackage");
            Toast.makeText(MainActivity.this, "Error Importing Project. See Log for details.", Toast.LENGTH_LONG).show();
        }
    }

    private void importFromFile(File item) {
        if (item.isDirectory()) {
            for (File subItem : item.listFiles()) {
                importFromFile(subItem);
            }
        } else if (item.getPath().toLowerCase().endsWith(Consts.FileExtensions.TWO_TRAILS)) {
            importTTX(Uri.fromFile(item));
        } else if (item.getPath().toLowerCase().endsWith(Consts.FileExtensions.TWO_TRAILS_MEDIA_PACKAGE)) {
            importTTMPX(Uri.fromFile(item));
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
        TwoTrailsApp context = getTtAppCtx();

        try {
            if (filePath != null) {
                File pcPkgFile = Export.exportProjectPackage(context);

                if (pcPkgFile != null) {
                    AndroidUtils.Files.copyFile(getTtAppCtx(), Uri.fromFile(pcPkgFile), filePath);
                } else {
                    throw new Exception("Unable to create export file");
                }

                pcPkgFile.deleteOnExit();
            } else {
                throw new Exception("Unable to get file path from uri");
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:exportProject", e.getStackTrace());
            Toast.makeText(MainActivity.this, "Error Exporting Project", Toast.LENGTH_LONG).show();
        }
    }
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

    private void overwriteLocalProjectByImportDialog(final String fileName, Uri filePath) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Project Exists");
        alert.setMessage(String.format(Locale.getDefault(), "The current file '%s' already exists. Do you want to overwrite or rename the project?", fileName));

        alert.setPositiveButton("Overwrite", (dialog, which) -> {
            try {
                DataAccessManager dam = DataAccessManager.importDAL(getTtAppCtx(), filePath);

                String projectName = dam.getDAL().getProjectID();
                dam.close();

                openProject(projectName, fileName);
            } catch (IOException e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:overwriteLocalProjectByImportDialog:overwrite");
                Toast.makeText(MainActivity.this, "Error Overwriting Project", Toast.LENGTH_LONG).show();
            }
        });

        alert.setNegativeButton("Rename", (dialog, which) -> {
            final InputDialog inputDialog = new InputDialog(MainActivity.this);
            inputDialog.setTitle("New File Name");

            inputDialog.setPositiveButton("Rename", (d2, wb2) -> {
                try {
                    String newFileName = inputDialog.getText();

                    if (!newFileName.endsWith(Consts.FileExtensions.TWO_TRAILS)) {
                        newFileName += Consts.FileExtensions.TWO_TRAILS;
                    }

                    DataAccessManager dam = DataAccessManager.importAndRenameDAL(getTtAppCtx(), filePath, newFileName);

                    String projectName = dam.getDAL().getProjectID();
                    dam.close();

                    openProject(projectName, fileName);
                } catch (IOException e) {
                    getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:overwriteLocalProjectByImportDialog:rename");
                    Toast.makeText(MainActivity.this, "Error Renaming Project", Toast.LENGTH_LONG).show();
                }
            });

            inputDialog.setNegativeButton("Cancel", null);

            inputDialog.show();
        });

        alert.setNeutralButton(R.string.str_cancel, null);
        alert.show();
    }

    private void overwriteLocalMediaPackageByImportDialog(final String fileName, Uri filePath) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Media File Exists");
        alert.setMessage(String.format("The current file '%s' already exists. Do you want to overwrite or rename it?", fileName));

        alert.setPositiveButton("Overwrite", (dialog, which) -> {
            try {
                MediaAccessManager mam = MediaAccessManager.importMAL(getTtAppCtx(), filePath);
                if (fileName.equals(getTtAppCtx().getDAM().getDBFile().getName())) {
                    getTtAppCtx().setMAM(mam);
                }
            } catch (IOException e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:overwriteLocalMediaPackageByImportDialog:overwrite");
                Toast.makeText(MainActivity.this, "Error Overwriting Media Package", Toast.LENGTH_LONG).show();
            }
        });

        alert.setNegativeButton("Rename", (dialog, which) -> {
            final InputDialog inputDialog = new InputDialog(MainActivity.this);
            inputDialog.setTitle("New File Name");

            inputDialog.setPositiveButton("Rename", (d2, wb2) -> {
                try {
                    String newFileName = inputDialog.getText();

                    if (!newFileName.endsWith(Consts.FileExtensions.TWO_TRAILS)) {
                        newFileName += Consts.FileExtensions.TWO_TRAILS;
                    }

                    MediaAccessManager mam = MediaAccessManager.importAndRenameMAL(getTtAppCtx(), filePath, newFileName);
                    if (FileUtils.getFileName(newFileName).equals(getTtAppCtx().getDAM().getDBFile().getName())) {
                        getTtAppCtx().setMAM(mam);
                    }
                } catch (IOException e) {
                    getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:overwriteLocalMediaPackageByImportDialog:rename");
                    Toast.makeText(MainActivity.this, "Error Renaming Media Package", Toast.LENGTH_LONG).show();
                }
            });

            inputDialog.setNegativeButton("Cancel", null);

            inputDialog.show();
        });

        alert.setNeutralButton(R.string.str_cancel, null);
        alert.show();
    }

    private void overwriteLocalProjectAndMediaPackageByImportDialog(String baseFileName, Uri ttxFilePath, Uri ttmpxFilePath) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Project Exists");
        alert.setMessage(String.format(Locale.getDefault(), "The project files '%s' already exist. Do you want to overwrite or rename the project?", baseFileName));

        alert.setPositiveButton("Overwrite", (dialog, which) -> {
            try {
                DataAccessManager dam = DataAccessManager.importDAL(getTtAppCtx(), ttxFilePath);
                MediaAccessManager.importMAL(getTtAppCtx(), ttmpxFilePath).close();

                String projectID = dam.getDAL().getProjectID();
                String fileName = dam.getDatabaseName();
                dam.close();

                openProject(projectID, fileName);
            } catch (IOException e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:overwriteLocalProjectAndMediaPackageByImportDialog:overwrite");
                Toast.makeText(MainActivity.this, "Error Overwriting Project", Toast.LENGTH_LONG).show();
            }
        });

        alert.setNegativeButton("Rename", (dialog, which) -> {
            final InputDialog inputDialog = new InputDialog(MainActivity.this);
            inputDialog.setTitle("New File Name");

            inputDialog.setPositiveButton("Rename", (d2, wb2) -> {
                try {
                    String newTtxFileName = inputDialog.getText(), newTtmpxFileName;
                    String newBaseFileName = newTtxFileName.substring(0, newTtxFileName.lastIndexOf("."));

                    newTtxFileName = newBaseFileName + Consts.FileExtensions.TWO_TRAILS;
                    newTtmpxFileName = newBaseFileName + Consts.FileExtensions.TWO_TRAILS_MEDIA_PACKAGE;

                    DataAccessManager dam = DataAccessManager.importAndRenameDAL(getTtAppCtx(), ttxFilePath, newTtxFileName);
                    MediaAccessManager.importAndRenameMAL(getTtAppCtx(), ttmpxFilePath, newTtmpxFileName).close();
                    dam.close();

                    openProject(newBaseFileName, newTtmpxFileName);
                } catch (IOException e) {
                    getTtAppCtx().getReport().writeError(e.getMessage(), "MainActivity:overwriteLocalProjectAndMediaPackageByImportDialog:rename");
                    Toast.makeText(MainActivity.this, "Error Renaming Project", Toast.LENGTH_LONG).show();
                }
            });

            inputDialog.setNegativeButton("Cancel", null);

            inputDialog.show();
        });

        alert.setNeutralButton(R.string.str_cancel, null);
        alert.show();
    }


    private void importFromProtectedFolderDialog(Uri filePath) {
        final InputDialog inputDialog = new InputDialog(MainActivity.this);
        inputDialog.setTitle("Filename");

        inputDialog.setPositiveButton("Import Project", (dialog, whichButton) -> {
                    String filename = inputDialog.getText().trim();

                    if (filename.length() > 0) {
                        filename = TtUtils.projectToFileNameTTX(filename);
                        if (DataAccessManager.localDALExists(getTtAppCtx(), filename)) {
                            overwriteLocalProjectByImportFromProtectedFolderDialog(filename, filePath);
                        } else {
                            importFromProtectedFolder(filename, filePath);
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Invalid File Name", Toast.LENGTH_LONG).show());
                    }
                })
                .setNegativeButton(R.string.str_cancel, null)
                .show();
    }

    private void overwriteLocalProjectByImportFromProtectedFolderDialog(final String filename, Uri filePath) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Project Exists");
        alert.setMessage(String.format("The file '%s' already exists. Do you want to overwrite the project?", filename));

        alert.setPositiveButton("Yes", (dialog, whichButton) -> importFromProtectedFolder(filename, filePath));

        alert.setNegativeButton("No",
                (dialog, which) -> importFromProtectedFolderDialog(filePath));
        alert.show();
    }

    private void importFromProtectedFolder(String filename, Uri filePath) {
        TtReport report = getTtAppCtx().getReport();

        String tempProjectFileName = TtUtils.projectToFileNameTTX(String.format(Locale.getDefault(), "temp_%s", TtUtils.Date.nowToString()));
        File tempProjectFile = getDatabasePath(tempProjectFileName);

        Uri tempProjectUri = Uri.fromFile(tempProjectFile);
        if (tempProjectUri == null) {
            report.writeWarn("tempProjectUri is NULL", "importProject");
        } else {
            try {
                AndroidUtils.Files.copyFile(getTtAppCtx(), filePath, tempProjectUri);
                try {
                    DataAccessManager dam = DataAccessManager.openDAL(getTtAppCtx(), tempProjectFileName);
                    try {
                        String projectName = dam.getDAL().getProjectID();
                        if (projectName == null) {
                            throw new NullPointerException("Invalid File");
                        } else {
                            dam.close();
                            try {
                                DataAccessManager.importAndRenameDAL(getTtAppCtx(), tempProjectUri, filename).close();
                                openProject(projectName, filename);
                                return;
                            } catch (IOException e) {
                                report.writeError("Unable to rename." + e.getMessage(), "importProject", e.getStackTrace());
                            }
                        }
                    } catch (Exception e) {
                        report.writeError("Invalid Project File. " + e.getMessage(), "importProject", e.getStackTrace());
                    }
                } catch (Exception e) {
                    report.writeError("Unable to open DAL. " + e.getMessage(), "importProject", e.getStackTrace());
                }
            } catch (IOException e) {
                report.writeError("Unable to copy tempProjectUri. " + e.getMessage(), "importProject", e.getStackTrace());
            }
        }

        Toast.makeText(MainActivity.this, "Error Importing File: Please send Error Report.", Toast.LENGTH_LONG).show();
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

        if (twoTrailsProjects.size() < 1) {
            for (String file : databaseList()) {
                if (file.toLowerCase().endsWith(Consts.FileExtensions.TWO_TRAILS)) {
                    DataAccessManager dam = DataAccessManager.openDAL(getTtAppCtx(), file);
                    String projId = dam.getDAL().getProjectID();
                    String mediaFile = file.toLowerCase().replace(Consts.FileExtensions.TWO_TRAILS, Consts.FileExtensions.TWO_TRAILS_MEDIA_PACKAGE);
                    if (!MediaAccessManager.localMALExists(getTtAppCtx(), mediaFile)) {
                        mediaFile = null;
                    }
                    TwoTrailsProject proj = new TwoTrailsProject(projId, file, mediaFile);
                    twoTrailsProjects.add(proj);
                }
            }

            if (twoTrailsProjects.size() > 0) {
                getTtAppCtx().getProjectSettings().setRecentProjects(twoTrailsProjects);
            }
        }

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
            importFileOnResult.launch("*/*");
        }
    }

    public void btnExportProjectClick(View view) {
        if (continueIfNotProcessing()) {

            String filename = String.format(Locale.getDefault(), "%s_%s.zip",
                    TtUtils.projectToFileName(getTtAppCtx().getDAL().getProjectID()),
                    TtUtils.Date.toStringDateMillis(new DateTime(getTtAppCtx().getDAM().getDBFile().lastModified()))
            );

            exportProjectLauncher.launch(filename);
        }
    }

    public void btnImportDataClick(View view) {
        updateAppInfoOnResult.launch(new Intent(this, ImportActivity.class));
    }

    public void btnRemoveProjectClick(View view) {
        //TODO remove project
        //ask to export first
    }

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
            updateAppInfoOnResult.launch(new Intent(this, PointsActivity.class));
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnPolygonsClick(View view) {
        updateAppInfoOnResult.launch(new Intent(this, PolygonsActivity.class));
    }

    public void btnMetadataClick(View view) {
        updateAppInfoOnResult.launch(new Intent(this, MetadataActivity.class));
    }

    public void btnProjectInfoClick(View view) {
        updateAppInfoOnResult.launch(new Intent(this, ProjectActivity.class));
    }

    public void btnPointTableClick(View view) {
        if (getTtAppCtx().getDAL().getItemsCount(TwoTrailsSchema.PointSchema.TableName) > 0) {
            updateAppInfoOnResult.launch(new Intent(this, TableViewActivity.class));
        } else {
            Toast.makeText(this, "No Points in Project", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnSATClick(View view) {
        if(getTtAppCtx().getDAL().hasPolygons()) {
            final ArrayList<TtPolygon> polygons = getTtAppCtx().getDAL().getPolygons();
            final ArrayList<TtPolygon> filteredPolygons = TtUtils.Collections.filterOutPltsAndSats(getTtAppCtx().getDAL().getPolygons());

            Function<TtPolygon, Integer> startSAT = (targetPoly -> {
                TtPolygon valPoly = null;

                String valPolyName = String.format(Locale.getDefault(), "%s_sat", targetPoly.getName());

                for (TtPolygon poly : polygons) {
                    if (poly.getName().equals(valPolyName)) {
                        valPoly = poly;
                        break;
                    }
                }

                if (valPoly == null) {
                    int polyCount = polygons.size();

                    valPoly = new TtPolygon(polyCount * 1000 + 1010);
                    valPoly.setName(String.format(Locale.getDefault(), valPolyName, polyCount + 1));
                    valPoly.setAccuracy(Consts.Default_Point_Accuracy);
                    valPoly.setDescription(String.format(Locale.getDefault(), "Validation points for %s", targetPoly.getName()));
                    getTtAppCtx().getDAL().insertPolygon(valPoly);
                }

                Intent intent = new Intent(this, SalesAdminToolsActivity.class);
                intent.putExtra(Consts.Codes.Data.METADATA_DATA, getTtAppCtx().getDAL().getDefaultMetadata());
                intent.putExtra(Consts.Codes.Data.POLYGON_DATA, targetPoly);

                updateAppInfoOnResult.launch(intent);

                return 0;
            });


            if (polygons.size() > 1) {
                final String[] polyStrs = new String[filteredPolygons.size()];

                for (int i = 0; i < filteredPolygons.size(); i++) {
                    polyStrs[i] = filteredPolygons.get(i).getName();
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                dialogBuilder.setTitle("Track Polygon");

                dialogBuilder.setItems(polyStrs, (dialog, which) -> startSAT.apply(filteredPolygons.get(which)));

                dialogBuilder.setNegativeButton(R.string.str_cancel, null);

                final AlertDialog dialog = dialogBuilder.create();

                dialog.show();
            } else {
                startSAT.apply(filteredPolygons.get(0));
            }
        } else {
            Toast.makeText(this, "No Polygons in Project", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    //region Tools
    public void btnMapClick(View view) {
        if (getTtAppCtx().getDAL().needsAdjusting()) {
            askToAdjust();
        } else {
            startActivity(new Intent(this, MapActivity.class));
        }
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

    public void btnTest(View view) {
        startActivity(new Intent(this, TestActivity.class));
    }
    //endregion
    //endregion
}
