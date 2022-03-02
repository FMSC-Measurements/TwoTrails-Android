package com.usda.fmsc.twotrails.objects.map;


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoogleMapsLineGraphic implements ILineGraphic {
    private final GoogleMap map;

    private Extent lineBounds;
    private Polyline polyline;

    private boolean visible = true;


    public GoogleMapsLineGraphic(GoogleMap map) {
        this.map = map;
    }

    @Override
    public void build(Position point1, Position point2, LineGraphicOptions graphicOptions) {
        PolylineOptions plo = new PolylineOptions();
        plo.width(graphicOptions.getLineWidth());
        plo.color(graphicOptions.getLineColor());
        plo.pattern(getLinePattern(graphicOptions.getLineStyle()));

        if (polyline != null) {
            polyline.remove();
        }

        polyline = map.addPolyline(plo);

        update(point1, point2);
    }

    @Override
    public void update(Position point1, Position point2) {
        ArrayList<LatLng> latLngs = new ArrayList<>();
        LatLngBounds.Builder llBuilder = new LatLngBounds.Builder();

        latLngs.add(new LatLng(point1.getLatitudeSignedDecimal(), point1.getLongitudeSignedDecimal()));
        latLngs.add(new LatLng(point2.getLatitudeSignedDecimal(), point2.getLongitudeSignedDecimal()));

        llBuilder.include(latLngs.get(0));
        llBuilder.include(latLngs.get(1));

        LatLngBounds bounds = llBuilder.build();
        lineBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                bounds.southwest.latitude, bounds.southwest.longitude);

        polyline.setPoints(latLngs);
    }


    @Override
    public Extent getExtents() {
        return lineBounds;
    }

    @Override
    public Position getPosition() {
        return lineBounds.getCenter();
    }


    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        polyline.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }


    private List<PatternItem> getLinePattern(LineGraphicOptions.LineStyle style) {
        List<PatternItem> pattern = null;

        switch (style) {
            case Dashed: pattern = Arrays.asList(new Dash(30), new Gap(10)); break;
            case Dotted: pattern = Arrays.asList(new Dot(), new Gap(10));
        }

        return  pattern;
    }
}
