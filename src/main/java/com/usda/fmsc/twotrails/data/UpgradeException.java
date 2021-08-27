package com.usda.fmsc.twotrails.data;


public class UpgradeException extends RuntimeException {
    private final TtVersion upgradingVersion;

    public UpgradeException(TtVersion upgradingVersion, String message, Throwable cause, StackTraceElement[] trace) {
        super(message, cause);
        if (trace != null) setStackTrace(trace);
        this.upgradingVersion = upgradingVersion;
    }

    public TtVersion getUpgradingVersion() {
        return upgradingVersion;
    }
}
