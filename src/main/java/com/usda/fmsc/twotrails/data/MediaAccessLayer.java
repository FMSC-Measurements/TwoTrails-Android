package com.usda.fmsc.twotrails.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;
import java.util.ArrayList;

public class MediaAccessLayer extends IDataLayer {

    public String getFilePath() {
        return _FilePath;
    }

    private String _FileName;
    public String getFileName() {
        if (_FileName == null) {
            _FileName = FileUtils.getFileNameWoType(_FilePath);
        }
        return _FileName;
    }

    public File getDBFile() { return _dbFile; }

    public TtDalVersion getDalVersion() { return _DalVersion; }


    private TtDalVersion _DalVersion;
    private String _FilePath;
    private File _dbFile;

    //region Constructors / Open / Close
    public MediaAccessLayer(String filePath) {
        _FilePath = filePath;

        if (FileUtils.fileExists(_FilePath))
            open();
        else
            CreateDB();
    }

    public boolean open() {
        if (!isOpen() && FileUtils.fileExists(_FilePath)) {
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
        if (isOpen()) {
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



    public String getTtDbVersion() {
        return null;//getProjectInfoField(TwoTrailsMediaSchema.ProjectInfoSchema.TtDbSchemaVersion);
    }
    //endregion

    //region Create DB
    private void CreateDB() {
        try {
            _dbFile = new File(_FilePath);

            _db = SQLiteDatabase.openOrCreateDatabase(_dbFile, null);
            //_db.rawQuery("PRAGMA journal_mode = MEMORY", null);
            _DalVersion = TwoTrailsMediaSchema.SchemaVersion;

            CreateMediaTable();
            CreatePictureTable();

        } catch (Exception ex) {
            //say that db creation failed, specific tables have already been logged

            TtUtils.TtReport.writeError(ex.getMessage(), "MediaAccessLayer:CreateDB");
        }
    }


    private void CreateMediaTable() {
        try
        {
            _db.execSQL(TwoTrailsMediaSchema.Media.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateMediaTable");
            throw ex;
        }
    }

    private void CreatePictureTable() {
        try
        {
            _db.execSQL(TwoTrailsMediaSchema.PictureSchema.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreatePictureTable");
            throw ex;
        }
    }

    private void CreateDataTable() {
        try
        {
            _db.execSQL(TwoTrailsMediaSchema.Data.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreatePictureTable");
            throw ex;
        }
    }
    //endregion

    //region Get
    public ArrayList<TtImage> getPictureByCN(String cn) {
        return getPictures(
                String.format("%s.%s = '%s'",
                    TwoTrailsMediaSchema.Media.TableName,
                    TwoTrailsMediaSchema.SharedSchema.CN,
                    cn),
                0);
    }

    public ArrayList<TtImage> getPicturesInPoint(String pointCN) {
        return getPictures(
                String.format("%s = '%s'",
                        TwoTrailsMediaSchema.Media.PointCN,
                        pointCN),
                0);
    }

//    public ArrayList<TtImage> getPicturesInPolygon(String polygonCN) {
//        StringBuilder sb = new StringBuilder();
//
//        for (String cn : getCNs(TwoTrailsMediaSchema.PointSchema.TableName,
//                String.format("%s = '%s'", TwoTrailsMediaSchema.PointSchema.PolyCN, polygonCN))) {
//            sb.append(String.format("%s = '%s' or ", TwoTrailsMediaSchema.Media.PointCN, cn));
//        }
//
//        if (sb.length() < 1)
//            return new ArrayList<>();
//
//        return getPictures(sb.substring(0, sb.length() - 5), 0);
//    }

//    public ArrayList<TtImage> getPicturesInGroup(String groupCN) {
//        StringBuilder sb = new StringBuilder();
//
//        for (String cn : getCNs(TwoTrailsMediaSchema.PointSchema.TableName,
//                String.format("%s = '%s'", TwoTrailsMediaSchema.PointSchema.GroupCN, groupCN))) {
//            sb.append(String.format("%s = '%s' or ", TwoTrailsMediaSchema.Media.PointCN, cn));
//        }
//
//        if (sb.length() < 1)
//            return new ArrayList<>();
//
//        return getPictures(sb.substring(0, sb.length() - 5), 0);
//    }

    private ArrayList<TtImage> getPictures(String where, int limit) {
        ArrayList<TtImage> pictures = new ArrayList<>();

        try {
            String query = String.format("%s where %s = %d%s order by datetime(%s) asc %s",
                    SelectPictures,
                    TwoTrailsMediaSchema.Media.MediaType,
                    MediaType.Picture.getValue(),
                    StringEx.isEmpty(where) ? StringEx.Empty : String.format(" and %s", where),
                    TwoTrailsMediaSchema.Media.CreationTime,
                    limit > 0 ? " limit " + limit : StringEx.Empty);

            Cursor c = _db.rawQuery(query, null);

            TtImage pic;

            if (c.moveToFirst()) {
                do {
                    if(!c.isNull(8)) {
                        pic = TtUtils.getPictureByType(PictureType.parse(c.getInt(8)));
                    } else {
                        throw new Exception("Picture has no PictureType");
                    }

                    if (!c.isNull(0))
                        pic.setCN(c.getString(0));
                    else
                        throw new Exception("Picture has no CN");

                    if (!c.isNull(1))
                        pic.setPointCN(c.getString(1));

                    if (!c.isNull(3))
                        pic.setName(c.getString(3));

                    if (!c.isNull(4))
                        pic.setFilePath(c.getString(4));

                    if (!c.isNull(5))
                        pic.setTimeCreated(parseDateTime(c.getString(5)));

                    if (!c.isNull(6))
                        pic.setComment(c.getString(6));

                    if (!c.isNull(7))
                        pic.setIsExternal(ParseEx.parseBoolean(c.getString(7)));

                    if (!c.isNull(9))
                        pic.setAzimuth(c.getFloat(9));

                    if (!c.isNull(10))
                        pic.setPitch(c.getFloat(10));

                    if (!c.isNull(11))
                        pic.setRoll(c.getFloat(11));

                    pictures.add(pic);
                } while (c.moveToNext());
            }

            c.close();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getPictures");
            return null;
        }

        return pictures;
    }
    //endregion

    //region Insert
    public boolean insertMedia(TtMedia media) {
        boolean success = false;

        if (media == null)
            return false;

        try {
            _db.beginTransaction();

            success = insertBaseMedia(media);

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertMedia");
            success = false;
        } finally {
            _db.endTransaction();
        }

        return success;
    }

    private boolean insertBaseMedia(TtMedia media) {
        boolean result = true;

        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.SharedSchema.CN, media.getCN());
            cvs.put(TwoTrailsMediaSchema.Media.PointCN, media.getPointCN());
            cvs.put(TwoTrailsMediaSchema.Media.MediaType, media.getMediaType().getValue());
            cvs.put(TwoTrailsMediaSchema.Media.Name, media.getName());
            cvs.put(TwoTrailsMediaSchema.Media.FilePath, media.getFilePath());
            cvs.put(TwoTrailsMediaSchema.Media.Comment, media.getComment());
            cvs.put(TwoTrailsMediaSchema.Media.CreationTime, dtf.print(media.getTimeCreated()));

            _db.insert(TwoTrailsMediaSchema.Media.TableName, null, cvs);

            switch (media.getMediaType()) {
                case Picture:
                    result = insertPictureData((TtImage)media);
                    break;
                case Video:
                    break;
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertBaseMedia");
            result = false;
        }

        return result;
    }

    private boolean insertPictureData(TtImage picture) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.SharedSchema.CN, picture.getCN());
            cvs.put(TwoTrailsMediaSchema.PictureSchema.PicType, picture.getPictureType().getValue());
            cvs.put(TwoTrailsMediaSchema.PictureSchema.Azimuth, picture.getAzimuth());
            cvs.put(TwoTrailsMediaSchema.PictureSchema.Pitch, picture.getPitch());
            cvs.put(TwoTrailsMediaSchema.PictureSchema.Roll, picture.getRoll());

            _db.insert(TwoTrailsMediaSchema.PictureSchema.TableName, null, cvs);

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertPictureData");
            return false;
        }

        return true;
    }


    //TODO insert Image Binary Data
    //endregion

    //region Update
    public boolean updateMedia(TtMedia media) {
        boolean success = false;

        if (media == null)
            return false;

        try {
            _db.beginTransaction();

            success = updateBaseMedia(media);

            if(success)
                _db.setTransactionSuccessful();
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateMedia");
            success = false;
        } finally {
            _db.endTransaction();
        }

        return success;
    }

    private boolean updateBaseMedia(TtMedia media) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.Media.PointCN, media.getPointCN());
            cvs.put(TwoTrailsMediaSchema.Media.Name, media.getName());
            cvs.put(TwoTrailsMediaSchema.Media.FilePath, media.getFilePath());
            cvs.put(TwoTrailsMediaSchema.Media.Comment, media.getComment());

            _db.update(TwoTrailsMediaSchema.Media.TableName, cvs,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?", new String[] { media.getCN() });

            switch (media.getMediaType()) {
                case Picture:
                    updatePictureData((TtImage)media);
                    break;
                case Video:
                    break;
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:updateBaseMedia");
            return false;
        }

        return true;
    }

    private void updatePictureData(TtImage picture) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.PictureSchema.Azimuth, picture.getAzimuth());
            cvs.put(TwoTrailsMediaSchema.PictureSchema.Pitch, picture.getPitch());
            cvs.put(TwoTrailsMediaSchema.PictureSchema.Roll, picture.getRoll());

