package com.usda.fmsc.twotrails.data;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.utilities.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class AccessManager<DL extends IDataLayer> extends SQLiteOpenHelper {
    protected final TwoTrailsApp _Context;
    private boolean justCreated, justUpgraded;


    public AccessManager(TwoTrailsApp context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        _Context = context;
    }


    public File getDBFile() {
        return _Context.getDatabasePath(getDatabaseName());
    }

    public Uri getDBUri() {
        return Uri.fromFile(getDBFile());
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        justCreated = true;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        justUpgraded = true;
    }



    protected abstract DL getDataLayer();

    public boolean checkDB() {
        DL dl = getDataLayer();
        return !dl.hasErrors();
    }


    public boolean justCreated() {
        return justCreated;
    }

    public boolean justUpgraded() {
        return justUpgraded;
    }


    protected static String importAL(TwoTrailsApp context, Uri filePath, String newFileName) throws IOException {
        if (filePath != null && filePath.getPath() != null) {
            String fileName = newFileName != null ? newFileName : FileUtils.getFileName(filePath.getPath());
            Uri localDB = Uri.fromFile(context.getDatabasePath(fileName));

            FileUtils.copyFile(context, filePath, localDB);
            return fileName;
        } else {
            throw new RuntimeException("Invalid filePath");
        }
    }


    public void export(Uri extFilePath) throws IOException {
        if (extFilePath != null && extFilePath.getPath() != null) {
            Uri localDB = getDBUri();
            FileUtils.copyFile(_Context, localDB, extFilePath);
        } else {
            throw new RuntimeException("Invalid External File Path");
        }
    }


    protected static void exportFile(TwoTrailsApp context, String fileName, Uri exportFilePath) throws IOException {
        if (exportFilePath != null && exportFilePath.getPath() != null) {
            if (localFileExists(context, fileName)) {
                Uri localDB = Uri.fromFile(context.getDatabasePath(fileName));
                FileUtils.copyFile(context, localDB, exportFilePath);
            } else {
                throw new FileNotFoundException("Database Name: " + fileName);
            }
        } else {
            throw new RuntimeException("Invalid filePath");
        }
    }

    public static boolean localFileExists(TwoTrailsApp context, String fileName) {
        File localDbFile = context.getDatabasePath(fileName);
        return localDbFile != null && localDbFile.exists();
    }
}
