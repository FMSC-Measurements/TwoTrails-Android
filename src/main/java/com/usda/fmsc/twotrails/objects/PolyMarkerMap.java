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
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PolyMarkerMap {
    private TtPolygon polygon;
    private PolyDrawOptions options;

    private Listener listener;

    private GoogleMap map;

    private ArrayList<Marker> _AllAdjPts, _AllUnadjPts, _AdjBndPts, _UnadjBndPts, _AdjNavPts, _UnadjNavPts, _WayPts, _AdjMiscPts, _UnadjMiscPts;
    private Polygon _AdjBndCB, _UnadjBndCB;
    private Polyline _AdjBnd, _UnadjBnd, _AdjNav, _UnadjNav;
    private LatLngBounds polyBounds;
    private HashMap<String, MarkerData> _MarkerData;


    public PolyMarkerMap(GoogleMap map, PolyDrawOptions options, List<TtPoint> points, TtPolygon polygon, HashMap<String, TtMetadata> meta) {
        this.map = map;
        this.options = options;
        this.polygon = polygon;

        init(points, meta);
    }

    private void init(List<TtPoint> points, HashMap<String, TtMetadata> meta) {
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
        boolean isWway, isMisc;

        for (TtPoint point : points) {
            isWway = isMisc = false;
            
            adj = TtUtils.GMap.createMarkerOptions(point, true, meta);
            unadj = TtUtils.GMap.createMarkerOptions(point, false, meta);

            adjmk = map.addMarker(adj.visible(false));
            unadjmk = map.addMarker(unadj.visible(false));

            _MarkerData.put(adjmk.getId(), new MarkerData(point, true));
            _MarkerData.put(unadjmk.getId(), new MarkerData(point, false));

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
                isWway = true;
            }

            if (point.getOp() == Units.OpType.SideShot && !point.isOnBnd()) {
                _AdjMiscPts.add(adjmk);
                _UnadjMiscPts.add(unadjmk);
                
                isMisc = true;
            }
            
            adjmk.setVisible(options.Visible && (
                    options.AdjBndPts ||
                    options.AdjNavPts ||
                    (isMisc && options.AdjMiscPts)
            ));
            
            unadjmk.setVisible(options.Visible && (
                    options.UnadjBndPts ||
                    options.UnadjNavPts ||
                    (isMisc && options.UnadjMiscPts) ||
                    (isWway && options.WayPts)
            ));

            llBuilder.include(adjLL);
        }

        if (points.size() > 0) {
            polyBounds = llBuilder.build();
        } else {
            polyBounds = null;
        }

        if (_AdjBndPts.size() > 0) {
            _AdjBnd = map.addPolyline(adjBndPLO.visible(false));
            _UnadjBnd = map.addPolyline(unadjBndPLO.visible(false));
            
            _AdjBndCB = map.addPolygon(adjBndPO.visible(false));
            _UnadjBndCB = map.addPolygon(unadjBndPO.visible(false));

            if (options.Visible) {
                if (options.AdjBnd) {
                    if (options.AdjBndClose) {
                        _AdjBndCB.setVisible(true);
                    } else {
                        _AdjBnd.setVisible(true);
                    }
                }

                if (options.UnadjBnd) {
                    if (options.UnadjBndClose) {
                        _UnadjBndCB.setVisible(true);
                    } else {
                        _UnadjBnd.setVisible(true);
                    }
                }
            }
        }
        
        if (_AdjNavPts.size() > 0) {
            _AdjNav = map.addPolyline(adjNavPLO.visible(false));
            _UnadjNav = map.addPolyline(unadjNavPLO.visible(false));
            
            if (options.Visible) {
                if (options.AdjNav) {
                    _AdjNav.setVisible(true);
                }
                
                if (options.UnadjNav) {
                    _UnadjNav.setVisible(true);
                }
            }
        }
    }


    public String getPolyName() {
        return polygon.getName();
    }

    public String getPolyCN() { return polygon.getCN();}

    public PolyDrawOptions getOptions() {
        return options;
    }

    public LatLngBounds getPolyBounds() {
        return polyBounds;
    }

    public HashMap<String, MarkerData> getMarkerData() {
        return _MarkerData;
    }


    public void setVisible(boolean visible) {
        options.Visible = visible;

        if (visible) {
            for (Marker m : _AllAdjPts) {
                m.setVisible(options.AdjBndPts || options.AdjNavPts);
            }
            
            for (Marker m : _AllUnadjPts) {
                m.setVisible(options.UnadjBndPts || options.UnadjNavPts);
            }
            
            if (options.AdjMiscPts) {
                for (Marker m : _AdjMiscPts) {
                    m.setVisible(true);
                }
            }
            
            if (options.WayPts) {
                for (Marker m : _WayPts) {
                    m.setVisible(true);
                }
            }

            if (_AdjBnd != null) {
                if (options.AdjBnd) {
                    if (options.AdjBndClose) {
                        _AdjBndCB.setVisible(true);
                    } else {
                        _AdjBnd.setVisible(true);
                    }
                }

                if (options.UnadjBnd) {
                    if (options.UnadjBndClose) {
                        _UnadjBndCB.setVisible(true);
                    } else {
                        _UnadjBnd.setVisible(true);
                    }
                }
            }

            if (_AdjNav != null) {
                if (options.AdjNav) {
                    _AdjNav.setVisible(true);
                }

                if (options.UnadjNav) {
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

    
    public void setAdjBndVisible(boolean visible) {
        options.AdjBnd = visible;

        if (_AdjBnd != null) {
            visible &= options.Visible;

            if (options.AdjBndClose) {
                _AdjBndCB.setVisible(visible);
            } else {
                _AdjBnd.setVisible(visible);
            }
        }
    }

    public void setAdjBndPtsVisible(boolean visible) {
        options.AdjBndPts = visible;

        visible &= options.Visible;
        for (Marker m : _AdjBndPts) {
            m.setVisible(visible);
        }
    }


    public void setUnadjBndVisible(boolean visible) {
        options.UnadjBnd = visible;

        if (_UnadjBnd != null) {
            visible &= options.Visible;

            if (options.AdjBndClose) {
                _UnadjBndCB.setVisible(visible);
            } else {
                _UnadjBnd.setVisible(visible);
            }
        }
    }

    public void setUnadjBndPtsVisible(boolean visible) {
        options.UnadjBndPts = visible;

        visible &= options.Visible;
        for (Marker m : _UnadjBndPts) {
            m.setVisible(visible);
        }
    }

    
    public void setAdjNavVisible(boolean visible) {
        options.AdjNav = visible;

        if (_AdjNav != null) {
            _AdjNav.setVisible(visible && options.Visible);
        }
    }
    
    public void setAdjNavPtsVisible(boolean visible) {
        options.AdjNavPts = visible;

        visible &= options.Visible;
        for (Marker m : _AdjNavPts) {
            m.setVisible(visible);
        }
    }


    public void setUnadjNavVisible(boolean visible) {
        options.UnadjNav = visible;

        if (_UnadjNav != null) {
            _UnadjNav.setVisible(visible && options.Visible);
        }
    }

    public void setUnadjNavPtsVisible(boolean visible) {
        options.UnadjNavPts = visible;

        visible &= options.Visible;
        for (Marker m : _UnadjNavPts) {
            m.setVisible(visible);
        }
    }


    public void setAdjMiscPtsVisible(boolean visible) {
        options.AdjMiscPts = visible;

        for (Marker m : _AdjMiscPts) {
            m.setVisible(visible && options.Visible);
        }
    }

    public void setUnadjMiscPtsVisible(boolean visible) {
        options.UnadjMiscPts = visible;

        visible &= options.Visible;
        for (Marker m : _UnadjMiscPts) {
            m.setVisible(visible);
        }
    }


    public void setWayPtsVisible(boolean visible) {
        options.WayPts = visible;

        for (Marker m : _WayPts) {
            m.setVisible(visible);
        }
    }


    public void setAdjBndClose(boolean close) {
        if (options.AdjBndClose != close) {
            boolean isvis = options.AdjBnd;

            setAdjBndVisible(false);

            options.AdjBndClose = close;

            if (options.Visible && isvis) {
                setAdjBndVisible(true);
            }
        }
    }

    public void setUnadjBndClose(boolean close) {
        if (options.UnadjBndClose != close) {
            boolean isvis = options.AdjBnd;

            setUnadjBndVisible(false);

            options.UnadjBndClose = close;

            if (options.Visible && isvis) {
                setUnadjBndVisible(true);
            }
        }
    }



    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void remvoeListener() {
        this.listener = null;
    }


    public void onOptionChange(String option, boolean value) {
        if (listener != null) {
            listener.onOptionChanged(option, value);
        }
    }


    public interface Listener {
        void onOptionChanged(String option, boolean value);
    }


    public class MarkerData {
        public TtPoint Point;
        public boolean Adjusted;

        public MarkerData(TtPoint point, boolean adjusted) {
            Point = point;
            Adjusted = adjusted;
        }
    }
}
