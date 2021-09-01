package com.usda.fmsc.twotrails;

import androidx.annotation.ColorInt;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicOptions;

import java.util.HashMap;

public class MapSettings extends Settings {
    private final HashMap<String, PolygonDrawOptions> _PolyDrawOptions = new HashMap<>();
    private final PolygonDrawOptions _MasterPolyDrawOptions = new PolygonDrawOptions();

    private HashMap<String, PolygonGraphicOptions> _PolyGraphicOptions = new HashMap<>();
    private PolygonGraphicOptions _MasterPolyGraphicOptions;

    public MapSettings(TwoTrailsApp context) {
        super(context);
    }

    private @ColorInt Integer adjBnd;
    public @ColorInt int getDefaultAdjBndColor() {
        if (adjBnd == null)
            adjBnd = AndroidUtils.UI.getColor(getContext(), R.color.map_adj_bnd);
        return adjBnd;
    }

    private @ColorInt Integer adjNav;
    public @ColorInt int getDefaultAdjNavColor() {
        if (adjNav == null)
            adjNav = AndroidUtils.UI.getColor(getContext(), R.color.map_adj_nav);
        return adjNav;
    }

    private @ColorInt Integer unadjBnd;
    public @ColorInt int getDefaultUnAdjBndColor() {
        if (unadjBnd == null)
            unadjBnd = AndroidUtils.UI.getColor(getContext(), R.color.map_unadj_bnd);
        return unadjBnd;
    }

    private @ColorInt Integer unadjNav;
    public @ColorInt int getDefaultUnAdjNavColor() {
        if (unadjNav == null)
            unadjNav = AndroidUtils.UI.getColor(getContext(), R.color.map_unadj_nav);
        return unadjNav;
    }

    private @ColorInt Integer adjpts;
    public @ColorInt int getDefaultAdjPtsColor() {
        if (adjpts == null)
            adjpts = AndroidUtils.UI.getColor(getContext(), R.color.map_adj_pts);
        return adjpts;
    }

    private @ColorInt Integer unadjpts;
    public @ColorInt int getDefaultUnAdjPtsColor() {
        if (unadjpts == null)
            unadjpts = AndroidUtils.UI.getColor(getContext(), R.color.map_unadj_pts);
        return unadjpts;
    }

    private @ColorInt Integer waypts;
    public @ColorInt int getDefaultWayPtsColor() {
        if (waypts == null)
            waypts = AndroidUtils.UI.getColor(getContext(), R.color.map_way_pts);
        return waypts;
    }

    public void reset() {
        for (PolygonGraphicOptions pgo : _PolyGraphicOptions.values()) {
            pgo.removeListener(updateListener);
        }

        _PolyDrawOptions.clear();

        _PolyGraphicOptions = getContext().getDAL().getPolygonGraphicOptionsMap();

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
                    getContext().getDeviceSettings().getMapAdjLineWidth(),
                    getContext().getDeviceSettings().getMapUnAdjLineWidth()
            );

            if (getContext().hasDAL()) {
                getContext().getDAL().insertPolygonGraphicOption(_MasterPolyGraphicOptions);
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

            if (getContext().hasDAL()) {
                getContext().getDAL().insertPolygonGraphicOption(pgo);
            }

            pgo.addListener(updateListener);

            return pgo;
        }
    }

    private final PolygonGraphicOptions.Listener updateListener = (pgo, code, value) -> {
        if (getContext().hasDAL() && _PolyGraphicOptions.containsKey(pgo.getCN())) {
            getContext().getDAL().updatePolygonGraphicOption(pgo);
        }
    };
}