package com.usda.fmsc.twotrails.utilities;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.TaskRunner;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.data.DataAccessManager;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
import com.usda.fmsc.twotrails.data.MediaAccessManager;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.MimeTypes;
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
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.utilities.StringEx;

import com.usda.fmsc.twotrails.units.OpType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.DateTime;

public class Export {
    public static File exportProjectPackage(TwoTrailsApp context) {
        DataAccessLayer dal = context.getDAL();
        MediaAccessLayer mal = context.getMAL();

        String projectName = scrubProjectName(dal.getProjectID());

        File tcProjDir = new File(context.getCacheDir(), projectName);

        if (!tcProjDir.exists()) {
            if (!tcProjDir.mkdirs())
                throw new RuntimeException("Unable to create directories");
        }

        File tcZip = new File(context.getCacheDir(), String.format("%s_Export.zip", projectName));

        try {
            ArrayList<File> files = new ArrayList<>();

            files.add(context.getDatabasePath(dal.getFileName()));
            if (mal != null) {
                files.add(context.getDatabasePath(mal.getFileName()));

                File md = context.getProjectMediaDir();

                if (md != null && md.exists() && md.listFiles().length > 0) {
                    files.add(md);
                }
            }

            FileUtils.zipFiles(tcZip, files);

            return tcZip;
        } catch (Exception e) {
            context.getReport().writeError(e.getMessage(), "Export:exportProjectPackage");
            throw new RuntimeException("Error Exporting Project Package");
        }
    }

    public static File points(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        File tcPointsFile = new File(dir != null ? dir : context.getCacheDir(), "Points.csv");

        try {
            HashMap<String, TtMetadata> metadata = dal.getMetadataMap();

            //region Point Headers
            CSVPrinter printer = new CSVPrinter(new FileWriter(tcPointsFile), CSVFormat.DEFAULT);

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
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:points", ex.getStackTrace());
            throw new RuntimeException("Error Exporting Points");
        }

        return tcPointsFile;
    }

    public static File groups(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        File tcGroupsFile = new File(dir != null ? dir : context.getCacheDir(), "Groups.csv");

        try {
            CSVPrinter printer = new CSVPrinter(new FileWriter(tcGroupsFile), CSVFormat.DEFAULT);

            printer.printRecord(
                    "Name",
                    "Type",
                    "Description",
                    "CN"
            );

            ArrayList<String> values = new ArrayList<>(4);

            for (TtGroup group : dal.getGroups()) {
                values.add(group.getName());
                values.add(group.getGroupType().toString());
                values.add(group.getDescription());
                values.add(group.getCN());

                printer.printRecord(values);
                values.clear();
            }

            printer.close();
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:groups", ex.getStackTrace());
            throw new RuntimeException("Error Exporting Groups");
        }

        return tcGroupsFile;
    }

    public static File polygons(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        File tcPolysFile = new File(dir != null ? dir : context.getCacheDir(), "Polygons.csv");

        try {
            CSVPrinter printer = new CSVPrinter(new FileWriter(tcPolysFile), CSVFormat.DEFAULT);

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

            return tcPolysFile;
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:polygons", ex.getStackTrace());
            throw new RuntimeException("Error Exporting Polygons");
        }
    }

    public static File metadata(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        File tcMetaFile = new File(dir != null ? dir : context.getCacheDir(), "Metadata.csv");

        try {
            CSVPrinter writer = new CSVPrinter(new FileWriter(tcMetaFile), CSVFormat.DEFAULT);

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

            return tcMetaFile;
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:metadata", ex.getStackTrace());
            throw new RuntimeException("Error Exporting Metadata");
        }
    }

    public static File project(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        File tcProjectFile = new File(dir != null ? dir : context.getCacheDir(), "Project.csv");

        try {
            CSVPrinter writer = new CSVPrinter(new FileWriter(tcProjectFile), CSVFormat.DEFAULT);

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

            return tcProjectFile;
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:metadata", ex.getStackTrace());
            throw new RuntimeException("Error Exporting Project Info");
        }
    }

