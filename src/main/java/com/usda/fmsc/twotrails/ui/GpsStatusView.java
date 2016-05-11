package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.NmeaIDs;
import com.usda.fmsc.geospatial.nmea.Satellite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GpsStatusView extends View {
    public static final long SATELLITE_VISIBILITY_TIMEOUT = 60000; //60 sec

    private List<Integer> usedSats;
    private ConcurrentHashMap<Integer, Satellite> satellites;
    private ConcurrentHashMap<Integer, Boolean> satellitesVisibility;
    private ConcurrentHashMap<Integer, Boolean> satellitesUsed;
    private ConcurrentHashMap<Integer, Boolean> satellitesValid;
    private ConcurrentHashMap<Integer, Long> satellitesLastSeen;

    private int satsUsedCount, satsVisCount, satsTrackedCount, satValidCount;


    public GpsStatusView(Context context) {
        this(context, null, 0);
    }

    public GpsStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GpsStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        usedSats = new ArrayList<>();
        satellitesUsed = new ConcurrentHashMap<>();
        satellitesValid = new ConcurrentHashMap<>();
        satellites = new ConcurrentHashMap<>();
        satellitesVisibility = new ConcurrentHashMap<>();
        satellitesLastSeen = new ConcurrentHashMap<>();

        satsUsedCount = satsVisCount = satsTrackedCount = satValidCount = 0;
    }

    public void update(INmeaBurst burst) {
        if (burst.isValid(NmeaIDs.SentenceID.GSV)) {
            satsVisCount = burst.getSatellitesInViewCount();
            satsTrackedCount = burst.isValid(NmeaIDs.SentenceID.GGA) ? burst.getTrackedSatellitesCount() : 0;

            satellitesUsed.clear();
            usedSats.clear();

            if (burst.isValid(NmeaIDs.SentenceID.GSA)) {
                usedSats = burst.getUsedSatelliteIDs();
                satsUsedCount = burst.getUsedSatellitesCount();
            } else {
                satsUsedCount = 0;
            }

            for (Integer id : satellitesVisibility.keySet()) {
                satellitesVisibility.put(id, false);
                satellitesUsed.put(id, false);
            }

            int nid;
            for (Satellite sat : burst.getSatellitesInView()) {
                nid = sat.getNmeaID();
                if (nid != 0) {
                    satellites.put(nid, sat);
                    satellitesLastSeen.put(nid, System.currentTimeMillis());
                    satellitesVisibility.put(nid, true);
                    satellitesUsed.put(nid, usedSats.contains(nid));

                    boolean valid = sat.getAzimuth() != null && sat.getElevation() != null;
                    boolean wasValid = satellitesValid.containsKey(nid) && satellitesValid.get(nid);

                    if (!wasValid && valid) {
                        satValidCount++;
                    }else if (wasValid && !valid) {
                        satValidCount--;
                    }

                    satellitesValid.put(nid, valid);
                }
            }
        }

        long now = System.currentTimeMillis();
        Set<Integer> nids = satellites.keySet();
        for (Integer id : nids) {
            if (now - satellitesLastSeen.get(id) > SATELLITE_VISIBILITY_TIMEOUT) {
                satellites.remove(id);
                satellitesLastSeen.remove(id);
                satellitesVisibility.remove(id);
                satellitesUsed.remove(id);

                if (satellitesValid.get(id)) {
                    satValidCount--;
                }

                satellitesValid.remove(id);
            }
        }

        invalidate();
    }


    public ConcurrentHashMap<Integer, Satellite> getSatellites() {
        return satellites;
    }

    public ConcurrentHashMap<Integer, Boolean> getSatellitesUsed() {
        return satellitesUsed;
    }

    public ConcurrentHashMap<Integer, Boolean> getSatellitesVisibility() {
        return satellitesVisibility;
    }

    public ConcurrentHashMap<Integer, Boolean> getSatellitesValid() {
        return satellitesValid;
    }

    public int getValidSatelliteCount() {
        return satValidCount;
    }

    public int getUsedSatelliteCount() {
        return satsUsedCount;
    }

    public int getTrackedSatelliteCount() {
        return satsTrackedCount;
    }

    public int getVisibleSatelliteCount() {
        return satsVisCount;
    }
}
