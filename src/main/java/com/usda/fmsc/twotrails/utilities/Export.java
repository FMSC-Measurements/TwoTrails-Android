package com.usda.fmsc.twotrails.utilities;

import android.os.AsyncTask;

import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.utilities.gpx.GpxDocument;
import com.usda.fmsc.utilities.gpx.GpxMetadata;
import com.usda.fmsc.utilities.gpx.GpxPoint;
import com.usda.fmsc.utilities.gpx.GpxRoute;
import com.usda.fmsc.utilities.gpx.GpxTrack;
import com.usda.fmsc.utilities.gpx.GpxTrackSeg;
import com.usda.fmsc.utilities.gpx.GpxWriter;
import com.usda.fmsc.utilities.kml.*;
import com.usda.fmsc.utilities.kml.Types.*;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.logic.HaidLogic;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.utilities.StringEx;

import com.usda.fmsc.twotrails.units.OpType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.DateTime;

public class Export {
    public static String points(DataAccessLayer dal, String dir) {
        String pointsFilename = String.format("%s/points.csv", dir);
        String groupsFilename = String.format("%s/groups.csv", dir);

        try {
            HashMap<String, TtMetadata> metadata = dal.getMetadataMap();

            //region Point Headers
            CSVPrinter printer = new CSVPrinter(new FileWriter(pointsFilename), CSVFormat.DEFAULT);

            printer.printRecord(
                    "Point ID",
                    "OpType",
                    "Index",
                    "Polygon",
                    "Group",
                    "DateTime",
                    "Metadata",
                    "OnBound",
                    "AdjX",
                    "AdjY",
                    "AdjZ",
                    "UnAdjX",
                    "UnAdjY",
                    "UnAdjZ",
                    "Accuracy",
                    "Man Acc (M)",
                    "Latitude",
                    "Longitude",
                    "Elevation (M)",
                    "RMSEr",
                    "Fwd Az",
                    "Bk Az",
                    "Horiz Dist (M)",
                    "Slope Dist",
                    "Slope D Type",
                    "Slope Angle",
                    "Slope A Type",
                    "Parent",
                    "Comment",
                    "Point CN",
                    "Poly CN",
                    "Metadata CN",
                    "Group CN",
                    "Parent CN",
                    "Linked CNs"
            );
            //endregion

            //region Point Values
            ArrayList<String> values = new ArrayList<>(33);
            TtMetadata tMeta;

            for (TtPoint point : dal.getPoints()) {
                
                if (metadata.containsKey(point.getMetadataCN())) {
                    tMeta = metadata.get(point.getMetadataCN());
                } else {
                    throw new RuntimeException("Invalid Point Metadata");
                }

                values.add(StringEx.toString(point.getPID()));
                values.add(point.getOp().toString());
                values.add(Long.toString(point.getIndex()));
                values.add(point.getPolyName());
                values.add(point.getGroupName());
                values.add(Consts.DateTimeFormatter.print(point.getTime()));
                values.add(tMeta.getName());
                values.add(Boolean.toString(point.isOnBnd()));

                values.add(StringEx.toString(point.getAdjX()));
                values.add(StringEx.toString(point.getAdjY()));
                values.add(StringEx.toString(point.getAdjZ()));
                values.add(StringEx.toString(point.getUnAdjX()));
                values.add(StringEx.toString(point.getUnAdjY()));
                values.add(StringEx.toString(point.getUnAdjZ()));

                values.add(StringEx.toString(point.getAccuracy()));

                if (point instanceof TtPoint.IManualAccuracy) {
                    values.add(StringEx.toString(((TtPoint.IManualAccuracy) point).getManualAccuracy()));
                } else {
                    values.add(StringEx.Empty);
                }

                if (point instanceof GpsPoint) {
                    GpsPoint gps = (GpsPoint)point;

                    values.add(StringEx.toString(gps.getLatitude()));
                    values.add(StringEx.toString(gps.getLongitude()));
                    values.add(StringEx.toString(gps.getElevation()));
                    values.add(StringEx.toString(gps.getRMSEr()));
                } else {
                    for (int i = 0; i < 4; i++) {
                        values.add(StringEx.Empty);
                    }
                }

                if (point instanceof TravPoint) {
                    TravPoint trav = (TravPoint)point;

                    values.add(StringEx.toString(trav.getFwdAz()));
                    values.add(StringEx.toString(trav.getBkAz()));
                    values.add(StringEx.toString(trav.getHorizontalDistance()));
                    
                    values.add(StringEx.toString(
                            TtUtils.Convert.distance(
                                    trav.getSlopeDistance(),
                                    tMeta.getDistance(),
                                    Dist.Meters
                            )
                    ));
                    values.add(tMeta.getDistance().toString());

                    values.add(StringEx.toString(
                            TtUtils.Convert.angle(
                                    trav.getSlopeAngle(),
                                    tMeta.getSlope(),
                                    Slope.Percent
                            )
                    ));
                    values.add(tMeta.getSlope().toString());
                } else {
                    for (int i = 0; i < 7; i++) {
                        values.add(StringEx.Empty);
                    }
                }

                QuondamPoint qp = null;
                if (point instanceof QuondamPoint) {
                    qp = (QuondamPoint)point;
                    
                    if (qp.hasParent()) {
                        values.add(StringEx.toString(qp.getParentPID()));
                    } else {
                        values.add(StringEx.Empty);
                    }
                } else {
                    values.add(StringEx.Empty);
                }

                values.add(point.getComment());
                values.add(point.getCN());
                values.add(point.getPolyCN());
                values.add(point.getMetadataCN());
                values.add(point.getGroupCN());

                if (qp != null && qp.hasParent()) {
                    values.add(qp.getParentCN());
                } else {
                    values.add(StringEx.Empty);
                }

                if (point.hasQuondamLinks()) {
                    values.add(point.getLinkedPointsString());
                } else {
                    values.add(StringEx.Empty);
                }

                printer.printRecord(values);
                values.clear();
            }

            printer.close();
            //endregion

            //region Groups
            printer = new CSVPrinter(new FileWriter(groupsFilename), CSVFormat.DEFAULT);

            printer.printRecord(
                    "Name",
                    "Type",
                    "Description",
                    "CN"
            );


            values = new ArrayList<>(4);

            for (TtGroup group : dal.getGroups()) {
                values.add(group.getName());
                values.add(group.getGroupType().toString());
                values.add(group.getDescription());
                values.add(group.getCN());

                printer.printRecord(values);
                values.clear();
            }

            printer.close();
            //endregion
        } catch (Exception ex) {
            pointsFilename = null;
            TtUtils.TtReport.writeError(ex.getMessage(), "Export:points", ex.getStackTrace());
        }

        return pointsFilename;
    }

