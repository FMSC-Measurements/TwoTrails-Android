package com.usda.fmsc.twotrails.gps;

public class GpsSyncer {



    boolean gpsSynced = false, syncing = false, discarding = false;
    long timeStart, timeStop;
    byte collectCount;

    String startString;
    Long longestInterval = 0L;


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
                timeStop = System.currentTimeMillis();

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
