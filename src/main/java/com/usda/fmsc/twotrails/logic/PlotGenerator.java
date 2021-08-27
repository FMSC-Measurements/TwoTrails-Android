package com.usda.fmsc.twotrails.logic;

import android.os.AsyncTask;

import com.usda.fmsc.android.utilities.TaskRunner;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.points.WayPoint;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.utilities.PolygonCalculator;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.usda.fmsc.geospatial.utm.UTMCoords;

//public class PlotGenerator extends AsyncTask<PlotGenerator.PlotParams, Void, TtPolygon> {
public class PlotGenerator extends TaskRunner.Task<PlotGenerator.PlotParams, TtPolygon> {
    
    private final PlotGenListener listener;
    private final TwoTrailsApp app;
    private Exception ex;
    private Map<String, TtMetadata> metadata;

    public PlotGenerator(TwoTrailsApp app) {
        this(app, null, null);
    }

    public PlotGenerator(TwoTrailsApp app, Map<String, TtMetadata> metadata) {
        this (app, metadata, null);
    }

    public PlotGenerator(TwoTrailsApp app, PlotGenListener listener) {
        this(app, null, listener);
    }

    public PlotGenerator(TwoTrailsApp app, Map<String, TtMetadata> metadata, PlotGenListener listener) {
        this.app = app;
        this.metadata = metadata;
        this.listener = listener;
    }

    @Override
    protected TtPolygon onBackgroundWork(PlotParams params) {
        DataAccessLayer dal = app.getDAL();

        if (metadata == null)
            metadata = dal.getMetadataMap();

        TtPolygon poly = new TtPolygon();
        poly.setName(params.PolyName);
        poly.setPointStartIndex(dal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName) * 1000 + 1010);
        poly.setIncrementBy(1);
        poly.setAccuracy(Consts.Default_Point_Accuracy);

        //start progress