    public static String polygons(DataAccessLayer dal, String dir) {
        String polysFilename = String.format("%s/polygons.csv", dir);

        try {
            CSVPrinter printer = new CSVPrinter(new FileWriter(polysFilename), CSVFormat.DEFAULT);

            printer.printRecord(
                    "Name",
                    "Accuracy (M)",
                    "Area (Ac)",
                    "Perimeter (Ft)",
                    "CN"
            );

            ArrayList<String> values = new ArrayList<>(5);

            for (TtPolygon poly : dal.getPolygons()) {
                values.add(poly.getName());
                values.add(StringEx.toString(poly.getAccuracy()));

                values.add(StringEx.toString(
                        TtUtils.Convert.metersSquaredToAcres(poly.getArea())));

                values.add(StringEx.toString(
                        TtUtils.Convert.distance(poly.getPerimeter(),
                                Dist.FeetTenths,
                                Dist.Meters)
                ));

                values.add(poly.getCN());

                printer.printRecord(values);
                values.clear();
            }

            printer.close();
        } catch (Exception ex) {
            polysFilename = null;
            TtUtils.TtReport.writeError(ex.getMessage(), "Export:polygons", ex.getStackTrace());
        }

        return polysFilename;
    }

    public static String metadata(DataAccessLayer dal, String dir) {
        String metaFilename = String.format("%s/metadata.csv", dir);

        try {
            CSVPrinter writer = new CSVPrinter(new FileWriter(metaFilename), CSVFormat.DEFAULT);

            writer.printRecord(
                    "Name",
                    "Zone",
                    "Datum",
                    "Elevation",
                    "Distance",
                    "Slope Angle",
                    "Declination",
                    "Dec Type",
                    "GPS",
                    "Range Finder",
                    "Compass",
                    "Crew",
                    "Comment",
                    "CN"
            );

            ArrayList<String> values = new ArrayList<>(14);

            for (TtMetadata meta : dal.getMetadata()) {
                values.add(meta.getName());
                values.add(StringEx.toString(meta.getZone()));
                values.add(meta.getDatum().toString());
                values.add(meta.getElevation().toString());
                values.add(meta.getDistance().toString());
                values.add(meta.getSlope().toString());
                values.add(StringEx.toString(meta.getMagDec()));
                values.add(meta.getDecType().toString());
                values.add(meta.getGpsReceiver());
                values.add(meta.getRangeFinder());
                values.add(meta.getCompass());
                values.add(meta.getCrew());
                values.add(meta.getComment());
                values.add(meta.getCN());

                writer.printRecord(values);
                values.clear();
            }

            writer.close();
        } catch (Exception ex) {
            metaFilename = null;
            TtUtils.TtReport.writeError(ex.getMessage(), "Export:metadata", ex.getStackTrace());
        }

        return metaFilename;
    }

    public static String project(DataAccessLayer dal, String dir) {
        String projFilename = String.format("%s/project.csv", dir);

        try {
            CSVPrinter writer = new CSVPrinter(new FileWriter(projFilename), CSVFormat.DEFAULT);

            writer.printRecord(
                    "Project Name",
                    "Region",
                    "Forest",
                    "District",
                    "Created",
                    "Description",
                    "Device",
                    "TtDbVersion",
                    "TtVersion",
                    "CreatedTtVersion"
            );

            writer.printRecord(
                    dal.getProjectID(),
                    dal.getProjectRegion(),
                    dal.getProjectForest(),
                    dal.getProjectDistrict(),
                    dal.getProjectDateCreated(),
                    dal.getProjectDescription(),
                    dal.getProjectDeviceID(),
                    dal.getTtDbVersion(),
                    dal.getProjectTtVersion(),
                    dal.getProjectCreatedTtVersion()
            );

            writer.close();
        } catch (Exception ex) {
            projFilename = null;
            TtUtils.TtReport.writeError(ex.getMessage(), "Export:metadata", ex.getStackTrace());
        }

        return projFilename;
    }

