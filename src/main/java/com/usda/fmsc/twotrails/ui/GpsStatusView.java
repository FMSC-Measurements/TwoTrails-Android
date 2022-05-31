package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.NmeaIDs;
import com.usda.fmsc.geospatial.nmea41.Satellite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GpsStatusView extends View {
    public static final long SATELLITE_VISIBILITY_TIMEOUT = 60000; //60 sec
    public static final long SBAS_VISIBILITY_TIMEOUT = 120000; //120 sec

    private List<Integer> usedSats;
    private final ConcurrentHashMap<Integer, Satellite> satellites;
    private final ConcurrentHashMap<Integer, Boolean> satellitesVisibility;
    private final ConcurrentHashMap<Integer, Boolean> satellitesUsed;
    private final ConcurrentHashMap<Integer, Boolean> satellitesLocValid;
    private final ConcurrentHashMap<Integer, Boolean> satellitesSrnValid;
    private final ConcurrentHashMap<Integer, Long> satellitesLastSeen;

    private int satsUsedCount, satsVisCount, satsTrackedCount, satValidLocCount, satValidSrnCount;


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
        satellitesLocValid = new ConcurrentHashMap<>();
        satellitesSrnValid = new ConcurrentHashMap<>();
        satellites = new ConcurrentHashMap<>();
        satellitesVisibility = new ConcurrentHashMap<>();
        satellitesLastSeen = new ConcurrentHashMap<>();

        satsUsedCount = satsVisCount = satsTrackedCount = satValidLocCount = 0;
    }

    public void update(NmeaBurst burst) {
        final long now = System.currentTimeMillis();

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

            for (Satellite sat : satellites.values()) {
                if (!sat.isSBAS() || (now - satellitesLastSeen.get(sat.getNmeaID())) > 90000)
                    satellitesVisibility.put(sat.getNmeaID(), false);
                satellitesUsed.put(sat.getNmeaID(), false);
            }

            int nid;
            for (Satellite sat : burst.getSatellitesInView()) {
                nid = sat.getNmeaID();

                if (nid != 0) {
                    satellites.put(nid, sat);
                    satellitesLastSeen.put(nid, now);
                    satellitesVisibility.put(nid, true);
                    satellitesUsed.put(nid, usedSats.contains(nid));

                    boolean locValid = sat.getAzimuth() != null && sat.getElevation() != null;
                    boolean locWasValid = satellitesLocValid.containsKey(nid) && satellitesLocValid.get(nid);
                    boolean srnValid = sat.getSRN() != null;
                    boolean srnWasValid = satellitesSrnValid.containsKey(nid) && satellitesSrnValid.get(nid);

                    if (!locWasValid && locValid)
                        satValidLocCount++;
                    else if (locWasValid && !locValid)
                        satValidLocCount--;

                    if (!srnWasValid && srnValid)
                        satValidSrnCount++;
                    else if (srnWasValid && !srnValid)
                        satValidSrnCount--;

                    satellitesLocValid.put(nid, locValid);
                    satellitesSrnValid.put(nid, srnValid);
                }
            }
        }

        for (Satellite sat : satellites.values()) {
            int id = sat.getNmeaID();
            if (now - satellitesLastSeen.get(id) > (sat.isSBAS() ? SBAS_VISIBILITY_TIMEOUT : SATELLITE_VISIBILITY_TIMEOUT)) {
                satellites.remove(id);
                satellitesLastSeen.remove(id);
                satellitesVisibility.remove(id);
                satellitesUsed.remove(id);

                if (satellitesLocValid.get(id)) {
                    satValidLocCount--;
                }

                if (satellitesSrnValid.get(id)) {
                    satValidSrnCount--;
                }

                satellitesLocValid.remove(id);
                satellitesSrnValid.remove(id);
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

    public ConcurrentHashMap<Integer, Boolean> getSatellitesLocValid() {
        return satellitesLocValid;
    }

    public ConcurrentHashMap<Integer, Boolean> getSatellitesSrnValid() {
        return satellitesSrnValid;
    }

    public int getValidLocSatelliteCount() {
        return satValidLocCount;
    }

    public int getValidSrnSatelliteCount() {
        return satValidSrnCount;
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
