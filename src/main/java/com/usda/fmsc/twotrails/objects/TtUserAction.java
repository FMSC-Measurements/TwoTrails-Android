package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtUserAction {
    private final String UserName;
    private final String DeviceName;
    private final DataActionType Action;
    private final String AppVersion;
    private DateTime Date;
    private String Notes;

    public TtUserAction(String userName, String deviceName, String appVersion) {
        this(userName, deviceName, appVersion, DateTime.now(), new DataActionType(), StringEx.Empty);
    }

    public TtUserAction(String userName, String deviceName, String appVersion, DateTime date, DataActionType action, String notes) {
        if (StringEx.isEmpty(userName))
            throw new RuntimeException("Invalid UserName");

        if (StringEx.isEmpty(deviceName))
            throw new RuntimeException("Invalid Device");

        if (StringEx.isEmpty(appVersion))
            throw new RuntimeException("Invalid AppVersion");

        UserName = userName;
        DeviceName = deviceName;
        Date = date;
        Action = action;
        Notes = notes;
        AppVersion = appVersion;
    }

    public String getUserName() {
        return UserName;
    }

    public String getDeviceName() {
        return DeviceName;
    }

    public DateTime getDate() {
        return Date;
    }

    public DataActionType getAction() {
        return Action;
    }

    public String getNotes() {
        return Notes;
    }

    public String getAppVersion() {
        return AppVersion;
    }

    public void updateAction(int action) {
        Action.setFlag(action);
        Date = DateTime.now();
    }

    public void updateAction(int action, String notes) {
        updateAction(action);

        if (Notes == null)
            Notes = notes;
        else
            Notes = String.format("%s | %s", Notes, notes);
    }
}
