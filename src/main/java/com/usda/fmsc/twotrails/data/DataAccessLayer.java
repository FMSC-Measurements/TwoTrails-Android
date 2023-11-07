package com.usda.fmsc.twotrails.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.usda.fmsc.geospatial.codes.EastWest;
import com.usda.fmsc.geospatial.codes.Mode;
import com.usda.fmsc.geospatial.codes.NorthSouth;
import com.usda.fmsc.geospatial.codes.UomElevation;
import com.usda.fmsc.geospatial.gnss.codes.GnssFix;
import com.usda.fmsc.geospatial.gnss.codes.GnssFixQuality;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.MapSettings;
import com.usda.fmsc.twotrails.ProjectSettings;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.ins.TtInsData;
import com.usda.fmsc.twotrails.objects.DataActionType;
import com.usda.fmsc.twotrails.objects.TtUserAction;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.points.InertialPoint;
import com.usda.fmsc.twotrails.objects.points.InertialStartPoint;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicOptions;
import com.usda.fmsc.twotrails.units.Datum;
import com.usda.fmsc.twotrails.units.DeclinationType;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

@SuppressWarnings({"UnusedReturnValue", "unused", "WeakerAccess"})
public class DataAccessLayer extends IDataLayer {
    private TtVersion _DalVersion;
    public TtVersion getVersion() {
        if (_DalVersion == null)
            _DalVersion = new TtVersion(getTtDbVersion(), getUserVersion());

        return _DalVersion;
    }

    private TtUserAction _Activity = null;


    //region Constructors / Open / Close / Create
    public DataAccessLayer(TwoTrailsApp context, SQLiteDatabase db, String fileName) {
        super(context, db, fileName);
    }

    protected DataAccessLayer(TwoTrailsApp context, SQLiteDatabase db, String fileName, boolean create) {
        super(context, db, fileName, create);
    }

    
    public static DataAccessLayer createDAL(TwoTrailsApp context, SQLiteDatabase db, String projectName, String fileName) {
        DataAccessLayer dal = new DataAccessLayer(context, db, fileName, true);

        dal.setupProjectInfo(projectName);
        dal.insertMetadata(context.getMetadataSettings().getDefaultMetadata());
        dal.insertGroup(Consts.Defaults.createDefaultGroup());

        return dal;
    }

    @Override
    protected void onCreateDB(SQLiteDatabase db) {
        _DalVersion = TwoTrailsSchema.SchemaVersion;
        _Activity = createUserAction();

        createPolygonTable(db);
        createMetaDataTable(db);
        createPointTable(db);
        createGpsPointDataTable(db);
        createTravPointDataTable(db);
        createQuondamPointDataTable(db);
        createInertialStartPointDataTable(db);
        createInertialPointDataTable(db);
        createProjectInfoDataTable(db);
        createTtNmeaTable(db);
        createTtInsTable(db);
        createGroupTable(db);
        createPolygonAttrTable(db);
        CreateActivityTable(db);
        createDataDictionaryTable(db);
    }


