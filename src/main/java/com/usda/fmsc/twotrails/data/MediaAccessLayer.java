package com.usda.fmsc.twotrails.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;

import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@SuppressWarnings({"UnusedReturnValue", "unused", "WeakerAccess"})
public class MediaAccessLayer extends IDataLayer implements BitmapManager.IBitmapProvider {
    public TtVersion getVersion() {
        if (_MalVersion == null)
            _MalVersion = new TtVersion(getTtDbVersion(), getUserVersion());

        return _MalVersion;
    }


    private TtVersion _MalVersion;

    //region Constructors / Open / Close
    public MediaAccessLayer(TwoTrailsApp context, SQLiteDatabase db, String fileName) {
        super(context, db, fileName);
    }

    protected MediaAccessLayer(TwoTrailsApp context, SQLiteDatabase db, String fileName, boolean create) {
        super(context, db, fileName, create);
    }
    //endregion

    //region Create DB

    @Override
    protected void onCreateDB(SQLiteDatabase db) {
        _MalVersion = TwoTrailsMediaSchema.SchemaVersion;

        createMediaTable(db);
        createImageTable(db);
        createDataTable(db);
        createInfoTable(db);

        ContentValues cvs = new ContentValues();
        cvs.put(TwoTrailsMediaSchema.Info.TtMediaDbSchemaVersion, _MalVersion.toString());
        db.insert(TwoTrailsMediaSchema.Info.TableName, null, cvs);
    }


