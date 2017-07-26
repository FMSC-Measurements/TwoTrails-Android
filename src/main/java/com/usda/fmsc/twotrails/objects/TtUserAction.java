package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtUserAction {
    private String UserName;
    private String DeviceName;
    private DateTime Date;
    private DataActionType Action;

    public TtUserAction(String userName, String deviceName) {
        this(userName, deviceName, DateTime.now(), new DataActionType());
    }

    public TtUserAction(String userName, String deviceName, DateTime date, DataActionType action) {
        if (StringEx.isEmpty(userName))
            throw new RuntimeException("Invalid UserName");

        if (StringEx.isEmpty(deviceName))
            throw new RuntimeException("Invalid Device");

        UserName = userName;
        DeviceName = deviceName;
        Date = date;
        Action = action;
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

    public void updateAction(int action) {
        Action.setFlag(action);
        Date = DateTime.now();
    }
}
