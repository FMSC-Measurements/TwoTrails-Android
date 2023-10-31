package com.usda.fmsc.twotrails.objects.map;

import androidx.annotation.ColorInt;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.gnss.GeoTools;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment.MarkerData;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoogleMapsPolygonGraphic implements IPolygonGraphic, IMarkerDataGraphic {
    private final GoogleMap map;

    private TtPolygon polygon;
    private PolygonDrawOptions drawOptions;
    private PolygonGraphicOptions graphicOptions;

    private ArrayList<Marker> _AllAdjPts, _AllUnadjPts, _AdjBndPts, _UnadjBndPts, _AdjNavPts, _UnadjNavPts, _WayPts, _AdjMiscPts, _UnadjMiscPts;
    private Polygon _AdjBndCB, _UnadjBndCB;
    private Polyline _AdjBnd, _UnadjBnd, _AdjNav, _UnadjNav;
    private Extent polyBounds;
    private HashMap<String, MarkerData> _MarkerData;


    public GoogleMapsPolygonGraphic(GoogleMap map) {
        this.map = map;
    }

    @Override
    public void build(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions) {
        this.polygon = polygon;
        this.drawOptions = drawOptions;
        this.graphicOptions = graphicOptions;

        _MarkerData = new HashMap<>();

        _AllAdjPts = new ArrayList<>();
        _AllUnadjPts = new ArrayList<>();
        _AdjBndPts = new ArrayList<>();
        _AdjNavPts = new ArrayList<>();
        _UnadjBndPts = new ArrayList<>();
        _UnadjNavPts = new ArrayList<>();
        _WayPts = new ArrayList<>();
        _AdjMiscPts = new ArrayList<>();
        _UnadjMiscPts = new ArrayList<>();

        PolygonOptions adjBndPO = new PolygonOptions();
        PolygonOptions unadjBndPO = new PolygonOptions();

        PolylineOptions adjBndPLO = new PolylineOptions();
        PolylineOptions unadjBndPLO = new PolylineOptions();
        PolylineOptions adjNavPLO = new PolylineOptions();
        PolylineOptions unadjNavPLO = new PolylineOptions();

        LatLngBounds.Builder llBuilder = new LatLngBounds.Builder();

        MarkerOptions adj, unadj;
        Marker adjmk, unadjmk;
        LatLng adjLL, unadjLL;
        boolean isWay, isMisc;

        TtMetadata metadata;


        for (TtPoint point : points) {
            isWay = isMisc = false;

            metadata = meta.get(point.getMetadataCN());
            
            adj = TtUtils.GMap.createMarkerOptions(point, true, meta);
            unadj = TtUtils.GMap.createMarkerOptions(point, false, meta);

            adjmk = map.addMarker(adj.visible(false).icon(BitmapDescriptorFactory.defaultMarker(
                    AndroidUtils.Convert.rgbToHsvHue(graphicOptions.getAdjPtsColor())
            )));

            unadjmk = map.addMarker(unadj.visible(false).icon(BitmapDescriptorFactory.defaultMarker(
                    AndroidUtils.Convert.rgbToHsvHue(
                    point.getOp() == OpType.WayPoint ? graphicOptions.getWayPtsColor() : graphicOptions.getUnAdjPtsColor())
            )));

            _MarkerData.put(adjmk.getId(), new MarkerData(point, metadata, true));
            _MarkerData.put(unadjmk.getId(), new MarkerData(point, metadata, false));

            _AllAdjPts.add(adjmk);
            _AllUnadjPts.add(unadjmk);

            adjLL = adjmk.getPosition();
            unadjLL = unadjmk.getPosition();

            if (point.isBndPoint()) {
                _AdjBndPts.add(adjmk);
                _UnadjBndPts.add(unadjmk);
                
                adjBndPO.add(adjLL);
                unadjBndPO.add(unadjLL);
                
                adjBndPLO.add(adjLL);
                unadjBndPLO.add(unadjLL);
            }

            if (point.isNavPoint()) {
                _AdjNavPts.add(adjmk);
                _UnadjNavPts.add(unadjmk);
                
                adjNavPLO.add(adjLL);
                unadjNavPLO.add(unadjLL);
            }

            if (point.getOp() == OpType.WayPoint) {
                _WayPts.add(unadjmk);
                isWay = true;
            }

            if (point.getOp() == OpType.SideShot && !point.isOnBnd()) {
                _AdjMiscPts.add(adjmk);
                _UnadjMiscPts.add(unadjmk);
                
                isMisc = true;
            }
            
            adjmk.setVisible(drawOptions.isVisible() && (
                    drawOptions.isAdjBndPts() ||
                    drawOptions.isAdjNavPts() ||
                    (isMisc && drawOptions.isAdjMiscPts())
            ));
            
            unadjmk.setVisible(drawOptions.isVisible() && (
                    drawOptions.isUnadjBndPts() ||
                    drawOptions.isUnadjNavPts() ||
                    (isMisc && drawOptions.isUnadjMiscPts()) ||
                    (isWay && drawOptions.isWayPts())
            ));

            llBuilder.include(adjLL);
        }

        if (points.size() > 0) {
            LatLngBounds bounds = llBuilder.build();
            polyBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                    bounds.southwest.latitude, bounds.southwest.longitude) ;
        } else {
            polyBounds = null;
        }

        if (_AdjBndPts.size() > 0) {
            adjBndPLO.color(graphicOptions.getAdjBndColor()).width(graphicOptions.getAdjWidth()).zIndex(4);
            unadjBndPLO.color(graphicOptions.getUnAdjBndColor()).width(graphicOptions.getUnAdjWidth()).zIndex(3);

            _AdjBnd = map.addPolyline(adjBndPLO.visible(false));
            _UnadjBnd = map.addPolyline(unadjBndPLO.visible(false));


            adjBndPO.strokeColor(graphicOptions.getAdjBndColor()).strokeWidth(graphicOptions.getAdjWidth()).zIndex(6);
            unadjBndPO.strokeColor(graphicOptions.getUnAdjBndColor()).strokeWidth(graphicOptions.getUnAdjWidth()).zIndex(5);

            _AdjBndCB = map.addPolygon(adjBndPO.visible(false));
            _UnadjBndCB = map.addPolygon(unadjBndPO.visible(false));


            if (drawOptions.isVisible()) {
                if (drawOptions.isAdjBnd()) {
                    if (drawOptions.isAdjBndClose()) {
                        _AdjBndCB.setVisible(true);
                    } else {
                        _AdjBnd.setVisible(true);
                    }
                }

                if (drawOptions.isUnadjBnd()) {
                    if (drawOptions.isUnadjBndClose()) {
                        _UnadjBndCB.setVisible(true);
                    } else {
                        _UnadjBnd.setVisible(true);
                    }
                }
            }
        }
        
        if (_AdjNavPts.size() > 0) {
            adjNavPLO.color(graphicOptions.getAdjNavColor()).width(graphicOptions.getAdjWidth()).zIndex(2);
            unadjNavPLO.color(graphicOptions.getUnAdjNavColor()).width(graphicOptions.getUnAdjWidth()).zIndex(1);

            _AdjNav = map.addPolyline(adjNavPLO.visible(false));
            _UnadjNav = map.addPolyline(unadjNavPLO.visible(false));
            
            if (drawOptions.isVisible()) {
                if (drawOptions.isAdjNav()) {
                    _AdjNav.setVisible(true);
                }
                
                if (drawOptions.isUnadjNav()) {
                    _UnadjNav.setVisible(true);
                }
            }
        }
    }


    private boolean isInList(ArrayList<Marker> markers, Marker marker) {
        String id = marker.getId();
        for (Marker m : markers) {
            if (m.getId().equals(id))
                return true;
        }
        return false;
    }

    //region Setters
    @Override
    public void setVisible(boolean visible) {
        drawOptions.setVisible(visible);

        if (visible) {
            for (Marker m : _AllAdjPts) {
                m.setVisible(drawOptions.isAdjBndPts() || drawOptions.isAdjNavPts());
            }
            
            for (Marker m : _AllUnadjPts) {
                m.setVisible(drawOptions.isUnadjBndPts() || drawOptions.isUnadjNavPts());
            }
            
            if (drawOptions.isAdjMiscPts()) {
                for (Marker m : _AdjMiscPts) {
                    m.setVisible(true);
                }
            }
            
            if (drawOptions.isWayPts()) {
                for (Marker m : _WayPts) {
                    m.setVisible(true);
                }
            }

            if (_AdjBnd != null) {
                if (drawOptions.isAdjBnd()) {
                    if (drawOptions.isAdjBndClose()) {
                        _AdjBndCB.setVisible(true);
                    } else {
                        _AdjBnd.setVisible(true);
                    }
                }

                if (drawOptions.isUnadjBnd()) {
                    if (drawOptions.isUnadjBndClose()) {
                        _UnadjBndCB.setVisible(true);
                    } else {
                        _UnadjBnd.setVisible(true);
                    }
                }
            }

            if (_AdjNav != null) {
                if (drawOptions.isAdjNav()) {
                    _AdjNav.setVisible(true);
                }

                if (drawOptions.isUnadjNav()) {
                    _UnadjNav.setVisible(true);
                }
            }
        } else {
            for (Marker m : _AllAdjPts) {
                m.setVisible(false);
            }

            for (Marker m : _AllUnadjPts) {
                m.setVisible(false);
            }

            if (_AdjBnd != null) {
                _AdjBndCB.setVisible(false);
                _AdjBnd.setVisible(false);
                _UnadjBndCB.setVisible(false);
                _UnadjBnd.setVisible(false);
            }

            if (_AdjNav != null) {
                _AdjNav.setVisible(false);
                _UnadjNav.setVisible(false);
            }
        }
    }


    @Override
    public void setAdjBndVisible(boolean visible) {
        drawOptions.setAdjBnd(visible);

        if (_AdjBnd != null) {
            visible &= drawOptions.isVisible();

            if (drawOptions.isAdjBndClose()) {
                _AdjBndCB.setVisible(visible);
            } else {
                _AdjBnd.setVisible(visible);
            }
        }
    }

    @Override
    public void setAdjBndPtsVisible(boolean visible) {
        drawOptions.setAdjBndPts(visible);

        visible &= drawOptions.isVisible();
        for (Marker m : _AdjBndPts) {
            m.setVisible(visible || (drawOptions.isAdjNavPts() && isInList(_AdjNavPts, m)) || (drawOptions.isAdjMiscPts() && isInList(_AdjMiscPts, m)));
        }
    }


    @Override
    public void setUnadjBndVisible(boolean visible) {
        drawOptions.setUnadjBnd(visible);

        if (_UnadjBnd != null) {
            visible &= drawOptions.isVisible();

            if (drawOptions.isAdjBndClose()) {
                _UnadjBndCB.setVisible(visible);
            } else {
                _UnadjBnd.setVisible(visible);
            }
        }
    }

    @Override
    public void setUnadjBndPtsVisible(boolean visible) {
        drawOptions.setUnadjBndPts(visible);

        visible &= drawOptions.isVisible();
        for (Marker m : _UnadjBndPts) {
            m.setVisible(visible || (drawOptions.isUnadjNavPts() && isInList(_UnadjNavPts, m) || drawOptions.isUnadjMiscPts() && isInList(_UnadjMiscPts, m)));
        }
    }


    @Override
    public void setAdjNavVisible(boolean visible) {
        drawOptions.setAdjNav(visible);

        if (_AdjNav != null) {
            _AdjNav.setVisible(visible && drawOptions.isVisible());
        }
    }

    @Override
    public void setAdjNavPtsVisible(boolean visible) {
        drawOptions.setAdjNavPts(visible);

        visible &= drawOptions.isVisible();
        for (Marker m : _AdjNavPts) {
            m.setVisible(visible || (drawOptions.isAdjBndPts() && isInList(_AdjBndPts, m)));
        }
    }


    @Override
    public void setUnadjNavVisible(boolean visible) {
        drawOptions.setUnadjNav(visible);

        if (_UnadjNav != null) {
            _UnadjNav.setVisible(visible && drawOptions.isVisible());
        }
    }

    @Override
    public void setUnadjNavPtsVisible(boolean visible) {
        drawOptions.setUnadjNavPts(visible);

        visible &= drawOptions.isVisible();
        for (Marker m : _UnadjNavPts) {
            m.setVisible(visible || (drawOptions.isUnadjBndPts() && isInList(_UnadjBndPts, m)));
        }
    }


    @Override
    public void setAdjMiscPtsVisible(boolean visible) {
        drawOptions.setAdjMiscPts(visible);

        visible &= drawOptions.isVisible();
        for (Marker m : _AdjMiscPts) {
            m.setVisible(visible || (drawOptions.isAdjBndPts() && isInList(_AdjBndPts, m)) || (drawOptions.isAdjNavPts() && isInList(_AdjNavPts, m)));
        }
    }

    @Override
    public void setUnadjMiscPtsVisible(boolean visible) {
        drawOptions.setUnadjMiscPts(visible);

        visible &= drawOptions.isVisible();
        for (Marker m : _UnadjMiscPts) {
            m.setVisible(visible || (drawOptions.isUnadjBndPts() && isInList(_UnadjBndPts, m)) || (drawOptions.isUnadjNavPts() && isInList(_UnadjNavPts, m)));
        }
    }


    @Override
    public void setWayPtsVisible(boolean visible) {
        drawOptions.setWayPts(visible);

        visible &= drawOptions.isVisible();
        for (Marker m : _WayPts) {
            m.setVisible(visible);
        }
    }


    @Override
    public void setAdjBndClose(boolean close) {
        if (drawOptions.isAdjBndClose() != close) {
            boolean isvis = drawOptions.isAdjBnd();

            setAdjBndVisible(false);

            drawOptions.setAdjBndClose(close);

            if (drawOptions.isVisible() && isvis) {
                setAdjBndVisible(true);
            }
        }
    }

    @Override
    public void setUnadjBndClose(boolean close) {
        if (drawOptions.isUnadjBndClose() != close) {
            boolean isvis = drawOptions.isAdjBnd();

            setUnadjBndVisible(false);

            drawOptions.setUnadjBndClose(close);

            if (drawOptions.isVisible() && isvis) {
                setUnadjBndVisible(true);
            }
        }
    }



    @Override
    public void setAdjBndColor(@ColorInt int color) {
        graphicOptions.setAdjBndColor(color);
        _AdjBnd.setColor(color);
        _AdjBndCB.setStrokeColor(color);
    }

    @Override
    public void setUnAdjBndColor(@ColorInt int color) {
        graphicOptions.setUnAdjBndColor(color);
        _UnadjBnd.setColor(color);
        _UnadjBndCB.setStrokeColor(color);
    }


    @Override
    public void setAdjNavColor(@ColorInt int color) {
        graphicOptions.setAdjNavColor(color);
        _AdjNav.setColor(color);
    }

    @Override
    public void setUnAdjNavColor(@ColorInt int color) {
        graphicOptions.setUnAdjNavColor(color);
        _UnadjNav.setColor(color);
    }


    @Override
    public void setAdjPtsColor(@ColorInt int color) {
        graphicOptions.setAdjPtsColor(color);

        for (Marker marker : _AllAdjPts) {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(AndroidUtils.Convert.rgbToHsvHue(color)));
        }
    }

    @Override
    public void setUnAdjPtsColor(@ColorInt int color) {
        graphicOptions.setUnAdjPtsColor(color);

        for (Marker marker : _AllUnadjPts) {
            if (!_WayPts.contains(marker)) {
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(AndroidUtils.Convert.rgbToHsvHue(color)));
            }
        }
    }


    @Override
    public void setWayPtsColor(@ColorInt int color) {
        graphicOptions.setWayPtsColor(color);

        for (Marker marker : _WayPts) {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(AndroidUtils.Convert.rgbToHsvHue(color)));
        }
    }
    //endregion


    //region Getters
    @Override
    public HashMap<String, MarkerData> getMarkerData() {
        return _MarkerData;
    }

    @Override
    public TtPolygon getPolygon() {
        return polygon;
    }

    @Override
    public PolygonDrawOptions getDrawOptions() {
        return drawOptions;
    }

    @Override
    public Extent getExtents() {
        return polyBounds;
    }

    @Override
    public Position getPosition() {
        return GeoTools.getMidPoint(polyBounds);
    }

    @Override
    public PolygonGraphicOptions getGraphicOptions() {
        return graphicOptions;
    }

    @Override
    public boolean isVisible() {
        return drawOptions.isVisible();
    }

    @Override
    public boolean isAdjBndVisible() {
        return drawOptions.isAdjBnd();
    }

    @Override
    public boolean isAdjBndPtsVisible() {
        return drawOptions.isAdjBndPts();
    }

    @Override
    public boolean isUnadjBndVisible() {
        return drawOptions.isUnadjBnd();
    }

    @Override
    public boolean isUnadjBndPtsVisible() {
        return drawOptions.isUnadjBndPts();
    }

    @Override
    public boolean isAdjNavVisible() {
        return drawOptions.isAdjNav();
    }

    @Override
    public boolean isAdjNavPtsVisible() {
        return drawOptions.isAdjNavPts();
    }

    @Override
    public boolean isUnadjNavVisible() {
        return drawOptions.isUnadjNav();
    }

    @Override
    public boolean isUnadjNavPtsVisible() {
        return drawOptions.isUnadjNavPts();
    }

    @Override
    public boolean isAdjMiscPtsVisible() {
        return drawOptions.isAdjMiscPts();
    }

    @Override
    public boolean isUnadjMiscPtsVisible() {
        return drawOptions.isUnadjMiscPts();
    }

    @Override
    public boolean isWayPtsVisible() {
        return drawOptions.isWayPts();
    }

    @Override
    public boolean isAdjBndClose() {
        return drawOptions.isAdjBndClose();
    }

    @Override
    public boolean isUnadjBndClose() {
        return drawOptions.isUnadjBndClose();
    }



    @Override
    public int getAdjBndColor() {
        return graphicOptions.getAdjBndColor();
    }

    @Override
    public int getUnAdjBndColor() {
        return graphicOptions.getUnAdjBndColor();
    }

    @Override
    public int getAdjNavColor() {
        return graphicOptions.getAdjNavColor();
    }

    @Override
    public int getUnAdjNavColor() {
        return graphicOptions.getUnAdjNavColor();
    }

    @Override
    public int getAdjPtsColor() {
        return graphicOptions.getAdjPtsColor();
    }

    @Override
    public int getUnAdjPtsColor() {
        return graphicOptions.getUnAdjPtsColor();
    }

    @Override
    public int getWayPtsColor() {
        return graphicOptions.getWayPtsColor();
    }
    //endregion
}