    public static String nmea(DataAccessLayer dal, String dir) {
        String nmeaFilename = String.format("%s/nmea.csv", dir);
        try {
            //region NMEA Headers
            CSVPrinter writer = new CSVPrinter(new FileWriter(nmeaFilename), CSVFormat.DEFAULT);

            writer.printRecord(
                    "Point CN",
                    "Used",
                    "Time Created",
                    "Time Fix",
                    "Latitude",
                    "Longitude",
                    "Elevation (Mt)",
                    "Ground Speed (knots)",
                    "Track Angle (deg true)",
                    "Mag Var",
                    "Mag Var Dir",
                    "Mode",
                    "Fix",
                    "PDOP",
                    "HDOP",
                    "VDOP",
                    "Fix Quality",
                    "Horiz Dilution",
                    "Geoid Height (Mt)",
                    "Tracked Satellites",
                    "Satellites In View",
                    "Satellites Used Count",
                    "Satellites Used"
            );
            //endregion

            //region NMEA Values
            ArrayList<String> values = new ArrayList<>(23);

            for (TtNmeaBurst burst : dal.getNmeaBursts()) {
                values.add(burst.getPointCN());
                values.add(Boolean.toString(burst.isUsed()));
                values.add(Consts.DateTimeFormatter.print(burst.getTimeCreated()));
                values.add(burst.getFixTime() == null ? StringEx.Empty : Consts.DateTimeFormatter.print(burst.getFixTime()));
                values.add(StringEx.toString(burst.getLatitude()));
                values.add(StringEx.toString(burst.getLongitude()));
                values.add(StringEx.toString(burst.getElevation()));
                values.add(StringEx.toString(burst.getGroundSpeed()));
                values.add(StringEx.toString(burst.getTrackAngle()));
                values.add(StringEx.toString(burst.getMagVar()));
                values.add(burst.getMagVarDir() == null ? StringEx.Empty : burst.getMagVarDir().toString());
                values.add(burst.getMode() == null ? StringEx.Empty : burst.getMode().toString());
                values.add(burst.getFix() == null ? StringEx.Empty : burst.getFix().toString());
                values.add(StringEx.toString(burst.getPDOP()));
                values.add(StringEx.toString(burst.getHDOP()));
                values.add(StringEx.toString(burst.getVDOP()));
                values.add(burst.getFixQuality() == null ? StringEx.Empty : burst.getFixQuality().toString());
                values.add(StringEx.toString(burst.getHorizDilution()));
                values.add(StringEx.toString(burst.getGeoidHeight()));
                values.add(StringEx.toString(burst.getTrackedSatellitesCount()));
                values.add(StringEx.toString(burst.getSatellitesInViewCount()));
                values.add(StringEx.toString(burst.getUsedSatellitesCount()));
                values.add(burst.getUsedSatelliteIDsString());

                writer.printRecord(values);
                values.clear();
            }

            writer.close();
            //endregion
        } catch (Exception ex) {
            nmeaFilename = null;
            TtUtils.TtReport.writeError(ex.getMessage(), "Export:nmea", ex.getStackTrace());
        }

        return nmeaFilename;
    }

    public static String gpx(DataAccessLayer dal, String dir) {
        String projName = dal.getProjectID();
        String gpxPath = String.format("%s/%s.gpx", dir, scrubProjectName(projName));

        GpxDocument doc = new GpxDocument();
        doc.setCreator(String.format("TwoTrails: %s", TtUtils.getDeviceName()));

        GpxMetadata gpxMeta = new GpxMetadata();
        gpxMeta.setTime(DateTime.now());
        gpxMeta.setName(projName);
        gpxMeta.setLink("http://www.fs.fed.us/fmsc/measure/geospatial/twotrails/");

        doc.setMetaData(gpxMeta);

        //region Create Polygons

        List<TtPolygon> polys = dal.getPolygons();
        HashMap<String, TtMetadata> meta = dal.getMetadataMap();
        TtMetadata tmpMeta;

        for (TtPolygon poly : polys) {
            GpxRoute AdjRoute = new GpxRoute(String.format("%s - Adj Boundary", poly.getName()), poly.getDescription());
            GpxTrack AdjTrack = new GpxTrack(String.format("%s - Adj Navigation", poly.getName()), poly.getDescription());

            GpxRoute UnAdjRoute = new GpxRoute(String.format("%s - UnAdj Boundary", poly.getName()), poly.getDescription());
            GpxTrack UnAdjTrack = new GpxTrack(String.format("%s - UnAdj Navigation", poly.getName()), poly.getDescription());

            AdjTrack.Segments.add(new GpxTrackSeg());
            UnAdjTrack.Segments.add(new GpxTrackSeg());

            List<TtPoint> points = dal.getPointsInPolygon(poly.getCN());

            if (points.size() > 0) {
                for (TtPoint point : points) {
                    GpxPoint adjpoint = null, unAdjpoint = null;
                    tmpMeta = meta.get(point.getMetadataCN());

                    if (point instanceof GpsPoint) {
                        GpsPoint gps = (GpsPoint)point;

                        if (gps.hasLatLon()) {
                            adjpoint = new GpxPoint(gps.getLatitude(), gps.getLongitude(), gps.getElevation());
                            unAdjpoint = new GpxPoint(gps.getLatitude(), gps.getLongitude(), gps.getElevation());
                        }
                    }

                    if (adjpoint == null) {
                        GeoPosition pos = UTMTools.convertUTMtoLatLonSignedDec(point.getAdjX(), point.getAdjY(), tmpMeta.getZone());
                        adjpoint = new GpxPoint(pos.getLatitudeSignedDecimal(), pos.getLongitudeSignedDecimal(), point.getAdjZ());

                        pos = UTMTools.convertUTMtoLatLonSignedDec(point.getUnAdjX(), point.getUnAdjY(), tmpMeta.getZone());
                        unAdjpoint = new GpxPoint(pos.getLatitudeSignedDecimal(), pos.getLongitudeSignedDecimal(), point.getUnAdjZ());
                    }

                    adjpoint.setName(StringEx.toString(point.getPID()));
                    adjpoint.setTime(point.getTime());
                    adjpoint.setComment(point.getComment());
                    adjpoint.setDescription(String.format("Point Operation: %s<br>UtmX: %s<br>UtmY: %s", point.getOp().toString(), point.getAdjX(), point.getAdjY()));

                    unAdjpoint.setName(StringEx.toString(point.getPID()));
                    unAdjpoint.setTime(point.getTime());
                    unAdjpoint.setComment(point.getComment());
                    unAdjpoint.setDescription(String.format("Point Operation: %s<br>UtmX: %s<br>UtmY: %s", point.getOp().toString(), point.getUnAdjX(), point.getUnAdjY()));

                    //region Add points to lists
                    if (point.isBndPoint()) {
                        AdjRoute.addPoint(adjpoint);
                        UnAdjRoute.addPoint(unAdjpoint);
                    }

                    if (point.isNavPoint()) {
                        AdjTrack.getSegments().get(0).addPoint(adjpoint);
                        UnAdjTrack.getSegments().get(0).addPoint(unAdjpoint);
                    } else if (point instanceof QuondamPoint) {
                        QuondamPoint qp = (QuondamPoint)point;

                        if (qp.isNavPoint()) {
                            AdjTrack.getSegments().get(0).addPoint(adjpoint);
                            UnAdjTrack.getSegments().get(0).addPoint(unAdjpoint);
                        }
                    }

                    if (point.getOp() == OpType.WayPoint) {
                        doc.addPoint(unAdjpoint);
                    }
                    //endregion
                }
            }

            doc.addRoute(AdjRoute);
            doc.addRoute(UnAdjRoute);
            doc.addTrack(AdjTrack);
            doc.addTrack(UnAdjTrack);
        }
        //endregion

        try {
            GpxWriter.createFile(doc, gpxPath);
        } catch (IOException e) {
            gpxPath = null;
            TtUtils.TtReport.writeError(e.getMessage(), "Export:gpx");
        }

        return gpxPath;
    }

