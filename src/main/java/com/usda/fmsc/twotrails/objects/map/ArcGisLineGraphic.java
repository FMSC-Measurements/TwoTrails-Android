package com.usda.fmsc.twotrails.objects.map;


import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.LineSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;

import java.util.Arrays;

public class ArcGisLineGraphic implements ILineGraphic {
   private final MapView map;
   private Extent polyBounds;

   private GraphicsOverlay _LineLayer;
   private Graphic _LineGraphic;
   private SimpleLineSymbol _LineOutline;

   private boolean visible = true;


   public ArcGisLineGraphic(MapView mapView) {
      this.map = mapView;
   }

   @Override
   public void build(Position point1, Position point2, LineGraphicOptions graphicOptions) {
      _LineLayer = new GraphicsOverlay();

      int drawSize = (int)(graphicOptions.getLineWidth() / 2);

      _LineOutline = new SimpleLineSymbol(getLineSymbolStyle(graphicOptions.getLineStyle()), graphicOptions.getLineColor(), drawSize);

      update(point1, point2);

      map.getGraphicsOverlays().add(_LineLayer);
   }

   @Override
   public void update(Position point1, Position point2) {
      Extent.Builder eBuilder = new Extent.Builder();
      PointCollection linePoints = new PointCollection(SpatialReferences.getWgs84());

      Point posLL = new Point(point1.getLongitudeSignedDecimal(), point1.getLatitudeSignedDecimal(), SpatialReferences.getWgs84());
      linePoints.add(posLL);

      posLL = new Point(point2.getLongitudeSignedDecimal(), point2.getLatitudeSignedDecimal(), SpatialReferences.getWgs84());
      linePoints.add(posLL);

      if (_LineGraphic != null) {
         _LineGraphic.setGeometry(new Polyline(linePoints));
      } else {
         _LineGraphic = new Graphic(new Polyline(linePoints), _LineOutline);
         _LineLayer.getGraphics().add(_LineGraphic);
      }

      eBuilder.include(point1);
      eBuilder.include(point2);
      polyBounds = eBuilder.build();
   }

   @Override
   public Extent getExtents() {
      return polyBounds;
   }

   @Override
   public Position getPosition() {
      return polyBounds.getCenter();
   }

   @Override
   public void setVisible(boolean visible) {
      this.visible = visible;
      _LineLayer.setVisible(visible);
   }

   @Override
   public boolean isVisible() {
      return this.visible;
   }


   private SimpleLineSymbol.Style getLineSymbolStyle(LineGraphicOptions.LineStyle style) {
      switch (style) {
         case Dashed: return SimpleLineSymbol.Style.DASH;
         case Dotted: return SimpleLineSymbol.Style.DOT;
         case Solid:
         default: return SimpleLineSymbol.Style.SOLID;
      }
   }
}
