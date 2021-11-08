package com.usda.fmsc.twotrails.data;


public class Upgrade {
    public final String[] SQL_Statements;
    public final TtVersion Version;

    public Upgrade(TtVersion version, String[] sql) {
        SQL_Statements = sql;
        Version = version;
    }
}