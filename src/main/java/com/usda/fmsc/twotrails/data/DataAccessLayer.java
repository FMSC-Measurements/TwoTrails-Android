package com.usda.fmsc.twotrails.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.objects.GpsPoint;
import com.usda.fmsc.twotrails.objects.QuondamPoint;
import com.usda.fmsc.twotrails.objects.TravPoint;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.nmea.Satellite;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea.sentences.GSASentence;
import com.usda.fmsc.geospatial.Units.*;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class DataAccessLayer {
    private static DateTimeFormatter dtf = Consts.DateTimeFormatter;

    public String getFilePath() {
        return _FilePath;
    }

    public File getDBFile() { return _dbFile; }

    public TtDalVersion getDalVersion() { return _DalVersion; }


    private TtDalVersion _DalVersion;
    private SQLiteDatabase _db;
    private String _FilePath;
    private File _dbFile;


    //region Constructors / Open / Close
    public DataAccessLayer(String filePath) {
        _FilePath = filePath;

        if(TtUtils.fileExists(_FilePath))
            open();
        else
            CreateDB();
    }

    public boolean open() {
        if(TtUtils.fileExists(_FilePath)) {
            _db = SQLiteDatabase.openDatabase(_FilePath, null, 0);

            try {
                _dbFile = new File(_FilePath);
                _DalVersion = new TtDalVersion(getTtDbVersion());
            } catch (Exception ex) {
                return false;
            }

            return true;
        }
        return false;
    }

    public void close() {
        if(isOpen()) {
            _db.close();

            File f = new File(_FilePath + "-journal");
            if (f.exists()) {
                if (!f.delete()) {
                    TtUtils.TtReport.writeError("sql journal not deleted", "DataAccessLayer:close");
                }
            }
        }
    }

    public boolean isOpen() {
        return (_db != null && _db.isOpen());
    }
    //endregion

    //region Create DB
    private void CreateDB() {
        try {
            _dbFile = new File(_FilePath);

            _db = SQLiteDatabase.openOrCreateDatabase(_dbFile, null);
            //_db.rawQuery("PRAGMA journal_mode = MEMORY", null);
            _DalVersion = TwoTrailsSchema.SchemaVersion;

            CreatePolygonTable();
            CreateMetaDataTable();
            CreatePointTable();
            CreateGpsPointDataTable();
            CreateTravPointDataTable();
            CreateQuondamPointDataTable();
            CreateProjectInfoDataTable();
            CreateTtNmeaTable();
            CreateGroupTable();

            SetupProjInfo();
            insertMetadata(Global.getDefaultMeta());
            insertGroup(Global.getMainGroup());

        } catch (Exception ex) {
            //say that db creation failed, specific tables have already been logged

            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateDB");
        }
    }


    private void CreatePointTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.PointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreatePointTable");
            throw ex;
        }
    }
    
    private void CreatePolygonTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.PolygonSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreatePolygonTable");
            throw ex;
        }
    }

    private void CreateGroupTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.GroupSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreatePolygonTable");
            throw ex;
        }

    }

    private void CreateMetaDataTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.MetadataSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateMetaDataTable");
            throw ex;
        }
    }

    private void CreateGpsPointDataTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.GpsPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateGpsPointDataTable");
        }
    }

    private void CreateTravPointDataTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.TravPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateTravPointDataTable");
        }
    }

    private void CreateQuondamPointDataTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.QuondamPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateQuondamPointDataTable");
        }
    }

    private void CreateProjectInfoDataTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.ProjectInfoSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateProjectInfoDataTable");
        }
    }

    private void CreateTtNmeaTable() {
        try
        {
            _db.execSQL(TwoTrailsSchema.TtNmeaSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateTtnmeaTable");
        }
    }
    //endregion


    //region Polygons
    //region Get
    public ArrayList<TtPolygon> getPolygons() {
        return getPolygons(null);
    }

    public HashMap<String, TtPolygon> getPolygonsMap() {
        HashMap<String, TtPolygon> polys = new HashMap<>();
        for(TtPolygon polygon : getPolygons()) {
            polys.put(polygon.getCN(), polygon);
        }
        return  polys;
    }

    public TtPolygon getPolygonByCN(String cn) {
        ArrayList<TtPolygon> polys = getPolygons(String.format("CN = '%s'", cn));

        if(polys != null && polys.size() > 0)
            return polys.get(0);
        return null;
    }

    private ArrayList<TtPolygon> getPolygons(String where) {
        ArrayList<TtPolygon> polys = new ArrayList<>();

        try {
            String query = String.format("select * from %s%s order by datetime(%s) asc",
                    TwoTrailsSchema.PolygonSchema.TableName,
                    StringEx.isEmpty(where) ? StringEx.Empty : String.format(" where %s", where),
                    TwoTrailsSchema.PolygonSchema.TimeCreated);

            Cursor c = _db.rawQuery(query, null);

            TtPolygon poly;

            if (c.moveToFirst()) {
                do {
                    poly = new TtPolygon();

                    if (!c.isNull(0))
                        poly.setCN(c.getString(0));
                    if (!c.isNull(1))
                        poly.setName(c.getString(1));
                    if (!c.isNull(2))
                        poly.setAccuracy(c.getDouble(2));
                    if (!c.isNull(3))
                        poly.setDescription(c.getString(3));
                    if (!c.isNull(4))
                        poly.setArea(c.getDouble(4));
                    if (!c.isNull(5))
                        poly.setPerimeter(c.getDouble(5));
                    if (!c.isNull(6))
                        poly.setIncrementBy(c.getInt(6));
                    if (!c.isNull(7))
                        poly.setPointStartIndex(c.getInt(7));
                    if (!c.isNull(8))
                        poly.setTime(dtf.parseDateTime(c.getString(8)));

                    polys.add(poly);
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getPolygonNames");
            return null;
        }

        return polys;
    }
    //endregion

    //region Insert
    public boolean insertPolygon(TtPolygon poly) {
        boolean success = false;

        try {
            _db.beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, poly.getCN());
            cvs.put(TwoTrailsSchema.PolygonSchema.PolyID, poly.getName());
            cvs.put(TwoTrailsSchema.PolygonSchema.Description, poly.getDescription());
            cvs.put(TwoTrailsSchema.PolygonSchema.Accuracy, poly.getAccuracy());
            cvs.put(TwoTrailsSchema.PolygonSchema.Area, poly.getArea());
            cvs.put(TwoTrailsSchema.PolygonSchema.Perimeter, poly.getPerimeter());
            cvs.put(TwoTrailsSchema.PolygonSchema.PointStartIndex, poly.getPointStartIndex());
            cvs.put(TwoTrailsSchema.PolygonSchema.IncrementBy, poly.getIncrementBy());
            cvs.put(TwoTrailsSchema.PolygonSchema.TimeCreated, dtf.print(poly.getTime()));

            _db.insert(TwoTrailsSchema.PolygonSchema.TableName, null, cvs);

            _db.setTransactionSuccessful();
            success = true;
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertPolygon");
        } finally {
            _db.endTransaction();
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updatePolygon(TtPolygon poly) {
        int success = -1;

        try {
            _db.beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.PolygonSchema.PolyID, poly.getName());
            cvs.put(TwoTrailsSchema.PolygonSchema.Description, poly.getDescription());
            cvs.put(TwoTrailsSchema.PolygonSchema.Accuracy, poly.getAccuracy());
            cvs.put(TwoTrailsSchema.PolygonSchema.Area, poly.getArea());
            cvs.put(TwoTrailsSchema.PolygonSchema.Perimeter, poly.getPerimeter());
            cvs.put(TwoTrailsSchema.PolygonSchema.PointStartIndex, poly.getPointStartIndex());
            cvs.put(TwoTrailsSchema.PolygonSchema.IncrementBy, poly.getIncrementBy());

            success = _db.update(TwoTrailsSchema.PolygonSchema.TableName, cvs,
                String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, poly.getCN()), null);

            _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updatePolygon");
        } finally {
            _db.endTransaction();
        }

        return success > 0;
    }
    //endregion

    //region Delete
    public boolean deletePolygon(String cn) {
        boolean success = false;

        try {
            success = _db.delete(TwoTrailsSchema.PolygonSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deletePolygon");
        }

        return success;
    }
    //endregion
    //endregion


    //region Points
    //region Get
    public TtPoint getPointByCN(String cn) {
        ArrayList<TtPoint> points =
            getPoints(String.format("%s = '%s'",
                    TwoTrailsSchema.SharedSchema.CN, cn));

        if(points.size() > 0)
            return points.get(0);
        return null;
    }

    public TtPoint getFirstPointInPolygon(String polyCN) {
        ArrayList<TtPoint> points =
                getPoints(String.format("%s = '%s'",
                        TwoTrailsSchema.PointSchema.PolyCN, polyCN), 1);

        if(points != null && points.size() > 0)
            return points.get(0);
        return null;
    }

    public int getPointCountInPolygon(String polyCN) {
        return getItemsCount(
                TwoTrailsSchema.PointSchema.TableName,
                TwoTrailsSchema.PointSchema.PolyCN,
                polyCN);
    }

    public int getPointCountInGroup(String groupCN) {
        return getItemsCount(
                TwoTrailsSchema.PointSchema.TableName,
                TwoTrailsSchema.PointSchema.GroupCN,
                groupCN);
    }

    public ArrayList<TtPoint> getBoundaryPointsInPoly(String polyCN) {
        return getPoints(String.format("%s = '%s' and %s = 1",
                TwoTrailsSchema.PointSchema.PolyCN, polyCN,
                TwoTrailsSchema.PointSchema.OnBoundary));
    }

    public ArrayList<TtPoint> getPointsInPolygon(String polyCN) {
        return getPoints(String.format("%s = '%s'",
                TwoTrailsSchema.PointSchema.PolyCN, polyCN));
    }

    public ArrayList<TtPoint> getPointsWithMeta(String metaCN) {
        return getPoints(String.format("%s = '%s'",
                TwoTrailsSchema.PointSchema.MetadataCN, metaCN));
    }

    public ArrayList<TtPoint> getGpsPointsWithMeta(String metaCN) {
        return getPoints(String.format("%s = '%s' and %s = '%s' or %s = '%s' or %s = '%s' or %s = '%s'",
                TwoTrailsSchema.PointSchema.MetadataCN, metaCN,
                TwoTrailsSchema.PointSchema.Operation, Units.OpType.GPS.toString(),
                TwoTrailsSchema.PointSchema.Operation, Units.OpType.Walk.toString(),
                TwoTrailsSchema.PointSchema.Operation, Units.OpType.Take5.toString(),
                TwoTrailsSchema.PointSchema.Operation, Units.OpType.WayPoint.toString()));
    }

    public ArrayList<TtPoint> getPointsInGroup(String groupCN) {
        return getPoints(String.format("%s = '%s'",
                TwoTrailsSchema.PointSchema.GroupCN, groupCN));
    }

    public ArrayList<TtPoint> getPoints() {
        return getPoints(null, 0);
    }

    private ArrayList<TtPoint> getPoints(String where) {
        return getPoints(where, 0);
    }

    @Nullable
    private ArrayList<TtPoint> getPoints(String where, int limit) {
        ArrayList<TtPoint> points = new ArrayList<>();

        try {
            String query = String.format("select * from %s%s order by %s, %s %s",
                    TwoTrailsSchema.PointSchema.TableName,
                    StringEx.isEmpty(where) ? StringEx.Empty : String.format(" where %s", where),
                    TwoTrailsSchema.PointSchema.PolyName,
                    TwoTrailsSchema.PointSchema.Order,
                    limit > 0 ? " limit " + limit : StringEx.Empty);

            Cursor c = _db.rawQuery(query, null);

            TtPoint point;

            if (c.moveToFirst()) {
                do {

                    if(!c.isNull(7)) {
                        point = TtUtils.getPointByOpType(Units.OpType.parse(c.getString(7)));
                    } else {
                        throw new Exception("Point has no OpType");
                    }

                    if (!c.isNull(0))
                        point.setCN(c.getString(0));

                    if (!c.isNull(1))
                        point.setIndex(c.getInt(1));

                    if (!c.isNull(2))
                        point.setPID(c.getInt(2));

                    if (!c.isNull(3))
                        point.setPolyName(c.getString(3));

                    if (!c.isNull(4))
                        point.setPolyCN(c.getString(4));

                    if (!c.isNull(5))
                        point.setOnBnd(ParseEx.parseBoolean(c.getString(5)));

                    if (!c.isNull(6))
                        point.setComment(c.getString(6));

                    if (!c.isNull(8))
                        point.setMetadataCN(c.getString(8));

                    if (!c.isNull(9))
                        point.setTime(dtf.parseDateTime(c.getString(9)));

                    if (!c.isNull(10))
                        point.setAdjX(c.getDouble(10));

                    if (!c.isNull(11))
                        point.setAdjY(c.getDouble(11));

                    if (!c.isNull(12))
                        point.setAdjZ(c.getDouble(12));

                    if (!c.isNull(13))
                        point.setUnAdjX(c.getDouble(13));

                    if (!c.isNull(14))
                        point.setUnAdjY(c.getDouble(14));

                    if (!c.isNull(15))
                        point.setUnAdjZ(c.getDouble(15));

                    if (!c.isNull(16))
                        point.setAccuracy(c.getDouble(16));

                    if (!c.isNull(17)) {
                        for (String cn : c.getString(17).split("_")) {
                            if (!StringEx.isEmpty(cn))
                                point.addQuondamLink(cn);
                        }
                    }

                    if (!c.isNull(18))
                        point.setGroupName(c.getString(18));
                    if (!c.isNull(19))
                        point.setGroupCN(c.getString(19));


                    if (point.isGpsType())
                        getGpsPointData(point);
                    else if (point.isTravType())
                        getTravPointData(point);
                    else if (point.getOp() == Units.OpType.Quondam)
                        getQuondamPointData(point);

                    points.add(point);

                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getPoints");
            return null;
        }

        return points;
    }

    public HashMap<String, TtPoint> getPointsMap() {
        HashMap<String, TtPoint> points = new HashMap<>();
        for(TtPoint point : getPoints()) {
            points.put(point.getCN(), point);
        }
        return  points;
    }

    //region subPoint Data
    private void getGpsPointData(TtPoint point) {
        GpsPoint g = (GpsPoint)point;

        try {
            String query = String.format("select * from %s where %s = '%s'",
                    TwoTrailsSchema.GpsPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    g.getCN());

            Cursor c = _db.rawQuery(query, null);

            if (c.moveToFirst()) {
                if(!c.isNull(1))
                    g.setLatitude(c.getDouble(1));
                if(!c.isNull(2))
                    g.setLongitude(c.getDouble(2));
                if(!c.isNull(3))
                    g.setElevation(c.getDouble(3));
                if(!c.isNull(4))
                    g.setManualAccuracy(c.getDouble(4));
                if(!c.isNull(5))
                    g.setRMSEr(c.getDouble(5));
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getGpsPointData");
        }
    }

    private void getTravPointData(TtPoint point) {
        TravPoint t = (TravPoint)point;

        try {
            String query = String.format("select * from %s where %s = '%s'",
                    TwoTrailsSchema.TravPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    t.getCN());

            Cursor c = _db.rawQuery(query, null);

            if (c.moveToFirst()) {
                if(!c.isNull(1))
                    t.setFwdAz(c.getDouble(1));
                if(!c.isNull(2))
                    t.setBkAz(c.getDouble(2));
                if(!c.isNull(3))
                    t.setSlopeDistance(c.getDouble(3));
                if(!c.isNull(4))
                    t.setSlopeAngle(c.getDouble(4));
                if(!c.isNull(5))
                    t.setAccuracy(c.getDouble(5));
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getTravPointData");
        }
    }

    private void getQuondamPointData(TtPoint point) {
        QuondamPoint q = (QuondamPoint)point;

        try {
            String query = String.format("select * from %s where %s = '%s'",
                    TwoTrailsSchema.QuondamPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    q.getCN());

            Cursor c = _db.rawQuery(query, null);

            if (c.moveToFirst()) {
                if(!c.isNull(1)) {
                    TtPoint p = getPointByCN(c.getString(1));
                    if (p != null)
                        q.setParentPoint(p);
                }

                if(!c.isNull(2))
                    q.setManualAccuracy(c.getDouble(2));
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getQuondamPointData");
        }
    }
    //endregion
    //endregion

    //region Insert
    public boolean insertPoint(TtPoint point) {
        boolean success = false;

        if (point == null)
            return false;

        try {
            _db.beginTransaction();

            success = insertBasePoint(point);

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertPoint");
            success = false;
        } finally {
            _db.endTransaction();
        }

        return success;
    }

    public boolean insertPoints(Collection<TtPoint> points) {
        boolean success = true;

        if (points == null || points.size() < 1)
            return false;

        try {
            _db.beginTransaction();

            for(TtPoint point : points) {
                if (!insertBasePoint(point)) {
                    success = false;
                    break;
                }
            }

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertPoints");
            success = false;
        } finally {
            _db.endTransaction();
        }

        return success;
    }


    private boolean insertBasePoint(TtPoint point) {
        if(point == null)
            return false;

        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.PointSchema.Order, point.getIndex());
            cvs.put(TwoTrailsSchema.PointSchema.Operation, point.getOp().toString());
            cvs.put(TwoTrailsSchema.PointSchema.ID, point.getPID());
            cvs.put(TwoTrailsSchema.PointSchema.PolyName, point.getPolyName());
            cvs.put(TwoTrailsSchema.PointSchema.PolyCN, point.getPolyCN());
            cvs.put(TwoTrailsSchema.PointSchema.MetadataCN, point.getMetadataCN());
            cvs.put(TwoTrailsSchema.PointSchema.OnBoundary, point.isOnBnd());
            cvs.put(TwoTrailsSchema.PointSchema.Comment, point.getComment());

            cvs.put(TwoTrailsSchema.PointSchema.AdjX, point.getAdjX());
            cvs.put(TwoTrailsSchema.PointSchema.AdjY, point.getAdjY());
            cvs.put(TwoTrailsSchema.PointSchema.AdjZ, point.getAdjZ());
            cvs.put(TwoTrailsSchema.PointSchema.UnAdjX, point.getUnAdjX());
            cvs.put(TwoTrailsSchema.PointSchema.UnAdjY, point.getUnAdjY());
            cvs.put(TwoTrailsSchema.PointSchema.UnAdjZ, point.getUnAdjZ());

            cvs.put(TwoTrailsSchema.PointSchema.Accuracy, point.getAccuracy());

            cvs.put(TwoTrailsSchema.PointSchema.GroupName, point.getGroupName());
            cvs.put(TwoTrailsSchema.PointSchema.GroupCN, point.getGroupCN());

            cvs.put(TwoTrailsSchema.PointSchema.Time, dtf.print(point.getTime()));

            if (point.hasQuondamLinks()) {
                cvs.put(TwoTrailsSchema.PointSchema.QuondamLinks, point.getLinkedPointsString());
            } else {
                cvs.put(TwoTrailsSchema.PointSchema.QuondamLinks, StringEx.Empty);
            }

            _db.insert(TwoTrailsSchema.PointSchema.TableName, null, cvs);

            switch (point.getOp()) {
                case GPS:
                case Take5:
                case Walk:
                case WayPoint:
                    insertGpsData((GpsPoint) point);
                    break;
                case Traverse:
                case SideShot:
                    insertTravData((TravPoint) point);
                    break;
                case Quondam:
                    insertQuondamData((QuondamPoint) point);
                    break;
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertBasePoint");
            return false;
        }
        return true;
    }

    private void insertGpsData(GpsPoint point) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.GpsPointSchema.UserAccuracy, point.getManualAccuracy());
            cvs.put(TwoTrailsSchema.GpsPointSchema.RMSEr, point.getRMSEr());
            cvs.put(TwoTrailsSchema.GpsPointSchema.Latitude, point.getLatitude());
            cvs.put(TwoTrailsSchema.GpsPointSchema.Longitude, point.getLongitude());
            cvs.put(TwoTrailsSchema.GpsPointSchema.Elevation, point.getElevation());

            _db.insert(TwoTrailsSchema.GpsPointSchema.TableName, null, cvs);

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertGpsData");
        }
    }

    private void insertTravData(TravPoint point) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.TravPointSchema.BackAz, point.getBkAz());
            cvs.put(TwoTrailsSchema.TravPointSchema.ForwardAz, point.getFwdAz());
            cvs.put(TwoTrailsSchema.TravPointSchema.HorizDistance, point.getHorizontalDistance());
            cvs.put(TwoTrailsSchema.TravPointSchema.SlopeDistance, point.getSlopeDistance());
            cvs.put(TwoTrailsSchema.TravPointSchema.VerticalAngle, point.getSlopeAngle());


            _db.insert(TwoTrailsSchema.TravPointSchema.TableName, null, cvs);

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertTravData");
        }
    }

    private void insertQuondamData(QuondamPoint point) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.QuondamPointSchema.ParentPointCN, point.getParentCN());
            cvs.put(TwoTrailsSchema.QuondamPointSchema.ManualAccuracy, point.getManualAccuracy());

            _db.insert(TwoTrailsSchema.QuondamPointSchema.TableName, null, cvs);

            updateQuondamLink(point);
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertQuondamData");
        }
    }
    //endregion

    //region Update

    public boolean updatePoint(TtPoint updatedPoint) {
        boolean success = false;

        try {
            _db.beginTransaction();

            success = updateBasePoint(updatedPoint);

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            _db.endTransaction();
        }
        return success;
    }

    public boolean updatePoint(TtPoint updatedPoint, TtPoint oldPoint) {
        boolean success = false;

        try {
            _db.beginTransaction();

            success = updateBasePoint(updatedPoint, oldPoint);

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            _db.endTransaction();
        }
        return success;
    }

    public boolean updatePoints(Collection<TtPoint> updatedPoints) {
        boolean success = false;

        try {
            _db.beginTransaction();

            for(TtPoint point : updatedPoints) {
                success = updateBasePoint(point);

                if(!success)
                    break;
            }

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            _db.endTransaction();
        }
        return success;
    }

    public boolean updatePoints(List<TtPoint> updatedPoints, List<TtPoint> oldPoints) {
        boolean success = false;

        if(updatedPoints == null || oldPoints == null ||
                updatedPoints.size() != oldPoints.size() ||
                updatedPoints.size() < 1)
            return false;

        try {
            _db.beginTransaction();

            for (int i = 0; i < updatedPoints.size(); i++) {
                success = updateBasePoint(updatedPoints.get(i), oldPoints.get(i));

                if(!success)
                    break;
            }

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            _db.endTransaction();
        }
        return success;
    }

    private boolean updateBasePoint(TtPoint updatedPoint) {
        return updateBasePoint(updatedPoint, updatedPoint);
    }

    private boolean updateBasePoint(TtPoint updatedPoint, TtPoint oldPoint) {
        if(updatedPoint == null)
            return false;

        if(oldPoint == null) {
            return insertBasePoint(updatedPoint);
        } else {
            if(!updatedPoint.getCN().equals(oldPoint.getCN()))
                return false;

            try {
                ContentValues cvs = new ContentValues();

                cvs.put(TwoTrailsSchema.PointSchema.Order, updatedPoint.getIndex());
                cvs.put(TwoTrailsSchema.PointSchema.Operation, updatedPoint.getOp().toString());
                cvs.put(TwoTrailsSchema.PointSchema.ID, updatedPoint.getPID());
                cvs.put(TwoTrailsSchema.PointSchema.PolyName, updatedPoint.getPolyName());
                cvs.put(TwoTrailsSchema.PointSchema.PolyCN, updatedPoint.getPolyCN());
                cvs.put(TwoTrailsSchema.PointSchema.MetadataCN, updatedPoint.getMetadataCN());
                cvs.put(TwoTrailsSchema.PointSchema.OnBoundary, updatedPoint.isOnBnd());
                cvs.put(TwoTrailsSchema.PointSchema.Comment, updatedPoint.getComment());

                cvs.put(TwoTrailsSchema.PointSchema.AdjX, updatedPoint.getAdjX());
                cvs.put(TwoTrailsSchema.PointSchema.AdjY, updatedPoint.getAdjY());
                cvs.put(TwoTrailsSchema.PointSchema.AdjZ, updatedPoint.getAdjZ());
                cvs.put(TwoTrailsSchema.PointSchema.UnAdjX, updatedPoint.getUnAdjX());
                cvs.put(TwoTrailsSchema.PointSchema.UnAdjY, updatedPoint.getUnAdjY());
                cvs.put(TwoTrailsSchema.PointSchema.UnAdjZ, updatedPoint.getUnAdjZ());

                cvs.put(TwoTrailsSchema.PointSchema.Accuracy, updatedPoint.getAccuracy());

                cvs.put(TwoTrailsSchema.PointSchema.GroupName, updatedPoint.getGroupName());
                cvs.put(TwoTrailsSchema.PointSchema.GroupCN, updatedPoint.getGroupCN());

                if (updatedPoint.hasQuondamLinks()) {
                    cvs.put(TwoTrailsSchema.PointSchema.QuondamLinks, updatedPoint.getLinkedPointsString());
                } else {
                    cvs.put(TwoTrailsSchema.PointSchema.QuondamLinks, StringEx.Empty);
                }


                int rows =_db.update(TwoTrailsSchema.PointSchema.TableName, cvs,
                        TwoTrailsSchema.SharedSchema.CN + "=?", new String[] { updatedPoint.getCN() });

                if (rows < 1)
                    return false;

                if(updatedPoint.getOp() != oldPoint.getOp()) {
                    changeOperation(updatedPoint, oldPoint);
                } else {
                    switch (updatedPoint.getOp()) {
                        case GPS:
                        case Take5:
                        case Walk:
                        case WayPoint:
                            updateGpsData((GpsPoint)updatedPoint);
                            break;
                        case Traverse:
                        case SideShot:
                            updateTravData((TravPoint)updatedPoint);
                            break;
                        case Quondam:
                            updateQuondamData((QuondamPoint)updatedPoint, (QuondamPoint)oldPoint);
                            break;
                    }
                }
            } catch (Exception ex) {
                TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateBasePoint");
                return false;
            }
        }

        return true;
    }


    private void changeOperation(TtPoint updatedPoint, TtPoint oldPoint) {
        switch (oldPoint.getOp()) {
            case GPS:
            case Take5:
            case Walk:
            case WayPoint:
                removeGpsData((GpsPoint)oldPoint);
                break;
            case Traverse:
            case SideShot:
                removeTravData((TravPoint)oldPoint);
                break;
            case Quondam:
                removeQuondamData((QuondamPoint)oldPoint);
                break;
        }

        switch (updatedPoint.getOp()) {
            case GPS:
            case Take5:
            case Walk:
            case WayPoint:
                insertGpsData((GpsPoint)updatedPoint);
                break;
            case Traverse:
            case SideShot:
                insertTravData((TravPoint)updatedPoint);
                break;
            case Quondam:
                insertQuondamData((QuondamPoint)updatedPoint);
                break;
        }
    }

    private void updateGpsData(GpsPoint updatedPoint) {
        ContentValues cvs = new ContentValues();

        cvs.put(TwoTrailsSchema.SharedSchema.CN, updatedPoint.getCN());
        cvs.put(TwoTrailsSchema.GpsPointSchema.UserAccuracy, updatedPoint.getManualAccuracy());
        cvs.put(TwoTrailsSchema.GpsPointSchema.RMSEr, updatedPoint.getRMSEr());
        cvs.put(TwoTrailsSchema.GpsPointSchema.Latitude, updatedPoint.getLatitude());
        cvs.put(TwoTrailsSchema.GpsPointSchema.Longitude, updatedPoint.getLongitude());
        cvs.put(TwoTrailsSchema.GpsPointSchema.Elevation, updatedPoint.getElevation());

        _db.update(TwoTrailsSchema.GpsPointSchema.TableName, cvs,
                TwoTrailsSchema.SharedSchema.CN + "=?", new String[]{updatedPoint.getCN()});
    }

    private void updateTravData(TravPoint updatedPoint) {
        ContentValues cvs = new ContentValues();

        cvs.put(TwoTrailsSchema.SharedSchema.CN, updatedPoint.getCN());
        cvs.put(TwoTrailsSchema.TravPointSchema.BackAz, updatedPoint.getBkAz());
        cvs.put(TwoTrailsSchema.TravPointSchema.ForwardAz, updatedPoint.getFwdAz());
        cvs.put(TwoTrailsSchema.TravPointSchema.HorizDistance, updatedPoint.getHorizontalDistance());
        cvs.put(TwoTrailsSchema.TravPointSchema.SlopeDistance, updatedPoint.getSlopeDistance());
        cvs.put(TwoTrailsSchema.TravPointSchema.VerticalAngle, updatedPoint.getSlopeAngle());

        _db.update(TwoTrailsSchema.TravPointSchema.TableName, cvs,
                TwoTrailsSchema.SharedSchema.CN + "=?", new String[]{ updatedPoint.getCN() });
    }

    private void updateQuondamData(QuondamPoint updatedPoint, QuondamPoint oldPoint) {
        if(!updatedPoint.getParentCN().equals(oldPoint.getParentCN())) {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, updatedPoint.getCN());
            cvs.put(TwoTrailsSchema.QuondamPointSchema.ParentPointCN, updatedPoint.getParentCN());
            cvs.put(TwoTrailsSchema.QuondamPointSchema.ManualAccuracy, updatedPoint.getManualAccuracy());

            _db.update(TwoTrailsSchema.QuondamPointSchema.TableName, cvs,
                    TwoTrailsSchema.SharedSchema.CN + "=?", new String[]{updatedPoint.getCN()});

            removeQuondamLink(oldPoint);
            updateQuondamLink(updatedPoint);
        }
    }


    private void updateQuondamLink(QuondamPoint point) {
        if(point.hasParent()) {
            TtPoint linkedPoint = getPointByCN(point.getParentCN());

            if(linkedPoint != null) {
                linkedPoint.addQuondamLink(point.getCN());
                updatePoint(linkedPoint, point.getParentPoint());
            }
        } else {
            throw new NullPointerException("Quondam has no parent to save.");
        }
    }

    private void removeQuondamLink(QuondamPoint point) {
        if(point.hasParent()) {
            TtPoint linkedPoint = getPointByCN(point.getParentCN());

            if(linkedPoint != null) {
                linkedPoint.removeQuondamLink(point.getCN());
                updatePoint(linkedPoint, point.getParentPoint());
            }
        }
    }
    //endregion

    //region Delete
    private boolean deletePoint(TtPoint point) {
        boolean success = false;
        String pointCN = point.getCN();

        try {
            success = _db.delete(TwoTrailsSchema.PointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { pointCN }) > 0;

            if(success) {
                switch (point.getOp()) {
                    case GPS:
                    case Take5:
                    case Walk:
                    case WayPoint:
                        removeGpsData((GpsPoint)point);
                        break;
                    case Traverse:
                    case SideShot:
                        removeTravData((TravPoint)point);
                        break;
                    case Quondam:
                        removeQuondamData((QuondamPoint)point);
                        break;
                }
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deletePoint");
        }

        return success;
    }


    public boolean deletePointSafe(TtPoint point) {
        boolean success = false;

        try {
            if (point.hasQuondamLinks()) {
                for (String qndmCN : point.getLinkedPoints()) {
                    TtPoint qndmPoint = getPointByCN(qndmCN);

                    if (qndmPoint != null) {
                        TtPoint newPoint = TtUtils.clonePoint(point);
                        newPoint.copyInfo(qndmPoint);

                        updatePoint(newPoint, qndmPoint);
                    }
                }
            }

            success = deletePoint(point);
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deletePointSafe");
        }

        return success;
    }


    public boolean deletePointsInGroup(String groupCN) {
        return deletePoints(getPointsInGroup(groupCN));
    }

    public boolean deletePointsInPolygon(String polyCN) {
        return deletePoints(getPointsInPolygon(polyCN));
    }


    public boolean deletePoints(Collection<TtPoint> points) {
        boolean success = true;

        try {
            for (TtPoint point : points) {
                success = deletePointSafe(point);

                if (!success)
                    break;
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deletePoints");
            success = false;
        }

        return success;
    }


    private void removeGpsData(GpsPoint point) {
        try {
            _db.delete(TwoTrailsSchema.GpsPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });

            deleteNmeaByPointCN(point.getCN());
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:removeGpsData");
        }
    }

    private void removeTravData(TravPoint point) {
        try {
            _db.delete(TwoTrailsSchema.TravPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:removeTravData");
        }
    }

    private void removeQuondamData(QuondamPoint point) {
        try {
            _db.delete(TwoTrailsSchema.QuondamPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });

            removeQuondamLink(point);
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:removeQuondamData");
        }
    }
    //endregion
    //endregion


    //region MetaData
    //region Get
    public TtMetadata getDefaultMetadata() {
        return getMetadataByCN(Consts.EmptyGuid);
    }

    public ArrayList<TtMetadata> getMetadata() {
        return getMetadata(null);
    }

    public TtMetadata getMetadataByCN(String cn) {
        ArrayList<TtMetadata> metas = getMetadata(String.format("CN = '%s'", cn));

        if(metas != null && metas.size() > 0)
            return metas.get(0);
        return null;
    }

    private ArrayList<TtMetadata> getMetadata(String where) {
        ArrayList<TtMetadata> metas = new ArrayList<>();

        try {
            String query = createSelectQuery(TwoTrailsSchema.MetadataSchema.TableName, where);

            Cursor c = _db.rawQuery(query, null);

            TtMetadata meta;

            if (c.moveToFirst()) {
                do {
                    meta = new TtMetadata();

                    if (!c.isNull(0))
                        meta.setCN(c.getString(0));
                    if (!c.isNull(1))
                        meta.setName(c.getString(1));
                    if (!c.isNull(2))
                        meta.setDistance(Units.Dist.valueOf(c.getString(2)));
                    if (!c.isNull(3))
                        meta.setSlope(Units.Slope.valueOf(c.getString(3)));
                    if (!c.isNull(4))
                        meta.setMagDec(c.getDouble(4));
                    if (!c.isNull(5))
                        meta.setDecType(Units.DeclinationType.valueOf(c.getString(5)));
                    if (!c.isNull(6))
                        meta.setElevation(UomElevation.valueOf(c.getString(6)));
                    if (!c.isNull(7))
                        meta.setComment(c.getString(7));
                    if (!c.isNull(8))
                        meta.setDatum(Units.Datum.valueOf(c.getString(8)));
                    if (!c.isNull(9))
                        meta.setGpsReceiver(c.getString(9));
                    if (!c.isNull(10))
                        meta.setRangeFinder(c.getString(10));
                    if (!c.isNull(11))
                        meta.setCompass(c.getString(11));
                    if (!c.isNull(12))
                        meta.setCrew(c.getString(12));
                    if (!c.isNull(13))
                        meta.setZone(c.getInt(13));

                    metas.add(meta);

                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getMetadata");
            return null;
        }

        return metas;
    }


    public HashMap<String, TtMetadata> getMetadataMap() {
        HashMap<String, TtMetadata> metadatas = new HashMap<>();
        for(TtMetadata metaData : getMetadata()) {
            metadatas.put(metaData.getCN(), metaData);
        }
        return  metadatas;
    }
    //endregion

    //region Insert
    public boolean insertMetadata(TtMetadata meta) {
        boolean success = false;

        try {
            _db.beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, meta.getCN());
            cvs.put(TwoTrailsSchema.MetadataSchema.ID, meta.getName());
            cvs.put(TwoTrailsSchema.MetadataSchema.Comment, meta.getComment());
            cvs.put(TwoTrailsSchema.MetadataSchema.Distance, meta.getDistance().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.Slope, meta.getSlope().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.MagDec, meta.getMagDec());
            cvs.put(TwoTrailsSchema.MetadataSchema.DeclinationType, meta.getDecType().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.Elevation, meta.getElevation().toString());

            cvs.put(TwoTrailsSchema.MetadataSchema.Datum, meta.getDatum().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.GpsReceiver, meta.getGpsReceiver());
            cvs.put(TwoTrailsSchema.MetadataSchema.RangeFinder, meta.getRangeFinder());
            cvs.put(TwoTrailsSchema.MetadataSchema.Compass, meta.getCompass());
            cvs.put(TwoTrailsSchema.MetadataSchema.Crew, meta.getCrew());
            cvs.put(TwoTrailsSchema.MetadataSchema.UtmZone, meta.getZone());

            _db.insert(TwoTrailsSchema.MetadataSchema.TableName, null, cvs);

            _db.setTransactionSuccessful();
            success = true;
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertMetadata");
        } finally {
            _db.endTransaction();
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updateMetadata(TtMetadata meta) {
        int success = -1;

        try {
            _db.beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.MetadataSchema.ID, meta.getName());
            cvs.put(TwoTrailsSchema.MetadataSchema.Comment, meta.getComment());
            cvs.put(TwoTrailsSchema.MetadataSchema.Distance, meta.getDistance().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.Slope, meta.getSlope().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.MagDec, meta.getMagDec());
            cvs.put(TwoTrailsSchema.MetadataSchema.DeclinationType, meta.getDecType().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.Elevation, meta.getElevation().toString());

            cvs.put(TwoTrailsSchema.MetadataSchema.Datum, meta.getDatum().toString());
            cvs.put(TwoTrailsSchema.MetadataSchema.GpsReceiver, meta.getGpsReceiver());
            cvs.put(TwoTrailsSchema.MetadataSchema.RangeFinder, meta.getRangeFinder());
            cvs.put(TwoTrailsSchema.MetadataSchema.Compass, meta.getCompass());
            cvs.put(TwoTrailsSchema.MetadataSchema.Crew, meta.getCrew());
            cvs.put(TwoTrailsSchema.MetadataSchema.UtmZone, meta.getZone());

            success = _db.update(TwoTrailsSchema.MetadataSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, meta.getCN()), null);

            _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateMetadata");
        } finally {
            _db.endTransaction();
        }

        return success > 0;
    }
    //endregion

    //region Delete
    public boolean deleteMetadataSafe(String cn) {
        boolean success;

        try {
            _db.beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.PointSchema.MetadataCN, Consts.EmptyGuid);

            _db.update(TwoTrailsSchema.PointSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.PointSchema.MetadataCN, cn), null);

            success = deleteMetadata(cn);

            _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deleteMetadataSafe");
            success = false;
        }

        return success;
    }

    private boolean deleteMetadata(String cn) {
        boolean success = false;

        try {
            success = _db.delete(TwoTrailsSchema.MetadataSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deleteMetadata");
        }

        return success;
    }
    //endregion
    //endregion


    //region Groups
    //region Get
    public ArrayList<TtGroup> getGroups()
    {
        return getGroups(null);
    }

    public ArrayList<TtGroup> getGroupsByType(TtGroup.GroupType type) {
        return getGroups(String.format("%s = '%s'",
                TwoTrailsSchema.GroupSchema.Type,
                type.toString()));
    }

    public TtGroup getGroupByCN(String cn) {
        ArrayList<TtGroup> groups = getGroups(String.format("%s = '%s'",
                TwoTrailsSchema.SharedSchema.CN, cn));

        if (groups != null && groups.size() > 0)
            return groups.get(0);
        else
            return null;
    }

    private ArrayList<TtGroup> getGroups(String where) {
        ArrayList<TtGroup> groups = new ArrayList<>();

        try {
            String query = createSelectQuery(TwoTrailsSchema.GroupSchema.TableName, where);

            Cursor c = _db.rawQuery(query, null);

            TtGroup group;

            if (c.moveToFirst()) {
                do {
                    group = new TtGroup();

                    if (!c.isNull(0))
                        group.setCN(c.getString(0));
                    if (!c.isNull(1))
                        group.setName(c.getString(1));
                    if (!c.isNull(2))
                        group.setDescription(c.getString(2));
                    if (!c.isNull(3))
                        group.setGroupType(TtGroup.GroupType.valueOf(c.getString(3)));

                    groups.add(group);

                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getGroups");
            return null;
        }

        return groups;
    }


    public HashMap<String, TtGroup> getGroupsMap() {
        HashMap<String, TtGroup> groups = new HashMap<>();
        for(TtGroup group : getGroups()) {
            groups.put(group.getCN(), group);
        }
        return  groups;
    }
    //endregion

    //region Insert
    public boolean insertGroup(TtGroup group) {
        boolean success = false;

        try {
            _db.beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, group.getCN());
            cvs.put(TwoTrailsSchema.GroupSchema.Name, group.getName());
            cvs.put(TwoTrailsSchema.GroupSchema.Description, group.getDescription());
            cvs.put(TwoTrailsSchema.GroupSchema.Type, group.getGroupType().toString());

            _db.insert(TwoTrailsSchema.GroupSchema.TableName, null, cvs);

            _db.setTransactionSuccessful();
            success = true;
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertGroup");
        } finally {
            _db.endTransaction();
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updateGroup(TtGroup group) {
        int success = -1;

        try {
            _db.beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, group.getCN());
            cvs.put(TwoTrailsSchema.GroupSchema.Name, group.getName());
            cvs.put(TwoTrailsSchema.GroupSchema.Description, group.getDescription());
            cvs.put(TwoTrailsSchema.GroupSchema.Type, group.getGroupType().toString());

            success = _db.update(TwoTrailsSchema.GroupSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, group.getCN()), null);

            _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateGroup");
        } finally {
            _db.endTransaction();
        }

        return success > 0;
    }
    //endregion

    //region Delete
    public boolean deleteGroup(String cn) {
        boolean success = false;

        try {
            success = _db.delete(TwoTrailsSchema.GroupSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deleteGroup");
        }

        return success;
    }
    //endregion
    //endregion


    //region NMEA
    //region Get
    public ArrayList<TtNmeaBurst> getNmeaBursts() {
        return getNmeaBursts(null);
    }

    public TtNmeaBurst getNmeaBurstsByCN(String cn) {
        ArrayList<TtNmeaBurst> nmeas =
                getNmeaBursts(String.format("%s = '%s'",
                    TwoTrailsSchema.SharedSchema.CN, cn));

        if (nmeas.size() > 0)
            return nmeas.get(0);
        else
            return null;
    }

    public ArrayList<TtNmeaBurst> getNmeaBurstsByPointCN(String pointCN) {
        return getNmeaBursts(String.format("%s = '%s'",
                TwoTrailsSchema.TtNmeaSchema.PointCN,
                pointCN));
    }

    public ArrayList<TtNmeaBurst> getNmeaBursts(String where) {
        ArrayList<TtNmeaBurst> nmeas = new ArrayList<>();

        try {
            String query = createSelectQuery(TwoTrailsSchema.TtNmeaSchema.TableName, where);

            Cursor c = _db.rawQuery(query, null);

            if (c.moveToFirst()) {
                do {
                    String cn; DateTime timeCreated; String pointCN; boolean used;
                    DateTime fixTime; double groundSpeed; double trackAngle;
                    double magVar; EastWest magVarDir; GSASentence.Mode mode; GSASentence.Fix fix;
                    List<Integer> satsUsed; double pdop; double hdop; double vdop; GGASentence.GpsFixType fixQuality;
                    int trackedSatellites; double horizDilution; double geoidHeight; UomElevation geoUom;
                    int numberOfSatellitesInView; List<Satellite> satellites;

                    double lat, lon, elev;
                    NorthSouth latDir;
                    EastWest lonDir;
                    UomElevation uomelev;

                    //region ID Time Pos
                    if (!c.isNull(0))
                        cn = c.getString(0);
                    else
                        continue;

                    if (!c.isNull(1))
                        pointCN = c.getString(1);
                    else
                        continue;

                    if (!c.isNull(2))
                        used = ParseEx.parseBoolean(c.getString(2));
                    else
                        continue;

                    if (!c.isNull(3))
                        timeCreated = dtf.parseDateTime(c.getString(3));
                    else
                        continue;

                    if (!c.isNull(4))
                        fixTime = dtf.parseDateTime(c.getString(4));
                    else
                        continue;

                    if (!c.isNull(5))
                        lat = c.getDouble(5);
                    else
                        continue;

                    if (!c.isNull(6))
                        latDir = NorthSouth.parse(c.getString(6));
                    else
                        continue;

                    if (!c.isNull(7))
                        lon = c.getDouble(7);
                    else
                        continue;

                    if (!c.isNull(8))
                        lonDir = EastWest.parse(c.getString(8));
                    else
                        continue;

                    if (!c.isNull(9))
                        elev = c.getDouble(9);
                    else
                        continue;

                    if (!c.isNull(10))
                        uomelev = UomElevation.parse(c.getString(10));
                    else
                        continue;
                    //endregion

                    //region Mag Fix Dop
                    if (!c.isNull(11))
                        magVar = c.getDouble(11);
                    else
                        continue;

                    if (!c.isNull(12))
                        magVarDir = EastWest.parse(c.getString(12));
                    else
                        magVarDir = null;

                    if (!c.isNull(13))
                        fix = GSASentence.Fix.parse(c.getInt(13));
                    else
                        continue;

                    if (!c.isNull(14))
                        fixQuality = GGASentence.GpsFixType.parse(c.getInt(14));
                    else
                        continue;

                    if (!c.isNull(15))
                        mode = GSASentence.Mode.parse(c.getInt(15));
                    else
                        continue;

                    if (!c.isNull(16))
                        pdop = c.getDouble(16);
                    else
                        continue;

                    if (!c.isNull(17))
                        hdop = c.getDouble(17);
                    else
                        continue;

                    if (!c.isNull(18))
                        vdop = c.getDouble(18);
                    else
                        continue;
                    //endregion

                    //region Horiz Geiod TrackAngle

                    if (!c.isNull(19))
                        horizDilution = c.getDouble(19);
                    else
                        continue;

                    if (!c.isNull(20))
                        geoidHeight = c.getDouble(20);
                    else
                        continue;

                    if (!c.isNull(21))
                        geoUom = UomElevation.parse(c.getString(21));
                    else
                        continue;

                    if (!c.isNull(22))
                        groundSpeed = c.getDouble(22);
                    else
                        continue;

                    if (!c.isNull(23))
                        trackAngle = c.getDouble(23);
                    else
                        continue;
                    //endregion

                    //region SatInfo
                    //24 not needed

                    if (!c.isNull(25))
                        trackedSatellites = c.getInt(25);
                    else
                        continue;

                    if (!c.isNull(26))
                        numberOfSatellitesInView = c.getInt(26);
                    else
                        continue;

                    satsUsed = new ArrayList<>();

                    if (!c.isNull(27)) {
                        for (String prn : c.getString(27).split("_")) {
                            satsUsed.add(ParseEx.parseInteger(prn));
                        }
                    }

                    satellites = new ArrayList<>();

                    try {
                        for (int i = 28; i < 76; i += 4) {
                            if (!c.isNull(i) && !c.isNull(i + 1) && !c.isNull(i + 2) && !c.isNull(i + 3)) {
                                satellites.add(new Satellite(
                                    c.getInt(i),
                                    c.getDouble(i + 1),
                                    c.getDouble(i + 2),
                                    c.getDouble(i + 3)
                                ));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //endregion

                    nmeas.add(new TtNmeaBurst(cn, timeCreated, pointCN, used, new GeoPosition(lat, latDir, lon, lonDir, elev, uomelev), fixTime, groundSpeed,
                            trackAngle, magVar, magVarDir, mode, fix, satsUsed, pdop, hdop, vdop, fixQuality,
                            trackedSatellites, horizDilution, geoidHeight, geoUom, numberOfSatellitesInView));//, satellites
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getNmeaBursts");
            return null;
        }

        return nmeas;
    }
    //endregion

    //region Insert
    public boolean insertNmeaBursts(Collection<TtNmeaBurst> bursts) {
        try {
            _db.beginTransaction();

            for (TtNmeaBurst burst : bursts) {
                if (!insertNmeaBurstNoTrans(burst)) {
                    _db.endTransaction();
                    return false;
                }
            }

            _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertNmeaBursts");
        } finally {
            _db.endTransaction();
        }

        return true;
    }

    public boolean insertNmeaBurst(TtNmeaBurst burst) {
        boolean success = false;

        try {
            _db.beginTransaction();

            success = insertNmeaBurstNoTrans(burst);

            if (success) {
                _db.setTransactionSuccessful();
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertNmeaBurst");
        } finally {
            _db.endTransaction();
        }

        return success;
    }

    private boolean insertNmeaBurstNoTrans(TtNmeaBurst burst) {
        boolean success = false;

        try {
            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, burst.getCN());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.PointCN, burst.getPointCN());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Used, burst.isUsed());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.TimeCreated, dtf.print(burst.getTimeCreated()));

            cvs.put(TwoTrailsSchema.TtNmeaSchema.FixTime, dtf.print(burst.getFixTime()));
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Latitude, burst.getLatitude());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LatDir, burst.getLatDir().toStringAbv());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Longitude, burst.getLongitude());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LonDir, burst.getLonDir().toStringAbv());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Elevation, burst.getElevation());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.ElevUom, burst.getUomElevation().toStringAbv());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.MagVar, burst.getMagVar());
            if (burst.getMagVarDir() != null) { cvs.put(TwoTrailsSchema.TtNmeaSchema.MagDir,burst.getMagVarDir().toStringAbv()); }
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Fix, burst.getFix().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.FixQuality, burst.getFixQuality().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Mode, burst.getMode().getValue());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.PDOP, burst.getPDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HDOP, burst.getHDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.VDOP, burst.getVDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HorizDilution, burst.getHorizDilution());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeight, burst.getGeoidHeight());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeightUom, burst.getGeoUom().toStringAbv());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GroundSpeed, burst.getGroundSpeed());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Track_Angle, burst.getTrackAngle());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesUsedCount, burst.getUsedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesTrackedCount, burst.getTrackedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesInViewCount, burst.getSatellitesInViewCount());


            cvs.put(TwoTrailsSchema.TtNmeaSchema.UsedSatPRNS, burst.getUsedSatelliteIDsString());

            /*
            int i = 0;
            for (Satellite sat : burst.getSatellitesInView()) {
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatIDs[i], sat.getID());
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatElevs[i], sat.getElevation());
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatAzs[i], sat.getAzimuth());
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatSRNs[i], sat.getSRN());
                i++;
            }
            */

            _db.insert(TwoTrailsSchema.TtNmeaSchema.TableName, null, cvs);

            success = true;
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertGroup");
        }

        return success;
    }
    //endregion

    //region Update

    public void updateNmeaBursts(Collection<TtNmeaBurst> bursts) {
        try {
            _db.beginTransaction();

            for (TtNmeaBurst burst : bursts) {
                updateNmeaBurst(burst);
            }

            _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateNmeaBursts");
        } finally {
            _db.endTransaction();
        }
    }

    public int updateNmeaBurst(TtNmeaBurst burst) {
        int success = -1;

        try {
            _db.beginTransaction();

            success = updateNmeaBurstNoTrans(burst);

            _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateNmeaBurst");
        } finally {
            _db.endTransaction();
        }

        return success;
    }

    private int updateNmeaBurstNoTrans(TtNmeaBurst burst) {
        int success = -1;

        try {

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, burst.getCN());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.PointCN, burst.getPointCN());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Used, burst.isUsed());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.TimeCreated, dtf.print(burst.getTimeCreated()));

            cvs.put(TwoTrailsSchema.TtNmeaSchema.FixTime, dtf.print(burst.getFixTime()));
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Latitude, burst.getLatitude());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LatDir, burst.getLatDir().toStringAbv());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Longitude, burst.getLongitude());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LonDir, burst.getLonDir().toStringAbv());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Elevation, burst.getElevation());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.ElevUom, burst.getUomElevation().toStringAbv());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.MagVar, burst.getMagVar());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.MagDir, burst.getMagVarDir().toStringAbv());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Fix, burst.getFix().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.FixQuality, burst.getFixQuality().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Mode, burst.getMode().getValue());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.PDOP, burst.getPDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HDOP, burst.getHDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.VDOP, burst.getVDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HorizDilution, burst.getHorizDilution());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeight, burst.getGeoidHeight());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeightUom, burst.getGeoUom().toStringAbv());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GroundSpeed, burst.getGroundSpeed());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Track_Angle, burst.getTrackAngle());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesUsedCount, burst.getUsedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesTrackedCount, burst.getTrackedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesInViewCount, burst.getSatellitesInViewCount());


            cvs.put(TwoTrailsSchema.TtNmeaSchema.UsedSatPRNS, burst.getUsedSatelliteIDsString());

            /*
            int i = 0;
            for (Satellite sat : burst.getSatellitesInView()) {
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatIDs[i], sat.getID());
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatElevs[i], sat.getElevation());
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatAzs[i], sat.getAzimuth());
                cvs.put(TwoTrailsSchema.TtNmeaSchema.SatSRNs[i], sat.getSRN());
                i++;
            }
            */

            success = _db.update(TwoTrailsSchema.TtNmeaSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, burst.getCN()), null);

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateNmea");
        }

        return success;
    }

    //endregion

    //region Delete
    public void deleteNmeaByCN(String cn) {
        try {
            _db.delete(TwoTrailsSchema.TtNmeaSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn });
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deleteNmea");
        }
    }

    public void deleteNmeaByPointCN(String pointCN) {
        try {
        _db.delete(TwoTrailsSchema.TtNmeaSchema.TableName,
                TwoTrailsSchema.TtNmeaSchema.PointCN + "=?",
                new String[] { pointCN });
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deleteNmea");
        }
    }
    //endregion
    //endregion


    //region Project Info
    //region Get
    public String getProjectID() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.ID);
    }

    public String getProjectDescription() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Description);
    }

    public String getProjectRegion() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Region);
    }

    public String getProjectForest() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Forest);
    }

    public String getProjectDateCreated() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Created);
    }

    public String getProjectDistrict() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.District);
    }

    public String getProjectDeviceID() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.DeviceID);
    }

    public String getTtDbVersion() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.TtDbSchemaVersion);
    }

    public String getProjectTtVersion() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.TtVersion);
    }

    public String getProjectCreatedTtVersion() {
        return getProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.CreatedTtVersion);
    }

    private String getProjectInfoField(String columnName) {
        String retString = StringEx.Empty;
        String getQuery = String.format("select %s from %s",
                columnName, TwoTrailsSchema.ProjectInfoSchema.TableName);

        Cursor c = null;

        try
        {
            c = _db.rawQuery(getQuery, null);

            if(c.moveToFirst())
            {
                if(!c.isNull(0))
                    retString = c.getString(0);
            }
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:getProjectInfoField");
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return retString;
    }
    //endregion

    //region Set
    private void SetupProjInfo() {
        try
        {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.TtDbSchemaVersion,
                    TwoTrailsSchema.SchemaVersion.toString());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.TtVersion,
                    Global.getTwoTrailsVersion());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.CreatedTtVersion,
                    Global.getTwoTrailsVersion());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Region,
                    Global.Settings.ProjectSettings.getRegion());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.ID, "Unnamed");

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.DeviceID, TtUtils.getDeviceName());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Forest,
                    Global.Settings.ProjectSettings.getForest());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.District,
                    Global.Settings.ProjectSettings.getDistrict());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Description, StringEx.Empty);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Created, new DateTime().toString());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.DeviceID, TtUtils.getDeviceName());

            _db.insert(TwoTrailsSchema.ProjectInfoSchema.TableName, null, cvs);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:SetupProjInfo");
        }
    }

    public void setProjectID(String ID) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.ID, ID);
    }

    public void setProjectDescription(String description) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Description, description);
    }

    public void setProjectRegion(String region) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Region, region);
    }

    public void setProjectForest(String forest) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Forest, forest);
    }

    public void setProjectDistrict(String district) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.District, district);
    }

    public void setProjectDateCreated(String dateCreated) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.Created, dateCreated);
    }

    public void setProjectCreatedTtVersion(String version) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.TtVersion, version);
    }

    private void setProjectDeviceID(String id) {
        setProjectInfoField(TwoTrailsSchema.ProjectInfoSchema.DeviceID, id);
    }

    private void setProjectInfoField(String columnName, String value) {
        String updateQuery = String.format("update %s set %s = '%s'",
                TwoTrailsSchema.ProjectInfoSchema.TableName,
                columnName,
                value);

        try
        {
            _db.execSQL(updateQuery);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:SetProjectInfoField");
        }
    }
    //endregion
    //endregion


    //region DbTools
    private String createSelectQuery(String table, String where) {
        return String.format("select * from %s%s",
                table, StringEx.isEmpty(where) ?
                        StringEx.Empty : String.format(" where %s", where));
    }

    public int getItemCount(String tableName) {
        String countQuery = "SELECT COUNT (*) FROM " + tableName;

        Cursor cursor = _db.rawQuery(countQuery, null);

        int count = 0;
        if (null != cursor) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            cursor.close();
        }
        return count;
    }

    public int getItemsCount(String tableName, String limiter, String value) {
        String countQuery = String.format("SELECT COUNT (*) FROM %s where %s = '%s'",
                tableName,
                limiter,
                value);

        Cursor cursor = _db.rawQuery(countQuery, null);

        int count = 0;
        if (null != cursor) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            cursor.close();
        }
        return count;
    }

    public boolean duplicate(String duplicateFileName) {
        try {
            DataAccessLayer dDal = new DataAccessLayer(duplicateFileName);

            dDal.setProjectID(getProjectID());
            dDal.setProjectDescription(getProjectDescription());
            dDal.setProjectRegion(getProjectRegion());
            dDal.setProjectForest(getProjectForest());
            dDal.setProjectDistrict(getProjectDistrict());
            dDal.setProjectDateCreated(getProjectDateCreated());
            dDal.setProjectDeviceID(getProjectDeviceID());
            dDal.setProjectCreatedTtVersion(getProjectCreatedTtVersion());

            for (TtMetadata meta : getMetadata()) {
                if (meta.getCN().equals(Consts.EmptyGuid)) {
                    dDal.updateMetadata(meta);
                } else {
                    dDal.insertMetadata(meta);
                }
            }

            for (TtGroup group : getGroups()) {
                if (group.getCN().equals(Consts.EmptyGuid)) {
                    dDal.updateGroup(group);
                } else {
                    dDal.insertGroup(group);
                }
            }

            for (TtPolygon poly : getPolygons()) {
                dDal.insertPolygon(poly);
            }

            dDal.insertPoints(getPoints());

            dDal.insertNmeaBursts(getNmeaBursts());

            dDal.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:duplicate", ex.getStackTrace());
            return false;
        }

        return true;
    }

    public void clean() {
        StringBuilder sbPoly = new StringBuilder();
        StringBuilder sbPoint = new StringBuilder();
        StringBuilder sbNmea = new StringBuilder();

        List<TtPolygon> polys = getPolygons();

        for (int i = 0; i < polys.size(); i++) {
            sbPoly.append(String.format("%s != '%s'%s",
                    TwoTrailsSchema.PointSchema.PolyCN,
                    polys.get(i).getCN(),
                    i < polys.size() - 1 ? " and " : StringEx.Empty
            ));
        }

        ArrayList<String> pointCNs = new ArrayList<>();

        try {
            String query = String.format("select %s from %s where %s",
                    TwoTrailsSchema.SharedSchema.CN,
                    TwoTrailsSchema.PointSchema.TableName,
                    sbPoly.toString());

            Cursor c = _db.rawQuery(query, null);

            if (c.moveToFirst()) {
                do {
                    if (!c.isNull(0))
                        pointCNs.add(c.getString(0));
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            //
        }

        ArrayList<String> pointDeleteQuerys = new ArrayList<>();
        ArrayList<String> nmeaDeleteQuerys = new ArrayList<>();

        int qSize = 0;
        final int pcns = pointCNs.size() - 1;

        for (int i = 0; i < pointCNs.size(); i++) {
            sbPoint.append(String.format("%s = '%s'%s",
                    TwoTrailsSchema.SharedSchema.CN,
                    pointCNs.get(i),
                    (qSize < 100 && i < pcns) ? " or " : StringEx.Empty
            ));

            sbNmea.append(String.format("%s = '%s'%s",
                    TwoTrailsSchema.TtNmeaSchema.PointCN,
                    pointCNs.get(i),
                    (qSize < 100 && i < pcns) ? " or " : StringEx.Empty
            ));

            qSize++;

            if (qSize > 100 || i == pcns) {
                pointDeleteQuerys.add(sbPoint.toString());
                nmeaDeleteQuerys.add(sbNmea.toString());

                sbPoint = new StringBuilder();
                sbNmea = new StringBuilder();
                qSize = 0;
            }
        }

        try {
            for (int i = 0; i < pointDeleteQuerys.size(); i++) {
                String wherePointCN = pointDeleteQuerys.get(i);
                _db.delete(TwoTrailsSchema.PointSchema.TableName, wherePointCN, null);
                _db.delete(TwoTrailsSchema.GpsPointSchema.TableName, wherePointCN, null);
                _db.delete(TwoTrailsSchema.TravPointSchema.TableName, wherePointCN, null);
                _db.delete(TwoTrailsSchema.QuondamPointSchema.TableName, wherePointCN, null);

                _db.delete(TwoTrailsSchema.TtNmeaSchema.TableName, nmeaDeleteQuerys.get(i), null);
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deleteNmea");
        }
    }
    //endregion
}