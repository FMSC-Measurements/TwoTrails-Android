package com.usda.fmsc.twotrails.logic;

import android.os.AsyncTask;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.TwoTrailApp;
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
import java.util.Map;
import java.util.Random;

import com.usda.fmsc.geospatial.utm.UTMCoords;

public class PlotGenerator extends AsyncTask<PlotGenerator.PlotParams, Void, TtPolygon> {
    private PlotGenListener listener;
    private DataAccessLayer dal;
    private Exception ex;
    private Map<String, TtMetadata> metadata;

    public PlotGenerator(DataAccessLayer dal) {
        this(dal, null, null);
    }

    public PlotGenerator(DataAccessLayer dal, Map<String, TtMetadata> metadata) {
        this (dal, metadata, null);
    }

    public PlotGenerator(DataAccessLayer dal, PlotGenListener listener) {
        this(dal, null, listener);
    }

    public PlotGenerator(DataAccessLayer dal, Map<String, TtMetadata> metadata, PlotGenListener listener) {
        this.dal = dal;
        this.metadata = metadata;
        this.listener = listener;
    }


    @Override
    protected TtPolygon doInBackground(PlotParams... params) {
        PlotParams pp = params[0];

        if (metadata == null)
            metadata = dal.getMetadataMap();

        TtPolygon poly = new TtPolygon();
        poly.setName(pp.PolyName);
        poly.setPointStartIndex(TwoTrailApp.getContext().getDAL().getItemCount(TwoTrailsSchema.PolygonSchema.TableName) * 1000 + 1010);
        poly.setIncrementBy(1);
        poly.setAccuracy(Consts.Default_Point_Accuracy);

        //start progress

        try {
            double gridX = pp.GridX;
            double gridY = pp.GridY;

            if (pp.DistUom != Dist.Meters) {
                gridX = TtUtils.Convert.distance(gridX, Dist.Meters, pp.DistUom);
                gridY = TtUtils.Convert.distance(gridY, Dist.Meters, pp.DistUom);
            }

            int angle;

            if (pp.Angle == null) {
                Random random = new Random();
                angle = random.nextInt(45) - 45;
            } else {
                angle = pp.Angle;
            }

            poly.setDescription(String.format("Angle: %d, GridX(Mt): %f, GridY(Mt): %f", angle, gridX, gridY));

            List<PointD> points = new ArrayList<>();

            boolean allMatchMeta = TtUtils.Points.allPointsHaveSameMetadata(pp.Points);

            if (isCancelled())
                return null;

            for (TtPoint point : pp.Points) {
                if (point.isOnBnd()) {
                    if (allMatchMeta)
                        points.add(new PointD(point.getAdjX(), point.getAdjY()));
                    else {
                        UTMCoords coords = TtUtils.Points.forcePointZone(point, pp.Metadata.getZone(), metadata.get(point.getMetadataCN()).getZone(), true);
                        points.add(new PointD(coords.getX(), coords.getY()));
                    }
                }
            }

            if (isCancelled())
                return null;

            double startX, startY;

            if (allMatchMeta) {
                startX = pp.StartPoint.getAdjX();
                startY = pp.StartPoint.getAdjY();
            } else {
                UTMCoords coords = TtUtils.Points.forcePointZone(pp.StartPoint, pp.Metadata.getZone(), metadata.get(pp.StartPoint.getMetadataCN()).getZone(), true);
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

                        if (pp.Inside) {
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

                if (pp.Sample) {
                    int maxPoints = pp.SamplePercent ? (int)((pp.SampleValue / 100.0) * dblPts.size()) : pp.SampleValue;

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

                    way.setMetadataCN(pp.Metadata.getCN());

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
    protected void onPostExecute(TtPolygon polygon) {
        super.onPostExecute(polygon);

        if (listener != null) {
            if (polygon == null)
                listener.onError(ex);
            else
                listener.onGenerated(polygon);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        if (listener != null)
            listener.onCaneled();
    }

    public static class PlotParams {
        private String PolyName;
        private TtPoint StartPoint;
        private List<TtPoint> Points;
        private int GridX, GridY;
        private Integer Angle, SampleValue;
        private TtMetadata Metadata;
        private boolean Inside, Sample, SamplePercent;
        private Dist DistUom;

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
        void onCaneled();
        void onGenerated(TtPolygon polygon);
        void onError(Exception ex);
    }
}
