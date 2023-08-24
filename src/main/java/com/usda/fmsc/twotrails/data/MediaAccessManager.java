package com.usda.fmsc.twotrails.data;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.usda.fmsc.twotrails.TwoTrailsApp;

import java.io.IOException;

public class MediaAccessManager extends AccessManager<MediaAccessLayer> {
    private MediaAccessLayer _MAL;

    public MediaAccessManager(TwoTrailsApp context, String fileName) {
        super(context, fileName, null, TwoTrailsSchema.SchemaVersion.DbVersion);
    } 


    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (isDbTTMPX(db)) {
            _MAL = new MediaAccessLayer(_Context, db, getDatabaseName());

            if (_MAL.getVersion().toIntVersion() < TwoTrailsMediaSchema.SchemaVersion.toIntVersion()) {
                onUpgrade(db, _MAL.getUserVersion(), TwoTrailsMediaSchema.SchemaVersion.DbVersion);
            }
        } else {
            _MAL = new MediaAccessLayer(_Context, db, getDatabaseName(), true);
        }
        super.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        _MAL = _MAL != null ? _MAL : new MediaAccessLayer(_Context, db, getDatabaseName());

        if (oldVersion < TwoTrailsMediaSchema.MAL_2_1_0.DbVersion) {
            if (!isDbTTMPX(db)) {
                throw new RuntimeException("Invalid TwoTrails Media File");
            }

            int ttmVersion = _MAL.getVersion().toIntVersion();

            if (ttmVersion < TwoTrailsMediaSchema.MAL_2_1_0.toIntVersion()) {
                try {
                    _MAL.upgrade(Upgrades.MAL_2_1_0);
                } catch (Exception ex) {
                    throw new UpgradeException(Upgrades.MAL_2_1_0.Version, ex.getMessage(), ex.getCause(), ex.getStackTrace());
                }
            }
        }

        super.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        if (_MAL == null) {
            _MAL = new MediaAccessLayer(_Context, db, getDatabaseName());
        }
    }

    private boolean isDbTTMPX(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TwoTrailsMediaSchema.Media.TableName + "';", null);

        if (cursor != null) {
            if (cursor.getCount() != 1) {
                return false;
            }

            cursor.close();
        }

        return true;
    }


    @Override
    protected MediaAccessLayer getDataLayer() {
        return getMAL();
    }

    public MediaAccessLayer getMAL() {
        return _MAL != null ? _MAL : (_MAL = new MediaAccessLayer(_Context, getWritableDatabase(), getDatabaseName(), false));
    }


    public static MediaAccessManager openMAL(TwoTrailsApp context, String fileName) {
        return new MediaAccessManager(context, fileName);
    }

    public static MediaAccessManager importMAL(TwoTrailsApp context, Uri filePath) throws IOException {
        return new MediaAccessManager(context, importAL(context, filePath, null));
    }

    public static MediaAccessManager importAndRenameMAL(TwoTrailsApp context, Uri filePath, String newFileName) throws IOException {
        return new MediaAccessManager(context, importAL(context, filePath, newFileName));
    }


    public static void exportMAL(TwoTrailsApp context, String fileName, Uri exportFilePath) throws IOException {
        exportFile(context, fileName, exportFilePath);
    }


    public static boolean localMALExists(TwoTrailsApp context, String fileName) {
        return localFileExists(context, fileName);
    }
}
