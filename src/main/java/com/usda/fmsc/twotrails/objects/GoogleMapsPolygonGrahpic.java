package com.usda.fmsc.twotrails.objects;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment.MarkerData;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoogleMapsPolygonGrahpic implements IPolygonGraphic {
    private TtPolygon polygon;
    private PolygonDrawOptions drawOptions;

    private GoogleMap map;

    private ArrayList<Marker> _AllAdjPts, _AllUnadjPts, _AdjBndPts, _UnadjBndPts, _AdjNavPts, _UnadjNavPts, _WayPts, _AdjMiscPts, _UnadjMiscPts;
    private Polygon _AdjBndCB, _UnadjBndCB;
    private Polyline _AdjBnd, _UnadjBnd, _AdjNav, _UnadjNav;
    private Extent polyBounds;
    private HashMap<String, MarkerData> _MarkerData;


    public GoogleMapsPolygonGrahpic(GoogleMap map) {
        this.map = map;
    }

    @Override
    public void build(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions) {
        this.polygon = polygon;
        this.drawOptions = drawOptions;

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

            adjmk = map.addMarker(adj.visible(false));
            unadjmk = map.addMarker(unadj.visible(false));

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

            if (point.getOp() == Units.OpType.WayPoint) {
                _WayPts.add(unadjmk);
                isWay = true;
            }

            if (point.getOp() == Units.OpType.SideShot && !point.isOnBnd()) {
                _AdjMiscPts.add(adjmk);
                _UnadjMiscPts.add(unadjmk);
                
                isMisc = true;
            }
            
            adjmk.setVisible(drawOptions.Visible && (
                    drawOptions.AdjBndPts ||
                    drawOptions.AdjNavPts ||
                    (isMisc && drawOptions.AdjMiscPts)
            ));
            
            unadjmk.setVisible(drawOptions.Visible && (
                    drawOptions.UnadjBndPts ||
                    drawOptions.UnadjNavPts ||
                    (isMisc && drawOptions.UnadjMiscPts) ||
                    (isWay && drawOptions.WayPts)
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
            adjBndPLO.color(graphicOptions.AdjBndColor).width(graphicOptions.AdjWidth).zIndex(4);
            unadjBndPLO.color(graphicOptions.UnAdjBndColor).width(graphicOptions.UnAdjWidth).zIndex(3);

            _AdjBnd = map.addPolyline(adjBndPLO.visible(false));
            _UnadjBnd = map.addPolyline(unadjBndPLO.visible(false));


            adjBndPO.strokeColor(graphicOptions.AdjBndColor).strokeWidth(graphicOptions.AdjWidth).zIndex(6);
            unadjBndPO.strokeColor(graphicOptions.UnAdjBndColor).strokeWidth(graphicOptions.UnAdjWidth).zIndex(5);

            _AdjBndCB = map.addPolygon(adjBndPO.visible(false));
            _UnadjBndCB = map.addPolygon(unadjBndPO.visible(false));


            if (drawOptions.Visible) {
                if (drawOptions.AdjBnd) {
                    if (drawOptions.AdjBndClose) {
                        _AdjBndCB.setVisible(true);
                    } else {
                        _AdjBnd.setVisible(true);
                    }
                }

                if (drawOptions.UnadjBnd) {
                    if (drawOptions.UnadjBndClose) {
                        _UnadjBndCB.setVisible(true);
                    } else {
                        _UnadjBnd.setVisible(true);
                    }
                }
            }
        }
        
        if (_AdjNavPts.size() > 0) {
            adjNavPLO.color(graphicOptions.AdjNavColor).width(graphicOptions.AdjWidth).zIndex(2);
            unadjNavPLO.color(graphicOptions.UnAdjNavColor).width(graphicOptions.UnAdjWidth).zIndex(1);

            _AdjNav = map.addPolyline(adjNavPLO.visible(false));
            _UnadjNav = map.addPolyline(unadjNavPLO.visible(false));
            
            if (drawOptions.Visible) {
                if (drawOptions.AdjNav) {
                    _AdjNav.setVisible(true);
                }
                
                if (drawOptions.UnadjNav) {
                    _UnadjNav.setVisible(true);
                }
            }
        }
    }


    //region Setters
    @Override
    public void setVisible(boolean visible) {
        drawOptions.Visible = visible;

        if (visible) {
            for (Marker m : _AllAdjPts) {
                m.setVisible(drawOptions.AdjBndPts || drawOptions.AdjNavPts);
            }
            
            for (Marker m : _AllUnadjPts) {
                m.setVisible(drawOptions.UnadjBndPts || drawOptions.UnadjNavPts);
            }
            
            if (drawOptions.AdjMiscPts) {
                for (Marker m : _AdjMiscPts) {
                    m.setVisible(true);
                }
            }
            
            if (drawOptions.WayPts) {
                for (Marker m : _WayPts) {
                    m.setVisible(true);
                }
            }

            if (_AdjBnd != null) {
                if (drawOptions.AdjBnd) {
                    if (drawOptions.AdjBndClose) {
                        _AdjBndCB.setVisible(true);
                    } else {
                        _AdjBnd.setVisible(true);
                    }
                }

                if (drawOptions.UnadjBnd) {
                    if (drawOptions.UnadjBndClose) {
                        _UnadjBndCB.setVisible(true);
                    } else {
                        _UnadjBnd.setVisible(true);
                    }
                }
            }

            if (_AdjNav != null) {
                if (drawOptions.AdjNav) {
                    _AdjNav.setVisible(true);
                }

                if (drawOptions.UnadjNav) {
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
        drawOptions.AdjBnd = visible;

        if (_AdjBnd != null) {
            visible &= drawOptions.Visible;

            if (drawOptions.AdjBndClose) {
                _AdjBndCB.setVisible(visible);
            } else {
                _AdjBnd.setVisible(visible);
            }
        }
    }

    @Override
    public void setAdjBndPtsVisible(boolean visible) {
        drawOptions.AdjBndPts = visible;

        visible &= drawOptions.Visible;
        for (Marker m : _AdjBndPts) {
            m.setVisible(visible);
        }
    }


    @Override
    public void setUnadjBndVisible(boolean visible) {
        drawOptions.UnadjBnd = visible;

        if (_UnadjBnd != null) {
            visible &= drawOptions.Visible;

            if (drawOptions.AdjBndClose) {
                _UnadjBndCB.setVisible(visible);
            } else {
                _UnadjBnd.setVisible(visible);
            }
        }
    }

    @Override
    public void setUnadjBndPtsVisible(boolean visible) {
        drawOptions.UnadjBndPts = visible;

        visible &= drawOptions.Visible;
        for (Marker m : _UnadjBndPts) {
            m.setVisible(visible);
        }
    }


    @Override
    public void setAdjNavVisible(boolean visible) {
        drawOptions.AdjNav = visible;

        if (_AdjNav != null) {
            _AdjNav.setVisible(visible && drawOptions.Visible);
        }
    }

    @Override
    public void setAdjNavPtsVisible(boolean visible) {
        drawOptions.AdjNavPts = visible;

        visible &= drawOptions.Visible;
        for (Marker m : _AdjNavPts) {
            m.setVisible(visible);
        }
    }


    @Override
    public void setUnadjNavVisible(boolean visible) {
        drawOptions.UnadjNav = visible;

        if (_UnadjNav != null) {
            _UnadjNav.setVisible(visible && drawOptions.Visible);
        }
    }

    @Override
    public void setUnadjNavPtsVisible(boolean visible) {
        drawOptions.UnadjNavPts = visible;

        visible &= drawOptions.Visible;
        for (Marker m : _UnadjNavPts) {
            m.setVisible(visible);
        }
    }


    @Override
    public void setAdjMiscPtsVisible(boolean visible) {
        drawOptions.AdjMiscPts = visible;

        visible &= drawOptions.Visible;
        for (Marker m : _AdjMiscPts) {
            m.setVisible(visible && drawOptions.Visible);
        }
    }

    @Override
    public void setUnadjMiscPtsVisible(boolean visible) {
        drawOptions.UnadjMiscPts = visible;

        visible &= drawOptions.Visible;
        for (Marker m : _UnadjMiscPts) {
            m.setVisible(visible);
        }
    }


    @Override
    public void setWayPtsVisible(boolean visible) {
        drawOptions.WayPts = visible;

        for (Marker m : _WayPts) {
            m.setVisible(visible);
        }
    }


    @Override
    public void setAdjBndClose(boolean close) {
        if (drawOptions.AdjBndClose != close) {
            boolean isvis = drawOptions.AdjBnd;

            setAdjBndVisible(false);

            drawOptions.AdjBndClose = close;

            if (drawOptions.Visible && isvis) {
                setAdjBndVisible(true);
            }
        }
    }

    @Override
    public void setUnadjBndClose(boolean close) {
        if (drawOptions.UnadjBndClose != close) {
            boolean isvis = drawOptions.AdjBnd;

            setUnadjBndVisible(false);

            drawOptions.UnadjBndClose = close;

            if (drawOptions.Visible && isvis) {
                setUnadjBndVisible(true);
            }
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
    public boolean isVisible() {
        return drawOptions.Visible;
    }

    @Override
    public boolean isAdjBndVisible() {
        return drawOptions.AdjBnd;
    }

    @Override
    public boolean isAdjBndPtsVisible() {
        return drawOptions.AdjBndPts;
    }

    @Override
    public boolean isUnadjBndVisible() {
        return drawOptions.UnadjBnd;
    }

    @Override
    public boolean isUnadjBndPtsVisible() {
        return drawOptions.UnadjBndPts;
    }

    @Override
    public boolean isAdjNavVisible() {
        return drawOptions.AdjNav;
    }

    @Override
    public boolean isAdjNavPtsVisible() {
        return drawOptions.AdjNavPts;
    }

    @Override
    public boolean isUnadjNavVisible() {
        return drawOptions.UnadjNav;
    }

    @Override
    public boolean isUnadjNavPtsVisible() {
        return drawOptions.UnadjNavPts;
    }

    @Override
    public boolean isAdjMiscPtsVisible() {
        return drawOptions.AdjMiscPts;
    }

    @Override
    public boolean isUnadjMiscPtsVisible() {
        return drawOptions.UnadjMiscPts;
    }

    @Override
    public boolean isWayPtsVisible() {
        return drawOptions.WayPts;
    }

    @Override
    public boolean isAdjBndClose() {
        return drawOptions.AdjBndClose;
    }

    @Override
    public boolean isUnadjBndClose() {
        return drawOptions.UnadjBndClose;
    }
    //endregion

}
