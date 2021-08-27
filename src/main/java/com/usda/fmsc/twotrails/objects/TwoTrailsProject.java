package com.usda.fmsc.twotrails.objects;

public class TwoTrailsProject {
    public final String Name;
    public final String TTXFile;
    public final String TTMPXFile;

    public TwoTrailsProject(String name, String ttxFile) {
        this(name, ttxFile, null);
    }

    public TwoTrailsProject(String name, String ttxFile, String ttmpxFile) {
        Name = name;
        TTXFile = ttxFile;
        TTMPXFile = ttmpxFile;
    }
}