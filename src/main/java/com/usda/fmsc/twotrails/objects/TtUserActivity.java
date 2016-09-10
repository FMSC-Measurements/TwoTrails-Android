package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtUserActivity {
    private String UserName;
    private String DeviceName;
    private DateTime Date;
    private DataActivityType Activity;

    public TtUserActivity(String userName, String deviceName) {
        this(userName, deviceName, DateTime.now(), new DataActivityType());
    }

    public TtUserActivity(String userName, String deviceName, DateTime date, DataActivityType activity) {
        if (StringEx.isEmpty(userName))
            throw new RuntimeException("Invalid UserName");

        if (StringEx.isEmpty(deviceName))
            throw new RuntimeException("Invalid Device");

        UserName = userName;
        DeviceName = deviceName;
        Date = date;
        Activity = activity;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getDeviceName() {
        return DeviceName;
    }

    public void setDeviceName(String deviceName) {
        DeviceName = deviceName;
    }

    public DateTime getDate() {
        return Date;
    }

    public void setDate(DateTime date) {
        Date = date;
    }

    public DataActivityType getActivity() {
        return Activity;
    }

    public void setActivity(DataActivityType activity) {
        Activity = activity;
    }

    public void updateActivity(int activity) {
        Activity.setFlag(activity);
    }
}
