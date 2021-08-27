package com.usda.fmsc.twotrails.data;


public class Upgrade {
    public final String SQL;
    public final TtVersion Version;

    public Upgrade(TtVersion version, String sql) {
        SQL = sql;
        Version = version;
    }
}