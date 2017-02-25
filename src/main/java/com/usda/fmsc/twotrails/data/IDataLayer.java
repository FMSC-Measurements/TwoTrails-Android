package com.usda.fmsc.twotrails.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.usda.fmsc.utilities.StringEx;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

public abstract class IDataLayer {
    protected static DateTimeFormatter dtf = DateTimeFormat.forPattern("M/d/yyyy h:mm:ss.SSS");
    protected static DateTimeFormatter dtfAlt = DateTimeFormat.forPattern("yyyy-dd-M h:mm:ss"); //Alt Format, PC might be using it

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
        String countQuery = String.format("SELECT COUNT (*) FROM %s where %s = %d",
                tableName,
                column,
                value);

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

    public int getItemsCount(String tableName, String column, String value) {
        String countQuery = String.format("SELECT COUNT (*) FROM %s where %s = '%s'",
                tableName,
                column,
                value);

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
            return dtfAlt.parseDateTime(date);
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
}