    private void createMediaTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsMediaSchema.Media.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "MediaAccessLayer:CreateMediaTable");
            throw ex;
        }
    }

    private void createImageTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsMediaSchema.Images.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "MediaAccessLayer:CreateImageTable");
            throw ex;
        }
    }

    private void createDataTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsMediaSchema.Data.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "MediaAccessLayer:CreateDataTable");
            throw ex;
        }
    }

    private void createInfoTable(SQLiteDatabase db) {
        try
        {
            db.execSQL(TwoTrailsMediaSchema.Info.CreateTable);
        }
        catch (Exception ex)
        {
            logError(ex.getMessage(), "MediaAccessLayer:CreateInfoTable");
            throw ex;
        }
    }

    @Override
    protected void onOpenDB(SQLiteDatabase db) {
        if (_MalVersion == null) _MalVersion = new TtVersion(getTtDbVersion(), getUserVersion());
    }
    //endregion


    //region Get
    public String getTtDbVersion() {
        String retString = StringEx.Empty;
        String getQuery = String.format("select %s from %s",
                TwoTrailsMediaSchema.Info.TtMediaDbSchemaVersion, TwoTrailsMediaSchema.Info.TableName);

        try (Cursor c = getDB().rawQuery(getQuery, null)) {
            if (c.moveToFirst()) {
                if (!c.isNull(0))
                    retString = c.getString(0);
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "MediaAccessLayer:getTtDbVersion");
            throw new RuntimeException("MAL:getTtDbVersion");
        }

        return retString;
    }
    
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

    public ArrayList<TtImage> getImages() {
        return getImages(null, 0);
    }

    private ArrayList<TtImage> getImages(String where, int limit) {
        ArrayList<TtImage> pictures = new ArrayList<>();

        try {
            String query = String.format(Locale.getDefault(), "%s where %s = %d%s order by datetime(%s) asc %s",
                    SelectImages,
                    TwoTrailsMediaSchema.Media.MediaType,
                    MediaType.Picture.getValue(),
                    StringEx.isEmpty(where) ? StringEx.Empty : String.format(" and %s", where),
                    TwoTrailsMediaSchema.Media.CreationTime,
                    limit > 0 ? " limit " + limit : StringEx.Empty);

            Cursor c = getDB().rawQuery(query, null);

            TtImage pic;

            if (c.moveToFirst()) {
                do {
                    if(!c.isNull(8)) {
                        pic = TtUtils.Media.getPictureByType(PictureType.parse(c.getInt(8)));
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
                        pic.setFileName(c.getString(4));

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
            logError(ex.getMessage(), "DAL:getImages");
            throw new RuntimeException("DAL:getImages");
        }

        return pictures;
    }

    private byte[] getImageByteData(String cn) {
        byte[] data;

        String query = String.format("select %s from %s where %s = '%s'",
                TwoTrailsMediaSchema.Data.BinaryData,
                TwoTrailsMediaSchema.Data.TableName,
                TwoTrailsMediaSchema.SharedSchema.CN,
                cn);

        Cursor c = getDB().rawQuery(query, null);

        if (c.moveToFirst()) {
            data = getLargeBlob(
                    TwoTrailsMediaSchema.Data.TableName,
                    TwoTrailsMediaSchema.Data.BinaryData,
                    String.format("%s = '%s'",TwoTrailsMediaSchema.SharedSchema.CN, cn));
        } else {
            throw new RuntimeException("No Image Data Found");
        }

        c.close();

        return data;
    }
    //endregion

    //region Insert
    private boolean insertMedia(TtMedia media) {
        switch (media.getMediaType()) {
            case Picture: return insertImage((TtImage) media);
            case Video:
            default: return false;
        }
    }

    private boolean insertBaseMedia(TtMedia media) {
        boolean result = false;

        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.SharedSchema.CN, media.getCN());
            cvs.put(TwoTrailsMediaSchema.Media.PointCN, media.getPointCN());
            cvs.put(TwoTrailsMediaSchema.Media.MediaType, media.getMediaType().getValue());
            cvs.put(TwoTrailsMediaSchema.Media.Name, media.getName());
            cvs.put(TwoTrailsMediaSchema.Media.FileName, media.getFileName());
            cvs.put(TwoTrailsMediaSchema.Media.Comment, media.getComment());
            cvs.put(TwoTrailsMediaSchema.Media.CreationTime, dtf.print(media.getTimeCreated()));
            cvs.put(TwoTrailsMediaSchema.Media.IsExternal, media.isExternal());

            result = getDB().insert(TwoTrailsMediaSchema.Media.TableName, null, cvs) > 0;
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertBaseMedia");
        }

        return result;
    }

    public boolean insertImage(TtImage image) {
        boolean success = false;

        if (image == null)
            return false;

        try {
            getDB().beginTransaction();

            if (insertImageData(image)) {
                ContentValues cvs = new ContentValues();

                cvs.put(TwoTrailsMediaSchema.SharedSchema.CN, image.getCN());
                cvs.put(TwoTrailsMediaSchema.Images.PicType, image.getPictureType().getValue());
                cvs.put(TwoTrailsMediaSchema.Images.Azimuth, image.getAzimuth());
                cvs.put(TwoTrailsMediaSchema.Images.Pitch, image.getPitch());
                cvs.put(TwoTrailsMediaSchema.Images.Roll, image.getRoll());

                getDB().insert(TwoTrailsMediaSchema.Images.TableName, null, cvs);

                success = insertBaseMedia(image);

                if(success)
                    getDB().setTransactionSuccessful();
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertImage");
            success = false;
        } finally {
            getDB().endTransaction();
        }

        return success;
    }


    private boolean insertImageData(TtImage image) {
        boolean success = false;

        try {
            Uri path = getTtAppContext().getMediaUri(image);
            ContentResolver cr = getTtAppContext().getContentResolver();
            InputStream fs = cr.openInputStream(path);

            insertImageData(image, FileUtils.readInputStream(fs));

            success = true;
        } catch (IOException e) {
            logError(e.getMessage(), "DAL:insertImage(image)");
        }

        return success;
    }

    private boolean insertImageData(TtImage image, byte[] data) {
        boolean success = false;
        try {
            ContentValues cvs = new ContentValues();

            String ext = image.getFileName().substring(image.getFileName().lastIndexOf('.') + 1);

            cvs.put(TwoTrailsMediaSchema.SharedSchema.CN, image.getCN());
            cvs.put(TwoTrailsMediaSchema.Data.BinaryData, data);
            cvs.put(TwoTrailsMediaSchema.Data.DataType, ext);

            success = getDB().insert(TwoTrailsMediaSchema.Data.TableName, null, cvs) > 0;

            if (success)
                image.setIsExternal(false);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:insertImage(image,byte[])");
        }

        return success;
    }
    //endregion

    //region Update
    public boolean updateMedia(TtMedia media) {
        boolean success;

        if (media == null)
            return false;

        try {
            getDB().beginTransaction();

            success = updateBaseMedia(media);

            if(success)
                getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateMedia");
            success = false;
        } finally {
            getDB().endTransaction();
        }

        return success;
    }

    private boolean updateBaseMedia(TtMedia media) {
        try {
            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.Media.PointCN, media.getPointCN());
            cvs.put(TwoTrailsMediaSchema.Media.Name, media.getName());
            cvs.put(TwoTrailsMediaSchema.Media.FileName, media.getFileName());
            cvs.put(TwoTrailsMediaSchema.Media.Comment, media.getComment());
            cvs.put(TwoTrailsMediaSchema.Media.IsExternal, media.isExternal());

            getDB().update(TwoTrailsMediaSchema.Media.TableName, cvs,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?", new String[] { media.getCN() });
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateBaseMedia");
            return false;
        }

        return true;
    }

    private boolean updateImage(TtImage image) {
        boolean success = false;

        try {
            getDB().beginTransaction();

            ContentValues cvs = new ContentValues();

            cvs.put(TwoTrailsMediaSchema.Images.Azimuth, image.getAzimuth());
            cvs.put(TwoTrailsMediaSchema.Images.Pitch, image.getPitch());
            cvs.put(TwoTrailsMediaSchema.Images.Roll, image.getRoll());

            if (getDB().update(TwoTrailsMediaSchema.Images.TableName, cvs,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?", new String[]{ image.getCN() }) > 0) {

                if (image.isExternal()) {
                    success = updateImageData(image);
                }

                if (success)
                    success = updateBaseMedia(image);
            }

            if(success)
                getDB().setTransactionSuccessful();
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateImage");
            success = false;
        } finally {
            getDB().endTransaction();
        }

        return success;
    }

    private boolean updateImageData(TtImage image) {
        boolean success = false;

        try {
            InputStream fs = getTtAppContext().getContentResolver().openInputStream(getTtAppContext().getMediaUri(image));

            updateImageData(image, FileUtils.readInputStream(fs));

            success = true;
        } catch (IOException e) {
            logError(e.getMessage(), "DAL:updateImage(image)");
        }

        return success;
    }

    private boolean updateImageData(TtImage image, byte[] data) {
        boolean success = false;
        try {
            ContentValues cvs = new ContentValues();

            String ext = image.getFileName().substring(image.getFileName().lastIndexOf('.') + 1);

            cvs.put(TwoTrailsMediaSchema.Data.BinaryData, data);
            cvs.put(TwoTrailsMediaSchema.Data.DataType, ext);

            success = getDB().update(TwoTrailsMediaSchema.Data.TableName, cvs, TwoTrailsMediaSchema.SharedSchema.CN + "=?", new String[]{ image.getCN() }) > 0;

            if (success)
                image.setIsExternal(false);
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:updateImage(image,byte[])");
        }

        return success;
    }
    //endregion

    //region Delete
    public boolean deleteMedia(TtMedia media) {
        boolean success = false;
        String cn = media.getCN();

        try {
            success = getDB().delete(TwoTrailsMediaSchema.Media.TableName,
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
            logError(ex.getMessage(), "DAL:deleteMedia");
        }

        return success;
    }
    
    private void removeImageData(TtImage image) {
        try {
            getDB().delete(TwoTrailsMediaSchema.Images.TableName,
                    TwoTrailsMediaSchema.SharedSchema.CN + "=?",
                    new String[] { image.getCN() });

            if (!image.isExternal()) {
                getDB().delete(TwoTrailsMediaSchema.Data.TableName,
                        TwoTrailsMediaSchema.SharedSchema.CN + "=?",
                        new String[] { image.getCN()} );
            }
        } catch (Exception ex) {
            logError(ex.getMessage(), "DAL:removeImageData");
        }
    }
    //endregion


    //region DbTools

    //TODO implement error checking
    @Override
    public boolean hasErrors() {
        return false;
    }

    public void clean() {

    }

    public boolean hasExternalImages() {
        return getItemsCount(TwoTrailsMediaSchema.Media.TableName,
                String.format(Locale.getDefault(), "%s = %d and %s = 1",
                    TwoTrailsMediaSchema.Media.MediaType,
                    MediaType.Picture.getValue(),
                    TwoTrailsMediaSchema.Media.IsExternal
                )
            ) > 0;
    }

//    public void internalizeImages(IMalListener listener) {
//        List<TtImage> images;
//        List<TtImage> internalizedImages = new ArrayList<>();
//        List<TtImage> failedImages = new ArrayList<>();
//
//        images = getImages(String.format("%s = 1", TwoTrailsMediaSchema.Media.IsExternal), 0);
//
//        try {
//            for (TtImage img : images) {
//                if (img.externalFileExists(getTtAppContext())) {
//                    File file = new File(img.getPath());
//
//                    FileInputStream is = new FileInputStream(file);
//                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//
//                    int nRead;
//                    byte[] data = new byte[(int)file.length()];
//
//                    while ((nRead = is.read(data, 0, data.length)) != -1) {
//                        buffer.write(data, 0, nRead);
//                    }
//
//                    buffer.flush();
//
//
//                    img.setIsExternal(false);
//                    img.setPath(file.getName());
//
//                    if (insertImageData(img, buffer.toByteArray())) {
//                        updateMedia(img);
//                        internalizedImages.add(img);
//                    } else {
//                        failedImages.add(img);
//                    }
//                } else {
//                    failedImages.add(img);
//                }
//            }
//
//            if (listener != null) {
//                listener.internalizeImagesCompleted(internalizedImages, failedImages);
//            }
//        } catch (Exception e) {
//            logError(e.getMessage(), "MAL:internalizeImages", e.getStackTrace());
//            if (listener != null)
//                listener.internalizeImagesFailed(internalizedImages, failedImages, e.getMessage());
//        }
//    }

//    public void loadImage(final TtImage image, final IMalListener listener) {
//        new Thread(() -> {
//            try {
//                byte[] data = null;
//
//                if (image.isExternal()) {
//                    if (image.externalFileExists(getTtAppContext())) {
//
//                        data = FileUtils.readInputStream(getTtAppContext().getContentResolver().openInputStream(image.getPath()));
//
////                ImageLoader.getInstance().loadImage("file://" + image.getPath(), new SimpleImageLoadingListener() {
////                    @Override
////                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
////                        if (listener != null)
////                            listener.imageLoaded(image, view, loadedImage);
////                    }
////
////                    @Override
////                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
////                        if (listener != null)
////                            listener.loadingFailed(image, view, failReason.getCause().toString());
////                    }
////                });
//                    } else {
//                        if (listener != null) {
//                            listener.loadingFailed(image, null, "File does not exist");
//                            return;
//                        }
//                    }
//                } else {
//                    String query = String.format("select %s from %s where %s = '%s'",
//                            TwoTrailsMediaSchema.Data.BinaryData,
//                            TwoTrailsMediaSchema.Data.TableName,
//                            TwoTrailsMediaSchema.SharedSchema.CN,
//                            image.getCN());
//
//                    Cursor c = getDB().rawQuery(query, null);
//
//                    if (c.moveToFirst()) {
//                            data = getLargeBlob(
//                                TwoTrailsMediaSchema.Data.TableName,
//                                TwoTrailsMediaSchema.Data.BinaryData,
//                                String.format("%s = '%s'",TwoTrailsMediaSchema.SharedSchema.CN, image.getCN()));
//                    }
//
//                    c.close();
//                }
//
//                if (data != null) {
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//
//                    if (listener != null) {
//                        if (bitmap != null) {
//                            listener.imageLoaded(image, null, bitmap);
//                        } else {
//                            listener.loadingFailed(image, null, "Bitmap is NULL");
//                        }
//                    }
//                } else {
//                    listener.loadingFailed(image, null, "No image data");
//                }
//            } catch (Exception ex) {
//                logError(ex.getMessage(), "DAL:loadImage");
//            }
//        }).start();
//    }


    public void upgrade(Upgrade upgrade) {
        int dbVersion = getVersion().toIntVersion();

        if (dbVersion < upgrade.Version.toIntVersion()) {
            SQLiteDatabase db = getDB();
            for (String sql : upgrade.SQL_Statements) {
                db.execSQL(sql);
            }
        }

        getDB().setTransactionSuccessful();
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

    //region BitmapProvider

    @Override
    public String getProviderId() {
        return getTtAppContext().getPackageName() + getFileName();
    }

    @Override
    public Bitmap getBitmap(String key) {
        byte[] data = getImageByteData(key);
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    @Override
    public boolean hasBitmap(String key) {
        return this.getItemsCount(TwoTrailsMediaSchema.Media.TableName,
                String.format("%s == '%s' and %s == 0 and %s == 0",
                        TwoTrailsMediaSchema.SharedSchema.CN, key,
                        TwoTrailsMediaSchema.Media.IsExternal,
                        TwoTrailsMediaSchema.Media.MediaType
        )) > 0;
    }
    //endregion

    public interface IMalListener {
        void imageLoaded(TtImage image, View view, Bitmap bitmap);
        void loadingFailed(TtImage image, View view, String reason);

        void internalizeImagesCompleted(List<TtImage> imagesInternalized, List<TtImage> failedImages);
        void internalizeImagesFailed(List<TtImage> imagesInternalized, List<TtImage> failedImages, String failedReason);
    }

    public static class SimpleMalListener implements IMalListener {
        @Override
        public void imageLoaded(TtImage image, View view, Bitmap bitmap) {

        }

        @Override
        public void loadingFailed(TtImage image, View view, String reason) {

        }

        @Override
        public void internalizeImagesCompleted(List<TtImage> imagesInternalized, List<TtImage> failedImages) {

        }

        @Override
        public void internalizeImagesFailed(List<TtImage> imagesInternalized, List<TtImage> failedImages, String failedReason) {

        }
    }
}