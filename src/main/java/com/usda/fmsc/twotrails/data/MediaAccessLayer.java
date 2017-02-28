package com.usda.fmsc.twotrails.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    public TtVersion getVersion() { return _Version; }


    private TtVersion _Version;
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
                _Version = new TtVersion(getTtDbVersion());
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
            _Version = TwoTrailsMediaSchema.SchemaVersion;

            CreateMediaTable();
            CreateImageTable();

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

    private void CreateImageTable() {
        try
        {
            _db.execSQL(TwoTrailsMediaSchema.Images.CreateTable);
        }
        catch (Exception ex)
        {
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateImageTable");
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
            TtUtils.TtReport.writeError(ex.getMessage(), "DataAccessLayer:CreateImageTable");
            throw ex;
        }
    }
    //endregion

    //region Get
    public ArrayList<TtImage> getImageByCN(String cn) {
        return getImages(
                String.format("%s.%s = '%s'",
                    TwoTrailsMediaSchema.Media.TableName,
                    TwoTrailsMediaSchema.SharedSchema.CN,
                    cn),
                0);
    }

    public ArrayList<TtImage> getImagesInPoint(String pointCN) {
        return getImages(
                String.format("%s = '%s'",
                        TwoTrailsMediaSchema.Media.PointCN,
                        pointCN),
                0);
    }

    private ArrayList<TtImage> getImages(String where, int limit) {
        ArrayList<TtImage> pictures = new ArrayList<>();

        try {
            String query = String.format("%s where %s = %d%s order by datetime(%s) asc %s",
                    SelectImages,
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
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:getImages");
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
                    result = insertImage((TtImage)media);
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

    private boolean insertImage(TtImage image) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.SharedSchema.CN, image.getCN());
            cvs.put(TwoTrailsMediaSchema.Images.PicType, image.getPictureType().getValue());
            cvs.put(TwoTrailsMediaSchema.Images.Azimuth, image.getAzimuth());
            cvs.put(TwoTrailsMediaSchema.Images.Pitch, image.getPitch());
            cvs.put(TwoTrailsMediaSchema.Images.Roll, image.getRoll());

            _db.insert(TwoTrailsMediaSchema.Images.TableName, null, cvs);

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertImage");
            return false;
        }

        return true;
    }

    public boolean insertImageData(TtImage image, Bitmap bitmap) {
        try {
            ContentValues cvs = new ContentValues();
            Bitmap.CompressFormat format;

            switch (image.getFilePath().substring(image.getFilePath().lastIndexOf('.'))) {
                case "jpg":
                case "jpeg":
                    format = Bitmap.CompressFormat.JPEG;
                    break;
                case "png":
                    format = Bitmap.CompressFormat.PNG;
                    break;
                default:
                    throw new RuntimeException("Invalid File Type");
            }

            ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
            bitmap.copyPixelsToBuffer(buffer);

            cvs.put(TwoTrailsMediaSchema.SharedSchema.CN, image.getCN());
            cvs.put(TwoTrailsMediaSchema.Data.BinaryData, buffer.array());
            cvs.put(TwoTrailsMediaSchema.Data.DataType, format.toString());

            _db.insert(TwoTrailsMediaSchema.Images.TableName, null, cvs);

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertImage");
            return false;
        }

        return true;
    }
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
                    updateImageData((TtImage)media);
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

    private void updateImageData(TtImage image) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.Images.Azimuth, image.getAzimuth());
            cvs.put(TwoTrailsMediaSchema.Images.Pitch, image.getPitch());
            cvs.put(TwoTrailsMediaSchema.Images.Roll, image.getRoll());

            _db.update(TwoTrailsMediaSchema.Images.TableName, cvs,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?", new String[]{ image.getCN() });

        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:insertImage");
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
                        removeImageData((TtImage)media);
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
    
    private void removeImageData(TtImage image) {
        try {
            _db.delete(TwoTrailsMediaSchema.Images.TableName,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?",
                    new String[] { image.getCN() });

            if (!image.isExternal()) {
                _db.delete(TwoTrailsMediaSchema.Data.TableName,
                        TwoTrailsMediaSchema.SharedSchema.CN + "=?",
                        new String[] { image.getCN()} );
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "DAL:removeImageData");
        }
    }
    //endregion


    //region DbTools
    public void clean() {

    }

    public boolean hasExternalImages() {
        return getItemsCount(TwoTrailsMediaSchema.Media.TableName,
                String.format("%s = %d and %s = %s",
                    TwoTrailsMediaSchema.Media.MediaType,
                    MediaType.Picture.getValue(),
                    TwoTrailsMediaSchema.Media.IsExternal,
                    "true"
                )
            ) > 0;
    }

    public void internalizeImages(IMalListener listener) {
        List<TtImage> images;
        List<TtImage> internalizedImages = new ArrayList<>();
        List<TtImage> failedImages = new ArrayList<>();

        images = getImages(
                String.format("%s where %s = %d and %s = %s",
                        SelectImages,
                        TwoTrailsMediaSchema.Media.MediaType,
                        MediaType.Picture.getValue(),
                        TwoTrailsMediaSchema.Media.IsExternal,
                        "true"
                ), 0);

        try {
            for (TtImage img : images) {
                if (img.externalFileExists()) {
                    File file = new File(img.getFilePath());

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);


                    img.setIsExternal(false);
                    img.setFilePath(file.getName());

                    insertImageData(img, bitmap);
                    updateMedia(img);

                    internalizedImages.add(img);
                } else {
                    failedImages.add(img);
                }
            }

            if (listener != null) {
                listener.internalizeImagesCompleted(internalizedImages, failedImages);
            }
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "MAL:internalizeImages", e.getStackTrace());
            if (listener != null)
                listener.internalizeImagesFailed(internalizedImages, failedImages, e.getMessage());
        }
    }

    public void loadImage(final TtImage image, final IMalListener listener) {
        if (image.isExternal()) {
            if (image.externalFileExists()) {
                ImageLoader.getInstance().loadImage("file://" + image.getFilePath(), new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (listener != null)
                            listener.imageLoaded(image, view, loadedImage);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        if (listener != null)
                            listener.loadingFailed(image, view, failReason.getCause().toString());
                    }
                });
            } else {
                if (listener != null)
                    listener.loadingFailed(image, null, "File does not exist");
            }
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String query = String.format("select from %s where %s = '%'",
                                TwoTrailsMediaSchema.Data.TableName,
                                TwoTrailsMediaSchema.SharedSchema.CN,
                                image.getCN());

                        Cursor c = _db.rawQuery(query, null);

                        if (c.moveToFirst()) {
                            do {
                                if (!c.isNull(0)) {
                                    byte[] data = c.getBlob(0);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                                    if (listener != null) {
                                        if (bitmap != null) {
                                            listener.imageLoaded(image, null, bitmap);
                                        } else {
                                            listener.loadingFailed(image, null, "Bitmap is NULL");
                                        }
                                    }
                                }
                                else
                                    throw new Exception("Image Binary Data is null");

                            } while (c.moveToNext());
                        }

                        c.close();
                    } catch (Exception ex) {
                        TtUtils.TtReport.writeError(ex.getMessage(), "DAL:loadImage");
                    }
                }
            }).start();
        }
    }

    //endregion

    //region Select Statements
    private static final String SelectImages = String.format("select %s.%s, %s from %s left join %s on %s.%s = %s.%s",
            TwoTrailsMediaSchema.Media.TableName,
            TwoTrailsMediaSchema.Media.SelectItems,
            TwoTrailsMediaSchema.Images.SelectItemsNoCN,
            TwoTrailsMediaSchema.Media.TableName,
            TwoTrailsMediaSchema.Images.TableName,
            TwoTrailsMediaSchema.Media.TableName,
            TwoTrailsMediaSchema.SharedSchema.CN,
            TwoTrailsMediaSchema.Images.TableName,
            TwoTrailsMediaSchema.SharedSchema.CN);

    //endregion

    public interface IMalListener {
        void imageLoaded(TtImage image, View view, Bitmap bitmap);
        void loadingFailed(TtImage image, View view, String reason);

        void internalizeImagesCompleted(List<TtImage> imagesInternalized, List<TtImage> externalImagesNotFound);
        void internalizeImagesFailed(List<TtImage> imagesInternalized, List<TtImage> externalImagesNotFound, String failedReason);
    }

    public static class SimpleMalListener implements IMalListener {
        @Override
        public void imageLoaded(TtImage image, View view, Bitmap bitmap) {

        }

        @Override
        public void loadingFailed(TtImage image, View view, String reason) {

        }

        @Override
        public void internalizeImagesCompleted(List<TtImage> imagesInternalized, List<TtImage> externalImagesNotFound) {

        }

        @Override
        public void internalizeImagesFailed(List<TtImage> imagesInternalized, List<TtImage> externalImagesNotFound, String failedReason) {

        }
    }
}