        try {
            double gridX = params.GridX;
            double gridY = params.GridY;

            if (params.DistUom != Dist.Meters) {
                gridX = TtUtils.Convert.distance(gridX, Dist.Meters, params.DistUom);
                gridY = TtUtils.Convert.distance(gridY, Dist.Meters, params.DistUom);
            }

            double angle;

            if (params.Angle == null) {
                Random random = new Random();
                angle = random.nextInt(45) - 45;
            } else {
                angle = params.Angle;
            }

            poly.setDescription(String.format(Locale.getDefault(), "Angle: %f, GridX(Mt): %f, GridY(Mt): %f", angle, gridX, gridY));

            //convert to radians
            angle = TtUtils.Convert.degreesToRadians(angle * -1);

            List<PointD> points = new ArrayList<>();

            boolean allMatchMeta = TtUtils.Points.allPointsHaveSameMetadata(params.Points);

            if (isCancelled())
                return null;

            for (TtPoint point : params.Points) {
                if (point.isOnBnd()) {
                    if (allMatchMeta)
                        points.add(new PointD(point.getAdjX(), point.getAdjY()));
                    else {
                        UTMCoords coords = TtUtils.Points.forcePointZone(point, params.Metadata.getZone(), metadata.get(point.getMetadataCN()).getZone(), true);
                        points.add(new PointD(coords.getX(), coords.getY()));
                    }
                }
            }

            if (isCancelled())
                return null;

            double startX, startY;

            if (allMatchMeta) {
                startX = params.StartPoint.getAdjX();
                startY = params.StartPoint.getAdjY();
            } else {
                UTMCoords coords = TtUtils.Points.forcePointZone(params.StartPoint, params.Metadata.getZone(), metadata.get(params.StartPoint.getMetadataCN()).getZone(), true);
                startX = coords.getX();
                startY = coords.getY();
            }

            if (isCancelled())
                return null;

            PolygonCalculator polyCalc = new PolygonCalculator(points);

            PolygonCalculator.Boundaries boundaries = polyCalc.getPointBoundaries();

            if (isCancelled())
                return null;

            if (boundaries != null) {
                PointD farCorner = TtUtils.Math.getFarthestCorner(startX, startY, boundaries.TopLeft.Y, boundaries.BottomRight.Y, boundaries.TopLeft.X, boundaries.BottomRight.X);

                double dist = TtUtils.Math.distance(startX, startY, farCorner.X, farCorner.Y);

                int ptAmtY = (int) (Math.floor(dist / gridY) + 1);
                int ptAmtX = (int) (Math.floor(dist / gridX) + 1);

                double farLeft, farRight, farTop, farBottom;

                farLeft = startX - (ptAmtX * gridX);
                farRight = startX + (ptAmtX * gridX);
                farTop = startY + (ptAmtY * gridY);
                farBottom = startY - (ptAmtY * gridY);

                double i = farLeft;
                double j = farTop;

                if (isCancelled())
                    return null;

                List<PointD> dblPts = new ArrayList<>();

                PointD _point;

                List<PointD> rec = new ArrayList<>();
                rec.add(new PointD(boundaries.TopLeft));
                rec.add(new PointD(boundaries.BottomRight.X, boundaries.TopLeft.Y));
                rec.add(new PointD(boundaries.BottomRight));
                rec.add(new PointD(boundaries.TopLeft.X, boundaries.BottomRight.Y));

                PolygonCalculator recPolyCalc = new PolygonCalculator(rec);

                while (i <= farRight) {
                    while (j >= farBottom) {
                        //add the rotated point

                        _point = TtUtils.Math.RotatePoint(i, j, angle, startX, startY);

                        if (params.Inside) {
                            //add if point inside the polygon
                            if (polyCalc.pointInPolygon(_point.X, _point.Y))
                                dblPts.add(_point);
                        } else {
                            //add if point inside the polygon box
                            if (recPolyCalc.pointInPolygon(_point.X, _point.Y))
                                dblPts.add(_point);
                        }

                        j -= gridY;

                        if (isCancelled())
                            return null;
                    }
                    i += gridX;
                    j = farTop;
                }

                if (params.Sample) {
                    int maxPoints = params.SamplePercent ? (int)((params.SampleValue / 100.0) * dblPts.size()) : params.SampleValue;

                    Random rand = new Random();

                    while (maxPoints < dblPts.size()) {
                        dblPts.remove(rand.nextInt(dblPts.size() - 1));
                    }
                }

                List<TtPoint> _NewPoints = new ArrayList<>();
                WayPoint way;
                WayPoint lastWay = null;

                //add points to polygon
                for (int a = 0; a < dblPts.size(); a++)
                {
                    way = new WayPoint();

                    PointD dp = dblPts.get(a);
                    way.setUnAdjX(dp.X);
                    way.setUnAdjY(dp.Y);
                    way.setUnAdjZ(0);
                    way.setPolyCN(poly.getCN());
                    way.setPolyName(poly.getName());
                    way.setOnBnd(false);
                    way.setIndex(a);
                    way.setComment("Generated Point");
                    way.setGroupCN(Consts.EmptyGuid);
                    way.setGroupName(Consts.Defaults.MainGroupName);

                    if (lastWay == null)
                        way.setPID(PointNamer.nameFirstPoint(poly));
                    else
                        way.setPID(PointNamer.namePoint(lastWay, poly));

                    way.setMetadataCN(params.Metadata.getCN());

                    _NewPoints.add(way);

                    lastWay = way;

                    if (isCancelled())
                        return null;
                }

                if (isCancelled())
                    return null;

                dal.insertPolygon(poly);
                dal.insertPoints(_NewPoints);

                return poly;
            } else {
                this.ex = new Exception("Invalid Boundaries");
            }
        } catch (Exception ex) {
            this.ex = ex;
        }

        return null;
    }

    @Override
    protected void onComplete(TtPolygon polygon) {
        if (listener != null) {
            if (isCancelled()) {
                listener.onCanceled();
            } else if (polygon == null)
                listener.onError(ex);
            else
                listener.onGenerated(polygon);
        }
    }

    @Override
    protected void onError(Exception exception) {
        if (listener != null)
            listener.onError(exception);
    }


    public static class PlotParams {
        private final String PolyName;
        private final TtPoint StartPoint;
        private final List<TtPoint> Points;
        private final int GridX, GridY;
        private final Integer Angle;
        private Integer SampleValue;
        private final TtMetadata Metadata;
        private final boolean Inside, Sample;
        private boolean SamplePercent;
        private final Dist DistUom;

        public PlotParams(String polyName, TtPoint startPoint, List<TtPoint> points, Dist distUom,
                          int gridX, int gridY, Integer angle, TtMetadata metadata, boolean inside, boolean sample) {
            PolyName = polyName;
            StartPoint = startPoint;
            Points = points;
            DistUom = distUom;
            GridX = gridX;
            GridY = gridY;
            Angle = angle;
            Metadata = metadata;
            Inside = inside;
            Sample = sample;
        }

        public void setSampleValue(int sampleValue) {
            SampleValue = sampleValue;
        }

        public void setSamplePercent(boolean samplePercent) {
            SamplePercent = samplePercent;
        }
    }

    public interface PlotGenListener {
        void onCanceled();
        void onGenerated(TtPolygon polygon);
        void onError(Exception ex);
    }
}
