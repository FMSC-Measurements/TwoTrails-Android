package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtUserAction {
    private String UserName;
    private String DeviceName;
    private DateTime Date;
    private DataActionType Action;
    private String Notes;

    public TtUserAction(String userName, String deviceName) {
        this(userName, deviceName, DateTime.now(), new DataActionType(), StringEx.Empty);
    }

    public TtUserAction(String userName, String deviceName, DateTime date, DataActionType action, String notes) {
        if (StringEx.isEmpty(userName))
            throw new RuntimeException("Invalid UserName");

        if (StringEx.isEmpty(deviceName))
            throw new RuntimeException("Invalid Device");

        UserName = userName;
        DeviceName = deviceName;
        Date = date;
        Action = action;
        Notes = notes;
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

    public void updateAction(int action) {
        Action.setFlag(action);
        Date = DateTime.now();
    }

    public void updateAction(int action, String notes) {
        updateAction(action);

        if (Notes == null)
            Notes = notes;
        else
            Notes = StringEx.format("%s|%s", Notes, notes);
    }
}
