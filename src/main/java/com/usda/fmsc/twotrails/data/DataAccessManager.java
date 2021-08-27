package com.usda.fmsc.twotrails.data;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.usda.fmsc.twotrails.TwoTrailsApp;

import java.io.IOException;

public class DataAccessManager extends AccessManager<DataAccessLayer>  {
    private DataAccessLayer _DAL;
    private final String _ProjectName;

    public DataAccessManager(TwoTrailsApp context, String fileName, String projectName) {
        super(context, fileName, null, TwoTrailsSchema.SchemaVersionInt);
        _ProjectName = projectName != null ? projectName : getDatabaseName();
    }


    @Override
    public void onConfigure(SQLiteDatabase db) {
        if (db.getVersion() == 0) {
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TwoTrailsSchema.ProjectInfoSchema.TableName + "';", null);

            if (cursor != null) {
                if (cursor.getCount() == 1) {
                    db.setVersion(1);
                }

                cursor.close();
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        _DAL = DataAccessLayer.createDAL(_Context, db, _ProjectName, getDatabaseName());
        super.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        _DAL = new DataAccessLayer(_Context, db, getDatabaseName());

        if (oldVersion == 1) {
            int ttVersion = _DAL.getVersion().toIntVersion();

            if (ttVersion < TwoTrailsSchema.DAL_2_0_2.toIntVersion()) {
                throw new UpgradeException(TwoTrailsSchema.DAL_2_0_2, "Unsupported Version", null, null);
            }

            if (ttVersion < TwoTrailsSchema.DAL_2_0_3.toIntVersion() && _DAL.getProjectCreatedTtVersion().startsWith("ANDROID")) {
                try {
                    _DAL.upgrade(Upgrades.DAL_2_0_3);
                } catch (Exception ex) {
                    throw new UpgradeException(Upgrades.DAL_2_0_3.Version, ex.getMessage(), ex.getCause(), ex.getStackTrace());
                }
            }
        }

        if (oldVersion < TwoTrailsSchema.DAL_2_1_0_INT) {
            try {
                _DAL.upgrade(Upgrades.DAL_2_1_0);
            } catch (Exception ex) {
                throw new UpgradeException(Upgrades.DAL_2_1_0.Version, ex.getMessage(), ex.getCause(), ex.getStackTrace());
            }
        }

        super.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        if (_DAL == null) {
            _DAL = new DataAccessLayer(_Context, db, getDatabaseName());
        }
    }

    @Override
    protected DataAccessLayer getDataLayer() {
        return getDAL();
    }

    public DataAccessLayer getDAL() {
        return _DAL != null ? _DAL : (_DAL = new DataAccessLayer(_Context, getWritableDatabase(), getDatabaseName(), false));
    }


    public static DataAccessManager openDAL(TwoTrailsApp context, String fileName) {
        return new DataAccessManager(context, fileName, null);
    }


    public static DataAccessManager importDAL(TwoTrailsApp context, Uri filePath) throws IOException {
        return importDAL(context, filePath, null);
    }

    public static DataAccessManager importDAL(TwoTrailsApp context, Uri filePath, String newFileName) throws IOException {
        return new DataAccessManager(context, importAL(context, filePath, newFileName), null);
    }


    public static void exportDAL(TwoTrailsApp context, String fileName, Uri extFilePath) throws IOException {
        exportFile(context, fileName, extFilePath);
    }


    public static boolean localDALExists(TwoTrailsApp context, String fileName) {
        return localFileExists(context, fileName);
    }
}
