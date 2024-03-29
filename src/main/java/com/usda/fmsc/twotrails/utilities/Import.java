package com.usda.fmsc.twotrails.utilities;

import android.net.Uri;

import com.usda.fmsc.android.utilities.TaskRunner;
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.gpx.GpxBaseTrack;
import com.usda.fmsc.utilities.gpx.GpxPoint;
import com.usda.fmsc.utilities.gpx.GpxRoute;
import com.usda.fmsc.utilities.gpx.GpxTrack;
import com.usda.fmsc.utilities.gpx.GpxTrackSeg;
import com.usda.fmsc.utilities.kml.Coordinates;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class Import {
    
    //region Text (CSV)
    public static class TextImportTask extends ImportTask<TextImportTask.TextImportParams> {

        @Override
        protected ImportResult onBackgroundWork(TextImportParams params) {
            if (params == null) {
                return new ImportResult(ImportResultCode.InvalidParams);
            }

            if (params.getFilePath() == null) {
                return new ImportResult(ImportResultCode.InvalidParams, "No File selected");
            }

            try {
                CSVParser parser = new CSVParser(new FileReader(params.getFilePath().toString()), CSVFormat.DEFAULT);

                HashMap<String, TtPolygon> polygons = new HashMap<>();
                HashMap<String, String> polyNameToCN = new HashMap<>();
                HashMap<String, Integer> indexes = new HashMap<>();
                HashMap<String, String> remapCNs = new HashMap<>();

                HashMap<String, TtGroup> groups = new HashMap<>();
                HashMap<String, String> groupNameToCN = new HashMap<>();

                HashMap<String, TtPoint> points = new HashMap<>();

                HashMap<QuondamPoint, String> qps = new HashMap<>();

                TtPolygon tempPoly = null;
                TtGroup tempGroup = null;

                DataAccessLayer dal = params.getDal();

                List<String> toUsePolys = params.getPolygonNames();
                Map<TextFieldType, Integer> columnMap = params.getColumnMap();
                int polyCount = dal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName);

                TtPoint point, prevPoint = null;
                String temp;
                double tempD;
                int tempI;
                int tempIndex;
                DateTime tempDT;

                
                //region Check and get Fields
                boolean useSpecificPolys = toUsePolys != null;

                boolean usePolyNames = columnMap.containsKey(TextFieldType.POLY_NAME);
                boolean usePIDs = columnMap.containsKey(TextFieldType.PID);
                boolean hasCN = columnMap.containsKey(TextFieldType.CN);

                boolean hasGroups = columnMap.containsKey(TextFieldType.GROUP_NAME);

                boolean hasUnAdjZ = columnMap.containsKey(TextFieldType.UNADJZ);
                
                boolean hasLatLon = columnMap.containsKey(TextFieldType.LATITUDE) && columnMap.containsKey(TextFieldType.LONGITUDE);
                boolean hasElevation = columnMap.containsKey(TextFieldType.ELEVATION);
                boolean hasRMSEr = columnMap.containsKey(TextFieldType.RMSER);

                boolean hasFwdAz = columnMap.containsKey(TextFieldType.FWD_AZ);
                boolean hasBkAz = columnMap.containsKey(TextFieldType.BK_AZ);

                boolean hasManAcc = columnMap.containsKey(TextFieldType.MAN_ACC);

                boolean hasTime = columnMap.containsKey(TextFieldType.TIME);
                boolean hasIndex = columnMap.containsKey(TextFieldType.INDEX);
                boolean hasBnd = columnMap.containsKey(TextFieldType.ONBND);
                boolean hasComment = columnMap.containsKey(TextFieldType.COMMENT);

                int fPolyName = getFieldColumn(columnMap, TextFieldType.POLY_NAME, usePolyNames);
                int fPID = getFieldColumn(columnMap, TextFieldType.PID, usePIDs);
                int fCN = getFieldColumn(columnMap, TextFieldType.CN, params.isAdvImport());
                int fOp = getFieldColumn(columnMap, TextFieldType.OPTYPE, params.isAdvImport());
                int fGroupName = getFieldColumn(columnMap, TextFieldType.GROUP_NAME, hasGroups);
                int fUnAjX = columnMap.get(TextFieldType.UNADJX);
                int fUnAjY = columnMap.get(TextFieldType.UNADJY);
                int fUnAjZ = getFieldColumn(columnMap, TextFieldType.UNADJZ, hasUnAdjZ);
                int fLat = getFieldColumn(columnMap, TextFieldType.LATITUDE, hasLatLon);
                int fLon = getFieldColumn(columnMap, TextFieldType.LONGITUDE, hasLatLon);
                int fElev = getFieldColumn(columnMap, TextFieldType.ELEVATION, hasElevation);
                int fRmser = getFieldColumn(columnMap, TextFieldType.RMSER, hasRMSEr);
                int fFwdAz = getFieldColumn(columnMap, TextFieldType.FWD_AZ, hasFwdAz);
                int fBkAz = getFieldColumn(columnMap, TextFieldType.BK_AZ, hasBkAz);
                int fSlopeDist = getFieldColumn(columnMap, TextFieldType.SLOPE_DIST, params.isAdvImport());
                int fSlopeDType = getFieldColumn(columnMap, TextFieldType.SLOPE_DIST_TYPE, params.isAdvImport());
                int fSlopeAng = getFieldColumn(columnMap, TextFieldType.SLOPE_ANG, params.isAdvImport());
                int fSlopeAngType = getFieldColumn(columnMap, TextFieldType.SLOPE_ANG_TYPE, params.isAdvImport());
                int fPCN = getFieldColumn(columnMap, TextFieldType.PARENT_CN, params.isAdvImport());
                int fManAcc = getFieldColumn(columnMap, TextFieldType.MAN_ACC, hasManAcc);
                int fTime = getFieldColumn(columnMap, TextFieldType.TIME, hasTime);
                int fIndex = getFieldColumn(columnMap, TextFieldType.INDEX, hasIndex);
                int fBnd = getFieldColumn(columnMap, TextFieldType.ONBND, hasBnd);
                int fCmt = getFieldColumn(columnMap, TextFieldType.COMMENT, hasComment);
                //endregion



                Iterator<CSVRecord> iterator = parser.iterator();
                CSVRecord record;

                //skip first line
                iterator.next();

                while (iterator.hasNext() && !isCancelled()) {
                    record = iterator.next();

                    if (usePolyNames && toUsePolys.size() > 0 && !toUsePolys.contains(record.get(fPolyName).toLowerCase())) {
                        continue;
                    }

                    if (params.isAdvImport()) {
                        OpType op = OpType.parse(record.get(fOp));
                        point = TtUtils.Points.createNewPointByOpType(op);

                        switch (op) {
                            case GPS:
                            case Take5:
                            case Walk:
                            case WayPoint: {
                                GpsPoint gps = (GpsPoint)point;

                                if (hasLatLon) {
                                    gps.setLatitude(ParseEx.parseDouble(record.get(fLat), null));
                                    gps.setLongitude(ParseEx.parseDouble(record.get(fLon), null));
                                }

                                if (hasElevation) {
                                    gps.setElevation(ParseEx.parseDouble(record.get(fElev), null));
                                }

                                if (hasRMSEr) {
                                    gps.setRMSEr(ParseEx.parseDouble(record.get(fRmser), null));
                                }
                                break;
                            }
                            case Traverse:
                            case SideShot: {
                                TravPoint trav = (TravPoint) point;

                                if (hasFwdAz) {
                                    trav.setFwdAz(ParseEx.parseDouble(record.get(fFwdAz), null));
                                }

                                if (hasBkAz) {
                                    trav.setBkAz(ParseEx.parseDouble(record.get(fBkAz), null));
                                }

                                if (trav.getFwdAz() == null && trav.getBkAz() == null) {
                                    throw new RuntimeException("No Forward or Back Azimuth");
                                }

                                tempD = ParseEx.parseDouble(record.get(fSlopeDist), 0d);
                                Dist tempDist = Dist.parse(record.get(fSlopeDType));

                                trav.setSlopeDistance(TtUtils.Convert.distance(tempD, Dist.Meters, tempDist));

                                tempD = ParseEx.parseDouble(record.get(fSlopeAng), 0d);
                                Slope tmpSlope = Slope.parse(record.get(fSlopeAngType));

                                trav.setSlopeDistance(TtUtils.Convert.angle(tempD, Slope.Percent, tmpSlope));
                                break;
                            }
                            case Quondam: {
                                QuondamPoint qp = (QuondamPoint)point;
                                qps.put(qp, record.get(fPCN));
                                break;
                            }
                        }

                        if (hasManAcc && point instanceof TtPoint.IManualAccuracy) {
                            ((TtPoint.IManualAccuracy)point).setManualAccuracy(ParseEx.parseDouble(record.get(fManAcc), null));
                        }

                        if (hasCN) {
                            temp = record.get(fCN);
                            if (dal.getItemsCount(TwoTrailsSchema.PointSchema.TableName, TwoTrailsSchema.SharedSchema.CN, temp) > 0) {
                                remapCNs.put(temp, point.getCN());
                            } else {
                                point.setCN(temp);
                            }
                        }
                    } else {
                        point = new GpsPoint();
                    }

                    //XYZ
                    Double d = ParseEx.parseDouble(record.get(fUnAjX));

                    if (d != null) {
                        point.setUnAdjX(d);
                    } else {
                        throw new Exception("No X value");
                    }

                    d = ParseEx.parseDouble(record.get(fUnAjY));

                    if (d != null) {
                        point.setUnAdjY(d);
                    } else {
                        throw new Exception("No Y value");
                    }

                    if (hasUnAdjZ) {
                        d = ParseEx.parseDouble(record.get(fUnAjZ), 0d);

                        if (d != null) {
                            point.setUnAdjZ(d);
                        }
                    } else {
                        point.setUnAdjZ(0d);
                    }

                    //Time
                    if (hasTime) {
                        try {
                            tempDT = Consts.DateTimeFormatter.parseDateTime(record.get(fTime));
                        } catch (Exception ex) {
                            tempDT = DateTime.now();
                        }
                    } else {
                        tempDT = DateTime.now();
                    }

                    point.setTime(tempDT);

                    //Polygons
                    if (usePolyNames) {
                        temp = record.get(fPolyName);

                        if (useSpecificPolys && toUsePolys.size() > 0 && !toUsePolys.contains(temp)) {
                            continue;
                        }

                        if (!polyNameToCN.containsKey(temp)) {
                            tempPoly = new TtPolygon(polyCount * 1000 + 1010);
                            tempPoly.setName(String.format("%s (Import)", temp));
                            tempPoly.setAccuracy(Consts.Default_Point_Accuracy);
                            tempPoly.setTime(point.getTime());
                            polyCount++;
                            polyNameToCN.put(temp, tempPoly.getCN());
                            polygons.put(tempPoly.getCN(), tempPoly);
                            indexes.put(tempPoly.getCN(), 0);
                        } else {
                            tempPoly = polygons.get(polyNameToCN.get(temp));
                        }

                        point.setPolyName(tempPoly.getName());
                        point.setPolyCN(tempPoly.getCN());
                    } else {
                        if (tempPoly == null) {
                            tempPoly = new TtPolygon(polyCount * 1000 + 1010);
                            tempPoly.setName("Imported Poly");
                            tempPoly.setAccuracy(Consts.Default_Point_Accuracy);
                            polygons.put(tempPoly.getCN(), tempPoly);
                            indexes.put(tempPoly.getCN(), 0);
                        } else {
                            point.setPolyName(tempPoly.getName());
                            point.setPolyCN(tempPoly.getCN());
                        }
                    }

                    //Groups
                    if (hasGroups) {
                        temp = record.get(fGroupName);

                        if (temp.equals(Consts.Defaults.MainGroupName)) {
                            tempGroup = params.getApp().getDAL().getGroupByCN(Consts.EmptyGuid);
                        } else if (!groupNameToCN.containsKey(temp)) {
                            tempGroup = new TtGroup(temp);
                            groupNameToCN.put(tempGroup.getName(), tempGroup.getCN());
                            groups.put(tempGroup.getCN(), tempGroup);
                        } else {
                            tempGroup = groups.get(groupNameToCN.get(temp));
                        }
                    } else if (tempGroup == null) {
                        tempGroup = params.getApp().getDAL().getGroupByCN(Consts.EmptyGuid);
                    }

                    point.setGroupCN(tempGroup.getCN());
                    point.setGroupName(tempGroup.getName());

                    //get point name
                    tempI = (prevPoint == null) ?
                            PointNamer.nameFirstPoint(tempPoly) :
                            PointNamer.namePoint(prevPoint, tempPoly);

                    if (usePIDs) {
                        point.setPID(ParseEx.parseInteger(record.get(fPID), tempI));
                    } else {
                        point.setPID(tempI);
                    }

                    //Metadata
                    point.setMetadataCN(Consts.EmptyGuid);

                    tempIndex = indexes.get(point.getPolyCN());
                    
                    //Index
                    if (hasIndex) {
                        tempIndex = ParseEx.parseInteger(record.get(fIndex), tempIndex);
                    }
                    
                    point.setIndex(tempIndex);
                    tempIndex++;
                    indexes.put(point.getPolyCN(), tempIndex);

                    //Boundary
                    if (hasBnd) {
                        point.setOnBnd(ParseEx.parseBoolean(record.get(fBnd), true));
                    } else {
                        point.setOnBnd(true);
                    }

                    //Comment
                    if (hasComment) {
                        point.setComment(record.get(fCmt));
                    }

                    points.put(point.getCN(), point);

                    prevPoint = point;
                }

                if (!isCancelled()) {
                    TtPoint tmpPoint;
                    for (QuondamPoint qp : qps.keySet()) {
                        temp = qps.get(qp);

                        if (remapCNs.containsKey(temp)) {
                            temp = remapCNs.get(temp);
                        }
    
                        if (points.containsKey(temp)) {
                            tmpPoint = points.get(temp);
                            qp.setParentPoint(tmpPoint);
                            tmpPoint.addQuondamLink(qp.getCN());
                        } else {
                            throw new Exception("parent point not found in points");
                        }
                    }
                }


                if (!isCancelled()) {
                    if (polygons.size() > 0) {
                        for (TtPolygon p : polygons.values()) {
                            if (!dal.insertPolygon(p)) {
                                throw new RuntimeException("Failed to insert Polygons");
                            }
                        }
                    }

                    if (groups.size() > 0) {
                        for (TtGroup group : groups.values()) {
                            if (!dal.insertGroup(group)) {
                                throw new RuntimeException("Failed to insert Groups");
                            }
                        }
                    }

                    if (points.size() > 0) {
                        if (!dal.insertPoints(points.values())) {
                            throw new RuntimeException("Failed to insert Points");
                        }
                    }

                    return new ImportResult(ImportResultCode.Success);
                } else {
                    return new ImportResult(ImportResultCode.Cancelled);
                }
            } catch (Exception ex) {
                params.getApp().getReport().writeError(ex.getMessage(), "Import:TextImportTask", ex.getStackTrace());
                return new ImportResult(ImportResultCode.ImportFailure, "Data error");
            }
        }

        private int getFieldColumn(Map<TextFieldType, Integer> map, TextFieldType type, boolean use) {
            if (use && map.containsKey(type)) {
                return map.get(type);
            }

            return -1;
        }


        public static class TextImportParams extends ImportParams {
            private final Map<TextFieldType, Integer> columnMap;
            private final boolean advImport;
            private final List<String> polygonNames;

            public TextImportParams(TwoTrailsApp app, Uri filePath, Map<TextFieldType, Integer> columnMap, List<String> polygonNames, boolean advImport) {
                super(app, filePath);
                this.columnMap = columnMap;
                this.polygonNames = polygonNames;
                this.advImport = advImport;

                //for import
                //make sure there is:
                //slope dist, ang and types, as well as at least fwd or bk az
                //parent cn
            }

            public Map<TextFieldType, Integer> getColumnMap() {
                return columnMap;
            }

            public boolean isAdvImport() {
                return advImport;
            }

            public List<String> getPolygonNames() {
                return polygonNames;
            }
        }
    }

    public enum TextFieldType {
        NO_FIELD(0),
        CN(1),
        OPTYPE(2),
        INDEX(3),
        PID(4),
        TIME(5),
        POLY_NAME(6),
        GROUP_NAME(7),
        COMMENT(8),
        META_CN(9),
        ONBND(10),
        UNADJX(11),
        UNADJY(12),
        UNADJZ(13),
        ACCURACY(14),
        MAN_ACC(15),
        RMSER(16),
        LATITUDE(17),
        LONGITUDE(18),
        ELEVATION(19),
        FWD_AZ(20),
        BK_AZ(21),
        SLOPE_DIST(22),
        SLOPE_DIST_TYPE(23),
        SLOPE_ANG(24),
        SLOPE_ANG_TYPE(25),
        PARENT_CN(26);


        private final int value;

        TextFieldType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TextFieldType parse(int id) {
            TextFieldType[] vals = values();
            if(vals.length > id && id > -1)
                return vals[id];
            throw new IllegalArgumentException("Invalid TextFieldType id: " + id);
        }

        public static TextFieldType parse(String value) {
            switch(value.toLowerCase()) {
                case "cn":
                case "point cn": return CN;
                case "op":
                case "optype":
                case "op type": return OPTYPE;
                case "index": return INDEX;
                case "point name":
                case "point id":
                case "pid": return PID;
                case "datetime":
                case "time":
                case "created":
                case "time created": return TIME;
                case "poly":
                case "polygon":
                case "poly name":
                case "polygon name": return POLY_NAME;
                case "group":
                case "group name": return GROUP_NAME;
                case "desc":
                case "description":
                case "cmt":
                case "comment": return COMMENT;
                case "meta":
                case "meta cn":
                case "metadata":
                case "metadata cn": return META_CN;
                case "onbnd":
                case "on bnd":
                case "onbound":
                case "boundary":
                case "onboundary":
                case "on boundary": return ONBND;
                case "x":
                case "unadjx":
                case "unadj x":
                case "unadjusted x": return UNADJX;
                case "y":
                case "unadjy":
                case "unadj y":
                case "unadjusted y": return UNADJY;
                case "z":
                case "unadjz":
                case "unadj z":
                case "unadjusted z": return UNADJZ;
                case "acc":
                case "accuracy": return ACCURACY;
                case "man acc (m)":
                case "manacc":
                case "man acc":
                case "manualacc":
                case "manual acc": return MAN_ACC;
                case "rmser": return RMSER;
                case "lat":
                case "latitude": return LATITUDE;
                case "lon":
                case "long":
                case "longitude": return LONGITUDE;
                case "elev":
                case "elevation":
                case "elevation (m)":
                case "alt":
                case "altitude": return ELEVATION;
                case "fwdaz":
                case "fwd az":
                case "forward az":
                case "forward azimuth": return FWD_AZ;
                case "bkaz":
                case "bk az":
                case "backward az":
                case "backward azimuth": return BK_AZ;
                case "slpdist":
                case "slopedist":
                case "slp dist":
                case "slope dist":
                case "slope distance": return SLOPE_DIST;
                case "slp d type":
                case "slope d type":
                case "slope dist type": return SLOPE_DIST_TYPE;
                case "slpang":
                case "slopeang":
                case "slp ang":
                case "slope ang":
                case "slopeangle":
                case "slope angle": return SLOPE_ANG;
                case "slp a type":
                case "slope a type":
                case "slope angle type": return SLOPE_ANG_TYPE;
                case "parent":
                case "parentcn":
                case "parent cn": return PARENT_CN;
                default: return NO_FIELD;
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case NO_FIELD: return "No Field";
                case CN: return "CN";
                case OPTYPE: return "Op Type";
                case INDEX: return "Index";
                case PID: return "PID";
                case TIME: return "Created Time";
                case POLY_NAME: return "Poly Name";
                case GROUP_NAME: return "Group Name";
                case COMMENT: return "Comment";
                case META_CN: return "Metadata CN";
                case ONBND: return "On Bnd";
                case UNADJX: return "UnAdjusted X";
                case UNADJY: return "UnAdjusted Y";
                case UNADJZ: return "UnAdjusted Z";
                case ACCURACY: return "Accuracy";
                case MAN_ACC: return "Manual Acc";
                case RMSER: return "RMSEr";
                case LATITUDE: return "Latitude";
                case LONGITUDE: return "Longitude";
                case ELEVATION: return "Elevation";
                case FWD_AZ: return "Forward Az";
                case BK_AZ: return "Backward Az";
                case SLOPE_DIST: return "Slope Dist";
                case SLOPE_DIST_TYPE: return "Slope D Type";
                case SLOPE_ANG: return "Slope Angle";
                case SLOPE_ANG_TYPE: return "Slope A Type";
                case PARENT_CN: return "Parent CN";
                default: throw new IllegalArgumentException("Invalid TextFieldType id: " + this.getValue());
            }
        }
    }

    //endregion


    //region GPX
    public static class GPXImportTask extends ImportTask<GPXImportTask.GPXImportParams> {

        @Override
        protected void onError(Exception exception) {

        }

        @Override
        protected ImportResult onBackgroundWork(GPXImportParams params) {
            if (params == null || !validatePolyParams(params.getPolyParms())) {
                return new ImportResult(ImportResultCode.InvalidParams);
            }

            if (params.getFilePath() == null) {
                return new ImportResult(ImportResultCode.InvalidParams, "No File selected");
            }

            DataAccessLayer dal = params.getDal();

            try {
                ArrayList<TtPolygon> polygons = new ArrayList<>();
                ArrayList<TtPoint> points = new ArrayList<>();

                TtPolygon poly;
                int polyCount = dal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName);

                for (GPXPolyParams gpp: params.getPolyParms()) {
                    poly = new TtPolygon();
                    poly.setName(gpp.PolyName);

                    if (gpp.Accuracy != null) {
                        poly.setAccuracy(gpp.Accuracy);
                    }

                    if (gpp.IncrementAmount != null) {
                        poly.setIncrementBy(gpp.IncrementAmount);
                    }

                    if (gpp.PointStartIndex != null) {
                        poly.setPointStartIndex(gpp.PointStartIndex);
                    } else {
                        poly.setPointStartIndex(polyCount * 1000 + 1010);
                    }

                    if (gpp.Description != null) {
                        poly.setDescription(gpp.Description);
                    }

                    polygons.add(poly);

                    ArrayList<GpxPoint> gpxPoints = new ArrayList<>();

                    if (gpp.Polygon instanceof GpxTrack) {
                        for (GpxTrackSeg seg : ((GpxTrack)gpp.Polygon).getSegments()) {
                            gpxPoints.addAll(seg.getPoints());
                        }
                    } else if (gpp.Polygon instanceof GpxRoute) {
                        gpxPoints.addAll(((GpxRoute)gpp.Polygon).getPoints());
                    } else {
                        throw new RuntimeException("Unknown GPX track type");
                    }

                    int index = 0;
                    GpsPoint point, prevPoint = null;

                    for (GpxPoint gpxPoint : gpxPoints) {
                        point = new GpsPoint();

                        point.setIndex(index);
                        index++;

                        point.setPID(PointNamer.namePoint(prevPoint, poly));

                        point.setPolyCN(poly.getCN());
                        point.setPolyName(poly.getName());

                        point.setGroupCN(Consts.EmptyGuid);
                        point.setGroupName(Consts.Defaults.MainGroupName);

                        point.setMetadataCN(gpp.Metadata.getCN());

                        point.setOnBnd(true);

                        if (!StringEx.isEmpty(gpxPoint.getComment())) {
                            point.setComment(gpxPoint.getComment());
                        }


                        UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(gpxPoint.getLatitude(), gpxPoint.getLongitude(), gpp.Metadata.getZone());

                        point.setUnAdjX(coords.getX());
                        point.setUnAdjY(coords.getY());

                        point.setLatitude(gpxPoint.getLatitude());
                        point.setLongitude(gpxPoint.getLongitude());

                        if (gpxPoint.getAltitude() != null) {
                            point.setElevation(gpxPoint.getAltitude());
                            point.setUnAdjZ(TtUtils.Convert.distance(gpxPoint.getAltitude(), UomElevation.Meters, gpp.Metadata.getElevation()));
                        }


                        points.add(point);
                        prevPoint = point;
                    }

                    polyCount++;
                }


                if (!isCancelled()) {
                    if (polygons.size() > 0) {
                        for (TtPolygon p : polygons) {
                            if (!dal.insertPolygon(p)) {
                                throw new RuntimeException("Failed to insert Polygons");
                            }
                        }
                    }

                    if (points.size() > 0) {
                        if (!dal.insertPoints(points)) {
                            throw new RuntimeException("Failed to insert Points");
                        }
                    }

                    return new ImportResult(ImportResultCode.Success);
                } else {
                    return new ImportResult(ImportResultCode.Cancelled);
                }
            } catch (Exception ex) {
                params.getApp().getReport().writeError(ex.getMessage(), "Import:GPXImportTask", ex.getStackTrace());
                return new ImportResult(ImportResultCode.ImportFailure, "Data error");
            }
        }

        private boolean validatePolyParams(Collection<GPXPolyParams>  params) {
            if (params == null || params.size() < 1) {
                return false;
            }

            for (GPXPolyParams gpp : params) {
                if ((gpp.PointStartIndex != null && gpp.PointStartIndex < 0) ||
                        (StringEx.isEmpty(gpp.PolyName)) ||
                        (gpp.IncrementAmount != null && gpp.IncrementAmount < 1) ||
                        (gpp.Polygon == null) ||
                        (gpp.Accuracy != null && gpp.Accuracy < 0) ||
                        (gpp.Metadata == null)) {
                    return false;
                }
            }

            return true;
        }

        public static class GPXImportParams extends ImportParams {
            private final Collection<GPXPolyParams> polyParms;

            public GPXImportParams(TwoTrailsApp app, Uri filePath, Collection<GPXPolyParams> polyParms) {
                super(app, filePath);
                this.polyParms = polyParms;
            }

            public Collection<GPXPolyParams> getPolyParms() {
                return polyParms;
            }
        }

        public static class GPXPolyParams {
            public final String PolyName;
            public final String Description;
            public final Integer PointStartIndex;
            public final Integer IncrementAmount;
            public final Double Accuracy;
            public final GpxBaseTrack Polygon;
            public final TtMetadata Metadata;

            public GPXPolyParams(String polyName, String description, Integer pointStartIndex,
                                 Integer incrementAmount, Double accuracy, GpxBaseTrack polygon,
                                 TtMetadata metadata) {
                this.PolyName = polyName;
                this.Description = description;
                this.PointStartIndex = pointStartIndex;
                this.IncrementAmount = incrementAmount;
                this.Accuracy = accuracy;
                this.Polygon = polygon;
                this.Metadata = metadata;
            }
        }
    }
    //endregion


    //region KML / KMZ
    public static class KMLImportTask extends ImportTask<KMLImportTask.KMLImportParams> {

        @Override
        protected ImportResult onBackgroundWork(KMLImportParams params) {
            if (params == null || !validatePolyParams(params.getPolyParms())) {
                return new ImportResult(ImportResultCode.InvalidParams);
            }

            if (params.getFilePath() == null) {
                return new ImportResult(ImportResultCode.InvalidParams, "No File selected");
            }

            DataAccessLayer dal = params.getDal();

            try {
                ArrayList<TtPolygon> polygons = new ArrayList<>();
                ArrayList<TtPoint> points = new ArrayList<>();

                TtPolygon poly;
                int polyCount = dal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName);

                for (KMLPolyParams gpp: params.getPolyParms()) {
                    poly = new TtPolygon();
                    poly.setName(gpp.PolyName);

                    if (gpp.Accuracy != null) {
                        poly.setAccuracy(gpp.Accuracy);
                    }

                    if (gpp.IncrementAmount != null) {
                        poly.setIncrementBy(gpp.IncrementAmount);
                    }

                    if (gpp.PointStartIndex != null) {
                        poly.setPointStartIndex(gpp.PointStartIndex);
                    } else {
                        poly.setPointStartIndex(polyCount * 1000 + 1010);
                    }

                    if (gpp.Description != null) {
                        poly.setDescription(gpp.Description);
                    }

                    polygons.add(poly);

                    int index = 0;
                    GpsPoint point, prevPoint = null;

                    for (Coordinates coord : gpp.Polygon.getInnerBoundary() != null ?
                            gpp.Polygon.getInnerBoundary() : gpp.Polygon.getOuterBoundary()) {
                        point = new GpsPoint();

                        point.setIndex(index);
                        index++;

                        point.setPID(PointNamer.namePoint(prevPoint, poly));

                        point.setPolyCN(poly.getCN());
                        point.setPolyName(poly.getName());

                        point.setGroupCN(Consts.EmptyGuid);
                        point.setGroupName(Consts.Defaults.MainGroupName);

                        point.setMetadataCN(gpp.Metadata.getCN());

                        point.setOnBnd(true);

                        UTMCoords utmcoords = UTMTools.convertLatLonSignedDecToUTM(coord.getLatitude(), coord.getLongitude(), gpp.Metadata.getZone());

                        point.setUnAdjX(utmcoords.getX());
                        point.setUnAdjY(utmcoords.getY());

                        point.setLatitude(coord.getLatitude());
                        point.setLongitude(coord.getLongitude());

                        if (coord.getAltitude() != null) {
                            point.setElevation(coord.getAltitude());
                            point.setUnAdjZ(TtUtils.Convert.distance(coord.getAltitude(), UomElevation.Meters, gpp.Metadata.getElevation()));
                        }


                        points.add(point);
                        prevPoint = point;
                    }

                    polyCount++;
                }


                if (!isCancelled()) {
                    if (polygons.size() > 0) {
                        for (TtPolygon p : polygons) {
                            if (!dal.insertPolygon(p)) {
                                throw new RuntimeException("Failed to insert Polygons");
                            }
                        }
                    }

                    if (points.size() > 0) {
                        if (!dal.insertPoints(points)) {
                            throw new RuntimeException("Failed to insert Points");
                        }
                    }

                    return new ImportResult(ImportResultCode.Success);
                } else {
                    return new ImportResult(ImportResultCode.Cancelled);
                }
            } catch (Exception ex) {
                params.getApp().getReport().writeError(ex.getMessage(), "Import:GPXImportTask", ex.getStackTrace());
                return new ImportResult(ImportResultCode.ImportFailure, "Data error");
            }
        }

        private boolean validatePolyParams(Collection<KMLPolyParams>  params) {
            if (params == null || params.size() < 1) {
                return false;
            }

            for (KMLPolyParams gpp : params) {
                if ((gpp.PointStartIndex != null && gpp.PointStartIndex < 0) ||
                        (StringEx.isEmpty(gpp.PolyName)) ||
                        (gpp.IncrementAmount != null && gpp.IncrementAmount < 1) ||
                        (gpp.Polygon == null) ||
                        (gpp.Accuracy != null && gpp.Accuracy < 0) ||
                        (gpp.Metadata == null)) {
                    return false;
                }
            }

            return true;
        }

        public static class KMLImportParams extends ImportParams {
            private final Collection<KMLPolyParams> polyParms;

            public KMLImportParams(TwoTrailsApp app, Uri filePath, Collection<KMLPolyParams> polyParms) {
                super(app, filePath);
                this.polyParms = polyParms;
            }

            public Collection<KMLPolyParams> getPolyParms() {
                return polyParms;
            }
        }

        public static class KMLPolyParams {
            public String PolyName;
            public String Description;
            public Integer PointStartIndex;
            public Integer IncrementAmount;
            public Double Accuracy;
            public com.usda.fmsc.utilities.kml.Polygon Polygon;
            public TtMetadata Metadata;

            public KMLPolyParams(String polyName, String description, Integer pointStartIndex,
                                 Integer incrementAmount, Double accuracy, com.usda.fmsc.utilities.kml.Polygon polygon,
                                 TtMetadata metadata) {
                this.PolyName = polyName;
                this.Description = description;
                this.PointStartIndex = pointStartIndex;
                this.IncrementAmount = incrementAmount;
                this.Accuracy = accuracy;
                this.Polygon = polygon;
                this.Metadata = metadata;
            }
        }
    }
    //endregion

    //region TTX
    public static class TTXImportTask extends ImportTask<TTXImportTask.TTXImportParams> {

        @Override
        protected ImportResult onBackgroundWork(TTXImportParams params) {
            //check to make sure all quondams have parent polygons imported
            if (params == null) {
                return new ImportResult(ImportResultCode.InvalidParams);
            }

            if (params.getFilePath() == null) {
                return new ImportResult(ImportResultCode.InvalidParams, "No File selected");
            }

            DataAccessLayer dal = params.getDal();
            DataAccessLayer idal = params.getImportDal();

            try {
                ArrayList<TtPolygon> polygons = new ArrayList<>();
                ArrayList<TtPoint> points = new ArrayList<>();

                TtPolygon poly;
                //int polyCount = idal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName);

                HashMap<String, String> polyCNCvt = new HashMap<>();
                HashMap<String, TtPolygon> currPolys = dal.getPolygonsMap();

                HashMap<String, TtPoint> cPoints = dal.getPointsMap();
                HashMap<String, TtPoint> iPoints = idal.getPointsMap();

                Function<String, List<TtPoint>> filterPointsByPoly = (cn) -> {
                    ArrayList<TtPoint> fp = new ArrayList<>();
                    for (TtPoint p : iPoints.values()) {
                        if (p.getPolyCN().equals(cn))
                            fp.add(p);
                    }

                    return fp;
                };

                for (TtPolygon polygon: params.getPolygons()) {
                    String oCN = polygon.getCN();

                    if (currPolys.containsKey(polygon.getCN())) {
                        String newPolyUuid = UUID.randomUUID().toString();
                        polyCNCvt.put(oCN, newPolyUuid);
                        polygon.setCN(newPolyUuid);
                    }

                    polygons.add(polygon);

                    for (TtPoint point : filterPointsByPoly.apply(oCN)) {
                        String opCN = point.getCN();
                        String pointCNCvt = null;

                        if (cPoints.containsKey(point.getCN())) {
                            pointCNCvt = UUID.randomUUID().toString();
                            point.setCN(pointCNCvt);
                        }

                        if (point.hasQuondamLinks() && pointCNCvt != null) {
                            for (String ql : point.getLinkedPoints()) {
                                if (cPoints.containsKey(ql)) {
                                    QuondamPoint qp = (QuondamPoint)cPoints.get(ql);
                                    qp.removeQuondamLink(opCN);
                                    qp.addQuondamLink(pointCNCvt);
                                }
                            }
                        }

                        points.add(point);
                    }

                    //polyCount++;
                }

                if (!isCancelled()) {
                    if (polygons.size() > 0) {
                        for (TtPolygon p : polygons) {
                            if (!dal.insertPolygon(p)) {
                                throw new RuntimeException("Failed to insert Polygons");
                            }
                        }
                    }

                    if (points.size() > 0) {
                        if (!dal.insertPoints(points)) {
                            throw new RuntimeException("Failed to insert Points");
                        }
                    }

                    return new ImportResult(ImportResultCode.Success);
                } else {
                    return new ImportResult(ImportResultCode.Cancelled);
                }
            } catch (Exception ex) {
                params.getApp().getReport().writeError(ex.getMessage(), "Import:GPXImportTask", ex.getStackTrace());
                return new ImportResult(ImportResultCode.ImportFailure, "Data error");
            }
        }

        public static class TTXImportParams extends ImportParams {
            private final Collection<TtPolygon> polygons;
            private final DataAccessLayer idal;

            public TTXImportParams(TwoTrailsApp app, Uri filePath, Collection<TtPolygon> polygons, DataAccessLayer idal) {
                super(app, filePath);
                this.polygons = polygons;
                this.idal = idal;
            }

            public Collection<TtPolygon> getPolygons() {
                return polygons;
            }

            public DataAccessLayer getImportDal() {
                return idal;
            }
        }
    }
    //endregion


    public static abstract class ImportTask<IP extends ImportParams> extends TaskRunner.Task<IP, ImportResult> {
        ImportTaskListener listener;

        @Override
        protected void onComplete(ImportResult result) {
            if (listener != null) {
                listener.onTaskFinish(result);
            }
        }

        @Override
        protected void onError(Exception exception) {

        }

        public void setListener(ImportTaskListener listener) {
            this.listener = listener;
        }
    }


    public static abstract class ImportParams {
        TwoTrailsApp app;
        Uri filePath;

        public ImportParams(TwoTrailsApp app, Uri filePath) {
            this.filePath = filePath;
            this.app = app;
        }

        public DataAccessLayer getDal() {
            return app.getDAL();
        }

        public  TwoTrailsApp getApp() { return app; }

        public Uri getFilePath() {
            return filePath;
        }
    }


    public interface ImportTaskListener {
        void onTaskFinish(ImportResult result);
    }


    public static class ImportResult {
        private final ImportResultCode code;
        private final String message;

        public ImportResult(ImportResultCode code) {
            this(code, null);
        }

        public ImportResult(ImportResultCode code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public ImportResultCode getCode() {
            return code;
        }
    }


    public enum ImportResultCode {
        Success,
        Cancelled,
        ImportFailure,
        InvalidParams
    }
}
