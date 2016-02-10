package com.usda.fmsc.twotrails.objects;


import com.usda.fmsc.utilities.StringEx;

public class RecentProject {
    public String Name;
    public String File;

    public RecentProject(){
        Name = File = StringEx.Empty;
    }

    public RecentProject(String name, String file) {
        Name = name;
        File = file;
    }
}