    public static String kml(DataAccessLayer dal, String dir) {
        String projName = dal.getProjectID();
        String kmlPath = String.format("%s/%s.kml", dir, scrubProjectName(projName));

        //region Create Document
        KmlDocument doc = new KmlDocument(projName, dal.getProjectDescription());

        Properties properties = new Properties();
        properties.setSnippit(String.format("Generated in TwoTrails: %s", TtUtils.getDeviceName()));

        doc.setOpen(true);
        doc.setProperties(properties);
        doc.setVisible(true);
        //endregion

        //region Create Styles / StyleMaps
        final double adjLineSize = 5;
        final double unadjLineSize = 7;

        Style sAdjBound = new Style("AdjBound");
        Style sUnAdjBound = new Style("UnAdjBound");
        Style sAdjNav = new Style("AdjNav");
        Style sUnAdjNav = new Style("UnAdjNav");
        Style sAdjMisc = new Style("AdjMisc");
        Style sUnAdjMisc = new Style("UnAdjMisc");
        Style sWay = new Style("Way");

        Color AdjBoundColor = new Color(27, 211, 224, 255);   //1BD3E0
        Color UnAdjBoundColor = new Color(27, 112, 224, 255); //1B70E0
        Color AdjNavColor = new Color(27, 224, 142, 255);     //1BE08E
        Color UnAdjNavColor = new Color(14, 168, 86, 255);    //0E8A56
        Color AdjMiscColor = new Color(234, 90, 250, 255);    //EA5AFA
        Color UnAdjMiscColor = new Color(164, 29, 179, 255);  //A41DB3
        Color WayColor = new Color(255, 0, 0, 255);           //FF0000
        Color KmlWhite = new Color(255, 255, 255, 255);       //FFFFFF

        sAdjBound.setColorsILP(AdjBoundColor);
        sAdjBound.setIconScale(1d);
        sAdjBound.setLineWidth(adjLineSize);
        sAdjBound.setLineLabelVisibility(true);
        sAdjBound.setPolygonFill(false);
        sAdjBound.setPolygonOutline(true);
        sAdjBound.setBalloonDisplayMode(DisplayMode.Default);

        sUnAdjBound.setColorsILP(UnAdjBoundColor);
        sUnAdjBound.setIconScale(1d);
        sUnAdjBound.setLineWidth(unadjLineSize);
        sUnAdjBound.setLineLabelVisibility(false);
        sUnAdjBound.setPolygonFill(false);
        sUnAdjBound.setPolygonOutline(true);
        sUnAdjBound.setBalloonDisplayMode(DisplayMode.Default);

        sAdjNav.setColorsILP(AdjNavColor);
        sAdjNav.setIconScale(1d);
        sAdjNav.setLineWidth(adjLineSize);
        sAdjNav.setLineLabelVisibility(false);
        sAdjNav.setPolygonFill(false);
        sAdjNav.setPolygonOutline(true);
        sAdjNav.setBalloonDisplayMode(DisplayMode.Default);

        sUnAdjNav.setColorsILP(UnAdjNavColor);
        sUnAdjNav.setIconScale(1d);
        sUnAdjNav.setLineWidth(unadjLineSize);
        sUnAdjNav.setLineLabelVisibility(false);
        sUnAdjNav.setPolygonFill(false);
        sUnAdjNav.setPolygonOutline(true);
        sUnAdjNav.setBalloonDisplayMode(DisplayMode.Default);

        sUnAdjMisc.setIconColorMode(ColorMode.normal);
        sUnAdjMisc.setIconColor(AdjMiscColor);
        sUnAdjMisc.setIconScale(1d);
        sUnAdjMisc.setBalloonDisplayMode(DisplayMode.Default);

        sWay.setIconColorMode(ColorMode.normal);
        sWay.setIconColor(WayColor);
        sWay.setIconScale(1d);
        sWay.setBalloonDisplayMode(DisplayMode.Default);

        Style sAdjBoundH = new Style(sAdjBound, "AdjBoundH");
        Style sUnAdjBoundH = new Style(sUnAdjBound, "UnAdjBoundH");
        Style sAdjNavH = new Style(sAdjNav, "AdjNavH");
        Style sUnAdjNavH = new Style(sUnAdjNav, "UnAdjNavH");
        Style sAdjMiscH = new Style(sAdjMisc, "AdjMiscH");
        Style sUnAdjMiscH = new Style(sUnAdjMisc, "UnAdjMiscH");
        Style sWayH = new Style(sWay, "WayH");

        sAdjBoundH.setIconScale(1.1);
        sAdjBoundH.setIconColor(KmlWhite);
        sUnAdjBoundH.setIconScale(1.1);
        sUnAdjBoundH.setIconColor(KmlWhite);
        sAdjNavH.setIconScale(1.1);
        sAdjNavH.setIconColor(KmlWhite);
        sUnAdjNavH.setIconScale(1.1);
        sUnAdjNavH.setIconColor(KmlWhite);
        sAdjMiscH.setIconScale(1.1);
        sAdjMiscH.setIconColor(KmlWhite);
        sUnAdjMiscH.setIconScale(1.1);
        sUnAdjMiscH.setIconColor(KmlWhite);
        sWayH.setIconScale(1.1);
        sWayH.setIconColor(KmlWhite);

        doc.addStyle(sAdjBound);
        doc.addStyle(sAdjBoundH);
        doc.addStyle(sUnAdjBound);
        doc.addStyle(sUnAdjBoundH);
        doc.addStyle(sAdjNav);
        doc.addStyle(sAdjNavH);
        doc.addStyle(sUnAdjNav);
        doc.addStyle(sUnAdjNavH);
        doc.addStyle(sAdjMisc);
        doc.addStyle(sAdjMiscH);
        doc.addStyle(sUnAdjMisc);
        doc.addStyle(sUnAdjMiscH);
        doc.addStyle(sWay);
        doc.addStyle(sWayH);

        StyleMap sAdjBoundMap = new StyleMap("AdjBoundMap", sAdjBound.getStyleUrl(), sAdjBoundH.getStyleUrl());
        StyleMap sUnAdjBoundMap = new StyleMap("UnAdjBoundMap", sUnAdjBound.getStyleUrl(), sUnAdjBoundH.getStyleUrl());
        StyleMap sAdjNavMap = new StyleMap("AdjNavMap", sAdjNav.getStyleUrl(), sAdjNavH.getStyleUrl());
        StyleMap sUnAdjNavMap = new StyleMap("UnAdjNavMap", sUnAdjNav.getStyleUrl(), sUnAdjNavH.getStyleUrl());
        StyleMap sAdjMiscMap = new StyleMap("AdjMiscMap", sAdjMisc.getStyleUrl(), sAdjMiscH.getStyleUrl());
        StyleMap sUnAdjMiscMap = new StyleMap("UnAdjMiscMap", sUnAdjMisc.getStyleUrl(), sUnAdjMiscH.getStyleUrl());
        StyleMap sWayMap = new StyleMap("WayMap", sWay.getStyleUrl(), sWayH.getStyleUrl());

        doc.addStyleMap(sAdjBoundMap);
        doc.addStyleMap(sUnAdjBoundMap);
        doc.addStyleMap(sAdjNavMap);
        doc.addStyleMap(sUnAdjNavMap);
        doc.addStyleMap(sAdjMiscMap);
        doc.addStyleMap(sUnAdjMiscMap);
        doc.addStyleMap(sWayMap);
        //endregion

        //region Create Polygons
        ArrayList<TtPolygon> polys = dal.getPolygons();

        for (TtPolygon poly : polys) {
            ArrayList<TtPoint> points = dal.getPointsInPolygon(poly.getCN());

            //region Create root Polygon Folder
            Folder folder = new Folder(poly.getName(), poly.getDescription());

            folder.setOpen(false);
            folder.setVisibility(true);

            Properties prop = new Properties();
            prop.setSnippit(poly.getDescription());
            
            ExtendedData ed = new ExtendedData();
            ed.add(new ExtendedData.Data(" ", "In Meters"));
            ed.add(new ExtendedData.Data("Accuracy", StringEx.toString(poly.getAccuracy())));
            ed.add(new ExtendedData.Data("Perimeter", StringEx.toString(poly.getPerimeter())));
            ed.add(new ExtendedData.Data("Area", StringEx.toString(poly.getArea())));
            
            folder.setProperties(prop);
            //endregion

            //region Create SubFolders under Polygon root
            Folder fAdjBound = new Folder("AdjBound", "Adjusted Boundary Polygon");
            Folder fUnAdjBound = new Folder("UnAdjBound", "UnAdjusted Boundary Polygon");
            Folder fAdjNav = new Folder("AdjNav", "Adjusted Navigation Polygon");
            Folder fUnAdjNav = new Folder("UnAdjNav", "UnAdjusted Navigation Polygon");
            Folder fMiscPoints = new Folder("Misc", "Misc Points");
            Folder fWayPoints = new Folder("Waypoints", "Waypoints");

            fAdjBound.setStyleUrl(sAdjBoundMap.getStyleUrl());
            fUnAdjBound.setStyleUrl(sUnAdjBoundMap.getStyleUrl());
            fAdjNav.setStyleUrl(sAdjNavMap.getStyleUrl());
            fUnAdjNav.setStyleUrl(sUnAdjNavMap.getStyleUrl());
            fMiscPoints.setStyleUrl(sAdjMiscMap.getStyleUrl());
            fWayPoints.setStyleUrl(sWayMap.getStyleUrl());

            fAdjBound.setVisibility(true);
            fUnAdjBound.setVisibility(false);
            fAdjNav.setVisibility(false);
            fUnAdjNav.setVisibility(false);
            fMiscPoints.setVisibility(false);
            fWayPoints.setVisibility(false);

            fAdjBound.setOpen(false);
            fUnAdjBound.setOpen(false);
            fAdjNav.setOpen(false);
            fUnAdjNav.setOpen(false);
            fMiscPoints.setOpen(false);
            fWayPoints.setOpen(false);

            //endregion

            //region Create SubFolders for Bound, Nav and Misc
            Folder fAdjBoundPoints = new Folder("Points", "Adjusted Boundary Points");
            Folder fUnAdjBoundPoints = new Folder("Points", "UnAdjusted Boundary Points");
            Folder fAdjNavPoints = new Folder("Points", "Adjusted Navigation Points");
            Folder fUnAdjNavPoints = new Folder("Points", "UnAdjusted Navigation Points");
            Folder fAdjMiscPoints = new Folder("Adj Points", "Adjusted Misc Points");
            Folder fUnAdjMiscPoints = new Folder("UnAdj Points", "UnAdjusted Misc Points");

            fAdjBoundPoints.setVisibility(true);
            fUnAdjBoundPoints.setVisibility(false);
            fAdjNavPoints.setVisibility(false);
            fUnAdjNavPoints.setVisibility(false);
            fAdjMiscPoints.setVisibility(false);
            fUnAdjMiscPoints.setVisibility(false);

            fAdjBoundPoints.setOpen(false);
            fUnAdjBoundPoints.setOpen(false);
            fAdjNavPoints.setOpen(false);
            fUnAdjNavPoints.setOpen(false);
            fAdjMiscPoints.setOpen(false);
            fUnAdjMiscPoints.setOpen(false);
            //endregion

            //region Create Geometry
            Polygon AdjBoundPoly = new Polygon("AdjBoundPoly");
            Polygon UnAdjBoundPoly = new Polygon("UnAdjBoundPoly");
            ArrayList<Coordinates> AdjBoundPointList = new ArrayList<>();
            ArrayList<Coordinates> UnAdjBoundPointList = new ArrayList<>();

            Polygon AdjNavPoly = new Polygon("AdjNavPoly");
            Polygon UnAdjNavPoly = new Polygon("UnAdjNavPoly");
            ArrayList<Coordinates> AdjNavPointList = new ArrayList<>();
            ArrayList<Coordinates> UnAdjNavPointList = new ArrayList<>();

            AdjBoundPoly.setAltMode(AltitudeMode.clampToGround);
            UnAdjBoundPoly.setAltMode(AltitudeMode.clampToGround);
            AdjNavPoly.setAltMode(AltitudeMode.clampToGround);
            UnAdjNavPoly.setAltMode(AltitudeMode.clampToGround);

            AdjBoundPoly.setIsPath(false);
            UnAdjBoundPoly.setIsPath(false);
            AdjNavPoly.setIsPath(true);
            UnAdjNavPoly.setIsPath(true);

            //endregion

            //region Add Placemarks
            HashMap<String, TtMetadata> md;

            if (points.size() > 0) {
                md = dal.getMetadataMap();

                if (md == null)
                    throw new RuntimeException("Meta Data is null. Cant obtain UTM Zone");

                for (TtPoint point : points) {
                    GeoPosition coords = TtUtils.getLatLonFromPoint(point, true, md.get(point.getMetadataCN()));

                    Coordinates adjCoord = new Coordinates(coords.getLatitudeSignedDecimal(), coords.getLongitudeSignedDecimal(), coords.getElevation());

                    coords = TtUtils.getLatLonFromPoint(point, false, md.get(point.getMetadataCN()));
                    Coordinates unAdjCoord = new Coordinates(coords.getLatitudeSignedDecimal(), coords.getLongitudeSignedDecimal(), coords.getElevation());

                    String snippit = "Point Operation: " + point.getOp().toString();

                    //region Create Placemarks for Bound/Nav
                    Placemark adjPm = new Placemark(Integer.toString(point.getPID()),
                            String.format("Point Operation: %s<br><div>\t     Adjusted<br>UtmX: %f<br>UtmY: %f</div><br>%s",
                                    point.getOp().toString(), point.getAdjX(), point.getAdjY(),
                                    point.getComment() != null ? point.getComment() : ""),
                            new View());

                    View view = adjPm.getView();

                    view.setTimeStamp(point.getTime());
                    view.setCoordinates(adjCoord);
                    view.setTilt(15d);
                    view.setAltMode(AltitudeMode.clampToGround);
                    view.setRange(150d);

                    Properties pointProp = new Properties();
                    pointProp.setSnippit(snippit);

                    adjPm.setStyleUrl(sAdjBoundMap.getStyleUrl());
                    adjPm.setOpen(false);
                    adjPm.setVisibility(true);
                    adjPm.addPoint(new Point(adjCoord, AltitudeMode.clampToGround));


                    Placemark unAdjPm = new Placemark(Integer.toString(point.getPID()),
                            String.format("Point Operation: %s<br><div>     Unadjusted<br>UtmX: %f<br>UtmY: %f</div><br>%s",
                                    point.getOp().toString(), point.getUnAdjX(), point.getUnAdjY(), point.getComment() != null ? point.getComment() : ""),
                            new View());

                    view = unAdjPm.getView();
                    view.setTimeStamp(point.getTime());
                    view.setCoordinates(unAdjCoord);
                    view.setTilt(15d);
                    view.setAltMode(AltitudeMode.clampToGround);
                    view.setRange(150);

                    unAdjPm.setProperties(pointProp);

                    unAdjPm.setStyleUrl(sUnAdjBoundMap.getStyleUrl());
                    unAdjPm.setOpen(false);
                    unAdjPm.setVisibility(false);
                    unAdjPm.addPoint(new Point(unAdjCoord, AltitudeMode.clampToGround));

                    //endregion

                    //region Add points and placemarks to lists
                    if (point.isBndPoint()) {
                        AdjBoundPointList.add(adjCoord);
                        UnAdjBoundPointList.add(unAdjCoord);
                        fAdjBoundPoints.addPlacemark(new Placemark(adjPm));
                        fUnAdjBoundPoints.addPlacemark(new Placemark(unAdjPm));
                    }

                    unAdjPm.setStyleUrl(sUnAdjNavMap.getStyleUrl());
                    adjPm.setStyleUrl(sAdjNavMap.getStyleUrl());
                    adjPm.setVisibility(false);

                    if (point.isNavPoint()) {
                        AdjNavPointList.add(adjCoord);
                        UnAdjNavPointList.add(unAdjCoord);
                        fAdjNavPoints.addPlacemark(new Placemark(adjPm));
                        fUnAdjNavPoints.addPlacemark(new Placemark(unAdjPm));
                    }

                    //region Create Way / Misc point placemarks
                    unAdjPm.setStyleUrl(sWayMap.getStyleUrl());

                    if (point.getOp() == OpType.WayPoint) {
                        fWayPoints.addPlacemark(new Placemark(unAdjPm));
                    }

                    //endregion
                    //endregion
                }
            }


            //region Create Poly Placemarks

            //assign points to polys

            if(AdjBoundPointList.size() > 0)
                AdjBoundPointList.add(AdjBoundPointList.get(0));

            if(UnAdjBoundPointList.size() > 0)
                UnAdjBoundPointList.add(UnAdjBoundPointList.get(0));

            AdjBoundPoly.setOuterBoundary(AdjBoundPointList);
            UnAdjBoundPoly.setOuterBoundary(UnAdjBoundPointList);
            AdjNavPoly.setOuterBoundary(AdjNavPointList);
            UnAdjNavPoly.setOuterBoundary(UnAdjNavPointList);

            //get default data for the placemarks
            View.TimeSpan ts = null;
            if(points.size() > 0 )
                ts = new View.TimeSpan(points.get(0).getTime(), points.get(points.size() - 1).getTime());

            Polygon.Dimensions adjDim = AdjBoundPoly.getOuterDimensions();
            Polygon.Dimensions unAdjDim = UnAdjBoundPoly.getOuterDimensions();

            Double adjRange = null, unAdjRange = null, width;

            if (adjDim != null) {
                adjRange = TtUtils.Math.distanceLL(0, adjDim.North, 0, adjDim.South);
                width = TtUtils.Math.distanceLL(adjDim.East, 0, adjDim.West, 0);

                if (width > adjRange)
                    adjRange = width;
            }

            if (unAdjDim != null) {
                unAdjRange = TtUtils.Math.distanceLL(0, unAdjDim.North, 0, unAdjDim.South);
                width = TtUtils.Math.distanceLL(unAdjDim.East, 0, unAdjDim.West, 0);

                if (width > unAdjRange)
                    unAdjRange = width;
            }

            if (adjRange == null)
                adjRange = 1000d;
            else
                adjRange *= 1.1;
            
            if (unAdjRange == null)
                unAdjRange = 1000d;
            else
                unAdjRange *= 1.1;

            //AdjBoundPlacemark
            Placemark AdjBoundPlacemark = new Placemark("AdjBoundPoly", "Adjusted Boundary Polygon", new View());
            
            View view = AdjBoundPlacemark.getView();
            
            view.setAltMode(AltitudeMode.clampToGround);
            view.setCoordinates(AdjBoundPoly.getAveragedCoords());
            
            if (points.size() > 1)
                view.setTimeSpan(ts);
            
            view.setRange(adjRange);
            view.setTilt(5d);
            
            prop = new Properties();
            prop.setSnippit(poly.getDescription());

            AdjBoundPlacemark.setProperties(prop);
            AdjBoundPlacemark.setOpen(false);
            AdjBoundPlacemark.setVisibility(true);
            AdjBoundPlacemark.addPolygon(AdjBoundPoly);
            AdjBoundPlacemark.setStyleUrl(sAdjBoundMap.getStyleUrl());

            //UnAdjBoundPlacemark
            Placemark UnAdjBoundPlacemark = new Placemark("UnAdjBoundPoly", "UnAdjusted Boundary Polygon", new View());
            
            view = UnAdjBoundPlacemark.getView();
            
            view.setAltMode(AltitudeMode.clampToGround);
            view.setCoordinates(UnAdjBoundPoly.getAveragedCoords());
            
            if (points.size() > 1)
                view.setTimeSpan(ts);
            
            view.setRange(unAdjRange);
            view.setTilt(5d);
            
            UnAdjBoundPlacemark.setProperties(prop);
            UnAdjBoundPlacemark.setOpen(false);
            UnAdjBoundPlacemark.setVisibility(false);
            UnAdjBoundPlacemark.addPolygon(UnAdjBoundPoly);
            UnAdjBoundPlacemark.setStyleUrl(sUnAdjBoundMap.getStyleUrl());

            //AdjNavPlacemark
            Placemark AdjNavPlacemark = new Placemark("AdjNavPoly", "Adjusted Navigation Path", new View());
            
            view = AdjNavPlacemark.getView();
            view.setAltMode(AltitudeMode.clampToGround);
            view.setCoordinates(AdjNavPoly.getAveragedCoords());
            
            if (points.size() > 1)
                view.setTimeSpan(ts);
            
            view.setRange(adjRange);
            view.setTilt(5d);

            AdjNavPlacemark.setProperties(prop);
            AdjNavPlacemark.setOpen(false);
            AdjNavPlacemark.setVisibility(false);
            AdjNavPlacemark.addPolygon(AdjNavPoly);
            AdjNavPlacemark.setStyleUrl(sAdjNavMap.getStyleUrl());

            //UnAdjNavPlacemark
            Placemark UnAdjNavPlacemark = new Placemark("UnAdjNavPoly", "UnAdjusted Navigation Path", new View());
            
            view = UnAdjNavPlacemark.getView();
            view.setAltMode(AltitudeMode.clampToGround);
            view.setCoordinates(UnAdjNavPoly.getAveragedCoords());
            
            if (points.size() > 1)
                view.setTimeSpan(ts);

            view.setRange(unAdjRange);
            view.setTilt(5d);

            UnAdjNavPlacemark.setProperties(prop);
            UnAdjNavPlacemark.setOpen(false);
            UnAdjNavPlacemark.setVisibility(false);
            UnAdjNavPlacemark.addPolygon(UnAdjNavPoly);
            UnAdjNavPlacemark.setStyleUrl(sUnAdjNavMap.getStyleUrl());

            //add placemarks
            fAdjBound.addPlacemark(AdjBoundPlacemark);
            fUnAdjBound.addPlacemark(UnAdjBoundPlacemark);
            fAdjNav.addPlacemark(AdjNavPlacemark);
            fUnAdjNav.addPlacemark(UnAdjNavPlacemark);

            //endregion

            //endregion

            //region Add Folders To eachother
            //added point folders to bound/nav/misc folders
            fAdjBound.addFolder(fAdjBoundPoints);
            fUnAdjBound.addFolder(fUnAdjBoundPoints);
            fAdjNav.addFolder(fAdjNavPoints);
            fUnAdjNav.addFolder(fUnAdjNavPoints);
            fMiscPoints.addFolder(fAdjMiscPoints);
            fMiscPoints.addFolder(fUnAdjMiscPoints);

            //add bound/nav/misc/way folders to root polygon folder
            folder.addFolder(fAdjBound);
            folder.addFolder(fUnAdjBound);
            folder.addFolder(fAdjNav);
            folder.addFolder(fUnAdjNav);
            folder.addFolder(fMiscPoints);
            folder.addFolder(fWayPoints);

            //add polygon root to KmlDoc
            doc.addFolder(folder);
            //endregion
        }
        //endregion

        try {
            KmlWriter.createFile(doc, kmlPath);
        } catch (IOException e) {
            kmlPath = null;
            TtUtils.TtReport.writeError(e.getMessage(), "Export:kml");
        }

        return kmlPath;
    }

