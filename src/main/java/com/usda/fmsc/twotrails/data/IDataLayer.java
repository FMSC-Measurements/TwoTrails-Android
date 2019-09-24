package com.usda.fmsc.twotrails.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.usda.fmsc.utilities.StringEx;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class IDataLayer {
    protected static DateTimeFormatter dtf= DateTimeFormat.forPattern("yyyy-M-dd H:mm:ss");
    protected static DateTimeFormatter dtfAlt = DateTimeFormat.forPattern("yyyy-M-dd H:mm:ss.SSS"); //Alt Format, PC might be using it

    protected SQLiteDatabase _db;

    protected String createSelectQuery(String table, String items, String where) {
        return String.format("select %s from %s%s",
                items, table, StringEx.isEmpty(where) ? StringEx.Empty : String.format(" where %s", where));
    }

    protected String createSelectAllQuery(String table, String where) {
        return String.format("select * from %s%s",
                table, StringEx.isEmpty(where) ?
                        StringEx.Empty : String.format(" where %s", where));
    }


    public int getItemCount(String tableName) {
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
                String.format("%s = %d",
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


    public abstract boolean open();

    public abstract boolean isOpen();

    public abstract void close();

    public abstract TtVersion getVersion();
}