            _db.update(TwoTrailsMediaSchema.PictureSchema.TableName, cvs,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?", new String[]{ picture.getCN() });

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertPictureData");
        }
    }
    //endregion

    //region Delete
    public boolean deleteMedia(TtMedia media) {
        boolean success = false;
        String cn = media.getCN();

        try {
            success = _db.delete(TwoTrailsMediaSchema.Media.TableName,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?",
                    new String[] { cn }) > 0;

            if (success) {
                switch (media.getMediaType()) {
                    case Picture:
                        removePictureData((TtImage)media);
                        break;
                    case Video:
                        break;
                }
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:deleteMedia");
        }

        return success;
    }
    
    private void removePictureData(TtImage picture) {
        try {
            _db.delete(TwoTrailsMediaSchema.PictureSchema.TableName,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?",
                    new String[] { picture.getCN() });
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:removePictureData");
        }
    }
    //endregion


    //region DbTools
    public void clean() {

    }
    //endregion

    //region Select Statements
    private static final String SelectPictures = String.format("select %s.%s, %s from %s left join %s on %s.%s = %s.%s",
            TwoTrailsMediaSchema.Media.TableName,
            TwoTrailsMediaSchema.Media.SelectItems,
            TwoTrailsMediaSchema.PictureSchema.SelectItemsNoCN,
            TwoTrailsMediaSchema.Media.TableName,
            TwoTrailsMediaSchema.PictureSchema.TableName,
            TwoTrailsMediaSchema.Media.TableName,
            TwoTrailsMediaSchema.SharedSchema.CN,
            TwoTrailsMediaSchema.PictureSchema.TableName,
            TwoTrailsMediaSchema.SharedSchema.CN);

    //private static final String SelectPoints = String.format("");
    //endregion
}