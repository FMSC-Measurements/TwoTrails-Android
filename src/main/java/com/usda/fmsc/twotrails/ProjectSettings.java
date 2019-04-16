package com.usda.fmsc.twotrails;

import android.content.Context;
import android.util.JsonWriter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.objects.RecentProject;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectSettings extends Settings {
    public static final String PROJECT_ID = "ProjectID";
    public static final String DESCRIPTION = "Description";
    public static final String REGION = "Region";
    public static final String FOREST = "Forest";
    public static final String DISTRICT = "District";
    public static final String RECENT_PROJS = "Recent";
    public static final String LAST_EDITED_POLY_CN = "LastEditedPolyCN";
    public static final String TRACKED_POLY_CN = "TrackedPolyCN";


    public ProjectSettings(Context context) {
        super(context);
    }

    public void writeToFile(JsonWriter js) throws IOException {
    }


    public void initProjectSettings(DataAccessLayer dal) {
        if(dal != null) {
            setProjectId(dal.getProjectID());
            setDescription(dal.getProjectDescription());
            setRegion(dal.getProjectRegion());
            setForest(dal.getProjectForest());
            setDistrict(dal.getProjectDistrict());
        } else {
            setProjectId("Unamed");
            setDescription(StringEx.Empty);
        }
    }


    //region Project Settings
    private String getProjectId() {
        return getString(PROJECT_ID);
    }

    private void setProjectId(String value) {
        setString(PROJECT_ID, value);
    }


    private String getDescription() {
        return getString(DESCRIPTION);
    }

    private void setDescription(String value) {
        setString(DESCRIPTION, value);
    }


    public String getRegion() {
        return getString(REGION, "13");
    }

    public void setRegion(String value) {
        setString(REGION, value);
    }


    public String getForest() {
        return getString(FOREST);
    }

    public void setForest(String value) {
        setString(FOREST, value);
    }


    public String getDistrict() {
        return getString(DISTRICT);
    }

    public void setDistrict(String value) {
        setString(DISTRICT, value);
    }

    public String getLastEditedPolyCN() {
        return getString(LAST_EDITED_POLY_CN, StringEx.Empty);
    }

    public void setLastEditedPolyCN(String value) {
        setString(LAST_EDITED_POLY_CN, value);
    }

    public String getTrackedPolyCN() {
        return getString(TRACKED_POLY_CN, StringEx.Empty);
    }

    public void setTrackedPolyCN(String value) {
        setString(TRACKED_POLY_CN, value);
    }
    //endregion


    //region Recent Projects
    @SuppressWarnings("unchecked")
    public ArrayList<RecentProject> getRecentProjects() {
        Gson gson = new Gson();
        String json = getPrefs().getString(RECENT_PROJS, null);

        if(json == null)
            return new ArrayList<>();

        ArrayList<RecentProject> projects = new ArrayList<>();

        for (RecentProject rp : (ArrayList<RecentProject>)gson.fromJson(json, new TypeToken<ArrayList<RecentProject>>() { }.getType())) {
            if (FileUtils.fileExists(rp.File)) {
                projects.add(rp);
            }
        }

        return projects;
    }

    public boolean setRecentProjects(List<RecentProject> recentProjects) {
        return getEditor().putString(RECENT_PROJS, new Gson().toJson(recentProjects)).commit();
    }

    public void updateRecentProjects(RecentProject project) {
        ArrayList<RecentProject> newList = new ArrayList<>();
        newList.add(project);

        for (RecentProject p : getRecentProjects()) {
            if (!project.File.equals(p.File)) {
                newList.add(p);
            }
        }

        if (newList.size() > 7)
            setRecentProjects(newList.subList(0, 8));
        else
            setRecentProjects(newList);
    }
    //endregion
}
