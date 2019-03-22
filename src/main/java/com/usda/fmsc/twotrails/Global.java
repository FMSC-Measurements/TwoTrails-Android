package com.usda.fmsc.twotrails;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.os.Environment;

import com.usda.fmsc.twotrails.data.DataAccessLayer;

import org.joda.time.DateTime;

import java.io.File;


public class Global {
    public static String getApplicationVersion(Application context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return String.format("ANDROID: %s", pInfo.versionName);
        } catch (Exception ex) {
            //
        }

        return "ANDROID: ???";
    }

    //region Files
    public static String getTtFilePath(String fileName) {
        if(!fileName.endsWith(Consts.FILE_EXTENSION))
            fileName += Consts.FILE_EXTENSION;

        return getTtFileDir() + File.separator + fileName;
    }

    public static String getDocumentsDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
    }

    private static String _OfflineMapsDir;
    public static String getOfflineMapsDir() {
        if (_OfflineMapsDir == null)
            _OfflineMapsDir = String.format("%s%s%s", getDocumentsDir(), File.separator, "OfflineMaps");
        return _OfflineMapsDir;
    }

    private static String _OfflineMapsRecoveryDir;
    public static String getOfflineMapsRecoveryDir() {
        if (_OfflineMapsRecoveryDir == null)
            _OfflineMapsRecoveryDir = String.format("%s%s%s", getOfflineMapsDir(), File.separator, "Recovery");
        return _OfflineMapsRecoveryDir;
    }

    private static String _TtFileDir;
    public static String getTtFileDir() {
        if (_TtFileDir == null)
            _TtFileDir = String.format("%s%s%s", getDocumentsDir(), File.separator, "TwoTrailsFiles");
        return _TtFileDir;
    }

    private static String _TtMediaDir;
    public static String getTtMediaDir() {
        if (_TtMediaDir == null)
            _TtMediaDir = String.format("%s%s%s", getTtFileDir(), File.separator, "Media");

        return _TtMediaDir;
    }
    public static String getTtMediaDir(DataAccessLayer dal) throws RuntimeException {
        _TtMediaDir = getTtMediaDir();

        if (dal != null) {
            String mdir = String.format("%s%s%s", _TtMediaDir, File.separator, dal.getFileName());

            File dir = new File(mdir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new RuntimeException("Unable to create Media Folder");
                }
            }

            return mdir;
        }

        return _TtMediaDir;
    }

    public static String getTtLogFileDir() {
        return getTtFileDir();
    }

    public static String getLogFileName() {
        return String.format("%s%sTtGpsLog_%s.txt",
                getTtLogFileDir(),
                File.separator,
                DateTime.now().toString());
    }
    //endregion
}
