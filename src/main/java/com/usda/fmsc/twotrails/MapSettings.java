package com.usda.fmsc.twotrails;

import android.support.annotation.ColorInt;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicOptions;

import java.util.HashMap;

public class MapSettings {
    private HashMap<String, PolygonDrawOptions> _PolyDrawOptions = new HashMap<>();
    private PolygonDrawOptions _MasterPolyDrawOptions = new PolygonDrawOptions();

    private HashMap<String, PolygonGraphicOptions> _PolyGraphicOptions = new HashMap<>();
    private PolygonGraphicOptions _MasterPolyGraphicOptions;

    private TwoTrailApp _Context;

    public MapSettings(TwoTrailApp context) {
        _Context = context;
    }


        private @ColorInt Integer adjBnd;
        public @ColorInt int getDefaultAdjBndColor() {
            if (adjBnd == null)
                adjBnd = AndroidUtils.UI.getColor(_Context, R.color.map_adj_bnd);
            return adjBnd;
        }

        private @ColorInt Integer adjNav;
        public @ColorInt int getDefaultAdjNavColor() {
            if (adjNav == null)
                adjNav = AndroidUtils.UI.getColor(_Context, R.color.map_adj_nav);
            return adjNav;
        }

        private @ColorInt Integer unadjBnd;
        public @ColorInt int getDefaultUnAdjBndColor() {
            if (unadjBnd == null)
                unadjBnd = AndroidUtils.UI.getColor(_Context, R.color.map_unadj_bnd);
            return unadjBnd;
        }

        private @ColorInt Integer unadjNav;
        public @ColorInt int getDefaultUnAdjNavColor() {
            if (unadjNav == null)
                unadjNav = AndroidUtils.UI.getColor(_Context, R.color.map_unadj_nav);
            return unadjNav;
        }

        private @ColorInt Integer adjpts;
        public @ColorInt int getDefaultAdjPtsColor() {
            if (adjpts == null)
                adjpts = AndroidUtils.UI.getColor(_Context, R.color.map_adj_pts);
            return adjpts;
        }

        private @ColorInt Integer unadjpts;
        public @ColorInt int getDefaultUnAdjPtsColor() {
            if (unadjpts == null)
                unadjpts = AndroidUtils.UI.getColor(_Context, R.color.map_unadj_pts);
            return unadjpts;
        }

        private @ColorInt Integer waypts;
        public @ColorInt int getDefaultWayPtsColor() {
            if (waypts == null)
                waypts = AndroidUtils.UI.getColor(_Context, R.color.map_way_pts);
            return waypts;
        }

    public void reset() {
        for (PolygonGraphicOptions pgo : _PolyGraphicOptions.values()) {
            pgo.removeListener(updateListener);
        }

        _PolyDrawOptions.clear();

        _PolyGraphicOptions = _Context.getDAL().getPolygonGraphicOptionsMap();

        if (_PolyGraphicOptions.containsKey(Consts.EmptyGuid)) {
            _MasterPolyGraphicOptions = _PolyGraphicOptions.get(Consts.EmptyGuid);
        } else {
            _MasterPolyGraphicOptions = new PolygonGraphicOptions(
                    Consts.EmptyGuid,
                    getDefaultAdjBndColor(),
                    getDefaultUnAdjNavColor(),
                    getDefaultAdjNavColor(),
                    getDefaultUnAdjNavColor(),
                    getDefaultAdjPtsColor(),
                    getDefaultUnAdjPtsColor(),
                    getDefaultWayPtsColor(),
                    _Context.getDeviceSettings().getMapAdjLineWidth(),
                    _Context.getDeviceSettings().getMapUnAdjLineWidth()
            );

            if (_Context.hasDAL()) {
                _Context.getDAL().insertPolygonGraphicOption(_MasterPolyGraphicOptions);
            }

            _PolyGraphicOptions.put(Consts.EmptyGuid, _MasterPolyGraphicOptions);
        }

        for (PolygonGraphicOptions pgo : _PolyGraphicOptions.values()) {
            pgo.addListener(updateListener);
        }
    }


    public PolygonDrawOptions getMasterPolyDrawOptions() {
        return _MasterPolyDrawOptions;
    }

    public PolygonDrawOptions getPolyDrawOptions(String cn) {
        if (_PolyDrawOptions.containsKey(cn)) {
            return _PolyDrawOptions.get(cn);
        } else {
            PolygonDrawOptions pdo = new PolygonDrawOptions();
            _PolyDrawOptions.put(cn, pdo);
            return pdo;
        }
    }


    public PolygonGraphicOptions getMasterPolyGraphicOptions() {
        return _MasterPolyGraphicOptions;
    }

    public PolygonGraphicOptions getPolyGraphicOptions(final String cn) {
        if (_PolyGraphicOptions.containsKey(cn)) {
            return _PolyGraphicOptions.get(cn);
        } else {
            PolygonGraphicOptions pgo = new PolygonGraphicOptions(cn, _MasterPolyGraphicOptions);
            _PolyGraphicOptions.put(cn, pgo);

            if (_Context.hasDAL()) {
                _Context.getDAL().insertPolygonGraphicOption(pgo);
            }

            pgo.addListener(updateListener);

            return pgo;
        }
    }

    private PolygonGraphicOptions.Listener updateListener = new PolygonGraphicOptions.Listener() {
        @Override
        public void onOptionChanged(PolygonGraphicOptions pgo, PolygonGraphicOptions.GraphicCode code, @ColorInt int value) {
            if (_Context.hasDAL() && _PolyGraphicOptions.containsKey(pgo.getCN())) {
                _Context.getDAL().updatePolygonGraphicOption(pgo);
            }
        }
    };
}