    public static File nmea(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        File tcNmeaFile = new File(dir != null ? dir : context.getCacheDir(), "TtNmea.csv");

        try {
            //region NMEA Headers
            CSVPrinter writer = new CSVPrinter(new FileWriter(tcNmeaFile), CSVFormat.DEFAULT);

            writer.printRecord(
                    "Point CN",
                    "IsUsed",

                    "Time Created",
                    "Time Fix",

                    "Latitude",
                    "Longitude",
                    "Elevation (Mt)",

                    "Mag Var",
                    "Mag Var Dir",

                    "Ground Speed (knots)",
                    "Track Angle (deg true)",

                    "Fix",
                    "Fix Quality",
                    "Operation Mode",

                    "PDOP",
                    "HDOP",
                    "VDOP",

                    "Horiz Dilution",
                    "Geoid Height (Mt)",

                    "Tracked Satellites Count",
                    "Satellites In View Count",
                    "Satellites Used Count",
                    "Satellites Used",
                    "Satellites In View Info",

                    "CN"
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

                values.add(StringEx.toString(burst.getMagVar()));
                values.add(burst.getMagVarDir() == null ? StringEx.Empty : burst.getMagVarDir().toString());

                values.add(StringEx.toString(burst.getGroundSpeed()));
                values.add(StringEx.toString(burst.getTrackAngle()));

                values.add(burst.getFix().toStringF());
                values.add(burst.getFixQuality() == null ? StringEx.Empty : burst.getFixQuality().toStringF());
                values.add(burst.getOperationMode().toString());

                values.add(StringEx.toString(burst.getPDOP()));
                values.add(StringEx.toString(burst.getHDOP()));
                values.add(StringEx.toString(burst.getVDOP()));

                values.add(StringEx.toString(burst.getHorizDilution()));
                values.add(StringEx.toString(burst.getGeoidHeight()));

                values.add(StringEx.toString(burst.getTrackedSatellitesCount()));
                values.add(StringEx.toString(burst.getSatellitesInViewCount()));
                values.add(StringEx.toString(burst.getUsedSatellitesCount()));
                values.add(burst.getUsedSatelliteIDsString());
                values.add(burst.getSatellitesInViewString());

                writer.printRecord(values);
                values.clear();
            }

            writer.close();
            //endregion

            return tcNmeaFile;
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:nmea", ex.getStackTrace());
            throw new RuntimeException("Error Exporting Nmea");
        }
    }