    private void createPointTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.PointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createPointTable");
            throw new RuntimeException("DAL:createPointTable");
        }
    }
    
    private void createPolygonTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.PolygonSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createPolygonTable");
            throw new RuntimeException("DAL:createPolygonTable");
        }
    }

    private void createGroupTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.GroupSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createPolygonTable");
            throw new RuntimeException("DAL:createPolygonTable");
        }

    }

    private void createMetaDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.MetadataSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createMetaDataTable");
            throw new RuntimeException("DAL:createMetaDataTable");
        }
    }

    private void createGpsPointDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.GpsPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createGpsPointDataTable");
            throw new RuntimeException("DAL:createGpsPointDataTable");
        }
    }

    private void createTravPointDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.TravPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createTravPointDataTable");
            throw new RuntimeException("DAL:createTravPointDataTable");
        }
    }

    private void createQuondamPointDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.QuondamPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createQuondamPointDataTable");
            throw new RuntimeException("DAL:createQuondamPointDataTable");
        }
    }

    private void createInertialStartPointDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.InertialStartPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createInertialPointStartDataTable");
            throw new RuntimeException("DAL:createInertialStartInertialDataTable");
        }
    }

    private void createInertialPointDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.InertialPointSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createInertialPointDataTable");
            throw new RuntimeException("DAL:createInertialPointDataTable");
        }
    }

    private void createProjectInfoDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.ProjectInfoSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createProjectInfoDataTable");
            throw new RuntimeException("DAL:createProjectInfoDataTable");
        }
    }

    private void createTtNmeaTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.TtNmeaSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createTtnmeaTable");
            throw new RuntimeException("DAL:createTtnmeaTable");
        }
    }

    private void createTtInsTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.TtInsSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createTtInsTable");
            throw new RuntimeException("DAL:createTtInsTable");
        }
    }

    private void createPolygonAttrTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.PolygonAttrSchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createPolygonAttrTable");
            throw new RuntimeException("DAL:createPolygonAttrTable");
        }
    }

    private void CreateActivityTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.ActivitySchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:CreateActivityTable");
            throw new RuntimeException("DAL:CreateActivityTable");
        }
    }

    private  void createDataDictionaryTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsSchema.DataDictionarySchema.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:createDataDictionaryTable");
            throw new RuntimeException("DAL:createDataDictionaryTable");
        }
    }


    @Override
    protected void onOpenDB(SQLiteDatabase db) {
        if (_DalVersion == null) _DalVersion = new TtVersion(getTtDbVersion(), db.getVersion());
        if (_Activity == null) _Activity = createUserAction();
    }

    @Override
    protected void onCloseDB(SQLiteDatabase db) {
        if (_Activity != null)
            insertUserActivity(_Activity);
    }
    //endregion


    //region Polygons
    //region Get
    public ArrayList<TtPolygon> getPolygons() {
        return getPolygons(null);
    }

    public HashMap<String, TtPolygon> getPolygonsMap() {
        HashMap<String, TtPolygon> polys = new HashMap<>();
        ArrayList<TtPolygon> pl = getPolygons();

        if (pl != null) {
            for(TtPolygon polygon : pl) {
                polys.put(polygon.getCN(), polygon);
            }
        }

        return  polys;
    }

    public TtPolygon getPolygonByCN(String cn) {
        ArrayList<TtPolygon> polys = getPolygons(String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, cn));

        if (polys.size() > 0)
            return polys.get(0);
        return null;
    }

    private ArrayList<TtPolygon> getPolygons(String where) {
        ArrayList<TtPolygon> polys = new ArrayList<>();

        try {
            String query = String.format("%s order by datetime(%s) asc",
                    createSelectQuery(
                            TwoTrailsSchema.PolygonSchema.TableName,
                            TwoTrailsSchema.PolygonSchema.SelectItems,
                            where),
                    TwoTrailsSchema.PolygonSchema.TimeCreated);

            Cursor c = getDB().rawQuery(query, null);

            TtPolygon poly;

            if (c.moveToFirst()) {
                do {
                    poly = new TtPolygon();

                    if (!c.isNull(0))
                        poly.setCN(c.getString(0));
                    if (!c.isNull(1))
                        poly.setName(c.getString(1));
                    //Unit Type (int to enum)
                    if (!c.isNull(3))
                        poly.setAccuracy(c.getDouble(3));
                    if (!c.isNull(4))
                        poly.setDescription(c.getString(4));
                    if (!c.isNull(5))
                        poly.setArea(c.getDouble(5));
                    if (!c.isNull(6))
                        poly.setPerimeter(c.getDouble(6));
                    if (!c.isNull(7))
                        poly.setPerimeterLine(c.getDouble(7));
                    if (!c.isNull(8))
                        poly.setIncrementBy(c.getInt(8));
                    if (!c.isNull(9))
                        poly.setPointStartIndex(c.getInt(9));
                    //ParentUnitCN
                    if (!c.isNull(11))
                        poly.setTime(parseDateTime(c.getString(11)));

                    polys.add(poly);
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getPolygons");
            throw new RuntimeException("DAL:getPolygons");
        }

        return polys;
    }
    //endregion

    //region Insert
    public boolean insertPolygon(TtPolygon poly) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, poly.getCN());
            cvs.put(TwoTrailsSchema.PolygonSchema.Name, poly.getName());
            cvs.put(TwoTrailsSchema.PolygonSchema.Description, poly.getDescription());
            cvs.put(TwoTrailsSchema.PolygonSchema.Accuracy, poly.getAccuracy());
            cvs.put(TwoTrailsSchema.PolygonSchema.Area, poly.getArea());
            cvs.put(TwoTrailsSchema.PolygonSchema.Perimeter, poly.getPerimeter());
            cvs.put(TwoTrailsSchema.PolygonSchema.PerimeterLine, poly.getPerimeterLine());
            cvs.put(TwoTrailsSchema.PolygonSchema.PointStartIndex, poly.getPointStartIndex());
            cvs.put(TwoTrailsSchema.PolygonSchema.IncrementBy, poly.getIncrementBy());
            cvs.put(TwoTrailsSchema.PolygonSchema.TimeCreated, dtf.print(poly.getTime()));

//            cvs.put(TwoTrailsSchema.PolygonSchema.UnitType, poly.getUnitType().getValue());
//            cvs.put(TwoTrailsSchema.PolygonSchema.ParentUnitCN, poly.getParentUnitCN());

            getDB().insert(TwoTrailsSchema.PolygonSchema.TableName, null, cvs);

            getDB().setTransactionSuccessful();

            updateUserActivity(DataActionType.InsertedPolygons);

            success = true;
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertPolygon");
        } finally {
            getDB().endTransaction();
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updatePolygon(TtPolygon poly) {
        int success = -1;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.PolygonSchema.Name, poly.getName());
            cvs.put(TwoTrailsSchema.PolygonSchema.Description, poly.getDescription());
            cvs.put(TwoTrailsSchema.PolygonSchema.Accuracy, poly.getAccuracy());
            cvs.put(TwoTrailsSchema.PolygonSchema.Area, poly.getArea());
            cvs.put(TwoTrailsSchema.PolygonSchema.Perimeter, poly.getPerimeter());
            cvs.put(TwoTrailsSchema.PolygonSchema.PerimeterLine, poly.getPerimeterLine());
            cvs.put(TwoTrailsSchema.PolygonSchema.PointStartIndex, poly.getPointStartIndex());
            cvs.put(TwoTrailsSchema.PolygonSchema.IncrementBy, poly.getIncrementBy());

            success = getDB().update(TwoTrailsSchema.PolygonSchema.TableName, cvs,
                String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, poly.getCN()), null);

            getDB().setTransactionSuccessful();

            updateUserActivity(DataActionType.ModifiedPolygons);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updatePolygon");
        } finally {
            getDB().endTransaction();
        }

        return success > 0;
    }
    //endregion

    //region Delete
    public boolean deletePolygon(String cn) {
        boolean success = false;

        try {
            success = getDB().delete(TwoTrailsSchema.PolygonSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;

            if (success) {
                deletePolygonGraphicOption(cn);

                updateUserActivity(DataActionType.DeletedPolygons);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deletePolygon");
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

        if (points.size() > 0)
            return points.get(0);
        return null;
    }

    public TtPoint getFirstPointInPolygon(String polyCN) {
        ArrayList<TtPoint> points =
                getPoints(String.format("%s = '%s'",
                        TwoTrailsSchema.PointSchema.PolyCN, polyCN), 1);

        if (points.size() > 0)
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
        return getPoints(String.format("%s = '%s' and %s != %s and %s = 1",
                TwoTrailsSchema.PointSchema.PolyCN, polyCN,
                TwoTrailsSchema.PointSchema.Operation, OpType.WayPoint.getValue(),
                TwoTrailsSchema.PointSchema.OnBoundary));
    }

    public int getBoundaryPointsCountInPoly(String polyCN) {
        String countQuery = String.format("SELECT COUNT (*) FROM %s where %s = '%s' and %s != %s and %s = 1",
                TwoTrailsSchema.PointSchema.TableName,
                TwoTrailsSchema.PointSchema.PolyCN, polyCN,
                TwoTrailsSchema.PointSchema.Operation, OpType.WayPoint.getValue(),
                TwoTrailsSchema.PointSchema.OnBoundary);

        Cursor cursor = getDB().rawQuery(countQuery, null);

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

    public ArrayList<TtPoint> getPointsInPolygon(String polyCN) {
        return getPoints(String.format("%s = '%s'",
                TwoTrailsSchema.PointSchema.PolyCN, polyCN));
    }

    public ArrayList<TtPoint> getPointsWithMeta(String metaCN) {
        return getPoints(String.format("%s = '%s'",
                TwoTrailsSchema.PointSchema.MetadataCN, metaCN));
    }

    public ArrayList<TtPoint> getGpsTypePointsWithMeta(String metaCN) {
        return getPoints(String.format("%s = '%s' and %s = %s or %s = %s or %s = %s or %s = %s",
                TwoTrailsSchema.PointSchema.MetadataCN, metaCN,
                TwoTrailsSchema.PointSchema.Operation, OpType.GPS.getValue(),
                TwoTrailsSchema.PointSchema.Operation, OpType.Walk.getValue(),
                TwoTrailsSchema.PointSchema.Operation, OpType.Take5.getValue(),
                TwoTrailsSchema.PointSchema.Operation, OpType.WayPoint.getValue()));
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

    //TODO merge to single query
    private ArrayList<TtPoint> getPoints(String where, int limit) {
        ArrayList<TtPoint> points = new ArrayList<>();

        try {
            String query = String.format("select %s from %s%s order by %s, %s %s",
                    TwoTrailsSchema.PointSchema.SelectItems,
                    TwoTrailsSchema.PointSchema.TableName,
                    StringEx.isEmpty(where) ? StringEx.Empty : String.format(" where %s", where),
                    TwoTrailsSchema.PointSchema.PolyName,
                    TwoTrailsSchema.PointSchema.Order,
                    limit > 0 ? " limit " + limit : StringEx.Empty);

            Cursor c = getDB().rawQuery(query, null);

            TtPoint point;

            if (c.moveToFirst()) {
                do {

                    if (!c.isNull(7)) {
                        point = TtUtils.Points.createNewPointByOpType(OpType.parse(c.getInt(7)));
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
                        point.setTime(parseDateTime(c.getString(9)));

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


                    switch (point.getOp()) {
                        case InertialStart:
                            getInertialStartPointData(point);
                        case GPS:
                        case Take5:
                        case Walk:
                        case WayPoint:
                            getGpsPointData(point);
                            break;
                        case Traverse:
                        case SideShot:
                            getTravPointData(point);
                            break;
                        case Quondam:
                            getQuondamPointData(point);
                            break;
                        case Inertial:
                            getInertialPointData(point);
                            break;
                    }

                    points.add(point);

                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getPoints");
            throw new RuntimeException("DAL:getPoints");
        }

        return points;
    }

    public HashMap<String, TtPoint> getPointsMap() {
        HashMap<String, TtPoint> points = new HashMap<>();
        ArrayList<TtPoint> pl = getPoints();

        if (pl != null) {
            for(TtPoint point : pl) {
                points.put(point.getCN(), point);
            }
        }

        return  points;
    }

    //region subPoint Data
    private void getGpsPointData(TtPoint point) {
        GpsPoint g = (GpsPoint)point;

        try {
            String query = String.format("select %s from %s where %s = '%s'",
                    TwoTrailsSchema.GpsPointSchema.SelectItems,
                    TwoTrailsSchema.GpsPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    g.getCN());

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                if (!c.isNull(1))
                    g.setLatitude(c.getDouble(1));
                if (!c.isNull(2))
                    g.setLongitude(c.getDouble(2));
                if (!c.isNull(3))
                    g.setElevation(c.getDouble(3));
                if (!c.isNull(4))
                    g.setManualAccuracy(c.getDouble(4));
                if (!c.isNull(5))
                    g.setRMSEr(c.getDouble(5));
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getGpsPointData");
            throw new RuntimeException("DAL:getGpsPointData");
        }
    }

    private void getTravPointData(TtPoint point) {
        TravPoint t = (TravPoint)point;

        try {
            String query = String.format("select %s from %s where %s = '%s'",
                    TwoTrailsSchema.TravPointSchema.SelectItems,
                    TwoTrailsSchema.TravPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    t.getCN());

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                if (!c.isNull(1))
                    t.setFwdAz(c.getDouble(1));
                if (!c.isNull(2))
                    t.setBkAz(c.getDouble(2));
                if (!c.isNull(3))
                    t.setSlopeDistance(c.getDouble(3));
                if (!c.isNull(4))
                    t.setSlopeAngle(c.getDouble(4));
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getTravPointData");
            throw new RuntimeException("DAL:getTravPointData");
        }
    }

    private void getQuondamPointData(TtPoint point) {
        QuondamPoint q = (QuondamPoint)point;

        try {
            String query = String.format("select %s from %s where %s = '%s'",
                    TwoTrailsSchema.QuondamPointSchema.SelectItems,
                    TwoTrailsSchema.QuondamPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    q.getCN());

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                if (!c.isNull(1)) {
                    TtPoint p = getPointByCN(c.getString(1));
                    if (p != null)
                        q.setParentPoint(p);
                }

                if (!c.isNull(2))
                    q.setManualAccuracy(c.getDouble(2));
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getQuondamPointData");
            throw new RuntimeException("DAL:getQuondamPointData");
        }
    }

    private void getInertialStartPointData(TtPoint point) {
        InertialStartPoint isp = (InertialStartPoint) point;

        try {
            String query = String.format("select %s from %s where %s = '%s'",
                    TwoTrailsSchema.InertialStartPointSchema.SelectItems,
                    TwoTrailsSchema.InertialStartPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    isp.getCN());

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                if (!c.isNull(1))
                    isp.setFwdAz(c.getDouble(1));
                if (!c.isNull(2))
                    isp.setBkAz(c.getDouble(2));
                if (!c.isNull(3))
                    isp.setAzOffset(c.getDouble(3));
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getInertialStartPointData");
            throw new RuntimeException("DAL:getInertialStartPointData");
        }
    }

    private void getInertialPointData(TtPoint point) {
        InertialPoint ip = (InertialPoint) point;

        try {
            String query = String.format("select %s from %s where %s = '%s'",
                    TwoTrailsSchema.InertialPointSchema.SelectItems,
                    TwoTrailsSchema.InertialPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN,
                    ip.getCN());

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                ip.setInertialValues(
                        c.getDouble(1),
                        ParseEx.parseBoolean(c.getString(2)),
                        c.getDouble(3),
                        c.getDouble(4),
                        c.getDouble(5),
                        c.getDouble(6)
                );
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getInertialPointData");
            throw new RuntimeException("DAL:getInertialPointData");
        }
    }

    //endregion
    //endregion

    //region Insert
    public boolean insertPoint(TtPoint point) {
        boolean success;

        if (point == null)
            return false;

        try {
            getDB().beginTransaction();

            success = insertBasePoint(point);

            if (success) {
                getDB().setTransactionSuccessful();

                updateUserActivity(DataActionType.InsertedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertPoint");
            success = false;
        } finally {
            getDB().endTransaction();
        }

        return success;
    }

    public boolean insertPoints(Collection<TtPoint> points) {
        boolean success = true;

        if (points == null || points.size() < 1)
            return false;

        try {
            getDB().beginTransaction();

            for(TtPoint point : points) {
                if (!insertBasePoint(point)) {
                    success = false;
                    break;
                }
            }

            if (success) {
                getDB().setTransactionSuccessful();

                updateUserActivity(DataActionType.InsertedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertPoints");
            success = false;
        } finally {
            getDB().endTransaction();
        }

        return success;
    }


    private boolean insertBasePoint(TtPoint point) {
        if (point == null)
            return false;

        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.PointSchema.Order, point.getIndex());
            cvs.put(TwoTrailsSchema.PointSchema.Operation, point.getOp().getValue());
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

            cvs.put(TwoTrailsSchema.PointSchema.CreationTime, dtf.print(point.getTime()));

            if (point.hasQuondamLinks()) {
                cvs.put(TwoTrailsSchema.PointSchema.QuondamLinks, point.getLinkedPointsString());
            } else {
                cvs.put(TwoTrailsSchema.PointSchema.QuondamLinks, StringEx.Empty);
            }

            getDB().insert(TwoTrailsSchema.PointSchema.TableName, null, cvs);

            switch (point.getOp()) {
                case InertialStart:
                    insertInertialStartData((InertialStartPoint) point);
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
                case Inertial:
                    insertInertialData((InertialPoint) point);
                    break;
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertBasePoint");
            return false;
        }
        return true;
    }

    private void insertGpsData(GpsPoint point) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.GpsPointSchema.ManualAccuracy, point.getManualAccuracy());
            cvs.put(TwoTrailsSchema.GpsPointSchema.RMSEr, point.getRMSEr());
            cvs.put(TwoTrailsSchema.GpsPointSchema.Latitude, point.getLatitude());
            cvs.put(TwoTrailsSchema.GpsPointSchema.Longitude, point.getLongitude());
            cvs.put(TwoTrailsSchema.GpsPointSchema.Elevation, point.getElevation());

            getDB().insert(TwoTrailsSchema.GpsPointSchema.TableName, null, cvs);

        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertGpsData");
            throw new RuntimeException("DAL:insertGpsData");
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


            getDB().insert(TwoTrailsSchema.TravPointSchema.TableName, null, cvs);

        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertTravData");
            throw new RuntimeException("DAL:insertTravData");
        }
    }

    private void insertQuondamData(QuondamPoint point) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.QuondamPointSchema.ParentPointCN, point.getParentCN());
            cvs.put(TwoTrailsSchema.QuondamPointSchema.ManualAccuracy, point.getManualAccuracy());

            getDB().insert(TwoTrailsSchema.QuondamPointSchema.TableName, null, cvs);

            updateQuondamLink(point);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertQuondamData");
            throw new RuntimeException("DAL:insertQuondamData");
        }
    }


    private void insertInertialStartData(InertialStartPoint point) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.InertialStartPointSchema.BackAz, point.getBkAz());
            cvs.put(TwoTrailsSchema.InertialStartPointSchema.ForwadAz, point.getFwdAz());
            cvs.put(TwoTrailsSchema.InertialStartPointSchema.AzimuthOffset, point.getAzOffset());

            getDB().insert(TwoTrailsSchema.InertialStartPointSchema.TableName, null, cvs);

        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertInertialStartData");
            throw new RuntimeException("DAL:insertInertialStartData");
        }
    }

    private void insertInertialData(InertialPoint point) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.SharedSchema.CN, point.getCN());
            cvs.put(TwoTrailsSchema.InertialPointSchema.AllSegmentsValid, point.areAllSegmentsValid());
            cvs.put(TwoTrailsSchema.InertialPointSchema.TimeSpan, point.getTimeSpan());
            cvs.put(TwoTrailsSchema.InertialPointSchema.Azimuth, point.getAzimuth());
            cvs.put(TwoTrailsSchema.InertialPointSchema.DistanceX, point.getDistX());
            cvs.put(TwoTrailsSchema.InertialPointSchema.DistanceY, point.getDistY());
            cvs.put(TwoTrailsSchema.InertialPointSchema.DistanceZ, point.getDistZ());

            getDB().insert(TwoTrailsSchema.InertialPointSchema.TableName, null, cvs);

        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertInertialData");
            throw new RuntimeException("DAL:insertInertialData");
        }
    }
    //endregion

    //region Update
    public boolean updatePoint(GpsPoint updatedPoint) {
        return updatePointSame(updatedPoint);
    }

    public boolean updatePoint(TravPoint updatedPoint) {
        return updatePointSame(updatedPoint);
    }

    private boolean updatePointSame(TtPoint updatedPoint) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            success = updateBasePoint(updatedPoint, updatedPoint);

            if (success) {
                getDB().setTransactionSuccessful();

                updateUserActivity(DataActionType.ModifiedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            getDB().endTransaction();
        }
        return success;
    }

    public boolean updatePoint(TtPoint updatedPoint, TtPoint oldPoint) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            success = updateBasePoint(updatedPoint, oldPoint);

            if (success) {
                getDB().setTransactionSuccessful();

                updateUserActivity(DataActionType.ModifiedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            getDB().endTransaction();
        }
        return success;
    }

    public boolean updatePoints(Collection<TtPoint> updatedPoints) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            for(TtPoint point : updatedPoints) {
                success = updateBasePoint(point, point);

                if (!success)
                    break;
            }

            if (success) {
                getDB().setTransactionSuccessful();

                updateUserActivity(DataActionType.ModifiedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            getDB().endTransaction();
        }
        return success;
    }

    public boolean updatePoints(List<TtPoint> updatedPoints, List<TtPoint> oldPoints) {
        boolean success = false;

        if (updatedPoints == null || oldPoints == null ||
                updatedPoints.size() != oldPoints.size() ||
                updatedPoints.size() < 1)
            return false;

        try {
            getDB().beginTransaction();

            for (int i = 0; i < updatedPoints.size(); i++) {
                success = updateBasePoint(updatedPoints.get(i), oldPoints.get(i));

                if (!success)
                    break;
            }

            if (success) {
                getDB().setTransactionSuccessful();

                updateUserActivity(DataActionType.ModifiedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updatePoint");
        } finally {
            getDB().endTransaction();
        }
        return success;
    }

    private boolean updateBasePoint(TtPoint updatedPoint, TtPoint oldPoint) {
        if (updatedPoint == null)
            return false;

        if (oldPoint == null) {
            return insertBasePoint(updatedPoint);
        } else {
            if (!updatedPoint.getCN().equals(oldPoint.getCN()))
                return false;

            try {
                ContentValues cvs = new ContentValues();

                cvs.put(TwoTrailsSchema.PointSchema.Order, updatedPoint.getIndex());
                cvs.put(TwoTrailsSchema.PointSchema.Operation, updatedPoint.getOp().getValue());
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


                int rows = getDB().update(TwoTrailsSchema.PointSchema.TableName, cvs,
                        TwoTrailsSchema.SharedSchema.CN + "=?", new String[] { updatedPoint.getCN() });

                if (rows < 1)
                    return false;

                if (updatedPoint.getOp() != oldPoint.getOp()) {
                    changeOperation(updatedPoint, oldPoint);
                } else {
                    switch (updatedPoint.getOp()) {
                        case InertialStart:
                            updateInertialStartData((InertialStartPoint)updatedPoint);
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
                        case Inertial:
                            updateInertialData((InertialPoint)updatedPoint);
                            break;
                    }
                }
            } catch (Exception ex) {
                logError(ex.getMessage(), "DAL:updateBasePoint");
                return false;
            }
        }

        return true;
    }


    private void changeOperation(TtPoint updatedPoint, TtPoint oldPoint) {
        switch (oldPoint.getOp()) {
            case InertialStart:
                removeGpsData((InertialStartPoint)oldPoint);
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
            case InertialStart:
                insertInertialStartData((InertialStartPoint)updatedPoint);
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
            case Inertial:
                insertInertialData((InertialPoint)updatedPoint);
                break;
        }
    }

    private void updateGpsData(GpsPoint updatedPoint) {
        ContentValues cvs = new ContentValues();

        cvs.put(TwoTrailsSchema.SharedSchema.CN, updatedPoint.getCN());
        cvs.put(TwoTrailsSchema.GpsPointSchema.ManualAccuracy, updatedPoint.getManualAccuracy());
        cvs.put(TwoTrailsSchema.GpsPointSchema.RMSEr, updatedPoint.getRMSEr());
        cvs.put(TwoTrailsSchema.GpsPointSchema.Latitude, updatedPoint.getLatitude());
        cvs.put(TwoTrailsSchema.GpsPointSchema.Longitude, updatedPoint.getLongitude());
        cvs.put(TwoTrailsSchema.GpsPointSchema.Elevation, updatedPoint.getElevation());

        getDB().update(TwoTrailsSchema.GpsPointSchema.TableName, cvs,
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

        getDB().update(TwoTrailsSchema.TravPointSchema.TableName, cvs,
                TwoTrailsSchema.SharedSchema.CN + "=?", new String[]{ updatedPoint.getCN() });
    }

    private void updateQuondamData(QuondamPoint updatedPoint, QuondamPoint oldPoint) {
        ContentValues cvs = new ContentValues();

        cvs.put(TwoTrailsSchema.SharedSchema.CN, updatedPoint.getCN());
        cvs.put(TwoTrailsSchema.QuondamPointSchema.ManualAccuracy, updatedPoint.getManualAccuracy());

        if (!updatedPoint.getParentCN().equals(oldPoint.getParentCN())) {
            cvs.put(TwoTrailsSchema.QuondamPointSchema.ParentPointCN, updatedPoint.getParentCN());

            removeQuondamLink(oldPoint);
            updateQuondamLink(updatedPoint);
        }

        getDB().update(TwoTrailsSchema.QuondamPointSchema.TableName, cvs,
                TwoTrailsSchema.SharedSchema.CN + "=?", new String[]{updatedPoint.getCN()});
    }

    private void updateQuondamLink(QuondamPoint point) {
        if (point.hasParent()) {
            TtPoint linkedPoint = getPointByCN(point.getParentCN());

            if (linkedPoint != null) {
                linkedPoint.addQuondamLink(point.getCN());
                updatePoint(linkedPoint, point.getParentPoint());
            }
        } else {
            throw new NullPointerException("Quondam has no parent to save.");
        }
    }

    private void removeQuondamLink(QuondamPoint point) {
        if (point.hasParent()) {
            TtPoint linkedPoint = getPointByCN(point.getParentCN());

            if (linkedPoint != null) {
                linkedPoint.removeQuondamLink(point.getCN());
                updatePoint(linkedPoint, point.getParentPoint());
            }
        }
    }

    private void updateInertialStartData(InertialStartPoint updatedPoint) {
        ContentValues cvs = new ContentValues();

        cvs.put(TwoTrailsSchema.SharedSchema.CN, updatedPoint.getCN());
        cvs.put(TwoTrailsSchema.InertialStartPointSchema.BackAz, updatedPoint.getBkAz());
        cvs.put(TwoTrailsSchema.InertialStartPointSchema.ForwadAz, updatedPoint.getFwdAz());
        cvs.put(TwoTrailsSchema.InertialStartPointSchema.AzimuthOffset, updatedPoint.getAzOffset());

        getDB().update(TwoTrailsSchema.InertialStartPointSchema.TableName, cvs,
                TwoTrailsSchema.SharedSchema.CN + "=?", new String[]{ updatedPoint.getCN() });
    }

    private void updateInertialData(InertialPoint updatedPoint) {
        ContentValues cvs = new ContentValues();

        cvs.put(TwoTrailsSchema.SharedSchema.CN, updatedPoint.getCN());
        cvs.put(TwoTrailsSchema.InertialPointSchema.AllSegmentsValid, updatedPoint.areAllSegmentsValid());
        cvs.put(TwoTrailsSchema.InertialPointSchema.TimeSpan, updatedPoint.getTimeSpan());
        cvs.put(TwoTrailsSchema.InertialPointSchema.Azimuth, updatedPoint.getAzimuth());
        cvs.put(TwoTrailsSchema.InertialPointSchema.DistanceX, updatedPoint.getDistX());
        cvs.put(TwoTrailsSchema.InertialPointSchema.DistanceY, updatedPoint.getDistY());
        cvs.put(TwoTrailsSchema.InertialPointSchema.DistanceZ, updatedPoint.getDistZ());

        getDB().update(TwoTrailsSchema.InertialPointSchema.TableName, cvs,
                TwoTrailsSchema.SharedSchema.CN + "=?", new String[]{ updatedPoint.getCN() });
    }
    //endregion

    //region Delete
    private boolean deletePoint(TtPoint point) {
        boolean success = false;
        String pointCN = point.getCN();

        try {
            success = getDB().delete(TwoTrailsSchema.PointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { pointCN }) > 0;

            if (success) {
                switch (point.getOp()) {
                    case InertialStart:
                        removeInertialStartData(point);
                    case GPS:
                    case Take5:
                    case Walk:
                    case WayPoint:
                        removeGpsData(point);
                        break;
                    case Traverse:
                    case SideShot:
                        removeTravData(point);
                        break;
                    case Quondam:
                        removeQuondamData((QuondamPoint) point);
                        break;
                    case Inertial:
                        removeInertialData(point);
                }

                updateUserActivity(DataActionType.DeletedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deletePoint");
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
                        TtPoint newPoint = TtUtils.Points.clonePoint(point);
                        newPoint.copyInfo(qndmPoint);

                        updatePoint(newPoint, qndmPoint);
                    }
                }
            }

            success = deletePoint(point);

            if (success)
                updateUserActivity(DataActionType.DeletedPoints);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deletePointSafe");
        }

        return success;
    }


    public boolean deletePointsInGroup(String groupCN) {
        return deletePoints(getPointsInGroup(groupCN));
    }

    public boolean deletePointsInPolygon(String polyCN) {
        return deletePoints(getPointsInPolygon(polyCN));
    }


    private boolean deletePoints(Collection<TtPoint> points) {
        boolean success = true;

        try {
            for (TtPoint point : points) {
                success = deletePointSafe(point);

                if (success)
                    updateUserActivity(DataActionType.DeletedPoints);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deletePoints");
            success = false;
        }

        return success;
    }


    private void removeGpsData(TtPoint point) {
        try {
            getDB().delete(TwoTrailsSchema.GpsPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });

            deleteNmeaByPointCN(point.getCN());
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:removeGpsData");
            throw new RuntimeException("DAL:removeGpsData");
        }
    }

    private void removeTravData(TtPoint point) {
        try {
            getDB().delete(TwoTrailsSchema.TravPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:removeTravData");
            throw new RuntimeException("DAL:removeTravData");
        }
    }

    private void removeQuondamData(QuondamPoint point) {
        try {
            getDB().delete(TwoTrailsSchema.QuondamPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });

            removeQuondamLink(point);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:removeQuondamData");
            throw new RuntimeException("DAL:removeQuondamData");
        }
    }

    private void removeInertialStartData(TtPoint point) {
        try {
            getDB().delete(TwoTrailsSchema.InertialStartPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:removeInertialStartData");
            throw new RuntimeException("DAL:removeInertialStartData");
        }
    }

    private void removeInertialData(TtPoint point) {
        try {
            getDB().delete(TwoTrailsSchema.InertialPointSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { point.getCN() });
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:removeInertialData");
            throw new RuntimeException("DAL:removeInertialData");
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
        ArrayList<TtMetadata> metas = getMetadata(String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, cn));

        if (metas.size() > 0)
            return metas.get(0);
        return null;
    }

    private ArrayList<TtMetadata> getMetadata(String where) {
        ArrayList<TtMetadata> metas = new ArrayList<>();

        try {
            String query = createSelectQuery(
                    TwoTrailsSchema.MetadataSchema.TableName,
                    TwoTrailsSchema.MetadataSchema.SelectItems,
                    where);

            Cursor c = getDB().rawQuery(query, null);

            TtMetadata meta;

            if (c.moveToFirst()) {
                do {
                    meta = new TtMetadata();

                    if (!c.isNull(0))
                        meta.setCN(c.getString(0));
                    if (!c.isNull(1))
                        meta.setName(c.getString(1));
                    if (!c.isNull(2))
                        meta.setDistance(Dist.parse(c.getInt(2)));
                    if (!c.isNull(3))
                        meta.setSlope(Slope.parse(c.getInt(3)));
                    if (!c.isNull(4))
                        meta.setMagDec(c.getDouble(4));
                    if (!c.isNull(5))
                        meta.setDecType(DeclinationType.parse(c.getInt(5)));
                    if (!c.isNull(6))
                        meta.setElevation(UomElevation.parse(c.getInt(6)));
                    if (!c.isNull(7))
                        meta.setComment(c.getString(7));
                    if (!c.isNull(8))
                        meta.setDatum(Datum.parse(c.getInt(8)));
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
            logError(ex.getMessage(), "DAL:getMetadata");
            throw new RuntimeException("DAL:getMetadata ");
        }

        return metas;
    }


    public HashMap<String, TtMetadata> getMetadataMap() {
        HashMap<String, TtMetadata> metadata = new HashMap<>();
        ArrayList<TtMetadata> ml = getMetadata();

        if (ml != null) {
            for(TtMetadata metaData : ml) {
                metadata.put(metaData.getCN(), metaData);
            }
        }

        return  metadata;
    }
    //endregion

    //region Insert
    public boolean insertMetadata(TtMetadata meta) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, meta.getCN());
            cvs.put(TwoTrailsSchema.MetadataSchema.Name, meta.getName());
            cvs.put(TwoTrailsSchema.MetadataSchema.Comment, meta.getComment());
            cvs.put(TwoTrailsSchema.MetadataSchema.Distance, meta.getDistance().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.Slope, meta.getSlope().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.MagDec, meta.getMagDec());
            cvs.put(TwoTrailsSchema.MetadataSchema.DeclinationType, meta.getDecType().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.Elevation, meta.getElevation().getValue());

            cvs.put(TwoTrailsSchema.MetadataSchema.Datum, meta.getDatum().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.GpsReceiver, meta.getGpsReceiver());
            cvs.put(TwoTrailsSchema.MetadataSchema.RangeFinder, meta.getRangeFinder());
            cvs.put(TwoTrailsSchema.MetadataSchema.Compass, meta.getCompass());
            cvs.put(TwoTrailsSchema.MetadataSchema.Crew, meta.getCrew());
            cvs.put(TwoTrailsSchema.MetadataSchema.UtmZone, meta.getZone());

            getDB().insert(TwoTrailsSchema.MetadataSchema.TableName, null, cvs);

            getDB().setTransactionSuccessful();
            success = true;

            updateUserActivity(DataActionType.InsertedMetadata);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertMetadata");
        } finally {
            getDB().endTransaction();
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updateMetadata(TtMetadata meta) {
        int success = -1;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.MetadataSchema.Name, meta.getName());
            cvs.put(TwoTrailsSchema.MetadataSchema.Comment, meta.getComment());
            cvs.put(TwoTrailsSchema.MetadataSchema.Distance, meta.getDistance().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.Slope, meta.getSlope().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.MagDec, meta.getMagDec());
            cvs.put(TwoTrailsSchema.MetadataSchema.DeclinationType, meta.getDecType().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.Elevation, meta.getElevation().getValue());

            cvs.put(TwoTrailsSchema.MetadataSchema.Datum, meta.getDatum().getValue());
            cvs.put(TwoTrailsSchema.MetadataSchema.GpsReceiver, meta.getGpsReceiver());
            cvs.put(TwoTrailsSchema.MetadataSchema.RangeFinder, meta.getRangeFinder());
            cvs.put(TwoTrailsSchema.MetadataSchema.Compass, meta.getCompass());
            cvs.put(TwoTrailsSchema.MetadataSchema.Crew, meta.getCrew());
            cvs.put(TwoTrailsSchema.MetadataSchema.UtmZone, meta.getZone());

            success = getDB().update(TwoTrailsSchema.MetadataSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, meta.getCN()), null);

            getDB().setTransactionSuccessful();

            updateUserActivity(DataActionType.ModifiedMetadata);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateMetadata");
        } finally {
            getDB().endTransaction();
        }

        return success > 0;
    }
    //endregion

    //region Delete
    public boolean deleteMetadataSafe(String cn) {
        boolean success;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.PointSchema.MetadataCN, Consts.EmptyGuid);

            getDB().update(TwoTrailsSchema.PointSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.PointSchema.MetadataCN, cn), null);

            success = deleteMetadata(cn);


            if (success) {
                getDB().setTransactionSuccessful();
                updateUserActivity(DataActionType.DeletedMetadata);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deleteMetadataSafe");
            success = false;
        }

        return success;
    }

    private boolean deleteMetadata(String cn) {
        boolean success = false;

        try {
            success = getDB().delete(TwoTrailsSchema.MetadataSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;

            if (success)
                updateUserActivity(DataActionType.DeletedMetadata);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deleteMetadata");
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
        return getGroups(String.format(Locale.US, "%s = %d",
                TwoTrailsSchema.GroupSchema.Type,
                type.getValue()));
    }

    public TtGroup getGroupByCN(String cn) {
        ArrayList<TtGroup> groups = getGroups(String.format("%s = '%s'",
                TwoTrailsSchema.SharedSchema.CN, cn));

        if (groups.size() > 0)
            return groups.get(0);
        else
            return null;
    }

    private ArrayList<TtGroup> getGroups(String where) {
        ArrayList<TtGroup> groups = new ArrayList<>();

        try {
            String query = createSelectQuery(
                            TwoTrailsSchema.GroupSchema.TableName,
                            TwoTrailsSchema.GroupSchema.SelectItems,
                            where);

            Cursor c = getDB().rawQuery(query, null);

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
                        group.setGroupType(TtGroup.GroupType.parse(c.getInt(3)));

                    groups.add(group);

                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getGroups");
            throw new RuntimeException("DAL:getGroups");
        }

        return groups;
    }


    public HashMap<String, TtGroup> getGroupsMap() {
        HashMap<String, TtGroup> groups = new HashMap<>();
        ArrayList<TtGroup> gl = getGroups();

        if (gl != null) {
            for (TtGroup group : getGroups()) {
                groups.put(group.getCN(), group);
            }
        }

        return  groups;
    }
    //endregion

    //region Insert
    public boolean insertGroup(TtGroup group) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, group.getCN());
            cvs.put(TwoTrailsSchema.GroupSchema.Name, group.getName());
            cvs.put(TwoTrailsSchema.GroupSchema.Description, group.getDescription());
            cvs.put(TwoTrailsSchema.GroupSchema.Type, group.getGroupType().getValue());

            getDB().insert(TwoTrailsSchema.GroupSchema.TableName, null, cvs);

            getDB().setTransactionSuccessful();
            success = true;

            updateUserActivity(DataActionType.InsertedGroups);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertGroup");
        } finally {
            getDB().endTransaction();
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updateGroup(TtGroup group) {
        int success = -1;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.GroupSchema.Name, group.getName());
            cvs.put(TwoTrailsSchema.GroupSchema.Description, group.getDescription());
            cvs.put(TwoTrailsSchema.GroupSchema.Type, group.getGroupType().getValue());

            success = getDB().update(TwoTrailsSchema.GroupSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, group.getCN()), null);


            if (success > 0) {
                updateUserActivity(DataActionType.ModifiedGroups);
            }

            getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateGroup");
        } finally {
            getDB().endTransaction();
        }

        return success > 0;
    }
    //endregion

    //region Delete
    public boolean deleteGroup(String cn) {
        boolean success = false;

        try {
            success = getDB().delete(TwoTrailsSchema.GroupSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;

            if (success)
                updateUserActivity(DataActionType.DeletedGroups);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deleteGroup");
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
            String query = createSelectQuery(
                    TwoTrailsSchema.TtNmeaSchema.TableName,
                    TwoTrailsSchema.TtNmeaSchema.SelectItems,
                    where);

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                do {
                    String cn;
                    DateTime timeCreated;
                    String pointCN, satsInView;
                    boolean used;
                    DateTime fixTime;
                    double groundSpeed, trackAngle, magVar, pdop, hdop, vdop, horizDilution, geoidHeight;
                    EastWest magVarDir;
                    Mode opMode;
                    GnssFix fix;
                    ArrayList<Integer> satsUsed;
                    GnssFixQuality fixQuality;
                    int trackedSatellites, numberOfSatellitesInView;
                    UomElevation geoUom;

                    double lat, lon, elev;
                    NorthSouth latDir;
                    EastWest lonDir;
                    UomElevation uomelev;

                    //region Name Time Pos
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
                        timeCreated = parseDateTime(c.getString(3));
                    else
                        continue;

                    if (!c.isNull(4))
                        fixTime = parseDateTime(c.getString(4));
                    else
                        continue;

                    if (!c.isNull(5))
                        lat = c.getDouble(5);
                    else
                        continue;

                    if (!c.isNull(6))
                        latDir = NorthSouth.parse(c.getInt(6));
                    else
                        continue;

                    if (!c.isNull(7))
                        lon = c.getDouble(7);
                    else
                        continue;

                    if (!c.isNull(8))
                        lonDir = EastWest.parse(c.getInt(8));
                    else
                        continue;

                    if (!c.isNull(9))
                        elev = c.getDouble(9);
                    else
                        continue;

                    if (!c.isNull(10))
                        uomelev = UomElevation.parse(c.getInt(10));
                    else
                        continue;
                    //endregion

                    //region Mag Fix Dop
                    if (!c.isNull(11))
                        magVar = c.getDouble(11);
                    else
                        continue;

                    if (!c.isNull(12))
                        magVarDir = EastWest.parse(c.getInt(12));
                    else
                        magVarDir = null;

                    if (!c.isNull(13))
                        fix = GnssFix.parse(c.getInt(13));
                    else
                        continue;

                    if (!c.isNull(14))
                        fixQuality = GnssFixQuality.parse(c.getInt(14));
                    else
                        continue;

                    if (!c.isNull(15))
                        opMode = Mode.parse(c.getInt(15));
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
                        geoUom = UomElevation.parse(c.getInt(21));
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

                    if (!c.isNull(28))
                        satsInView = c.getString(28);
                    else
                        continue;

                    //endregion

                    nmeas.add(new TtNmeaBurst(cn, timeCreated, pointCN, used, new Position(lat, latDir, lon, lonDir, elev, uomelev), fixTime, groundSpeed,
                            trackAngle, magVar, magVarDir, opMode, fix, satsUsed, pdop, hdop, vdop, fixQuality,
                            trackedSatellites, horizDilution, geoidHeight, geoUom, numberOfSatellitesInView, satsInView));
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getNmeaBursts");
            throw new RuntimeException("DAL:getNmeaBursts");
        }

        return nmeas;
    }
    //endregion

    //region Insert
    public boolean insertNmeaBursts(Collection<TtNmeaBurst> bursts) {
        try {
            getDB().beginTransaction();

            for (TtNmeaBurst burst : bursts) {
                if (!insertNmeaBurstNoTrans(burst)) {
                    return false;
                }
            }

            getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertNmeaBursts");
            throw new RuntimeException("DAL:insertNmeaBursts");
        } finally {
            getDB().endTransaction();
        }

        return true;
    }

    public boolean insertNmeaBurst(TtNmeaBurst burst) {
        boolean success;

        try {
            getDB().beginTransaction();

            success = insertNmeaBurstNoTrans(burst);

            if (success) {
                getDB().setTransactionSuccessful();
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertNmeaBurst");
            throw new RuntimeException("DAL:insertNmeaBurst");
        } finally {
            getDB().endTransaction();
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
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LatDir, burst.getLatDir().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Longitude, burst.getLongitude());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LonDir, burst.getLonDir().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Elevation, burst.getElevation());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.ElevUom, burst.getUomElevation().getValue());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.MagVar, burst.getMagVar());
            if (burst.getMagVarDir() != null) { cvs.put(TwoTrailsSchema.TtNmeaSchema.MagDir,burst.getMagVarDir().getValue()); }
            if (burst.getFix() != null) { cvs.put(TwoTrailsSchema.TtNmeaSchema.Fix,burst.getFix().getValue()); }
            if (burst.getFixQuality() != null) { cvs.put(TwoTrailsSchema.TtNmeaSchema.FixQuality,burst.getFixQuality().getValue()); }
            if (burst.getOperationMode() != null) { cvs.put(TwoTrailsSchema.TtNmeaSchema.Mode,burst.getOperationMode().getValue()); }

            cvs.put(TwoTrailsSchema.TtNmeaSchema.PDOP, burst.getPDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HDOP, burst.getHDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.VDOP, burst.getVDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HorizDilution, burst.getHorizDilution());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeight, burst.getGeoidHeight());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeightUom, burst.getGeoUom().getValue());
            if (burst.getGroundSpeed() != null) { cvs.put(TwoTrailsSchema.TtNmeaSchema.GroundSpeed, burst.getGroundSpeed()); }
            if (burst.getTrackAngle() != null) { cvs.put(TwoTrailsSchema.TtNmeaSchema.TrackAngle, burst.getTrackAngle()); }

            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesUsedCount, burst.getUsedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesTrackedCount, burst.getTrackedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesInViewCount, burst.getSatellitesInViewCount());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.UsedSatPRNS, burst.getUsedSatelliteIDsString());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesInView, burst.getSatellitesInViewString());

            getDB().insert(TwoTrailsSchema.TtNmeaSchema.TableName, null, cvs);

            success = true;

            updateUserActivity(DataActionType.InsertedNmea);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertNmea");
        }

        return success;
    }
    //endregion

    //region Update

    public void updateNmeaBursts(Collection<TtNmeaBurst> bursts) {
        try {
            getDB().beginTransaction();

            for (TtNmeaBurst burst : bursts) {
                updateNmeaBurst(burst);
            }

            getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateNmeaBursts");
            throw new RuntimeException("DAL:updateNmeaBursts");
        } finally {
            getDB().endTransaction();
        }
    }

    public int updateNmeaBurst(TtNmeaBurst burst) {
        int success = -1;

        try {
            getDB().beginTransaction();

            success = updateNmeaBurstNoTrans(burst);

            getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateNmeaBurst");
        } finally {
            getDB().endTransaction();
        }

        return success;
    }

    private int updateNmeaBurstNoTrans(TtNmeaBurst burst) {
        int success = -1;

        try {

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.TtNmeaSchema.PointCN, burst.getPointCN());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Used, burst.isUsed());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.TimeCreated, dtf.print(burst.getTimeCreated()));

            cvs.put(TwoTrailsSchema.TtNmeaSchema.FixTime, dtf.print(burst.getFixTime()));
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Latitude, burst.getLatitude());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LatDir, burst.getLatDir().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Longitude, burst.getLongitude());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.LonDir, burst.getLonDir().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Elevation, burst.getElevation());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.ElevUom, burst.getUomElevation().getValue());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.MagVar, burst.getMagVar());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.MagDir, burst.getMagVarDir().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Fix, burst.getFix().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.FixQuality, burst.getFixQuality().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.Mode, burst.getOperationMode().getValue());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.PDOP, burst.getPDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HDOP, burst.getHDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.VDOP, burst.getVDOP());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.HorizDilution, burst.getHorizDilution());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeight, burst.getGeoidHeight());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GeiodHeightUom, burst.getGeoUom().getValue());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.GroundSpeed, burst.getGroundSpeed());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.TrackAngle, burst.getTrackAngle());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesUsedCount, burst.getUsedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesTrackedCount, burst.getTrackedSatellitesCount());
            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesInViewCount, burst.getSatellitesInViewCount());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.UsedSatPRNS, burst.getUsedSatelliteIDsString());

            cvs.put(TwoTrailsSchema.TtNmeaSchema.SatellitesInView, burst.getSatellitesInViewString());

            success = getDB().update(TwoTrailsSchema.TtNmeaSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, burst.getCN()), null);

            if (success > 0) {
                updateUserActivity(DataActionType.ModifiedNmea);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateNmea");
        }

        return success;
    }

    //endregion

    //region Delete
    public void deleteNmeaByCN(String cn) {
        try {
            getDB().delete(TwoTrailsSchema.TtNmeaSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?", new String[] { cn });

            updateUserActivity(DataActionType.DeletedNmea);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deleteNmea");
            throw new RuntimeException("DAL:deleteNmea");
        }
    }

    public void deleteNmeaByPointCN(String pointCN) {
        try {
            getDB().delete(TwoTrailsSchema.TtNmeaSchema.TableName,
                TwoTrailsSchema.TtNmeaSchema.PointCN + "=?", new String[] { pointCN });

            updateUserActivity(DataActionType.DeletedNmea);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deleteNmea");
            throw new RuntimeException("DAL:deleteNmea");
        }
    }
    //endregion
    //endregion

    //region INS
    //region Get
    public ArrayList<TtInsData> getInsData() {
        return getInsData(null);
    }

    public TtInsData getInsDataByCN(String cn) {
        ArrayList<TtInsData> data =
                getInsData(String.format("%s = '%s'",
                        TwoTrailsSchema.SharedSchema.CN, cn));

        if (data.size() > 0)
            return data.get(0);
        else
            return null;
    }

    public ArrayList<TtInsData> getInsDataByPointCN(String pointCN) {
        return getInsData(String.format("%s = '%s'",
                TwoTrailsSchema.TtInsSchema.PointCN,
                pointCN));
    }

    public ArrayList<TtInsData> getInsData(String where) {
        ArrayList<TtInsData> data = new ArrayList<>();

        try {
            String query = createSelectQuery(
                    TwoTrailsSchema.TtInsSchema.TableName,
                    TwoTrailsSchema.TtInsSchema.SelectItems,
                    where);

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                do {
                    String cn, pointCN;
                    DateTime timeCreated;
                    boolean isConsecutive;
                    long timeSinceStart;

                    double timespan;
                    double distX, distY, distZ;
                    double rotX, rotY, rotZ;
                    double velX, velY, velZ;
                    double yaw, pitch, roll;

                    cn = c.getString(0);
                    pointCN = c.getString(1);
                    timeCreated = parseDateTime(c.getString(2));
                    
                    isConsecutive = ParseEx.parseBoolean(c.getString(3));
                    timeSinceStart = c.getLong(4);
                    timespan = c.getDouble(5);
                    
                    distX = c.getDouble(6);
                    distY = c.getDouble(7);
                    distZ = c.getDouble(8);
                    
                    rotX = c.getDouble(9);
                    rotY = c.getDouble(10);
                    rotZ = c.getDouble(11);

                    velX = c.getDouble(12);
                    velY = c.getDouble(13);
                    velZ = c.getDouble(14);
                    
                    yaw = c.getDouble(15);
                    pitch = c.getDouble(16);
                    roll = c.getDouble(17);
                    
                    data.add(new TtInsData(
                            cn, pointCN, timeCreated,
                            isConsecutive, timeSinceStart, timespan,
                            distX, distY, distZ,
                            rotX, rotY, rotZ,
                            velX, velY, velZ,
                            yaw, pitch, roll));
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getInsData");
            throw new RuntimeException("DAL:getInsData");
        }

        return data;
    }
    //endregion

    //region Insert
    public boolean insertInsData(Collection<TtInsData> data) {
        try {
            getDB().beginTransaction();

            for (TtInsData burst : data) {
                if (!insertInsDataNoTrans(burst)) {
                    return false;
                }
            }

            getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertInsData");
            throw new RuntimeException("DAL:insertInsData");
        } finally {
            getDB().endTransaction();
        }

        return true;
    }

    public boolean insertInsData(TtInsData data) {
        boolean success;

        try {
            getDB().beginTransaction();

            success = insertInsDataNoTrans(data);

            if (success) {
                getDB().setTransactionSuccessful();
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertInsData");
            throw new RuntimeException("DAL:insertInsData");
        } finally {
            getDB().endTransaction();
        }

        return success;
    }

    private boolean insertInsDataNoTrans(TtInsData data) {
        boolean success = false;

        try {
            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, data.getCN());
            cvs.put(TwoTrailsSchema.TtInsSchema.PointCN, data.getPointCN());
            cvs.put(TwoTrailsSchema.TtInsSchema.TimeCreated, dtf.print(data.getTimeCreated()));

            cvs.put(TwoTrailsSchema.TtInsSchema.IsConsecutive, data.isConsecutive());
            cvs.put(TwoTrailsSchema.TtInsSchema.TimeSinceStart, data.getTimeSinceStart());
            cvs.put(TwoTrailsSchema.TtInsSchema.TimeSpan, data.getTimeSpan());

            cvs.put(TwoTrailsSchema.TtInsSchema.DistanceX, data.getDistanceX());
            cvs.put(TwoTrailsSchema.TtInsSchema.DistanceY, data.getDistanceY());
            cvs.put(TwoTrailsSchema.TtInsSchema.DistanceZ, data.getDistanceZ());

            cvs.put(TwoTrailsSchema.TtInsSchema.RotationX, data.getRotationX());
            cvs.put(TwoTrailsSchema.TtInsSchema.RotationY, data.getRotationY());
            cvs.put(TwoTrailsSchema.TtInsSchema.RotationZ, data.getRotationZ());

            cvs.put(TwoTrailsSchema.TtInsSchema.VelocityX, data.getVelocityX());
            cvs.put(TwoTrailsSchema.TtInsSchema.VelocityY, data.getVelocityY());
            cvs.put(TwoTrailsSchema.TtInsSchema.VelocityZ, data.getVelocityZ());

            cvs.put(TwoTrailsSchema.TtInsSchema.Yaw, data.getYaw());
            cvs.put(TwoTrailsSchema.TtInsSchema.Pitch, data.getPitch());
            cvs.put(TwoTrailsSchema.TtInsSchema.Roll, data.getRoll());

            getDB().insert(TwoTrailsSchema.TtInsSchema.TableName, null, cvs);

            success = true;

            updateUserActivity(DataActionType.InsertedIns);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertInsData");
        }

        return success;
    }
    //endregion

    //region Delete
    public void deleteInsDataByCN(String cn) {
        try {
            getDB().delete(TwoTrailsSchema.TtInsSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?", new String[] { cn });

            updateUserActivity(DataActionType.DeletedIns);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deleteInsData");
            throw new RuntimeException("DAL:deleteInsData");
        }
    }

    public void deleteInsDataByPointCN(String pointCN) {
        try {
            getDB().delete(TwoTrailsSchema.TtInsSchema.TableName,
                    TwoTrailsSchema.TtInsSchema.PointCN + "=?", new String[] { pointCN });

            updateUserActivity(DataActionType.DeletedIns);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deleteInsData");
            throw new RuntimeException("DAL:deleteInsData");
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

        try (Cursor c = getDB().rawQuery(getQuery, null)) {
            if (c.moveToFirst()) {
                if (!c.isNull(0))
                    retString = c.getString(0);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DataAccessLayer:getProjectInfoField");
            throw new RuntimeException("DAL:getProjectInfoField");
        }

        return retString;
    }
    //endregion

    //region Set
    private void setupProjectInfo(String projectName) {
        try
        {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.TtDbSchemaVersion,
                    TwoTrailsSchema.SchemaVersion.toString());

            String version = TtUtils.getApplicationVersion(getTtAppContext());
            ProjectSettings ps = getTtAppContext().getProjectSettings();

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.TtVersion, version);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.CreatedTtVersion, version);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Region, StringEx.Empty);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.ID, projectName);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.DeviceID, TtUtils.getDeviceName());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Forest, StringEx.Empty);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.District, StringEx.Empty);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Description, StringEx.Empty);

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.Created, new DateTime().toString());

            cvs.put(TwoTrailsSchema.ProjectInfoSchema.DeviceID, TtUtils.getDeviceName());

            getDB().insert(TwoTrailsSchema.ProjectInfoSchema.TableName, null, cvs);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:SetupProjectInfo");
            throw new RuntimeException("DAL:SetupProjectInfo");
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
            getDB().execSQL(updateQuery);

            updateUserActivity(DataActionType.ModifiedProject);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "DataAccessLayer:SetProjectInfoField");
            throw new RuntimeException("DAL:SetProjectInfoField");
        }
    }
    //endregion
    //endregion


    //region Polygon Attr
    //region Get
    public ArrayList<PolygonGraphicOptions> getPolygonGraphicOptions() {
        return getPolygonGraphicOptions(null);
    }

    public PolygonGraphicOptions getPolygonGraphicOptionByCN(String cn) {
        ArrayList<PolygonGraphicOptions> groups = getPolygonGraphicOptions(String.format("%s = '%s'",
                TwoTrailsSchema.SharedSchema.CN, cn));

        if (groups.size() > 0)
            return groups.get(0);
        else
            return null;
    }

    private ArrayList<PolygonGraphicOptions> getPolygonGraphicOptions(String where) {
        ArrayList<PolygonGraphicOptions> graphicOptions = new ArrayList<>();

        try {
            String query = createSelectQuery(
                    TwoTrailsSchema.PolygonAttrSchema.TableName,
                    TwoTrailsSchema.PolygonAttrSchema.SelectItems,
                    where);

            Cursor c = getDB().rawQuery(query, null);
            String cn;
            int adjbnd, unadjbnd, adjnav, unadjnav, adjpts, unadjpts, waypts;

            TwoTrailsApp ctx = getTtAppContext();
            MapSettings ms = ctx.getMapSettings();

            if (c.moveToFirst()) {
                do {

                    if (!c.isNull(0))
                        cn = c.getString(0);
                    else
                        throw new RuntimeException("PolygonGraphicOptions has no CN");

                    if (!c.isNull(1))
                        adjbnd = c.getInt(1);
                    else
                        adjbnd = ms.getDefaultAdjBndColor();

                    if (!c.isNull(2))
                        unadjbnd = c.getInt(2);
                    else
                        unadjbnd = ms.getDefaultUnAdjBndColor();

                    if (!c.isNull(3))
                        adjnav = c.getInt(3);
                    else
                        adjnav = ms.getDefaultAdjNavColor();

                    if (!c.isNull(4))
                        unadjnav = c.getInt(4);
                    else
                        unadjnav = ms.getDefaultUnAdjNavColor();

                    if (!c.isNull(5))
                        adjpts = c.getInt(5);
                    else
                        adjpts = ms.getDefaultAdjPtsColor();

                    if (!c.isNull(6))
                        unadjpts = c.getInt(6);
                    else
                        unadjpts = ms.getDefaultUnAdjPtsColor();

                    if (!c.isNull(7))
                        waypts = c.getInt(7);
                    else
                        waypts = ms.getDefaultWayPtsColor();

                    graphicOptions.add(
                            new PolygonGraphicOptions(cn,
                                    adjbnd, unadjbnd, adjnav, unadjnav, adjpts, unadjpts, waypts,
                                    ctx.getDeviceSettings().getMapAdjLineWidth(),
                                    ctx.getDeviceSettings().getMapUnAdjLineWidth())
                    );
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getPolygonGraphicOptions");
            throw new RuntimeException("DAL:getPolygonGraphicOptions");
        }

        return graphicOptions;
    }


    public HashMap<String, PolygonGraphicOptions> getPolygonGraphicOptionsMap() {
        HashMap<String, PolygonGraphicOptions> pgos = new HashMap<>();
        ArrayList<PolygonGraphicOptions> opts = getPolygonGraphicOptions();

        if (opts != null) {
            for (PolygonGraphicOptions pgo : opts) {
                pgos.put(pgo.getCN(), pgo);
            }
        }

        return  pgos;
    }
    //endregion

    //region Insert
    public boolean insertPolygonGraphicOption(PolygonGraphicOptions pgo) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.SharedSchema.CN, pgo.getCN());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.AdjBndColor, pgo.getAdjBndColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.UnAdjBndColor, pgo.getUnAdjBndColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.AdjNavColor, pgo.getAdjNavColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.UnAdjNavColor, pgo.getUnAdjNavColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.AdjPtsColor, pgo.getAdjPtsColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.UnAdjPtsColor, pgo.getUnAdjPtsColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.WayPtsColor, pgo.getWayPtsColor());

            getDB().insert(TwoTrailsSchema.PolygonAttrSchema.TableName, null, cvs);

            getDB().setTransactionSuccessful();
            success = true;
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertPolygonGraphicOption");
        } finally {
            getDB().endTransaction();
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updatePolygonGraphicOption(PolygonGraphicOptions pgo) {
        int success = -1;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.AdjBndColor, pgo.getAdjBndColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.UnAdjBndColor, pgo.getUnAdjBndColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.AdjNavColor, pgo.getAdjNavColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.UnAdjNavColor, pgo.getUnAdjNavColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.AdjPtsColor, pgo.getAdjPtsColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.UnAdjPtsColor, pgo.getUnAdjPtsColor());
            cvs.put(TwoTrailsSchema.PolygonAttrSchema.WayPtsColor, pgo.getWayPtsColor());

            success = getDB().update(TwoTrailsSchema.PolygonAttrSchema.TableName, cvs,
                    String.format("%s = '%s'", TwoTrailsSchema.SharedSchema.CN, pgo.getCN()), null);

            getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updatePolygonGraphicOption");
        } finally {
            getDB().endTransaction();
        }

        return success > 0;
    }
    //endregion

    //region Delete
    public boolean deletePolygonGraphicOption(String cn) {
        boolean success = false;

        try {
            success = getDB().delete(TwoTrailsSchema.PolygonAttrSchema.TableName,
                    TwoTrailsSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:deletePolygonGraphicOption");
        }

        return success;
    }
    //endregion
    //endregion


    //region Activity
    private TtUserAction createUserAction() {
        return new TtUserAction("Android User", TtUtils.getDeviceName(), TtUtils.getApplicationVersion(getTtAppContext()));
    }

    public void updateUserActivity(int actionType) {
        if (_Activity == null) _Activity = createUserAction();
        _Activity.updateAction(actionType);
        onAction(actionType);
    }

    public void updateUserActivity(int actionType, String notes) {
        if (_Activity == null) _Activity = createUserAction();
        _Activity.updateAction(actionType, notes);
        onAction(actionType);
    }
    
    //region Get
    private ArrayList<TtUserAction> getUserActivity() {
        ArrayList<TtUserAction> activities = new ArrayList<>();

        try {

            String query = String.format("select %s from %s",
                    TwoTrailsSchema.ActivitySchema.SelectItems,
                    TwoTrailsSchema.ActivitySchema.TableName);

            Cursor c = getDB().rawQuery(query, null);

            if (c.moveToFirst()) {
                do {
                    activities.add(new TtUserAction(
                            c.getString(0),
                            c.getString(1),
                            c.getString(2),
                            parseDateTime(c.getString(3)),
                            new DataActionType(c.getInt(4)),
                            c.getString(5)));
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:getUserActivity");
            throw new RuntimeException("DAL:getUserActivity");
        }

        return activities;
    }
    //endregion

    //region Insert
    public boolean updateUserSession() {
        if (_Activity != null && _Activity.getAction().getValue() != 0) {
            insertUserActivity(_Activity);
            _Activity = createUserAction();
            return true;
        }

        return false;
    }

    protected boolean insertUserActivity(TtUserAction activity) {
        boolean success = false, inTrans = false;

        if (activity.getAction().equals(DataActionType.None)) {
            return true;
        }

        try {
            if (getDB().inTransaction()) {
                inTrans = true;
            } else {
                getDB().beginTransaction();
            }

            ContentValues cvs = new ContentValues();
            cvs.put(TwoTrailsSchema.ActivitySchema.UserName, activity.getUserName());
            cvs.put(TwoTrailsSchema.ActivitySchema.DeviceName, activity.getDeviceName());
            cvs.put(TwoTrailsSchema.ActivitySchema.ActivityDate, dtf.print(activity.getDate()));
            cvs.put(TwoTrailsSchema.ActivitySchema.DataActivity, activity.getAction().getValue());
            cvs.put(TwoTrailsSchema.ActivitySchema.ActivityNotes, activity.getNotes());
            cvs.put(TwoTrailsSchema.ActivitySchema.AppVersion, TtUtils.getApplicationVersion(getTtAppContext()));

            getDB().insert(TwoTrailsSchema.ActivitySchema.TableName, null, cvs);

            if (!inTrans) {
                getDB().setTransactionSuccessful();
                getDB().endTransaction();
            }

            success = true;
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertUserActivity");
        }

        return success;
    }
    //endregion
    //endregion


    //region DataDictionary

    //endregion


    //region DbTools
    public boolean hasPolygons() {
        return getItemsCount(TwoTrailsSchema.PolygonSchema.TableName) > 0;
    }

//    public boolean duplicate(String duplicateFileName) {
//        try {
//            DataAccessLayer dDal = new DataAccessLayer(Uri.fromFile(new File(duplicateFileName)), TtAppCtx);
//
//            dDal.setProjectID(getProjectID());
//            dDal.setProjectDescription(getProjectDescription());
//            dDal.setProjectRegion(getProjectRegion());
//            dDal.setProjectForest(getProjectForest());
//            dDal.setProjectDistrict(getProjectDistrict());
//            dDal.setProjectDateCreated(getProjectDateCreated());
//            dDal.setProjectDeviceID(getProjectDeviceID());
//            dDal.setProjectCreatedTtVersion(getProjectCreatedTtVersion());
//
//            for (TtMetadata meta : getMetadata()) {
//                if (meta.getCN().equals(Consts.EmptyGuid)) {
//                    dDal.updateMetadata(meta);
//                } else {
//                    dDal.insertMetadata(meta);
//                }
//            }
//
//            for (TtGroup group : getGroups()) {
//                if (group.getCN().equals(Consts.EmptyGuid)) {
//                    dDal.updateGroup(group);
//                } else {
//                    dDal.insertGroup(group);
//                }
//            }
//
//            for (TtPolygon poly : getPolygons()) {
//                dDal.insertPolygon(poly);
//            }
//
//            dDal.insertPoints(getPoints());
//
//            dDal.insertNmeaBursts(getNmeaBursts());
//
//            dDal.close();
//        } catch (Exception ex) {
//            logError(ex.getMessage(), "DAL:duplicate", ex.getStackTrace());
//            return false;
//        }
//
//        return true;
//    }

    public boolean needsAdjusting() {
        String countQuery = String.format("SELECT COUNT (*) FROM %s where %s IS NULL OR %s IS NULL OR %s IS NULL OR %s IS NULL",
                TwoTrailsSchema.PointSchema.TableName,
                TwoTrailsSchema.PointSchema.AdjX,
                TwoTrailsSchema.PointSchema.AdjY,
                TwoTrailsSchema.PointSchema.AdjZ,
                TwoTrailsSchema.PointSchema.Accuracy);

        Cursor cursor = getDB().rawQuery(countQuery, null);

        int count = 0;
        if (null != cursor) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            cursor.close();
        }

        return count > 0;
    }

    //TODO implement error checking
    public boolean hasErrors() {
        //just checking for project table for now.
        try {
            String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + TwoTrailsSchema.ProjectInfoSchema.TableName + "';";

            Cursor cursor = getDB().rawQuery(query, null);

            if (null != cursor) {
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return true;
                }
            } else {
                return true;
            }

            return false;
        } catch (Exception e) {
            //
        }

        return true;
    }


    public boolean clean() {
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

        ArrayList<String> pointCNs = getCNs(TwoTrailsSchema.PointSchema.TableName, sbPoly.toString());
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
                getDB().delete(TwoTrailsSchema.PointSchema.TableName, wherePointCN, null);
                getDB().delete(TwoTrailsSchema.GpsPointSchema.TableName, wherePointCN, null);
                getDB().delete(TwoTrailsSchema.TravPointSchema.TableName, wherePointCN, null);
                getDB().delete(TwoTrailsSchema.QuondamPointSchema.TableName, wherePointCN, null);

                getDB().delete(TwoTrailsSchema.TtNmeaSchema.TableName, nmeaDeleteQuerys.get(i), null);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:clean");
            return false;
        }

        return true;
    }


    public void upgrade(Upgrade upgrade) {
        int dbVersion = getVersion().toIntVersion();

        if (dbVersion < upgrade.Version.toIntVersion()) {
            SQLiteDatabase db = getDB();
            for (String sql : upgrade.SQL_Statements) {
                db.execSQL(sql);
            }
        }

        updateUserActivity(DataActionType.ProjectUpgraded, "Upgrade " + getVersion().toString() + " -> " + upgrade.Version.toString());
        insertUserActivity(_Activity);
        _Activity = createUserAction();
    }
    //endregion
}