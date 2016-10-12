package com.usda.fmsc.twotrails.gps;

public class GpsSyncer {
    private boolean gpsSynced = false, syncing = false, discarding = false;
    private long timeStart;
    private byte collectCount;

    private String startString;
    private Long longestInterval = 0L;


    public GpsSyncer() {
        reset();
    }

    public boolean sync(String nmea) {
        if (!syncing) {
            syncing = true;
            timeStart = System.currentTimeMillis();
        } else {
            if (discarding) {
                if (nmea.substring(0, nmea.indexOf(',')).equals(startString)) {
                    gpsSynced = true;
                    syncing = false;
                }
            } else {
                long timeStop = System.currentTimeMillis();

                long timeElapsed = timeStop - timeStart;

                if (timeElapsed > longestInterval) {
                    longestInterval = timeElapsed;
                    startString = nmea.substring(0, nmea.indexOf(','));
                }

                collectCount++;

                if (collectCount > 20) {
                    discarding = true;
                }

                timeStart = System.currentTimeMillis();
            }
        }

        return gpsSynced;
    }

    public boolean isSynced() {
        return gpsSynced;
    }

    public boolean isSyncing() {
        return syncing;
    }

    public void reset() {
        gpsSynced = false;
        discarding = false;
        syncing = false;
        collectCount = 0;
    }
}