    public static File gpx(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        String projName = dal.getProjectID();
        File tcGpxFile = new File(dir != null ? dir : context.getCacheDir(), String.format("%s.gpx", scrubProjectName(projName)));

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

                    if (tmpMeta == null) {
                        throw new RuntimeException("Metadata not found");
                    }

                    if (point instanceof GpsPoint) {
                        GpsPoint gps = (GpsPoint)point;

                        if (gps.hasLatLon()) {
                            adjpoint = new GpxPoint(gps.getLatitude(), gps.getLongitude(), gps.getElevation());
                            unAdjpoint = new GpxPoint(gps.getLatitude(), gps.getLongitude(), gps.getElevation());
                        }
                    }

                    if (adjpoint == null) {
                        Position pos = UTMTools.convertUTMtoLatLonSignedDec(point.getAdjX(), point.getAdjY(), tmpMeta.getZone());
                        adjpoint = new GpxPoint(pos.getLatitude(), pos.getLongitude(), point.getAdjZ());

                        pos = UTMTools.convertUTMtoLatLonSignedDec(point.getUnAdjX(), point.getUnAdjY(), tmpMeta.getZone());
                        unAdjpoint = new GpxPoint(pos.getLatitude(), pos.getLongitude(), point.getUnAdjZ());
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

                        if (qp.getParentPoint().isNavPoint()) {
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
            GpxWriter.create(tcGpxFile, doc);
            return tcGpxFile;
        } catch (IOException e) {
            context.getReport().writeError(e.getMessage(), "Export:gpx");
            throw new RuntimeException("Error Exporting GPX");
        }
    }

    public static File kml(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        String projName = dal.getProjectID();
        File tcKmlFile = new File(dir != null ? dir : context.getCacheDir(), String.format("%s.kml", scrubProjectName(projName)));

        //region Create Document
        KmlDocument doc = new KmlDocument(projName, dal.getProjectDescription());

        doc.setSnippit(String.format("Generated in TwoTrails: %s", TtUtils.getDeviceName()));

        doc.setOpen(true);
        doc.setVisibility(true);
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

        sAdjMisc.setIconColorMode(ColorMode.normal);
        sAdjMisc.setIconColor(AdjMiscColor);
        sAdjMisc.setIconScale(1d);
        sAdjMisc.setBalloonDisplayMode(DisplayMode.Default);

        sUnAdjMisc.setIconColorMode(ColorMode.normal);
        sUnAdjMisc.setIconColor(UnAdjMiscColor);
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

            folder.setSnippit(poly.getDescription());
            
            ExtendedData ed = new ExtendedData();
            ed.add(new ExtendedData.Data(" ", "In Meters"));
            ed.add(new ExtendedData.Data("Accuracy", StringEx.toString(poly.getAccuracy())));
            ed.add(new ExtendedData.Data("Perimeter", StringEx.toString(poly.getPerimeter())));
            ed.add(new ExtendedData.Data("Area", StringEx.toString(poly.getArea())));
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
                    Position coords = TtUtils.Points.getLatLonFromPoint(point, true, md.get(point.getMetadataCN()));

                    Coordinates adjCoord = new Coordinates(coords.getLatitude(), coords.getLongitude(), coords.getElevation());

                    coords = TtUtils.Points.getLatLonFromPoint(point, false, md.get(point.getMetadataCN()));
                    Coordinates unAdjCoord = new Coordinates(coords.getLatitude(), coords.getLongitude(), coords.getElevation());

                    String snippit = "Point Operation: " + point.getOp().toString();

                    //region Create Placemarks for Bound/Nav
                    Placemark adjPm = new Placemark(Integer.toString(point.getPID()),
                            String.format(Locale.getDefault(), "Point Operation: %s<br><div>\t     Adjusted<br>UtmX: %f<br>UtmY: %f</div><br>%s",
                                    point.getOp().toString(), point.getAdjX(), point.getAdjY(),
                                    point.getComment() != null ? point.getComment() : ""),
                            new View());

                    View view = adjPm.getView();

                    view.setTimeStamp(point.getTime());
                    view.setCoordinates(adjCoord);
                    view.setTilt(15d);
                    view.setAltMode(AltitudeMode.clampToGround);
                    view.setRange(150d);

                    adjPm.setSnippit(snippit);

                    adjPm.setStyleUrl(sAdjBoundMap.getStyleUrl());
                    adjPm.setOpen(false);
                    adjPm.setVisibility(true);
                    adjPm.addPoint(new Point(adjCoord, AltitudeMode.clampToGround));


                    Placemark unAdjPm = new Placemark(Integer.toString(point.getPID()),
                            String.format(Locale.getDefault(), "Point Operation: %s<br><div>     Unadjusted<br>UtmX: %f<br>UtmY: %f</div><br>%s",
                                    point.getOp().toString(), point.getUnAdjX(), point.getUnAdjY(), point.getComment() != null ? point.getComment() : ""),
                            new View());

                    view = unAdjPm.getView();
                    view.setTimeStamp(point.getTime());
                    view.setCoordinates(unAdjCoord);
                    view.setTilt(15d);
                    view.setAltMode(AltitudeMode.clampToGround);
                    view.setRange(150d);

                    unAdjPm.setSnippit(snippit);

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

            AdjBoundPlacemark.setSnippit(poly.getDescription());

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
            
            UnAdjBoundPlacemark.setSnippit(poly.getDescription());
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

            AdjNavPlacemark.setSnippit(poly.getDescription());
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

            UnAdjNavPlacemark.setSnippit(poly.getDescription());
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
            if (fAdjBoundPoints.getPlacemarks().size() > 0)
                fAdjBound.addFolder(fAdjBoundPoints);

            if (fUnAdjBoundPoints.getPlacemarks().size() > 0)
                fUnAdjBound.addFolder(fUnAdjBoundPoints);

            if (fAdjNavPoints.getPlacemarks().size() > 0)
                fAdjNav.addFolder(fAdjNavPoints);

            if (fUnAdjNavPoints.getPlacemarks().size() > 0)
                fUnAdjNav.addFolder(fUnAdjNavPoints);

            if (fAdjMiscPoints.getPlacemarks().size() > 0)
                fMiscPoints.addFolder(fAdjMiscPoints);

            if (fUnAdjMiscPoints.getPlacemarks().size() > 0)
                fMiscPoints.addFolder(fUnAdjMiscPoints);

            //add bound/nav/misc/way folders to root polygon folder
            if (fAdjBound.getSubFolders().size() > 0)
                folder.addFolder(fAdjBound);

            if (fUnAdjBound.getSubFolders().size() > 0)
                folder.addFolder(fUnAdjBound);

            if (fAdjNav.getSubFolders().size() > 0)
                folder.addFolder(fAdjNav);

            if (fUnAdjNav.getSubFolders().size() > 0)
                folder.addFolder(fUnAdjNav);

            if (fMiscPoints.getSubFolders().size() > 0)
                folder.addFolder(fMiscPoints);

            if (fMiscPoints.getSubFolders().size() > 0)
                folder.addFolder(fMiscPoints);

            if (fWayPoints.getPlacemarks().size() > 0)
                folder.addFolder(fWayPoints);

            //add polygon root to KmlDoc
            doc.addFolder(folder);
            //endregion
        }
        //endregion

        try {
            KmlWriter.write(tcKmlFile, doc);
            return tcKmlFile;
        } catch (IOException e) {
            context.getReport().writeError(e.getMessage(), "Export:kml");
            throw new RuntimeException("Error Exporting Kml");
        }
    }