    public static String kmz(DataAccessLayer dal, String dir) {
        String filename = scrubProjectName(dal.getProjectID());
        String kmzPath = String.format("%s/%s.kmz", dir, filename);

        try {
            byte[] buffer = new byte[1024];

            String kmlPath = kml(dal, dir);


            if (!StringEx.isEmpty(kmlPath)) {
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(kmzPath));

                zos.putNextEntry(new ZipEntry(String.format("%s.kml", filename)));
                FileInputStream fis = new FileInputStream(kmlPath);

                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                fis.close();

                zos.closeEntry();
                zos.close();

                new File(kmlPath).delete();

                return kmzPath;
            }
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "Export:kmz");
        }

        return null;
    }

    public static String summary(DataAccessLayer dal, String dir) {
        String summaryName = String.format("%s/%s_summary.txt", dir, scrubProjectName(dal.getProjectID()));

        try {
            FileWriter writer = new FileWriter(summaryName);

            writer.write(HaidLogic.generateAllPolyStats(dal, true, true));

            writer.close();
        } catch (Exception ex) {
            summaryName = null;
            TtUtils.TtReport.writeError(ex.getMessage(), "Export:summary", ex.getStackTrace());
        }

        return summaryName;
    }


    private static String scrubProjectName(String file) {
        return  file.replaceAll("[^a-zA-Z0-9.-]", "_");
    }


    public static class ExportTask extends AsyncTask<ExportTask.ExportParams, Void, ExportResult> {
        private Listener listener;

        @Override
        protected ExportResult doInBackground(ExportParams... params) {
            if (params.length < 1) {
                return new ExportResult(ExportResultCode.InvalidParams);
            }

            ExportParams ep = params[0];

            if (ep.getDirectory() == null) {
                return new ExportResult(ExportResultCode.InvalidParams, "No directory selected");
            }

            if (!ep.getDirectory().exists() && !ep.getDirectory().mkdirs()) {
                return new ExportResult(ExportResultCode.ExportFailure, "Failed to create main directory");
            }

            String dirPath = ep.getDirectory().getAbsolutePath();

            try {
                if (!isCancelled() && ep.isPoints()) {
                    if (points(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "Points");
                    }
                }

                if (!isCancelled() && ep.isPolys()) {
                    if (polygons(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "Polygons");
                    }
                }

                if (!isCancelled() && ep.isMeta()) {
                    if (metadata(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "Metadata");
                    }
                }

                if (!isCancelled() && ep.isProj()) {
                    if (project(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "Project");
                    }
                }

                if (!isCancelled() && ep.isNmea()) {
                    if (nmea(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "NMEA");
                    }
                }

                if (!isCancelled() && ep.isKmz()) {
                    if (kmz(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "KMZ");
                    }
                }

                if (!isCancelled() && ep.isGpx()) {
                    if (gpx(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "GPX");
                    }
                }

                if (!isCancelled() && ep.isSummary()) {
                    if (summary(ep.getDal(), dirPath) == null) {
                        return new ExportResult(ExportResultCode.ExportFailure, "Summary");
                    }
                }
            } catch (Exception ex) {
                TtUtils.TtReport.writeError(ex.getMessage(), "Export:ExportTask", ex.getStackTrace());
                return new ExportResult(ExportResultCode.ExportFailure, "Unknown failure");
            }

            return new ExportResult(isCancelled() ? ExportResultCode.Cancelled : ExportResultCode.Success);
        }

        @Override
        protected void onPostExecute(ExportResult exportResult) {
            super.onPostExecute(exportResult);

            if (listener != null) {
                listener.onTaskFinish(exportResult);
            }
        }

        public void setListener(Listener listener) {
            this.listener = listener;
        }

        public static class ExportParams {
            private DataAccessLayer dal;
            private File directory;
            private boolean points, polys, meta, proj, nmea, kmz, gpx, summary;

            public ExportParams(DataAccessLayer dal, File directory, boolean points, boolean polys, boolean meta,
                boolean proj, boolean nmea, boolean kmz, boolean gpx, boolean summary) {

                this.dal = dal;

                this.directory = directory;

                this.points = points;
                this.polys = polys;
                this.meta = meta;
                this.proj = proj;
                this.nmea = nmea;
                this.kmz = kmz;
                this.gpx = gpx;
                this.summary = summary;
            }

            public DataAccessLayer getDal() {
                return dal;
            }

            public File getDirectory() {
                return directory;
            }

            public boolean isPoints() {
                return points;
            }

            public boolean isPolys() {
                return polys;
            }

            public boolean isMeta() {
                return meta;
            }

            public boolean isProj() {
                return proj;
            }

            public boolean isNmea() {
                return nmea;
            }

            public boolean isKmz() {
                return kmz;
            }

            public boolean isGpx() {
                return gpx;
            }

            public boolean isSummary() {
                return summary;
            }
        }

        public interface Listener {
            void onTaskFinish(ExportResult result);
        }
    }


    public static class ExportResult {
        private ExportResultCode code;
        private String message;

        public ExportResult(ExportResultCode code) {
            this(code, null);
        }

        public ExportResult(ExportResultCode code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public ExportResultCode getCode() {
            return code;
        }
    }


    public enum ExportResultCode {
        Success,
        Cancelled,
        ExportFailure,
        InvalidParams
    }

}
