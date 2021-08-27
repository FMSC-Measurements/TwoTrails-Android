package com.usda.fmsc.twotrails.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.utilities.StringEx;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public abstract class IDataLayer {
    protected static DateTimeFormatter dtf= DateTimeFormat.forPattern("yyyy-M-dd H:mm:ss");
    protected static DateTimeFormatter dtfAlt = DateTimeFormat.forPattern("yyyy-M-dd H:mm:ss.SSS"); //Alt Format, PC might be using it

    private final SQLiteDatabase _db;
    private final TwoTrailsApp _Context;
    private final String _FileName;
    protected Listener _Listener;


    public IDataLayer(TwoTrailsApp context, SQLiteDatabase db, String fileName) {
        this(context, db, fileName, false);
    }

    protected IDataLayer(TwoTrailsApp context, SQLiteDatabase db, String fileName, boolean create) {
        _Context = context;
        _db = db;
        _FileName = fileName;

        if (create) {
            onCreateDB(_db);
        }

        onOpenDB(_db);
    }


    protected void onCreateDB(SQLiteDatabase db) {}

    protected void onOpenDB(SQLiteDatabase db) {}

    protected void onCloseDB(SQLiteDatabase db) {}


    protected SQLiteDatabase getDB() {
        return _db;
    }

    protected TwoTrailsApp getTtAppContext() {
        return _Context;
    }

    public String getFileName() {
        return _FileName;
    }

    public void close() {
        if (_db != null) {
            onCloseDB(_db);
        }
    }


    protected void logError(String msg, String codePage) {
        getTtAppContext().getReport().writeError(msg, codePage);

        if (_Listener != null) _Listener.onError(msg, codePage, null);
    }

    protected void logError(String msg, String codePage, StackTraceElement[] stack) {
        getTtAppContext().getReport().writeError(msg, codePage, stack);

        if (_Listener != null) _Listener.onError(msg, codePage, stack);
    }


    protected void onAction(int action) {
        if (_Listener != null) _Listener.onAction(action);
    }


    protected String createSelectQuery(String table, String items, String where) {
        return String.format("select %s from %s%s",
                items, table, StringEx.isEmpty(where) ? StringEx.Empty : String.format(" where %s", where));
    }

    protected String createSelectAllQuery(String table, String where) {
        return String.format("select * from %s%s",
                table, StringEx.isEmpty(where) ?
                        StringEx.Empty : String.format(" where %s", where));
    }


    public int getItemsCount(String tableName) {
        String countQuery = "SELECT COUNT (*) FROM " + tableName;

        Cursor cursor = _db.rawQuery(countQuery, null);

        int count = 0;
        if (null != cursor) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            cursor.close();
        }
        return count;
    }

    public int getItemsCount(String tableName, String column, int value) {
        return getItemsCount(tableName,
                String.format(Locale.getDefault(), "%s = %d",
                        column,
                        value)
        );
    }

    public int getItemsCount(String tableName, String column, String value) {
        return getItemsCount(tableName,
                String.format("%s = '%s'",
                    column,
                    value)
        );
    }

    public int getItemsCount(String tableName, String where) {
        String countQuery = String.format("select count(*) from %s where %s", tableName, where);

        Cursor cursor = _db.rawQuery(countQuery, null);

        int count = 0;
        if (null != cursor) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            cursor.close();
        }
        return count;
    }


    public DateTime parseDateTime(String date) {
        try {
            return dtf.parseDateTime(date);
        } catch (IllegalArgumentException e) {
            try {
                return dtfAlt.parseDateTime(date);
            } catch (IllegalArgumentException e2) {
                try {
                    return DateTime.parse(date);
                } catch (IllegalArgumentException e3) {
                    return DateTime.now();
                }
            }
        }
    }


    public ArrayList<String> getCNs(String tableName, String where) {
        ArrayList<String> cns = new ArrayList<>();

        try {
            String query = String.format("select %s from %s%s",
                    TwoTrailsSchema.SharedSchema.CN,
                    tableName,
                    StringEx.isEmpty(where) ? StringEx.Empty : String.format(" where %s", where));

            Cursor c = _db.rawQuery(query, null);

            if (c.moveToFirst()) {
                do {
                    if (!c.isNull(0))
                        cns.add(c.getString(0));
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            //
        }

        return cns;
    }


    public byte[] getLargeBlob(String tableName, String column, String where) {
        final int MAX_BLOB_SIZE = 1000000;
        byte[] data = null;

        String query = String.format("select length(%s) from %s%s",
                column, tableName,
                where != null ? String.format(" where %s", where) : null);

        Cursor cursor = _db.rawQuery(query, null);
        if (cursor.moveToFirst()){
            int size = cursor.getInt(0);

            cursor.close();

            if (size > MAX_BLOB_SIZE) {
                int start = 1, length = MAX_BLOB_SIZE;

                ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
                while (size > 0) {
                    query = String.format("select substr(%s,       %d, %d) from %s%s",
                            column, start, length, tableName,
                            where != null ? String.format(" where %s", where) : null);

                    size -= MAX_BLOB_SIZE;
                    start += MAX_BLOB_SIZE;
                    length = size > MAX_BLOB_SIZE ? MAX_BLOB_SIZE : size;

                    cursor = _db.rawQuery(query, null);

                    if (cursor.moveToFirst() && !cursor.isNull(0)) {
                        try {
                            baos.write(cursor.getBlob(0));
                            cursor.close();
                        } catch (IOException e) {
                            throw new RuntimeException("Unable to retrieve blob");
                        }
                    } else {
                        throw new RuntimeException("Unable to retrieve blob");
                    }
                }

                data = baos.toByteArray();
            } else {
                query = String.format("select %s from %s%s",
                        column, tableName,
                        where != null ? String.format(" where %s", where) : null);

                cursor = _db.rawQuery(query, null);

                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    data = cursor.getBlob(0);
                } else {
                    throw new RuntimeException("Unable to retrieve blob");
                }
            }

        } else {
            throw new RuntimeException("Blob doesn't exists");
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }

        return data;
    }


    public abstract TtVersion getVersion();


    public abstract boolean hasErrors();


    public interface Listener {
        void onAction(int action);
        void onError(String msg, String codePage, StackTraceElement[] stack);
    }

    public void addListener(Listener listener) {
        _Listener = listener;
    }


    public void removeListener() {
        _Listener = null;
    }
}