    public static File kmz(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        String projName = dal.getProjectID();
        File tcKmzFile = new File(dir != null ? dir : context.getCacheDir(), String.format("%s.kmz", scrubProjectName(projName)));

        try {
            byte[] buffer = new byte[1024];

            File kmlFile = kml(context, dal, null);

            if (kmlFile != null && kmlFile.exists()) {
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tcKmzFile));

                zos.putNextEntry(new ZipEntry(kmlFile.getName()));
                FileInputStream fis = new FileInputStream(kmlFile);

                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                fis.close();

                zos.closeEntry();
                zos.close();

                kmlFile.delete();

                return tcKmzFile;
            } else {
                throw new RuntimeException("KML not created");
            }
        } catch (Exception e) {
            context.getReport().writeError(e.getMessage(), "Export:kmz");
            throw new RuntimeException("Error Exporting KMZ");
        }
    }

    public static File summary(TwoTrailsApp context, DataAccessLayer dal, File dir) {
        File tcSummaryFile = new File(dir != null ? dir : context.getCacheDir(), String.format("%s_Summary.txt", scrubProjectName(dal.getProjectID())));

        try {
            FileWriter writer = new FileWriter(tcSummaryFile);

            writer.write(String.format(Locale.getDefault(), "Project File: %s\n", dal.getFileName()));
            writer.write(String.format(Locale.getDefault(), "Project Name: %s\n", dal.getProjectID()));
            writer.write(String.format(Locale.getDefault(), "Region: %s\n", dal.getProjectRegion()));
            writer.write(String.format(Locale.getDefault(), "Forest: %s\n", dal.getProjectForest()));
            writer.write(String.format(Locale.getDefault(), "District: %s\n", dal.getProjectDistrict()));
            writer.write(String.format(Locale.getDefault(), "Description: %s\n", dal.getProjectDescription()));
            writer.write(String.format(Locale.getDefault(), "Created On: %s\n", dal.getProjectDateCreated()));
            writer.write(String.format(Locale.getDefault(), "Version: %s\n", dal.getVersion()));
            writer.write(String.format(Locale.getDefault(), "Data Version: %s\n", dal.getTtDbVersion()));
            writer.write(String.format(Locale.getDefault(), "Creation Version: %s\n", dal.getProjectCreatedTtVersion()));

            writer.write("\n\n");

            writer.write(context.getString(R.string.haid_poly_info));

            writer.write("\n\n\n");

            writer.write(new HaidLogic(context).generateAllPolyStats(true, true));

            writer.close();

            return tcSummaryFile;
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:summary", ex.getStackTrace());
            throw new RuntimeException("Error Exporting Summary");
        }
    }

    public static File imageInfo(TwoTrailsApp context, MediaAccessLayer mal, File dir) {
        File tcImgInfoFile = new File(dir != null ? dir : context.getCacheDir(), "ImageInfo.csv");

        try {
            CSVPrinter printer = new CSVPrinter(new FileWriter(tcImgInfoFile), CSVFormat.DEFAULT);

            printer.printRecord(
                    "Name",
                    "Creation Time",
                    "IsExternal",
                    "Comment",
                    "Type",
                    "Azimuth",
                    "Pitch",
                    "Roll",
                    "CN",
                    "PointCN"
            );

            ArrayList<String> values = new ArrayList<>(5);

            for (TtImage img : mal.getImages()) {
                values.add(img.getName());
                values.add(Consts.DateTimeFormatter.print(img.getTimeCreated()));
                values.add(StringEx.toString(img.isExternal()));
                values.add(img.getComment());
                values.add(img.getPictureType().toString());
                values.add(StringEx.toString(img.getAzimuth()));
                values.add(StringEx.toString(img.getPitch()));
                values.add(StringEx.toString(img.getRoll()));
                values.add(img.getCN());
                values.add(img.getPointCN());

                printer.printRecord(values);
                values.clear();
            }

            printer.close();

            return tcImgInfoFile;
        } catch (Exception ex) {
            context.getReport().writeError(ex.getMessage(), "Export:imageInfo", ex.getStackTrace());
            throw new RuntimeException("Error Exporting ImageInfo");
        }
    }


    private static String scrubProjectName(String file) {
        return  file.replaceAll("[^a-zA-Z0-9.-]", "_");
    }


    public static class ExportTask extends TaskRunner.Task<ExportTask.Params, ExportTask.Result> {
        private Listener listener;

        @Override
        protected ExportTask.Result onBackgroundWork(Params params) {
            if (params == null) {
                return new Result(ResultCode.InvalidParams);
            }

            if (params.getPath() == null) {
                return new Result(ResultCode.InvalidParams, "No path or file selected");
            }

            String projectDirName = String.format(Locale.getDefault(), "%s_%s",
                    scrubProjectName(params.getDal().getProjectID()), TtUtils.Date.toStringDateMillis(new DateTime()));

            DocumentFile dfDir = null;
            File zfDir = null;
            Uri file = null;

            if (params.isSingleFile()) {
                file = params.getPath();
                if (file == null) {
                    return new Result(ResultCode.ExportFailure, "No File Selected");
                }

                if (params.isZipFile()) {
                    zfDir = new File(params.getContext().getCacheDir(), projectDirName);

                    if (zfDir.exists() && !zfDir.delete()) {
                        return new Result(ResultCode.ExportFailure, "Unable to delete ZipFile directory");
                    }

                    if (!zfDir.mkdirs())
                        return new Result(ResultCode.ExportFailure, "Unable to create ZipFile directory");
                }
            } else {
                dfDir = DocumentFile.fromTreeUri(params.getContext(), params.getPath());

                if (dfDir != null) {
                    if (!AndroidUtils.Files.fileOrFolderExistsInTree(params.getContext(), dfDir.getUri(), projectDirName))
                        dfDir = dfDir.createDirectory(projectDirName);

                    if (dfDir == null)
                        return new Result(ResultCode.ExportFailure, "Unable to create Export directory");
                }
            }

            if (!isCancelled() && params.isPoints()) {
                try {
                    if (params.isZipFile()) {
                        points(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File pointsFile = points(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(pointsFile), dfDir.createFile(MimeTypes.Text.CSV, pointsFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:points", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "Points");
                }

                try {

                    if (params.isZipFile()) {
                        groups(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File groupsFile = groups(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(groupsFile), dfDir.createFile(MimeTypes.Text.CSV, groupsFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:groups", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "Groups");
                }
            }

            if (!isCancelled() && params.isPolys()) {
                try {
                    if (params.isZipFile()) {
                        polygons(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File polysFile = polygons(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(polysFile), params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Text.CSV, polysFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:polygons", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "Polygons");
                }
            }

            if (!isCancelled() && params.isMeta()) {
                try {
                    if (params.isZipFile()) {
                        metadata(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File metaFile = metadata(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(metaFile), params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Text.CSV, metaFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:metadata", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "Metadata");
                }
            }

            if (!isCancelled() && params.isProj()) {
                try {
                    if (params.isZipFile()) {
                        project(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File projFile = project(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(projFile), params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Text.CSV, projFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:project", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "Project");
                }
            }

            if (!isCancelled() && params.isNmea()) {
                try {
                    if (params.isZipFile()) {
                        nmea(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File nmeaFile = nmea(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(nmeaFile), params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Text.CSV, nmeaFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:nmea", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "NMEA");
                }
            }

            if (!isCancelled() && params.isKmz()) {
                try {
                    if (params.isZipFile()) {
                        kmz(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File kmzFile = kmz(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(kmzFile),params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Application.GOOGLE_EARTH_KMZ, kmzFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:kmz", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "KMZ");
                }
            }

            if (!isCancelled() && params.isGpx()) {
                try {
                    if (params.isZipFile()) {
                        gpx(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File gpxFile = gpx(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(gpxFile), params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Application.GPS, gpxFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:gpx", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "GPX");
                }
            }

            if (!isCancelled() && params.isSummary()) {
                try {
                    if (params.isZipFile()) {
                        summary(params.getContext(), params.getDal(), zfDir);
                    } else {
                        File summaryFile = summary(params.getContext(), params.getDal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(summaryFile), params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Text.PLAIN, summaryFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:summary", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "Summary");
                }
            }

            if (!isCancelled() && params.isImgInfo() && params.getContext().hasMAL()) {
                try {
                    if (params.isZipFile()) {
                        imageInfo(params.getContext(), params.getMal(), zfDir);
                    } else {
                        File imgInfoFile = imageInfo(params.getContext(), params.getMal(), null);
                        AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(imgInfoFile), params.isSingleFile() ? file : dfDir.createFile(MimeTypes.Text.CSV, imgInfoFile.getName()).getUri());
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:imageinfo", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "ImageInfo");
                }
            }

            if (!isCancelled() && params.isPcExp()) {
                try {
                    if (params.isZipFile()) {
                        AndroidUtils.Files.copyFile(params.getContext(), params.getDam().getDBUri(), Uri.fromFile(new File(zfDir, params.getDam().getDatabaseName())));

                        if (params.getContext().hasMAL()) {
                            AndroidUtils.Files.copyFile(params.getContext(), params.getMam().getDBUri(), Uri.fromFile(new File(zfDir, params.getMam().getDatabaseName())));
                        }
                    } else {
                        AndroidUtils.Files.copyFile(params.getContext(), params.getDam().getDBUri(), dfDir.createFile(Consts.FileMimes.TPK, params.getDam().getDatabaseName()).getUri());

                        if (params.getContext().hasMAL()) {
                            AndroidUtils.Files.copyFile(params.getContext(), params.getMam().getDBUri(), dfDir.createFile(Consts.FileMimes.TWO_TRAILS_MEDIA_PACKAGE, params.getMam().getDatabaseName()).getUri());
                        }
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:pcPkg", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "pcPkg");
                }
            }

            if (params.isZipFile()) {
                try {
                    File tmpZip = new File(params.getContext().getCacheDir(), FileUtils.getFileName(file.getPath()));
                    FileUtils.zipToFile(zfDir, tmpZip);
                    AndroidUtils.Files.copyFile(params.getContext(), Uri.fromFile(tmpZip), file);
                    if (tmpZip.delete()) {
                        zfDir.delete();
                    }
                } catch (Exception e) {
                    params.getContext().getReport().writeError(e.getMessage(), "Export:ExportTask:onBackgroundWork:zip", e.getStackTrace());
                    return new Result(ResultCode.ExportFailure, "zip");
                }
            }

            return new Result(isCancelled() ? ResultCode.Cancelled : ResultCode.Success);
        }

        @Override
        public void onComplete(Result result) {
            if (listener != null) {
                listener.onTaskFinish(result);
            }
        }

        @Override
        public void onError(Exception exception) {
            if (listener != null) {
                listener.onTaskError(exception);
            }
        }

        public void setListener(Listener listener) {
            this.listener = listener;
        }


        public static class Params {
            private final TwoTrailsApp context;
            private final Uri path;
            private final boolean singleFile, zipFile, points, polys, meta, proj, nmea, kmz, gpx, summary, imgInfo, pcExp;

            public Params(TwoTrailsApp context, Uri path, boolean singleFile, boolean zipFile, boolean points, boolean polys, boolean meta,
                          boolean imgInfo, boolean proj, boolean nmea, boolean kmz, boolean gpx, boolean summary, boolean pcExp) {

                this.context = context;
                this.path = path;

                this.singleFile = singleFile;
                this.zipFile = zipFile;

                this.points = points;
                this.polys = polys;
                this.meta = meta;
                this.imgInfo = imgInfo;
                this.proj = proj;
                this.nmea = nmea;
                this.kmz = kmz;
                this.gpx = gpx;
                this.summary = summary;
                this.pcExp = pcExp;
            }

            public TwoTrailsApp getContext() { return context; }

            public DataAccessLayer getDal() {
                return context.getDAL();
            }

            public DataAccessManager getDam() {
                return context.getDAM();
            }

            public MediaAccessLayer getMal() {
                return context.hasMAL() ? context.getMAL() : null;
            }

            public MediaAccessManager getMam() {
                return context.hasMAL() ? context.getMAM() : null;
            }

            public Uri getPath() {
                return path;
            }

            public boolean isSingleFile() {
                return singleFile;
            }

            public boolean isZipFile() { return zipFile; }

            public boolean isPoints() {
                return points;
            }

            public boolean isPolys() {
                return polys;
            }

            public boolean isMeta() {
                return meta;
            }

            public boolean isImgInfo() { return imgInfo; }

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

            public boolean isPcExp() { return pcExp; }
        }


        public interface Listener {
            void onTaskFinish(Result result);
            void onTaskError(Exception e);
        }


        public static class Result {
            private final ResultCode code;
            private final String message;

            public Result(ResultCode code) {
                this(code, null);
            }

            public Result(ResultCode code, String message) {
                this.code = code;
                this.message = message;
            }

            public String getMessage() {
                return message;
            }

            public ResultCode getCode() {
                return code;
            }
        }


        public enum ResultCode {
            Success,
            Cancelled,
            ExportFailure,
            InvalidParams
        }
    }
}
